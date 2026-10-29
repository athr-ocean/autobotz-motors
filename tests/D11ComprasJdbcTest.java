import autobotz.dao.CompraDAO;
import autobotz.dao.FornecedorDAO;
import autobotz.model.Fornecedor;
import autobotz.model.OrcamentoCompra;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.model.StatusSolicitacao;
import autobotz.service.ComprasException;
import autobotz.service.ComprasService;
import autobotz.util.ConexaoBanco;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aceite de integração D11 com MariaDB real. Só grava em banco autobotz_*_test e
 * remove, ao terminar, apenas os registros com IDs criados por esta execução.
 * Aplique db/migrations/011_d11_compras.sql antes de executar.
 */
public final class D11ComprasJdbcTest {
  @FunctionalInterface
  interface AcaoComErro {
    void executar() throws Exception;
  }

  private static int aprovados;
  private static final List<Integer> fornecedoresCriados = new ArrayList<>();
  private static final List<Integer> orcamentosCriados = new ArrayList<>();
  private static final List<Integer> solicitacoesCriadas = new ArrayList<>();

  private D11ComprasJdbcTest() {}

  public static void main(String[] args) throws Exception {
    validarDestinoDeTeste();
    validarSchemaD11();
    String sufixo = Long.toString(System.currentTimeMillis(), 36);
    String cnpj = String.format(Locale.ROOT, "%014d", System.currentTimeMillis() % 100_000_000_000_000L);
    ComprasService compras = new ComprasService(acao -> true);

    try {
      // Fornecedor persistido com ID estável e chave natural única.
      Fornecedor fornecedor = compras.cadastrarFornecedor("d11it_fornecedor_" + sufixo, cnpj);
      fornecedoresCriados.add(fornecedor.getId());
      Fornecedor recarregado = new FornecedorDAO().buscarPorId(fornecedor.getId());
      check(recarregado != null && recarregado.getId() == fornecedor.getId()
          && cnpj.equals(recarregado.getCnpj()), "FORNECEDOR_ID_ESTAVEL_NO_JDBC");

      esperarCodigo("CNPJ duplicado", ComprasException.Codigo.DUPLICADO, () -> {
        Fornecedor duplicado = compras.cadastrarFornecedor("d11it_duplicado_" + sufixo, cnpj);
        fornecedoresCriados.add(duplicado.getId());
      });

      // Fluxo feliz: PENDENTE -> APROVADA -> PEDIDO_GERADO, com uma única baixa.
      OrcamentoCompra orcamento = compras.criarOrcamento("d11it_orcamento_" + sufixo,
          new BigDecimal("1000.00"));
      orcamentosCriados.add(orcamento.getId());
      SolicitacaoCompra solic = compras.criarSolicitacao(orcamento.getId(), fornecedor.getId(),
          "Compra de aceite " + sufixo, new BigDecimal("150.00"), "d11it_solicitante");
      solicitacoesCriadas.add(solic.getId());
      check(solic.getStatus() == StatusSolicitacao.PENDENTE
          && compras.consultarSaldo(orcamento.getId()).compareTo(new BigDecimal("1000.00")) == 0,
          "SOLICITACAO_PENDENTE_NAO_DEBITA_ORCAMENTO");

      PedidoCompra pedido = compras.aprovar(solic.getId(), "d11it_aprovador");
      check(pedido != null && pedido.getId() > 0, "APROVACAO_GERA_PEDIDO_PERSISTIDO");
      check(compras.consultarSolicitacao(solic.getId()).getStatus() == StatusSolicitacao.PEDIDO_GERADO,
          "STATUS_FINAL_PEDIDO_GERADO");
      check(compras.consultarSaldo(orcamento.getId()).compareTo(new BigDecimal("850.00")) == 0,
          "ORCAMENTO_DEBITADO_EXATAMENTE_150");
      check(contar("SELECT COUNT(*) FROM pedidos_compra WHERE solicitacao_id = ?", solic.getId()) == 1,
          "EXATAMENTE_UM_PEDIDO_POR_SOLICITACAO");

      esperarCodigo("Aprovação repetida", ComprasException.Codigo.TRANSICAO_INVALIDA,
          () -> compras.aprovar(solic.getId(), "d11it_aprovador_2"));
      check(compras.consultarSaldo(orcamento.getId()).compareTo(new BigDecimal("850.00")) == 0
          && contar("SELECT COUNT(*) FROM pedidos_compra WHERE solicitacao_id = ?", solic.getId()) == 1,
          "REPETICAO_NAO_DEBITA_NEM_DUPLICA_PEDIDO");

      // Saldo insuficiente: a solicitação permanece PENDENTE e não nasce pedido.
      OrcamentoCompra baixo = compras.criarOrcamento("d11it_saldo_baixo_" + sufixo,
          new BigDecimal("10.00"));
      orcamentosCriados.add(baixo.getId());
      SolicitacaoCompra acimaDoSaldo = compras.criarSolicitacao(baixo.getId(), fornecedor.getId(),
          "Compra acima do saldo", new BigDecimal("11.00"), "d11it_solicitante");
      solicitacoesCriadas.add(acimaDoSaldo.getId());
      esperarCodigo("Saldo insuficiente", ComprasException.Codigo.ORCAMENTO_INSUFICIENTE,
          () -> compras.aprovar(acimaDoSaldo.getId(), "d11it_aprovador"));
      check(compras.consultarSolicitacao(acimaDoSaldo.getId()).getStatus() == StatusSolicitacao.PENDENTE
          && compras.consultarSaldo(baixo.getId()).compareTo(new BigDecimal("10.00")) == 0
          && contar("SELECT COUNT(*) FROM pedidos_compra WHERE solicitacao_id = ?", acimaDoSaldo.getId()) == 0,
          "SALDO_INSUFICIENTE_NAO_MUDA_ESTADO_SALDO_OU_PEDIDO");

      // Rejeição registra o motivo e preserva o orçamento.
      OrcamentoCompra rejeicaoBudget = compras.criarOrcamento("d11it_rejeicao_" + sufixo,
          new BigDecimal("100.00"));
      orcamentosCriados.add(rejeicaoBudget.getId());
      SolicitacaoCompra rejeitada = compras.criarSolicitacao(rejeicaoBudget.getId(), fornecedor.getId(),
          "Compra rejeitada", new BigDecimal("20.00"), "d11it_solicitante");
      solicitacoesCriadas.add(rejeitada.getId());
      SolicitacaoCompra resultadoRejeicao = compras.rejeitar(rejeitada.getId(), "d11it_aprovador", "Sem necessidade");
      check(resultadoRejeicao.getStatus() == StatusSolicitacao.REJEITADA
          && "Sem necessidade".equals(resultadoRejeicao.getMotivoRejeicao()), "REJEICAO_COM_MOTIVO_PERSISTIDO");
      check(compras.consultarSaldo(rejeicaoBudget.getId()).compareTo(new BigDecimal("100.00")) == 0
          && contar("SELECT COUNT(*) FROM pedidos_compra WHERE solicitacao_id = ?", rejeitada.getId()) == 0,
          "REJEICAO_NAO_DEBITA_ORCAMENTO_NEM_GERA_PEDIDO");
      esperarCodigo("Aprovação de rejeitada", ComprasException.Codigo.TRANSICAO_INVALIDA,
          () -> compras.aprovar(rejeitada.getId(), "d11it_aprovador"));

      esperarCodigo("Valor zero", ComprasException.Codigo.DADO_INVALIDO,
          () -> compras.criarSolicitacao(orcamento.getId(), fornecedor.getId(), "Valor zero",
              BigDecimal.ZERO, "d11it_solicitante"));
      esperarCodigo("Acesso sem autorização", ComprasException.Codigo.NAO_AUTORIZADO,
          () -> new ComprasService(acao -> false).consultarSaldo(orcamento.getId()));

      // Falha após a atualização de status e a baixa: rollback observado no próprio MariaDB.
      OrcamentoCompra rollbackBudget = compras.criarOrcamento("d11it_rollback_" + sufixo,
          new BigDecimal("500.00"));
      orcamentosCriados.add(rollbackBudget.getId());
      SolicitacaoCompra rollbackSolic = compras.criarSolicitacao(rollbackBudget.getId(), fornecedor.getId(),
          "Compra com falha deliberada", new BigDecimal("75.00"), "d11it_solicitante");
      solicitacoesCriadas.add(rollbackSolic.getId());
      CompraDAO falhaAoInserirPedido = new CompraDAO() {
        @Override
        public int inserirPedido(Connection c, PedidoCompra p) throws SQLException {
          throw new SQLException("falha intermediaria deliberada do D11ComprasJdbcTest");
        }
      };
      ComprasService servicoComFalha = new ComprasService(acao -> true, new FornecedorDAO(), falhaAoInserirPedido);
      boolean falhaEsperada = false;
      try {
        servicoComFalha.aprovar(rollbackSolic.getId(), "d11it_aprovador");
      } catch (SQLException e) {
        falhaEsperada = e.getMessage() != null
            && e.getMessage().contains("falha intermediaria deliberada");
      }
      check(falhaEsperada, "FALHA_INTERMEDIARIA_FOI_PROPAGADA");
      check(compras.consultarSolicitacao(rollbackSolic.getId()).getStatus() == StatusSolicitacao.PENDENTE,
          "ROLLBACK_RESTAURA_STATUS_PENDENTE");
      check(compras.consultarSaldo(rollbackBudget.getId()).compareTo(new BigDecimal("500.00")) == 0,
          "ROLLBACK_RESTAURA_SALDO_INTEGRAL");
      check(contar("SELECT COUNT(*) FROM pedidos_compra WHERE solicitacao_id = ?", rollbackSolic.getId()) == 0,
          "ROLLBACK_NAO_DEIXA_PEDIDO_PARCIAL");
    } finally {
      limparSomenteRegistrosCriados();
    }

    System.out.println("D11_COMPRAS_JDBC_OK=" + aprovados);
  }

