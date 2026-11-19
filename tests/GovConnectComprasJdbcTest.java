import autobotz.integracao.govconnect.GovConnectIntegracao.Cliente;
import autobotz.integracao.govconnect.GovConnectIntegracao.PedidoResponse;
import autobotz.integracao.govconnect.GovConnectIntegracao.ServidorSimulado;
import autobotz.integracao.govconnect.GovConnectPedidoService;
import autobotz.model.Fornecedor;
import autobotz.model.OrcamentoCompra;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.service.ComprasException;
import autobotz.service.ComprasService;
import autobotz.service.ComprasService.Acao;
import autobotz.util.ConexaoBanco;

import java.math.BigDecimal;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Integra D11/MariaDB a D2/GovConnect simulado.
 * Só aceita banco autobotz_*_test e remove exclusivamente os IDs criados nesta execução.
 */
public final class GovConnectComprasJdbcTest {
    private static final List<Integer> fornecedores = new ArrayList<>();
    private static final List<Integer> orcamentos = new ArrayList<>();
    private static final List<Integer> solicitacoes = new ArrayList<>();
    private static int aprovados;

    private GovConnectComprasJdbcTest() { }

    public static void main(String[] args) throws Exception {
        verificarPermissaoEspecificaDeEnvio();
        validarBancoDeTeste();
        String sufixo = Long.toString(System.currentTimeMillis(), 36);
        String cnpj = String.format(Locale.ROOT, "%014d",
                System.currentTimeMillis() % 100_000_000_000_000L);
        ComprasService compras = new ComprasService(acao -> true);

        try {
            Fornecedor fornecedor = compras.cadastrarFornecedor("d2_gov_fornecedor_" + sufixo, cnpj);
            fornecedores.add(fornecedor.getId());
            OrcamentoCompra orcamento = compras.criarOrcamento("d2_gov_orcamento_" + sufixo,
                    new BigDecimal("500.00"));
            orcamentos.add(orcamento.getId());
            SolicitacaoCompra solicitacao = compras.criarSolicitacao(orcamento.getId(), fornecedor.getId(),
                    "Pedido de integração D2 " + sufixo, new BigDecimal("125.50"), "d2_gov_solicitante");
            solicitacoes.add(solicitacao.getId());
            PedidoCompra pedido = compras.aprovar(solicitacao.getId(), "d2_gov_aprovador");
            check(pedido != null && pedido.getId() > 0, "D11_GEROU_PEDIDO_PERSISTIDO");
            check(compras.consultarSolicitacao(solicitacao.getId()).getStatus().name().equals("PEDIDO_GERADO"),
                    "D11_STATUS_PEDIDO_GERADO");

            String token = "token-local-d2-jdbc";
            try (ServidorSimulado gov = new ServidorSimulado(token)) {
                Cliente cliente = new Cliente(gov.baseUri(), () -> token);
                GovConnectPedidoService integracao = new GovConnectPedidoService(compras, cliente);

                PedidoResponse resposta = integracao.enviarPedidoDaSolicitacao(solicitacao.getId());
                check(resposta.simulado(), "D2_RETORNO_IDENTIFICADO_COMO_SIMULADO");
                check("RECEBIDO".equals(resposta.status()), "D2_PEDIDO_RECEBIDO_PELO_MOCK");
                check(("AUTOBOTZ-PEDIDO-" + pedido.getId()).equals(resposta.referenciaERP()),
                        "D2_REFERENCIA_CORRESPONDE_AO_PEDIDO_D11");
                check(resposta.protocolo() != null && !resposta.protocolo().isBlank(),
                        "D2_PROTOCOLO_EXTERNO_RECEBIDO");

                PedidoResponse repetido = integracao.enviarPedidoDaSolicitacao(solicitacao.getId());
                check(resposta.protocolo().equals(repetido.protocolo()),
                        "D2_REENVIO_IDEMPOTENTE_DO_PEDIDO_D11");
            }

            check(compras.consultarSaldo(orcamento.getId()).compareTo(new BigDecimal("374.50")) == 0,
                    "D11_SALDO_NAO_FOI_ALTERADO_PELO_ENVIO_EXTERNO");
            check(compras.consultarSolicitacao(solicitacao.getId()).getStatus().name().equals("PEDIDO_GERADO"),
                    "D11_ESTADO_LOCAL_PRESERVADO_APOS_INTEGRACAO");
        } finally {
            limparRegistrosCriados();
        }

        System.out.println("GOVCONNECT_COMPRAS_JDBC_OK=" + aprovados);
    }

    /** A permissão de envio deve ser exigida antes de qualquer consulta ao banco ou chamada HTTP. */
    private static void verificarPermissaoEspecificaDeEnvio() throws Exception {
        java.util.concurrent.atomic.AtomicInteger consultas = new java.util.concurrent.atomic.AtomicInteger();
        ComprasService semPermissaoDeEnvio = new ComprasService(acao -> {
            if (acao == Acao.CONSULTAR) consultas.incrementAndGet();
            return acao != Acao.ENVIAR_PEDIDO_GOVCONNECT;
        });
        Cliente cliente = new Cliente(URI.create("http://127.0.0.1:1"), () -> "token-nao-usado");
        GovConnectPedidoService integracao = new GovConnectPedidoService(semPermissaoDeEnvio, cliente);
        try {
            integracao.enviarPedidoDaSolicitacao(1);
            throw new AssertionError("Envio sem permissão específica foi aceito");
        } catch (ComprasException esperado) {
            check(esperado.getCodigo() == ComprasException.Codigo.NAO_AUTORIZADO,
                    "D2_ENVIO_EXIGE_PERMISSAO_ESPECIFICA");
        }
        check(consultas.get() == 0, "D2_PERMISSAO_VERIFICADA_ANTES_DAS_CONSULTAS");
    }

    private static void validarBancoDeTeste() throws Exception {
        String url = System.getenv("AUTOBOTZ_DB_URL");
        if (url == null || !url.matches("(?i).*autobotz_.*_test(?:[?].*)?"))
            throw new IllegalStateException("RECUSADO: AUTOBOTZ_DB_URL deve apontar para autobotz_*_test");
        try (Connection c = ConexaoBanco.getConexao()) {
            String catalogo = c.getCatalog();
            if (catalogo == null || !catalogo.toLowerCase(Locale.ROOT).matches("autobotz_.*_test"))
                throw new IllegalStateException("RECUSADO: catálogo JDBC não é banco descartável *_test");
            check(c.isValid(2), "D2_CONEXAO_MARIADB_TESTE_VALIDA");
        }
    }

    private static void check(boolean ok, String nome) {
        if (!ok) throw new AssertionError("FAIL " + nome);
        aprovados++;
        System.out.println("PASS " + nome);
    }

    private static void limparRegistrosCriados() throws Exception {
        try (Connection c = ConexaoBanco.getConexao()) {
            boolean anterior = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                for (int id : solicitacoes) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM pedidos_compra WHERE solicitacao_id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                for (int id : solicitacoes) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM solicitacoes_compra WHERE id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                for (int id : orcamentos) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM orcamentos_compra WHERE id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                for (int id : fornecedores) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM fornecedores WHERE id = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (Exception erro) {
                c.rollback();
                throw erro;
            } finally {
                try { c.setAutoCommit(anterior); } catch (Exception ignored) { }
            }
        }
    }
}
