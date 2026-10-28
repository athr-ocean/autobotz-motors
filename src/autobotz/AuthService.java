package autobotz;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import autobotz.dao.SegurancaDAO;
import autobotz.dao.SegurancaRepositorio;
import autobotz.model.EventoSeguranca;
import autobotz.service.MfaService;
import autobotz.service.ResultadoLogin;
import autobotz.util.SenhaHash;

/**
 * Autenticação (D14): hash PBKDF2 com migração do SHA-256 legado, bloqueio
 * por 5 falhas em janela móvel de 10 minutos, MFA simulado e trilha de
 * segurança. Uma única sessão continua sendo {@link SessaoUsuario}.
 */
public class AuthService {
    public static final int MAX_FALHAS = 5;
    public static final Duration JANELA = Duration.ofMinutes(10);
    /** Perfis que exigem segundo fator antes de abrir sessão: todos os perfis atuais. */
    public static final Set<PerfilUsuario> PERFIS_COM_MFA = EnumSet.allOf(PerfilUsuario.class);

    private static final int MAX_NOME = 50;

    private final UsuarioDAO usuarioDAO;
    private final SegurancaRepositorio repo;
    private final Clock clock;
    private final MfaService mfa;
    private final Set<PerfilUsuario> perfisComMfa;

    public AuthService(UsuarioDAO usuarioDAO) {
        this(usuarioDAO, new SegurancaDAO(), Clock.systemUTC(), PERFIS_COM_MFA);
    }

    public AuthService(UsuarioDAO usuarioDAO, SegurancaRepositorio repo, Clock clock,
            Set<PerfilUsuario> perfisComMfa) {
        this.usuarioDAO = usuarioDAO;
        this.repo = repo;
        this.clock = clock;
        this.mfa = new MfaService(repo, clock);
        this.perfisComMfa = perfisComMfa;
    }

    /** Gera hash no formato atual (PBKDF2 com sal). */
    public String gerarHash(String senha) {
        return SenhaHash.gerar(senha);
    }

    public boolean cadastrar(String nomeUsuario, String senha, PerfilUsuario perfil) throws SQLException {
        if (usuarioDAO.buscarPorNome(nomeUsuario) != null) {
            return false;
        }
        usuarioDAO.salvar(new Usuario(nomeUsuario, gerarHash(senha), perfil));
        return true;
    }

    /**
     * Primeiro fator apenas (senha), com bloqueio, trilha e migração de hash.
     * NÃO aplica MFA: telas devem usar {@link #iniciarLogin} e {@link #concluirMfa}.
     */
    public Usuario autenticar(String nomeUsuario, String senha) throws SQLException {
        return verificarSenha(nomeUsuario, senha).usuario();
    }

    /** Passo 1 do login. Perfis protegidos devolvem MFA_NECESSARIA e nenhum usuário liberado. */
    public ResultadoLogin iniciarLogin(String nomeUsuario, String senha) throws SQLException {
        Primeiro passo = verificarSenha(nomeUsuario, senha);
        if (passo.segundosBloqueio() > 0) {
            return ResultadoLogin.bloqueado(passo.segundosBloqueio());
        }
        Usuario usuario = passo.usuario();
        if (usuario == null) {
            return ResultadoLogin.negado();
        }
        if (perfisComMfa.contains(usuario.getPerfil())) {
            return ResultadoLogin.mfaNecessaria(mfa.emitir(usuario));
        }
        return ResultadoLogin.autenticado(usuario);
    }

    /**
     * Passo 2 (MFA). Só devolve AUTENTICADO com código válido, no prazo e ainda não usado.
     * Nos demais casos devolve NEGADO com {@link ResultadoLogin#getMfaStatus()}:
     * INVALIDO permite nova tentativa; os outros exigem refazer o passo 1.
     */
    public ResultadoLogin concluirMfa(long idDesafio, String codigo) throws SQLException {
        MfaService.Resultado r = mfa.verificar(idDesafio, codigo);
        if (!r.valido()) {
            return ResultadoLogin.mfaFalha(r.status());
        }
        Usuario usuario = usuarioDAO.buscarPorId(r.idUsuario());
        return usuario == null ? ResultadoLogin.mfaFalha(MfaService.Status.INEXISTENTE)
                               : ResultadoLogin.autenticado(usuario);
    }

