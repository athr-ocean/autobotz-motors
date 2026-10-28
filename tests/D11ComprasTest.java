import autobotz.dao.CompraDAO;
import autobotz.dao.FornecedorDAO;
import autobotz.model.Fornecedor;
import autobotz.model.OrcamentoCompra;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.service.ComprasException;
import autobotz.service.ComprasService;
import autobotz.service.ComprasService.Acao;
import autobotz.service.ComprasService.Autorizador;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class D11ComprasTest {

    private static int acertos = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Iniciando Bateria de Testes do D11 (ComprasService)");
        System.out.println("==================================================");

        try {
            testarBloqueioAutorizacao();
            testarEntradasInvalidas();
            testarFluxoFelizEBaixaDeSaldo();
            testarOrcamentoInsuficiente();
            testarRepeticaoDeAprovacao();
            testarRollbackEmFalhaDeBanco();

            System.out.println("==================================================");
            System.out.println("SUCCESS: Todos os " + acertos + " testes do D11 passaram!");
            System.out.println("==================================================");
        } catch (Throwable e) {
            System.err.println("\nFALHA NO TESTE DO D11: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void check(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError("FALHA: " + mensagem);
        }
        acertos++;
        System.out.println("PASS " + mensagem);
    }

    /** 1. Valida se ações sem permissão lançam NAO_AUTORIZADO. */
    private static void testarBloqueioAutorizacao() throws SQLException {
        Autorizador semAprovacao = acao -> acao != Acao.APROVAR;
        ComprasService service = new ComprasService(semAprovacao);

        try {
            service.aprovar(1, "usuario_comum");
            check(false, "D11_BLOQUEIO_AUTORIZACAO (Deveria ter bloqueado aprovação)");
        } catch (ComprasException e) {
            check(e.getCodigo() == ComprasException.Codigo.NAO_AUTORIZADO,
                    "D11_BLOQUEIO_AUTORIZACAO");
        }
    }

    /** 2. Valida se entradas/dados inválidos lançam DADO_INVALIDO. */
    private static void testarEntradasInvalidas() throws Exception {
        Autorizador total = acao -> true;
        ComprasService service = new ComprasService(total);

        // Teste CNPJ com menos de 14 dígitos
        try {
            service.cadastrarFornecedor("Fornecedor Invalido", "12345");
            check(false, "D11_ENTRADAS_INVALIDAS (Deveria ter recusado CNPJ curto)");
        } catch (ComprasException e) {
            check(e.getCodigo() == ComprasException.Codigo.DADO_INVALIDO,
                    "D11_RECUSA_CNPJ_INVALIDO");
        }

        // Teste Orçamento com valor negativo
        try {
            service.criarOrcamento("Orçamento Negativo", new BigDecimal("-100.00"));
            check(false, "D11_ENTRADAS_INVALIDAS (Deveria ter recusado valor negativo)");
        } catch (ComprasException e) {
            check(e.getCodigo() == ComprasException.Codigo.DADO_INVALIDO,
                    "D11_RECUSA_VALOR_NEGATIVO");
        }
    }

    /** 3. Valida cadastro, solicitação, aprovação e baixa no saldo. */
    private static void testarFluxoFelizEBaixaDeSaldo() throws Exception {
        Autorizador total = acao -> true;
        ComprasService service = new ComprasService(total);

        Fornecedor f = service.cadastrarFornecedor("Fornecedor Teste D11", "12.345.678/0001-95");
        OrcamentoCompra o = service.criarOrcamento("Orçamento TI 2026", new BigDecimal("5000.00"));

        SolicitacaoCompra s = service.criarSolicitacao(
                o.getId(), f.getId(), "Compra de Servidores", new BigDecimal("1500.00"), "solicitante_d11"
        );

        PedidoCompra pedido = service.aprovar(s.getId(), "gerente_d11");
        check(pedido != null && pedido.getValor().compareTo(new BigDecimal("1500.00")) == 0,
                "D11_PEDIDO_GERADO_COM_SUCESSO");

        BigDecimal saldoAtual = service.consultarSaldo(o.getId());
        check(saldoAtual.compareTo(new BigDecimal("3500.00")) == 0,
                "D11_BAIXA_SALDO_ORCAMENTO");
    }

    /** 4. Valida se compras com valor acima do saldo são rejeitadas sem alterar o orçamento. */
    private static void testarOrcamentoInsuficiente() throws Exception {
        Autorizador total = acao -> true;
        ComprasService service = new ComprasService(total);

        Fornecedor f = service.cadastrarFornecedor("Fornecedor Limite", "98.765.432/0001-10");
        OrcamentoCompra o = service.criarOrcamento("Orçamento Peças", new BigDecimal("1000.00"));

        SolicitacaoCompra s = service.criarSolicitacao(
                o.getId(), f.getId(), "Compra Excedente", new BigDecimal("2000.00"), "solicitante_d11"
        );

        try {
            service.aprovar(s.getId(), "gerente_d11");
            check(false, "D11_ORCAMENTO_INSUFICIENTE (Deveria ter rejeitado compra cara)");
        } catch (ComprasException e) {
            check(e.getCodigo() == ComprasException.Codigo.ORCAMENTO_INSUFICIENTE,
                    "D11_REJEICAO_ORCAMENTO_INSUFICIENTE");
        }

        BigDecimal saldo = service.consultarSaldo(o.getId());
        check(saldo.compareTo(new BigDecimal("1000.00")) == 0,
                "D11_SALDO_PRESERVADO_APOS_FALHA");
    }

    /** 5. Valida se aprovar uma solicitação já aprovada lança TRANSICAO_INVALIDA. */
    private static void testarRepeticaoDeAprovacao() throws Exception {
        Autorizador total = acao -> true;
        ComprasService service = new ComprasService(total);

        Fornecedor f = service.cadastrarFornecedor("Fornecedor Repetição", "11.222.333/0001-44");
        OrcamentoCompra o = service.criarOrcamento("Orçamento Frota", new BigDecimal("3000.00"));
        SolicitacaoCompra s = service.criarSolicitacao(
                o.getId(), f.getId(), "Troca de Óleo", new BigDecimal("500.00"), "solicitante_d11"
        );

        service.aprovar(s.getId(), "gerente_d11");

        try {
            service.aprovar(s.getId(), "gerente_d11");
            check(false, "D11_REPETICAO_APROVACAO (Deveria ter rejeitado reaprovação)");
        } catch (ComprasException e) {
            check(e.getCodigo() == ComprasException.Codigo.TRANSICAO_INVALIDA,
                    "D11_BLOQUEIO_REPETICAO_APROVACAO");
        }
    }

    /** 6. Simula falha no banco durante a gravação do pedido para garantir o ROLLBACK. */
    private static void testarRollbackEmFalhaDeBanco() throws Exception {
        Autorizador total = acao -> true;
        FornecedorDAO fornecedorDAO = new FornecedorDAO();

        CompraDAO compraDAOFalha = new CompraDAOComFalha();

        ComprasService serviceComFalha = new ComprasService(total, fornecedorDAO, compraDAOFalha);

        Fornecedor f = serviceComFalha.cadastrarFornecedor("Fornecedor Rollback", "55.666.777/0001-88");
        OrcamentoCompra o = serviceComFalha.criarOrcamento("Orçamento Rollback", new BigDecimal("2000.00"));
        SolicitacaoCompra s = serviceComFalha.criarSolicitacao(
                o.getId(), f.getId(), "Pneus Novos", new BigDecimal("800.00"), "solicitante_d11"
        );

        try {
            serviceComFalha.aprovar(s.getId(), "gerente_d11");
            check(false, "D11_ROLLBACK (Deveria ter lançado erro de banco)");
        } catch (SQLException e) {
            check(true, "D11_FALHA_BANCO_PROPAGADA");
        }

        ComprasService serviceNormal = new ComprasService(total);
        BigDecimal saldoAposRollback = serviceNormal.consultarSaldo(o.getId());
        check(saldoAposRollback.compareTo(new BigDecimal("2000.00")) == 0,
                "D11_ROLLBACK_TRANSACIONAL_GARANTIDO");
    }

    /** Subclasse estática do CompraDAO usada para simular falha intencional de banco. */
    private static class CompraDAOComFalha extends CompraDAO {
        @Override
        public int inserirPedido(Connection c, PedidoCompra p) throws SQLException {
            throw new SQLException("Erro simulado para teste de Rollback Transacional");
        }
    }
}