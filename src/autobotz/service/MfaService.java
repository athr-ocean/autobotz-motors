package autobotz.service;

import autobotz.Usuario;
import autobotz.dao.SegurancaRepositorio;
import autobotz.model.DesafioMfa;
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

/**
 * MFA SIMULADO (D14): código numérico de uso único, com expiração e limite
 * de tentativas. NÃO há envio real de SMS/e-mail: o código é devolvido em
 * {@link Desafio#codigoSimulado()} para a interface exibi-lo rotulado como
 * demonstração. O código nunca é gravado em log nem no banco (só sal+hash).
 */
public class MfaService {
    public static final Duration VALIDADE = Duration.ofMinutes(5);
    public static final int MAX_TENTATIVAS = 3;
    public static final int DIGITOS = 6;

    public enum Status { VALIDO, INVALIDO, EXPIRADO, ESGOTADO, JA_UTILIZADO, INEXISTENTE }

    /** Desafio emitido. {@code codigoSimulado} existe só para a "entrega" de demonstração. */
    public record Desafio(long id, int idUsuario, Instant expiraEm, String codigoSimulado) {
        @Override
        public String toString() {
            return "Desafio[id=" + id + ", idUsuario=" + idUsuario + ", expiraEm=" + expiraEm + "]";
        }
    }

    public record Resultado(Status status, int idUsuario) {
        public boolean valido() {
            return status == Status.VALIDO;
        }
    }

    private final SegurancaRepositorio repo;
    private final Clock clock;
    private final SecureRandom random;

    public MfaService(SegurancaRepositorio repo, Clock clock) {
        this(repo, clock, new SecureRandom());
    }

    public MfaService(SegurancaRepositorio repo, Clock clock, SecureRandom random) {
        this.repo = repo;
        this.clock = clock;
        this.random = random;
    }

    /** Emite um novo desafio e invalida os anteriores ainda abertos do mesmo usuário. */
    public Desafio emitir(Usuario usuario) throws SQLException {
        Instant agora = clock.instant();
        String codigo = gerarCodigo();
        repo.invalidarDesafiosPendentes(usuario.getId());
        Instant expira = agora.plus(VALIDADE);
        long id = repo.criarDesafioMfa(usuario.getId(), hashCodigo(codigo), agora, expira,
                MAX_TENTATIVAS);
        repo.registrarEvento(EventoSeguranca.novo(agora, usuario.getNomeUsuario(),
                EventoSeguranca.MFA_EMITIDO, true, "ENTREGA_SIMULADA", null));
        return new Desafio(id, usuario.getId(), expira, codigo);
    }

    public Resultado verificar(long idDesafio, String codigoInformado) throws SQLException {
        Instant agora = clock.instant();
        DesafioMfa d = repo.buscarDesafioMfa(idDesafio);
        if (d == null) {
            return new Resultado(Status.INEXISTENTE, 0);
        }
        if (d.consumido()) {
            return falha(d, Status.JA_UTILIZADO, agora);
        }
        if (!agora.isBefore(d.expiraEm())) {
            repo.invalidarDesafio(d.id());
            return falha(d, Status.EXPIRADO, agora);
        }
        if (d.tentativas() >= d.maxTentativas() || !repo.incrementarTentativaMfa(d.id())) {
            repo.invalidarDesafio(d.id());
            return falha(d, Status.ESGOTADO, agora);
        }
        if (codigoConfere(codigoInformado, d.codigoHash())) {
            if (repo.consumirDesafioMfa(d.id(), agora)) {
                registrar(d, true, "OK", agora);
                return new Resultado(Status.VALIDO, d.idUsuario());
            }
            return falha(d, Status.JA_UTILIZADO, agora);
        }
        if (d.tentativas() + 1 >= d.maxTentativas()) {
            repo.invalidarDesafio(d.id());
            return falha(d, Status.ESGOTADO, agora);
        }
        return falha(d, Status.INVALIDO, agora);
    }

    private Resultado falha(DesafioMfa d, Status status, Instant agora) throws SQLException {
        registrar(d, false, status.name(), agora);
        return new Resultado(status, 0);
    }

    private void registrar(DesafioMfa d, boolean sucesso, String causa, Instant agora)
            throws SQLException {
        repo.registrarEvento(EventoSeguranca.novo(agora, "id_usuario:" + d.idUsuario(),
                EventoSeguranca.MFA_VERIFICACAO, sucesso, causa, null));
    }

    private String gerarCodigo() {
        int limite = (int) Math.pow(10, DIGITOS);
        return String.format("%0" + DIGITOS + "d", random.nextInt(limite));
    }

    private String hashCodigo(String codigo) {
        byte[] sal = new byte[16];
        random.nextBytes(sal);
        return Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(sha256(sal, codigo));
    }

    private boolean codigoConfere(String informado, String armazenado) {
        if (informado == null || armazenado == null) {
            return false;
        }
        String[] partes = armazenado.split("\\$");
        if (partes.length != 2) {
            return false;
        }
        try {
            byte[] sal = Base64.getDecoder().decode(partes[0]);
            byte[] esperado = Base64.getDecoder().decode(partes[1]);
            return MessageDigest.isEqual(sha256(sal, informado.trim()), esperado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] sha256(byte[] sal, String codigo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(sal);
            return md.digest(codigo.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
