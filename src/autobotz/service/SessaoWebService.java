package autobotz.service;

import autobotz.AuthService;
import autobotz.AutorizacaoService;
import autobotz.AutorizacaoService.Permissao;
import autobotz.Usuario;
import autobotz.UsuarioDAO;
import autobotz.dao.SegurancaDAO;
import autobotz.dao.SegurancaRepositorio;
import autobotz.model.EventoSeguranca;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Sessão de servidor para o portal HTTP (D14).
 *
 * <p>A identidade de uma requisição HTTP vem exclusivamente de um token opaco emitido
 * aqui, depois de {@link AuthService} validar senha e (para perfis em
 * {@link AuthService#PERFIS_COM_MFA}) o código MFA. Esta classe NUNCA lê nem escreve
 * {@code SessaoUsuario}, que é a sessão única do desktop.
 *
 * <p>Regras: token aleatório de 256 bits; só o hash SHA-256 do token fica em memória;
 * expira por ociosidade e por duração máxima; logout invalida no servidor; o perfil é
 * relido do banco a cada requisição (rebaixar ou apagar um usuário vale na próxima
 * chamada). As sessões vivem na memória do processo: reiniciar o servidor encerra todas.
 *
 * <p>Mapa HTTP sugerido: {@link Acesso#NAO_AUTENTICADO} = 401, {@link Acesso#PROIBIDO} = 403,
 * {@link Acesso#PERMITIDO} = 200.
 */
public class SessaoWebService {
    public static final Duration OCIOSIDADE = Duration.ofMinutes(30);
    public static final Duration DURACAO_MAXIMA = Duration.ofHours(8);
    /** Cookie sugerido: HttpOnly; SameSite=Strict; Path=/ (e Secure quando houver HTTPS). */
    public static final String NOME_COOKIE = "ABM_SESSAO";

    private static final Pattern TOKEN_VALIDO = Pattern.compile("[A-Za-z0-9_-]{20,128}");

    public enum Acesso {
        PERMITIDO(200), NAO_AUTENTICADO(401), PROIBIDO(403);

        private final int httpStatus;

        Acesso(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int httpStatus() {
            return httpStatus;
        }
    }

    /** Decisão de acesso. {@code usuario} só vem preenchido em PERMITIDO e PROIBIDO. */
    public record Decisao(Acesso acesso, Usuario usuario) {
        public int httpStatus() {
            return acesso.httpStatus();
        }

        public boolean permitido() {
            return acesso == Acesso.PERMITIDO;
        }
    }

    /**
     * Resultado de login/MFA pela web. {@code token} só existe em AUTENTICADO.
     * NEGADO não distingue usuário inexistente de senha errada; BLOQUEADO informa o
     * tempo restante (sugestão HTTP: 429 + Retry-After).
     */
    public record LoginWeb(ResultadoLogin.Status status, String token, MfaService.Desafio desafio,
            long segundosRestantes, MfaService.Status mfaStatus) {
        @Override
        public String toString() {
            return "LoginWeb[status=" + status + ", token=" + (token == null ? "-" : "***")
                    + ", segundosRestantes=" + segundosRestantes + ", mfaStatus=" + mfaStatus + "]";
        }
    }

    private static final class Sessao {
        final int idUsuario;
        final String nomeUsuario;
        final Instant criadaEm;
        volatile Instant ultimoAcesso;

        Sessao(int idUsuario, String nomeUsuario, Instant agora) {
            this.idUsuario = idUsuario;
            this.nomeUsuario = nomeUsuario;
            this.criadaEm = agora;
            this.ultimoAcesso = agora;
        }

        boolean expirada(Instant agora) {
            return !agora.isBefore(ultimoAcesso.plus(OCIOSIDADE))
                    || !agora.isBefore(criadaEm.plus(DURACAO_MAXIMA));
        }
    }

    private final AuthService auth;
    private final UsuarioDAO usuarioDAO;
    private final SegurancaRepositorio repo;
    private final AutorizacaoService autorizacao;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public SessaoWebService() {
        this(new UsuarioDAO(), new SegurancaDAO(), Clock.systemUTC());
    }

    public SessaoWebService(UsuarioDAO usuarioDAO, SegurancaRepositorio repo, Clock clock) {
        this(new AuthService(usuarioDAO, repo, clock, AuthService.PERFIS_COM_MFA),
                usuarioDAO, repo, new AutorizacaoService(null), clock);
    }

    public SessaoWebService(AuthService auth, UsuarioDAO usuarioDAO, SegurancaRepositorio repo,
            AutorizacaoService autorizacao, Clock clock) {
        this.auth = auth;
        this.usuarioDAO = usuarioDAO;
        this.repo = repo;
        this.autorizacao = autorizacao;
        this.clock = clock;
    }

    // ---- login / MFA / logout -------------------------------------------------

    /** Passo 1. Perfis com MFA recebem MFA_NECESSARIA e nenhum token. */
    public LoginWeb iniciarLogin(String nomeUsuario, String senha) throws SQLException {
        ResultadoLogin r = auth.iniciarLogin(nomeUsuario, senha);
        switch (r.getStatus()) {
            case AUTENTICADO:
                return abrirSessao(r.getUsuario());
            case MFA_NECESSARIA:
                return new LoginWeb(r.getStatus(), null, r.getDesafio(), 0, null);
            case BLOQUEADO:
                return new LoginWeb(r.getStatus(), null, null, r.getSegundosRestantes(), null);
            default:
                return new LoginWeb(ResultadoLogin.Status.NEGADO, null, null, 0, null);
        }
    }

    /** Passo 2. Só com código válido, no prazo e ainda não usado sai um token. */
    public LoginWeb concluirMfa(long idDesafio, String codigo) throws SQLException {
        ResultadoLogin r = auth.concluirMfa(idDesafio, codigo);
        if (r.getStatus() == ResultadoLogin.Status.AUTENTICADO) {
            return abrirSessao(r.getUsuario());
        }
        return new LoginWeb(ResultadoLogin.Status.NEGADO, null, null, 0, r.getMfaStatus());
    }

    /** Invalida a sessão no servidor. Idempotente; devolve true se havia sessão. */
    public boolean logout(String token) {
        String chave = chaveDe(token);
        Sessao s = chave == null ? null : sessoes.remove(chave);
        if (s == null) {
            return false;
        }
        registrar(s.nomeUsuario, EventoSeguranca.LOGOUT, true, "LOGOUT");
        return true;
    }

    // ---- resolução e autorização ---------------------------------------------------

    /** Qualquer usuário autenticado: 200 ou 401. */
    public Decisao autenticar(String token) throws SQLException {
        Usuario u = resolver(token);
        return u == null ? new Decisao(Acesso.NAO_AUTENTICADO, null)
                         : new Decisao(Acesso.PERMITIDO, u);
    }

    /** 401 sem sessão válida; 403 com sessão válida sem a permissão; 200 caso contrário. */
    public Decisao autorizar(String token, Permissao permissao) throws SQLException {
        Usuario u = resolver(token);
        if (u == null) {
            return new Decisao(Acesso.NAO_AUTENTICADO, null);
        }
        if (!autorizacao.temPermissao(u.getPerfil(), permissao)) {
            registrar(u.getNomeUsuario(), EventoSeguranca.ACESSO_NEGADO, false,
                    "SEM_PERMISSAO:" + permissao.name());
            return new Decisao(Acesso.PROIBIDO, u);
        }
        return new Decisao(Acesso.PERMITIDO, u);
    }

    /** Atalho para o endpoint de métricas do D15 (somente ADMIN autenticado no servidor). */
    public Decisao autorizarMetricas(String token) throws SQLException {
        return autorizar(token, Permissao.CONSULTAR_METRICAS_TREINAMENTO);
    }

    /**
     * Extrai o token de {@code Authorization: Bearer <token>} ou do cookie
     * {@link #NOME_COOKIE}. Devolve null se ausente ou com formato impossível.
     */
    public static String extrairToken(String authorization, String cookie) {
        if (authorization != null) {
            String a = authorization.trim();
            if (a.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String t = a.substring(7).trim();
                if (TOKEN_VALIDO.matcher(t).matches()) {
                    return t;
                }
            }
        }
        if (cookie != null) {
            for (String par : cookie.split(";")) {
                String p = par.trim();
                if (p.startsWith(NOME_COOKIE + "=")) {
                    String t = p.substring(NOME_COOKIE.length() + 1).trim();
                    if (TOKEN_VALIDO.matcher(t).matches()) {
                        return t;
                    }
                }
            }
        }
        return null;
    }

    // ---- internos --------------------------------------------------------------

    private LoginWeb abrirSessao(Usuario usuario) {
        Instant agora = clock.instant();
        sessoes.values().removeIf(s -> s.expirada(agora));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(chaveDe(token), new Sessao(usuario.getId(),
                usuario.getNomeUsuario().toLowerCase(Locale.ROOT), agora));
        return new LoginWeb(ResultadoLogin.Status.AUTENTICADO, token, null, 0, null);
    }

    /** Usuário da sessão válida (perfil atual do banco) ou null. */
    private Usuario resolver(String token) throws SQLException {
        String chave = chaveDe(token);
        if (chave == null) {
            return null;
        }
        Sessao s = sessoes.get(chave);
        if (s == null) {
            return null;
        }
        Instant agora = clock.instant();
        if (s.expirada(agora)) {
            sessoes.remove(chave, s);
            return null;
        }
        Usuario u = usuarioDAO.buscarPorId(s.idUsuario);
        if (u == null) {
            sessoes.remove(chave, s);
            return null;
        }
        s.ultimoAcesso = agora;
        return u;
    }

    /** Falha ao gravar a trilha nunca mantém uma sessão viva nem libera um acesso. */
    private void registrar(String nomeUsuario, String tipo, boolean sucesso, String causa) {
        try {
            repo.registrarEvento(EventoSeguranca.novo(clock.instant(),
                    nomeUsuario.toLowerCase(Locale.ROOT), tipo, sucesso, causa, null));
        } catch (SQLException e) {
            // A decisão (invalidar ou negar) já foi tomada; só a trilha ficou sem o registro.
        }
    }

    private static String chaveDe(String token) {
        if (token == null || !TOKEN_VALIDO.matcher(token).matches()) {
            return null;
        }
        try {
            byte[] h = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