    public MfaService getMfaService() {
        return mfa;
    }

    /** Resultado interno do passo 1: usuário liberado, ou segundos de bloqueio (>0). */
    private record Primeiro(Usuario usuario, long segundosBloqueio) {
        static Primeiro falha() { return new Primeiro(null, 0); }
    }

    private Primeiro verificarSenha(String nomeInformado, String senha) throws SQLException {
        String busca = nomeInformado == null ? "" : nomeInformado.trim();
        String nome = chave(busca);
        if (busca.isEmpty() || senha == null) {
            SenhaHash.verificarFalso(senha);
            return Primeiro.falha();
        }
        Instant agora = clock.instant();

        List<Instant> falhas = repo.listarFalhasLogin(nome, agora.minus(JANELA));
        if (falhas.size() >= MAX_FALHAS) {
            SenhaHash.verificarFalso(senha);
            evento(agora, nome, EventoSeguranca.LOGIN_BLOQUEADO, false, "LIMITE_DE_FALHAS");
            return new Primeiro(null, segundosRestantes(falhas, agora));
        }

        Usuario usuario = usuarioDAO.buscarPorNome(busca);
        if (usuario == null) {
            SenhaHash.verificarFalso(senha);
            evento(agora, nome, EventoSeguranca.LOGIN, false, "USUARIO_INEXISTENTE");
            return aposFalha(nome, agora);
        }
        if (!SenhaHash.verificar(senha, usuario.getSenhaHash())) {
            evento(agora, nome, EventoSeguranca.LOGIN, false, "SENHA_INVALIDA");
            return aposFalha(nome, agora);
        }

        evento(agora, nome, EventoSeguranca.LOGIN, true, "SENHA_OK");
        migrarHashSeNecessario(usuario, senha, agora, nome);
        return new Primeiro(usuario, 0);
    }

    /** Se esta falha foi a que atingiu o limite, já informa o bloqueio e o tempo. */
    private Primeiro aposFalha(String nome, Instant agora) throws SQLException {
        List<Instant> falhas = repo.listarFalhasLogin(nome, agora.minus(JANELA));
        return falhas.size() >= MAX_FALHAS
                ? new Primeiro(null, segundosRestantes(falhas, agora))
                : Primeiro.falha();
    }

    /**
     * O bloqueio cai quando restarem MAX_FALHAS-1 falhas na janela, isto é,
     * quando a falha de índice (n - MAX_FALHAS) sair dela (10 min após ocorrer).
     */
    private static long segundosRestantes(List<Instant> falhasOrdenadas, Instant agora) {
        Instant libera = falhasOrdenadas.get(falhasOrdenadas.size() - MAX_FALHAS).plus(JANELA);
        long millis = Duration.between(agora, libera).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private void migrarHashSeNecessario(Usuario usuario, String senha, Instant agora, String nome) {
        String atual = usuario.getSenhaHash();
        if (!SenhaHash.precisaAtualizar(atual)) {
            return;
        }
        try {
            String novo = SenhaHash.gerar(senha);
            if (usuarioDAO.atualizarSenhaHash(usuario.getId(), atual, novo)) {
                evento(agora, nome, EventoSeguranca.HASH_MIGRADO, true,
                        SenhaHash.ehLegado(atual) ? "SHA256_PARA_PBKDF2" : "AUMENTO_DE_ITERACOES");
            }
        } catch (SQLException e) {
            // Migração é best-effort: o login válido não pode falhar por causa dela.
        }
    }

    private void evento(Instant quando, String nome, String tipo, boolean ok, String causa)
            throws SQLException {
        // Origem fica nula: em execução desktop/console não há IP remoto observável.
        repo.registrarEvento(EventoSeguranca.novo(quando, nome, tipo, ok, causa, null));
    }

    /** Chave de bloqueio/trilha: sem diferença de caixa, limitada ao tamanho da coluna. */
    private static String chave(String nome) {
        String n = nome.toLowerCase(Locale.ROOT);
        return n.length() > MAX_NOME ? n.substring(0, MAX_NOME) : n;
    }
}
