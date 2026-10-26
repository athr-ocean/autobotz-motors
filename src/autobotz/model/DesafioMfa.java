package autobotz.model;

import java.time.Instant;

/** Desafio MFA persistido. {@code codigoHash} é sal+hash, nunca o código. */
public record DesafioMfa(
        long id,
        int idUsuario,
        String codigoHash,
        Instant expiraEm,
        int tentativas,
        int maxTentativas,
        boolean consumido) {
}
