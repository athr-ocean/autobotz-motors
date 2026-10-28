import autobotz.integracao.ApiBlockchainSimulada;
import autobotz.model.ContratoFornecedor;
import autobotz.model.StatusContrato;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Testes de ponta a ponta do cliente HTTP e da plataforma blockchain simulada do D13. */
public final class D13BlockchainApiTest {
    private static final String CHAVE = "chave-local-d13-simulacao-2026";
    private static int aprovados;

    private D13BlockchainApiTest() { }

    private static void verificar(boolean condicao, String nome) {
        if (!condicao) throw new AssertionError("FAIL " + nome);
        aprovados++;
        System.out.println("PASS " + nome);
    }

    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-27T12:00:00Z"), ZoneOffset.UTC);
        try (ApiBlockchainSimulada.Servidor servidor = new ApiBlockchainSimulada.Servidor(CHAVE, clock)) {
            ApiBlockchainSimulada.Cliente cliente = new ApiBlockchainSimulada.Cliente(servidor.endpoint(), CHAVE);

            ApiBlockchainSimulada.Resposta semNota = cliente.enviarContrato(contrato(
                    10, LocalDate.of(2026, 10, 27), StatusContrato.EM_EXECUCAO, false, false));
            verificar(semNota.status() == ApiBlockchainSimulada.Status.AGUARDANDO_NOTA_FISCAL,
                    "API_AGUARDA_NOTA_FISCAL");
            verificar(semNota.simulado() && semNota.contratoId() == 10
                    && semNota.hashLedger().equals("a".repeat(64))
                    && semNota.protocolo().matches("[0-9a-f]{64}"), "API_RETORNA_RECIBO_VINCULADO_A_VERSAO");

            ApiBlockchainSimulada.Resposta antesVencimento = cliente.enviarContrato(contrato(
                    11, LocalDate.of(2026, 11, 6), StatusContrato.EM_EXECUCAO, true, false));
            verificar(antesVencimento.status() == ApiBlockchainSimulada.Status.AGUARDANDO_VENCIMENTO,
                    "API_AGUARDA_VENCIMENTO");

            ApiBlockchainSimulada.Resposta elegivel = cliente.enviarContrato(contrato(
                    12, LocalDate.of(2026, 10, 26), StatusContrato.EM_EXECUCAO, true, false));
            verificar(elegivel.status() == ApiBlockchainSimulada.Status.LIBERACAO_SIMULADA_ELEGIVEL,
                    "API_RETORNA_ELEGIBILIDADE_SEM_PAGAMENTO_REAL");

            ApiBlockchainSimulada.Resposta encerrado = cliente.enviarContrato(contrato(
                    13, LocalDate.of(2026, 10, 26), StatusContrato.CANCELADO, true, false));
            verificar(encerrado.status() == ApiBlockchainSimulada.Status.CONTRATO_ENCERRADO,
                    "API_BLOQUEIA_CONTRATO_TERMINAL");

            ApiBlockchainSimulada.Resposta liberado = cliente.enviarContrato(contrato(
                    14, LocalDate.of(2026, 10, 26), StatusContrato.EM_EXECUCAO, true, true));
            verificar(liberado.status() == ApiBlockchainSimulada.Status.PAGAMENTO_SIMULADO_JA_LIBERADO,
                    "API_RECONHECE_ESTADO_SIMULADO_JA_LIBERADO");

            try {
                new ApiBlockchainSimulada.Cliente(URI.create("https://example.com" + ApiBlockchainSimulada.CAMINHO), CHAVE);
                throw new AssertionError("A API de demonstração não deveria aceitar endpoints externos");
            } catch (IllegalArgumentException esperado) {
                verificar(true, "CLIENTE_RECUSA_DESTINO_EXTERNO");
            }

            ApiBlockchainSimulada.Cliente chaveErrada = new ApiBlockchainSimulada.Cliente(
                    servidor.endpoint(), "outra-chave-local-de-teste");
            try {
                chaveErrada.enviarContrato(contrato(15, LocalDate.of(2026, 10, 26),
                        StatusContrato.EM_EXECUCAO, true, false));
                throw new AssertionError("Chave inválida deveria ser recusada");
            } catch (IOException esperado) {
                verificar(esperado.getMessage().contains("401"), "API_EXIGE_CREDENCIAL");
            }

            int respostaCampoExtra = enviarCampoNaoAutorizado(servidor.endpoint(), CHAVE);
            verificar(respostaCampoExtra == 400, "API_REJEITA_CAMPO_FORA_DA_LISTA_AUTORIZADA");
        }
        System.out.println("D13_BLOCKCHAIN_API_OK checks=" + aprovados);
    }

    private static ContratoFornecedor contrato(long id, LocalDate vencimento, StatusContrato estado,
                                               boolean nota, boolean pagamento) {
        return new ContratoFornecedor(id, 7, new BigDecimal("2500.00"), vencimento,
                estado, nota, pagamento, 1, "a".repeat(64), LocalDateTime.of(2026, 10, 27, 12, 0));
    }

    private static int enviarCampoNaoAutorizado(URI endpoint, String chave) throws Exception {
        String corpo = "contratoId=16&fornecedorId=7&valor=2500.00&vencimento=2026-10-26"
                + "&estado=EM_EXECUCAO&notaFiscalValidada=true&pagamentoSimuladoLiberado=false"
                + "&sequenciaLedger=1&hashLedger=" + "a".repeat(64) + "&cnpj=12345678000199";
        HttpRequest pedido = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header(ApiBlockchainSimulada.CABECALHO_CHAVE, chave)
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build();
        return HttpClient.newHttpClient().send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
