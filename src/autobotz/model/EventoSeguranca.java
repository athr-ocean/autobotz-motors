package autobotz.model;

import java.time.Instant;

/** Evento da trilha de segurança (D14). Não carrega segredos. */
public record EventoSeguranca(
        long id,
        Instant dataHora,
        String nomeUsuario,
        String tipo,
        boolean sucesso,
        String causa,
        String origem) {

    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_BLOQUEADO = "LOGIN_BLOQUEADO";
    public static final String MFA_EMITIDO = "MFA_EMITIDO";
    public static final String MFA_VERIFICACAO = "MFA_VERIFICACAO";
    public static final String HASH_MIGRADO = "HASH_MIGRADO";

    public static EventoSeguranca novo(Instant dataHora, String nomeUsuario, String tipo,
            boolean sucesso, String causa, String origem) {
        return new EventoSeguranca(0, dataHora, nomeUsuario, tipo, sucesso, causa, origem);
    }
}
