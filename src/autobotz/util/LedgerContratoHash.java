package autobotz.util;

import autobotz.model.ContratoFornecedor;
import autobotz.model.RegistroLedgerContrato;
import autobotz.model.ResultadoIntegridadeLedger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HexFormat;

/** Hashes determinísticos SHA-256 para a prova de conceito de ledger do D13. */
public final class LedgerContratoHash {
    private LedgerContratoHash() { }

    public static String hashConteudo(String payload) {
        return sha256(payload);
    }

    /** Campos persistidos que precisam coincidir com o último evento do ledger. */
    public static String retratoContrato(ContratoFornecedor contrato) {
        return "contratoId=" + contrato.getId() + ";fornecedorId=" + contrato.getFornecedorId()
                + ";valor=" + contrato.getValor().toPlainString()
                + ";vencimento=" + contrato.getDataVencimento()
                + ";estado=" + contrato.getEstado().name()
                + ";notaFiscalValidada=" + contrato.isNotaFiscalValidada()
                + ";pagamentoSimuladoLiberado=" + contrato.isPagamentoSimuladoLiberado();
    }

    public static boolean retratoCorrespondeAoContrato(ContratoFornecedor contrato,
                                                        RegistroLedgerContrato ultimoEvento) {
        return contrato != null && ultimoEvento != null
                && ultimoEvento.getContratoId() == contrato.getId()
                && ultimoEvento.getPayload().startsWith(retratoContrato(contrato) + ";detalhe=");
    }

    public static String hashRegistro(long contratoId, int sequencia, autobotz.model.TipoEventoContrato tipo,
                                      String hashConteudo, String hashAnterior, LocalDateTime dataHora) {
        String canonico = contratoId + "\n" + sequencia + "\n" + tipo.name() + "\n"
                + dataHora + "\n" + hashConteudo + "\n" + hashAnterior;
        return sha256(canonico);
    }

    public static ResultadoIntegridadeLedger verificar(List<RegistroLedgerContrato> registros,
                                                         int sequenciaAncorada, String hashAncorado) {
        if (registros == null || sequenciaAncorada < 0 || hashAncorado == null) {
            return ResultadoIntegridadeLedger.invalido(0, "Âncora ou lista inválida");
        }
        String hashAnteriorEsperado = ContratoFornecedor.HASH_GENESIS;
        int sequenciaEsperada = 1;
        long contratoEsperado = registros.isEmpty() ? 0 : registros.get(0).getContratoId();
        for (RegistroLedgerContrato registro : registros) {
            if (registro == null || registro.getSequencia() != sequenciaEsperada) {
                return ResultadoIntegridadeLedger.invalido(sequenciaEsperada, "Sequência ausente ou fora de ordem");
            }
            if (registro.getContratoId() != contratoEsperado) {
                return ResultadoIntegridadeLedger.invalido(sequenciaEsperada, "Registro pertence a outro contrato");
            }
            if (!hashAnteriorEsperado.equals(registro.getHashAnterior())) {
                return ResultadoIntegridadeLedger.invalido(sequenciaEsperada, "Hash anterior não corresponde");
            }
            String conteudoCalculado = hashConteudo(registro.getPayload());
            if (!conteudoCalculado.equals(registro.getHashConteudo())) {
                return ResultadoIntegridadeLedger.invalido(sequenciaEsperada, "Conteúdo alterado");
            }
            String registroCalculado = hashRegistro(registro.getContratoId(), registro.getSequencia(),
                    registro.getTipo(), conteudoCalculado, registro.getHashAnterior(), registro.getDataHora());
            if (!registroCalculado.equals(registro.getHashRegistro())) {
                return ResultadoIntegridadeLedger.invalido(sequenciaEsperada, "Hash do registro inválido");
            }
            hashAnteriorEsperado = registroCalculado;
            sequenciaEsperada++;
        }
        int ultimaSequencia = sequenciaEsperada - 1;
        if (ultimaSequencia != sequenciaAncorada || !hashAnteriorEsperado.equals(hashAncorado)) {
            return ResultadoIntegridadeLedger.invalido(ultimaSequencia, "Ledger diverge da âncora do contrato");
        }
        return ResultadoIntegridadeLedger.valido();
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível no JDK", e);
        }
    }
}