  private static void validarDestinoDeTeste() throws Exception {
    String url = System.getenv("AUTOBOTZ_DB_URL");
    if (url == null || !url.matches("(?i).*autobotz_.*_test(?:[?].*)?")) {
      throw new IllegalStateException(
          "RECUSADO: AUTOBOTZ_DB_URL deve apontar para autobotz_*_test; nenhum teste foi executado.");
    }
    try (Connection c = ConexaoBanco.getConexao()) {
      String catalogo = c.getCatalog();
      if (catalogo == null || !catalogo.toLowerCase(Locale.ROOT).matches("autobotz_.*_test")) {
        throw new IllegalStateException(
            "RECUSADO: catálogo JDBC não termina em _test; nenhum teste foi executado.");
      }
      check(c.isValid(2), "CONEXAO_MARIADB_TESTE_VALIDA");
    }
  }

  private static void validarSchemaD11() throws Exception {
    String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
        + "AND TABLE_NAME IN ('fornecedores','orcamentos_compra','solicitacoes_compra','pedidos_compra')";
    check(contar(sql) == 4, "MIGRATION_011_CRIou_AS_QUATRO_TABELAS");
    String engineSql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() "
        + "AND TABLE_NAME IN ('fornecedores','orcamentos_compra','solicitacoes_compra','pedidos_compra') "
        + "AND ENGINE = 'InnoDB'";
    check(contar(engineSql) == 4, "TABELAS_D11_USAM_INNODB_PARA_TRANSACAO");
  }

