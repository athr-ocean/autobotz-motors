import autobotz.integracao.govconnect.GovConnectIntegracao;
import autobotz.integracao.govconnect.GovConnectIntegracao.*;
import autobotz.integracao.govconnect.OAuth2ClientCredentialsTokenProvider;
import autobotz.model.Fornecedor;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.model.StatusSolicitacao;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/** Teste D2 sem banco ou internet. Servidores mock limitados a 127.0.0.1. */
public class GovConnectIntegracaoTest {
    private static final List<String> FALHAS = new ArrayList<>();
    private static int aprovados;
    private static int executados;

    @FunctionalInterface
    private interface Verificacao { void executar() throws Exception; }

    public static void main(String[] args) throws Exception {
        imprimirCabecalho();
        String token = "token-federado-local-de-teste";
        AtomicInteger emissoes = new AtomicInteger();
        HttpServer idp = HttpServer.create(
                new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        idp.createContext("/oauth2/token", x -> emitirToken(x, emissoes, token));
        idp.start();

        PedidoRequest[] pedido = new PedidoRequest[1];
        PedidoResponse[] resposta = new PedidoResponse[1];
        PedidoResponse[] consulta = new PedidoResponse[1];

        URI tokenUri = URI.create("http://127.0.0.1:" + idp.getAddress().getPort() + "/oauth2/token");
        try (ServidorSimulado gov = new ServidorSimulado(token)) {
            OAuth2ClientCredentialsTokenProvider tokens = new OAuth2ClientCredentialsTokenProvider(
                    tokenUri, "autobotz-client", "segredo-local-de-teste",
                    "compras.pedidos.write compras.pedidos.read");
            Cliente cliente = new Cliente(gov.baseUri(), tokens);

            executar("Mapeamento de PedidoCompra e Fornecedor para DTO", () -> {
                PedidoCompra persistido = new PedidoCompra(42, 9, 3, 7, new BigDecimal("250.00"));
                Fornecedor fornecedor = new Fornecedor(7, "Fornecedor Exemplo", "12345678000199");
                SolicitacaoCompra solicitacao = new SolicitacaoCompra(9, 3, 7,
                        "Compra de peças de reposição", new BigDecimal("250.00"),
                        StatusSolicitacao.PEDIDO_GERADO, "solicitante", "aprovador", null);
                pedido[0] = PedidoRequest.de(persistido, solicitacao, fornecedor);
                exigir("AUTOBOTZ-PEDIDO-42".equals(pedido[0].referenciaERP()), "Referência ERP incorreta");
                exigir("12345678000199".equals(pedido[0].fornecedorCnpj()), "CNPJ do fornecedor não foi mapeado");
                exigir(pedido[0].valor().compareTo(new BigDecimal("250.00")) == 0, "Valor incorreto");
                exigir("Compra de peças de reposição".equals(pedido[0].descricao()),
                        "Descrição não veio da solicitação D11");
            });

            executar("Autenticação OAuth2 Client Credentials", () -> {
                exigir(token.equals(tokens.obterToken()), "Token inesperado");
                exigir(emissoes.get() == 1, "O IdP não recebeu uma emissão");
            });
            executar("Cache e reutilização do access token", () -> {
                exigir(token.equals(tokens.obterToken()), "Token em cache diferente");
                exigir(emissoes.get() == 1, "Token foi solicitado novamente sem necessidade");
            });
            executar("Envio do pedido para o GovConnect simulado", () -> {
                resposta[0] = cliente.enviarPedido(pedido[0]);
                exigir("RECEBIDO".equals(resposta[0].status()), "Status inesperado");
                exigir(resposta[0].simulado(), "Retorno deveria identificar o ambiente simulado");
                exigir(pedido[0].referenciaERP().equals(resposta[0].referenciaERP()),
                        "Referência devolvida não corresponde ao pedido");
                exigir(resposta[0].protocolo() != null && !resposta[0].protocolo().isBlank(),
                        "Protocolo não foi devolvido");
            });
            executar("Idempotência: reenvio não duplica o pedido", () -> {
                PedidoResponse repetido = cliente.enviarPedido(pedido[0]);
                exigir(resposta[0].protocolo().equals(repetido.protocolo()),
                        "Reenvio gerou protocolo diferente");
            });
            executar("Consulta do pedido pelo protocolo", () -> {
                consulta[0] = cliente.consultar(resposta[0].protocolo());
                exigir(resposta[0].referenciaERP().equals(consulta[0].referenciaERP()),
                        "Consulta retornou outra referência");
                exigir("RECEBIDO".equals(consulta[0].status()), "Status de consulta inesperado");
            });
            executar("Rejeição de Bearer token inválido (HTTP 401)", () -> {
                try {
                    new Cliente(gov.baseUri(), () -> "token-inválido").enviarPedido(pedido[0]);
                    throw new AssertionError("Servidor aceitou token inválido");
                } catch (GovConnectException esperado) {
                    exigir(esperado.getStatusHttp() == 401, "HTTP inesperado: " + esperado.getStatusHttp());
                }
            });
            executar("Validação de dados obrigatórios e valor", () -> {
                try {
                    new PedidoRequest("pedido ruim", "", "cnpj-inválido",
                            new BigDecimal("-1"), "USD", "");
                    throw new AssertionError("Dados inválidos foram aceitos");
                } catch (IllegalArgumentException esperado) { }
            });
            executar("Bloqueio de envio antes do status PEDIDO_GERADO", () -> {
                PedidoCompra persistido = new PedidoCompra(44, 11, 4, 7, new BigDecimal("100.00"));
                SolicitacaoCompra pendente = new SolicitacaoCompra(11, 4, 7, "Peças",
                        new BigDecimal("100.00"), StatusSolicitacao.PENDENTE,
                        "solicitante", null, null);
                Fornecedor fornecedor = new Fornecedor(7, "Fornecedor Exemplo", "12345678000199");
                try {
                    PedidoRequest.de(persistido, pendente, fornecedor);
                    throw new AssertionError("Solicitação pendente foi aceita para envio");
                } catch (IllegalArgumentException esperado) { }
            });
            executar("Validação de correspondência do fornecedor", () -> {
                PedidoCompra persistido = new PedidoCompra(43, 10, 4, 8, new BigDecimal("100.00"));
                SolicitacaoCompra solicitacao = new SolicitacaoCompra(10, 4, 8, "Peças",
                        new BigDecimal("100.00"), StatusSolicitacao.PEDIDO_GERADO,
                        "solicitante", "aprovador", null);
                Fornecedor divergente = new Fornecedor(7, "Fornecedor Exemplo", "12345678000199");
                try {
                    PedidoRequest.de(persistido, solicitacao, divergente);
                    throw new AssertionError("Fornecedor divergente foi aceito");
                } catch (IllegalArgumentException esperado) { }
            });
        } finally {
            idp.stop(0);
        }

        executar("Bloqueio de HTTP para API externa", () -> {
            try {
                new Cliente(URI.create("http://govconnect.example.gov.br"), () -> token);
                throw new AssertionError("HTTP externo deveria ser recusado");
            } catch (IllegalArgumentException esperado) { }
        });
        executar("Bloqueio de HTTP para token endpoint externo", () -> {
            try {
                new OAuth2ClientCredentialsTokenProvider(
                        URI.create("http://idp.example.gov.br/token"), "id", "secret", "scope");
                throw new AssertionError("HTTP externo deveria ser recusado");
            } catch (IllegalArgumentException esperado) { }
        });

        imprimirPedido(pedido[0], resposta[0], consulta[0], emissoes.get());
        imprimirResumo();
        if (!FALHAS.isEmpty())
            throw new AssertionError(FALHAS.size() + " verificação(ões) falharam: " + String.join("; ", FALHAS));
    }

    private static void executar(String nome, Verificacao verificacao) {
        executados++;
        try {
            verificacao.executar();
            aprovados++;
            System.out.printf(Locale.ROOT, "[PASS] %s%n", nome);
        } catch (Throwable erro) {
            String detalhe = erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
            FALHAS.add(nome + " — " + detalhe);
            System.out.printf(Locale.ROOT, "[FAIL] %s — %s%n", nome, detalhe);
        }
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }

    private static void imprimirCabecalho() {
        System.out.println("============================================================");
        System.out.println(" AUTOBOTZ MOTORS | D2 - TESTE DE INTEGRAÇÃO GOVCONNECT");
        System.out.println("============================================================");
        System.out.println("Ambiente: SIMULADO (servidores locais em 127.0.0.1)");
        System.out.println("Banco de dados: não utilizado");
        System.out.println("Internet: não utilizada");
        System.out.println("------------------------------------------------------------");
        System.out.println("VERIFICAÇÕES");
    }

    private static void imprimirPedido(PedidoRequest pedido, PedidoResponse resposta,
                                       PedidoResponse consulta, int emissoesToken) {
        System.out.println("\n------------------------------------------------------------");
        System.out.println("DEMONSTRAÇÃO DO PEDIDO");
        if (pedido == null || resposta == null) {
            System.out.println("Não foi possível concluir o envio do pedido de demonstração.");
            return;
        }
        System.out.println("Referência ERP       : " + pedido.referenciaERP());
        System.out.println("Fornecedor           : " + pedido.fornecedorNome()
                + " (ID " + pedido.fornecedorId() + ")");
        System.out.println("Valor                : R$ " + pedido.valor().toPlainString());
        System.out.println("Descrição            : " + pedido.descricao());
        System.out.println("Protocolo externo    : " + resposta.protocolo());
        System.out.println("Status do envio      : " + resposta.status());
        System.out.println("Retorno identificado : " + (resposta.simulado() ? "SIMULADO" : "NÃO SIMULADO"));
        System.out.println("Consulta confirmada  : " + (consulta != null ? "SIM" : "NÃO"));
        System.out.println("Emissões de token    : " + emissoesToken);
        System.out.println("Recebido em          : " + resposta.recebidoEm());
        System.out.println("------------------------------------------------------------");
    }

    private static void imprimirResumo() {
        System.out.println("\n============================================================");
        System.out.printf(Locale.ROOT, "RESULTADO: %d/%d verificações aprovadas%n", aprovados, executados);
        if (FALHAS.isEmpty()) System.out.println("STATUS: SUCESSO");
        else {
            System.out.println("STATUS: FALHA");
            for (String falha : FALHAS) System.out.println(" - " + falha);
        }
        System.out.println("ATENÇÃO: o resultado comprova somente o protótipo com mocks locais;");
        System.out.println("         não comprova comunicação com o GovConnect governamental real.");
        System.out.println("============================================================");
    }

    private static void emitirToken(HttpExchange x, AtomicInteger emissoes, String token) throws IOException {
        try (x) {
            String basic = "Basic " + Base64.getEncoder().encodeToString(
                    "autobotz-client:segredo-local-de-teste".getBytes(StandardCharsets.UTF_8));
            String body = new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!basic.equals(x.getRequestHeaders().getFirst("Authorization"))
                    || !body.contains("grant_type=client_credentials")
                    || !body.contains("scope=compras.pedidos.write")) {
                byte[] erro = "{\"error\":\"invalid_client\"}".getBytes(StandardCharsets.UTF_8);
                x.getResponseHeaders().set("Content-Type", "application/json");
                x.sendResponseHeaders(401, erro.length);
                x.getResponseBody().write(erro);
                return;
            }
            emissoes.incrementAndGet();
            byte[] bodyResposta = ("{\"access_token\":\"" + token
                    + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}")
                    .getBytes(StandardCharsets.UTF_8);
            x.getResponseHeaders().set("Content-Type", "application/json");
            x.getResponseHeaders().set("Cache-Control", "no-store");
            x.sendResponseHeaders(200, bodyResposta.length);
            x.getResponseBody().write(bodyResposta);
        }
    }
}
