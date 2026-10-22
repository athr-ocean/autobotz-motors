package autobotz.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Criptografia de dados pessoais de clientes em repouso.
 *
 * A chave mestra deve ser uma chave aleatória de 32 bytes codificada em
 * Base64, fornecida por AUTOBOTZ_PII_KEY ou AUTOBOTZ_PII_KEY_FILE.
 * A chave nunca é persistida no banco nem deve ser versionada no Git.
 */
public final class CriptografiaDadosPessoais {
    private static final String PREFIXO = "enc:v1:";
    private static final String VAR_CHAVE = "AUTOBOTZ_PII_KEY";
    private static final String VAR_ARQUIVO_CHAVE = "AUTOBOTZ_PII_KEY_FILE";
    private static final int TAMANHO_IV = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile byte[] chaveMestraCache;

    private CriptografiaDadosPessoais() {
    }

    public static boolean isProtegido(String valor) {
        return valor != null && valor.startsWith(PREFIXO);
    }

    public static void validarConfiguracao() throws SQLException {
        chaveMestra();
    }

    public static String proteger(String valor, String campo) throws SQLException {
        if (valor == null) {
            return null;
        }
        validarCampo(campo);

        // Evita criptografar novamente um valor já protegido; valida sua
        // autenticação e o vínculo com o campo antes de aceitá-lo.
        if (isProtegido(valor)) {
            revelar(valor, campo);
            return valor;
        }

        byte[] chave = derivarChave("autobotz:clientes:aes-gcm:v1");
        byte[] iv = new byte[TAMANHO_IV];
        RANDOM.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(chave, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(campo));
            byte[] cifra = cipher.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + cifra.length)
                    .put(iv)
                    .put(cifra)
                    .array();
            return PREFIXO + Base64.getEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            throw new SQLException("Não foi possível proteger o campo de dados pessoais " + campo + ".", e);
        } finally {
            Arrays.fill(chave, (byte) 0);
        }
    }

    public static String revelar(String valor, String campo) throws SQLException {
        if (valor == null || !isProtegido(valor)) {
            // Compatibilidade de leitura com registros anteriores à migração.
            return valor;
        }
        validarCampo(campo);

        byte[] chave = derivarChave("autobotz:clientes:aes-gcm:v1");
        try {
            byte[] payload = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            if (payload.length < TAMANHO_IV + 16) {
                throw new SQLException("O campo protegido " + campo + " tem conteúdo inválido.");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[TAMANHO_IV];
            buffer.get(iv);
            byte[] cifra = new byte[buffer.remaining()];
            buffer.get(cifra);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(chave, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(campo));
            return new String(cipher.doFinal(cifra), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new SQLException(
                    "Não foi possível descriptografar o campo " + campo
                            + ". Confira a chave configurada e a integridade do banco.",
                    e);
        } finally {
            Arrays.fill(chave, (byte) 0);
        }
    }

    /**
     * Índice HMAC determinístico para permitir busca exata de CPF sem guardar
     * o CPF em texto puro. O valor não é reversível sem conhecer a chave.
     */
    public static String fingerprintCpf(String cpf) throws SQLException {
        if (cpf == null) {
            return null;
        }
        byte[] chaveIndice = derivarChave("autobotz:clientes:cpf-hmac-index:v1");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveIndice, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(cpf.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new SQLException("Não foi possível calcular o índice protegido de CPF.", e);
        } finally {
            Arrays.fill(chaveIndice, (byte) 0);
        }
    }

    private static byte[] aad(String campo) {
        return ("AutoBotz Motors|clientes|" + campo + "|enc:v1")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static void validarCampo(String campo) throws SQLException {
        if (!"nome".equals(campo)
                && !"cpf".equals(campo)
                && !"telefone".equals(campo)
                && !"email".equals(campo)) {
            throw new SQLException("Campo de dados pessoais não autorizado para criptografia.");
        }
    }

    private static byte[] derivarChave(String contexto) throws SQLException {
        byte[] mestra = chaveMestra();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(mestra, "HmacSHA256"));
            return mac.doFinal(contexto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new SQLException("Não foi possível derivar a chave de proteção de dados pessoais.", e);
        } finally {
            Arrays.fill(mestra, (byte) 0);
        }
    }

    private static byte[] chaveMestra() throws SQLException {
        byte[] atual = chaveMestraCache;
        if (atual != null) {
            return atual.clone();
        }

        synchronized (CriptografiaDadosPessoais.class) {
            atual = chaveMestraCache;
            if (atual != null) {
                return atual.clone();
            }

            String codificada = System.getenv(VAR_CHAVE);
            if (codificada == null || codificada.isBlank()) {
                String arquivo = System.getenv(VAR_ARQUIVO_CHAVE);
                if (arquivo == null || arquivo.isBlank()) {
                    throw new SQLException(
                            "Chave de dados pessoais ausente. Configure AUTOBOTZ_PII_KEY_FILE "
                                    + "ou AUTOBOTZ_PII_KEY antes de usar o cadastro de clientes.");
                }
                try {
                    codificada = Files.readString(Path.of(arquivo), StandardCharsets.UTF_8).trim();
                } catch (Exception e) {
                    throw new SQLException("Não foi possível ler o arquivo da chave de dados pessoais.", e);
                }
            }

            try {
                byte[] decodificada = Base64.getDecoder().decode(codificada.replaceAll("\\s", ""));
                if (decodificada.length != 32) {
                    Arrays.fill(decodificada, (byte) 0);
                    throw new SQLException(
                            "A chave de dados pessoais deve ser uma chave aleatória de 32 bytes em Base64.");
                }
                chaveMestraCache = decodificada.clone();
                return decodificada;
            } catch (IllegalArgumentException e) {
                throw new SQLException("A chave de dados pessoais não contém Base64 válido.", e);
            }
        }
    }
}