  private static long contar(String sql, Object... parametros) throws SQLException {
    try (Connection c = ConexaoBanco.getConexao();
         PreparedStatement ps = c.prepareStatement(sql)) {
      for (int i = 0; i < parametros.length; i++)
        ps.setObject(i + 1, parametros[i]);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next())
          throw new SQLException("Consulta de contagem não retornou linha");
        return rs.getLong(1);
      }
    }
  }

  private static void esperarCodigo(String nome, ComprasException.Codigo esperado, AcaoComErro acao)
      throws Exception {
    try {
      acao.executar();
      throw new AssertionError("Era esperado " + esperado + " para: " + nome);
    } catch (ComprasException e) {
      check(e.getCodigo() == esperado, "ERRO_DE_NEGOCIO_" + nome.replace(' ', '_'));
    }
  }

  private static void check(boolean condicao, String nome) {
    if (!condicao)
      throw new AssertionError("FAIL " + nome);
    aprovados++;
    System.out.println("PASS " + nome);
  }

  private static void limparSomenteRegistrosCriados() throws SQLException {
    try (Connection c = ConexaoBanco.getConexao()) {
      boolean anterior = c.getAutoCommit();
      c.setAutoCommit(false);
      try {
        for (int id : solicitacoesCriadas) {
          try (PreparedStatement ps = c.prepareStatement("DELETE FROM pedidos_compra WHERE solicitacao_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
          }
        }
        for (int id : solicitacoesCriadas) {
          try (PreparedStatement ps = c.prepareStatement("DELETE FROM solicitacoes_compra WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
          }
        }
        for (int id : orcamentosCriados) {
          try (PreparedStatement ps = c.prepareStatement("DELETE FROM orcamentos_compra WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
          }
        }
        for (int id : fornecedoresCriados) {
          try (PreparedStatement ps = c.prepareStatement("DELETE FROM fornecedores WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
          }
        }
        c.commit();
      } catch (SQLException e) {
        try {
          c.rollback();
        } catch (SQLException rollback) {
          e.addSuppressed(rollback);
        }
        throw e;
      } finally {
        try {
          c.setAutoCommit(anterior);
        } catch (SQLException ignored) {
          // A conexão é fechada pelo try-with-resources logo após a limpeza.
        }
      }
    }
  }
}
