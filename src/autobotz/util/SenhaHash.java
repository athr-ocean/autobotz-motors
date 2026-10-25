package autobotz.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de senha versionado (D14).
 *
 * <p>Formato atual: {@code pbkdf2_sha256$<iteracoes>$<salBase64>$<hashBase64>}.
 * Formato legado (somente leitura, para migração no login): SHA-256 em
 * hexadecimal, 64 caracteres, sem sal.
 *
 * <p>Nunca registrar senha nem hash em log.
 */
public final class SenhaHash {
    public static final String ALGORITMO = "pbkdf2_sha256";
    public static final int ITERACOES_PADRAO = 600_000;

    private static final int TAM_SAL = 16;
    private static final int TAM_HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DUMMY = gerar("dummy-timing-equalizer", ITERACOES_PADRAO);

    private SenhaHash() {
    }

    public static String gerar(String senha) {
        return gerar(senha, ITERACOES_PADRAO);
    }

    public static String gerar(String senha, int iteracoes) {
        if (senha == null) {
            throw new IllegalArgumentException("senha nula");
        }
        if (iteracoes < 1) {
            throw new IllegalArgumentException("iteracoes invalidas");
        }
        byte[] sal = new byte[TAM_SAL];
        RANDOM.nextBytes(sal);
        byte[] hash = derivar(senha, sal, iteracoes);
        return ALGORITMO + "$" + iteracoes + "$"
                + Base64.getEncoder().encodeToString(sal) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Aceita tanto o formato atual quanto o legado SHA-256. */
    public static boolean verificar(String senha, String armazenado) {
        if (senha == null || armazenado == null) {
            return false;
        }
        if (ehLegado(armazenado)) {
            return MessageDigest.isEqual(
                    legadoSha256(senha).getBytes(StandardCharsets.US_ASCII),
                    armazenado.toLowerCase(java.util.Locale.ROOT)
                            .getBytes(StandardCharsets.US_ASCII));
        }
        String[] partes = armazenado.split("\\$");
        if (partes.length != 4 || !ALGORITMO.equals(partes[0])) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (iteracoes < 1) {
                return false;
            }
            return MessageDigest.isEqual(derivar(senha, sal, iteracoes), esperado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Gasta o mesmo tempo de uma verificação real (evita distinguir usuário inexistente por tempo). */
    public static void verificarFalso(String senha) {
        verificar(senha == null ? "" : senha, DUMMY);
    }

    public static boolean ehLegado(String armazenado) {
        return armazenado != null && armazenado.matches("[0-9a-fA-F]{64}");
    }

    /** True se o hash é legado ou usa menos iterações que o padrão atual. */
    public static boolean precisaAtualizar(String armazenado) {
        if (armazenado == null) {
            return false;
        }
        if (ehLegado(armazenado)) {
            return true;
        }
        String[] partes = armazenado.split("\\$");
        if (partes.length != 4 || !ALGORITMO.equals(partes[0])) {
            return false;
        }
        try {
            return Integer.parseInt(partes[1]) < ITERACOES_PADRAO;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** SHA-256 hexadecimal sem sal: formato antigo, usado apenas para compatibilidade/migração. */
    public static String legadoSha256(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(I18nUtils.getString("erro.sha256"), e);
        }
    }

    private static byte[] derivar(String senha, byte[] sal, int iteracoes) {
        char[] chars = senha.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, sal, iteracoes, TAM_HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 indisponivel", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(chars, '\0');
        }
    }
}
