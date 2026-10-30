package autobotz.ui.swing;

import autobotz.PerfilUsuario;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.model.StatusSolicitacao;
import autobotz.service.ComprasService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Exercita formulário Swing de compra e aprovação sem abrir janela; só em autobotz_*_test. */
public final class D11ComprasSwingTest {
  private D11ComprasSwingTest() {}

  public static void main(String[] args) throws Exception {
    String url = System.getenv("AUTOBOTZ_DB_URL");
    String database = url == null ? "" : url.split("\\?", 2)[0].replaceFirst("^.*/", "");
    if (!database.matches("autobotz_[a-zA-Z0-9_]*_test"))
      throw new IllegalStateException("Use apenas autobotz_*_test");

    String id = Long.toString(System.nanoTime(), 36);
    String fornecedor = "d11ui_fornecedor_" + id;
    String orcamento = "d11ui_orcamento_" + id;
    String descricao = "d11ui_solicitacao_" + id;
    AtomicReference<Throwable> erro = new AtomicReference<>();
    try {
      SessaoUsuario.getInstancia().setUsuario(
          new Usuario(0, "d11ui_admin", "", PerfilUsuario.ADMIN));
      ComprasService servico = new ComprasService(acao -> true);

      SwingUtilities.invokeAndWait(() -> {
        try {
          ComprasPanel painel = new ComprasPanel(servico);
          preencher(painel, "fornecedorNome", fornecedor);
          clicar(painel, "gui.purchase_save_supplier");
          preencher(painel, "orcamentoNome", orcamento);
          preencher(painel, "orcamentoSaldo", "200.00");
          clicar(painel, "gui.purchase_save_budget");

          selecionar((JComboBox<?>) campo(painel, "fornecedores"), fornecedor);
          selecionar((JComboBox<?>) campo(painel, "orcamentos"), orcamento);
          preencher(painel, "solicitacaoDescricao", descricao);
          preencher(painel, "solicitacaoValor", "25.00");
          preencher(painel, "solicitante", "d11ui_admin");
          clicar(painel, "gui.purchase_submit_request");
          clicar(painel, "gui.purchase_approve");
        } catch (Throwable t) {
          erro.set(t);
        }
      });
      if (erro.get() != null) throw new AssertionError("Falha no formulário D11", erro.get());

      var solic = servico.listarSolicitacoes().stream()
          .filter(x -> descricao.equals(x.getDescricao())).findFirst()
          .orElseThrow(() -> new AssertionError("A solicitação não foi salva pelo formulário"));
      if (solic.getStatus() != StatusSolicitacao.PEDIDO_GERADO)
        throw new AssertionError("Botão Aprovar não gerou pedido");

      var budget = servico.listarOrcamentos().stream()
          .filter(x -> orcamento.equals(x.getNome())).findFirst()
          .orElseThrow(() -> new AssertionError("Orçamento não foi salvo"));
      if (servico.consultarSaldo(budget.getId()).compareTo(new BigDecimal("175.00")) != 0)
        throw new AssertionError("O pedido não debitou o orçamento corretamente");

      try (Connection con = DriverManager.getConnection(url,
          System.getenv("AUTOBOTZ_DB_USER"), System.getenv("AUTOBOTZ_DB_PASSWORD"));
          PreparedStatement stmt = con.prepareStatement(
              "SELECT COUNT(*) FROM pedidos_compra WHERE solicitacao_id=?")) {
        stmt.setInt(1, solic.getId());
        try (ResultSet rs = stmt.executeQuery()) {
          if (!rs.next() || rs.getInt(1) != 1)
            throw new AssertionError("Pedido de compra não persistiu");
        }
      }
      System.out.println("D11_COMPRAS_SWING_OK=4 (formulario, pedido, saldo, persistencia)");
    } finally {
      // Apaga exclusivamente os registros identificados pelo prefixo único do teste.
      try (Connection con = DriverManager.getConnection(url,
          System.getenv("AUTOBOTZ_DB_USER"), System.getenv("AUTOBOTZ_DB_PASSWORD"))) {
        apagar(con, "DELETE FROM pedidos_compra WHERE solicitacao_id IN "
            + "(SELECT id FROM solicitacoes_compra WHERE descricao=?)", descricao);
        apagar(con, "DELETE FROM solicitacoes_compra WHERE descricao=?", descricao);
        apagar(con, "DELETE FROM orcamentos_compra WHERE nome=?", orcamento);
        apagar(con, "DELETE FROM fornecedores WHERE nome=?", fornecedor);
      } finally {
        SessaoUsuario.getInstancia().encerrarSessao();
      }
    }
  }

  private static void apagar(Connection conexao, String sql, String valor) throws Exception {
    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
      stmt.setString(1, valor);
      stmt.executeUpdate();
    }
  }

  private static void preencher(ComprasPanel painel, String nome, String texto)
      throws ReflectiveOperationException {
    ((JTextField) campo(painel, nome)).setText(texto);
  }

  private static Object campo(ComprasPanel painel, String nome)
      throws ReflectiveOperationException {
    Field campo = ComprasPanel.class.getDeclaredField(nome);
    campo.setAccessible(true);
    return campo.get(painel);
  }

  private static void selecionar(JComboBox<?> combo, String trecho) {
    for (int i = 0; i < combo.getItemCount(); i++) {
      if (String.valueOf(combo.getItemAt(i)).contains(trecho)) {
        combo.setSelectedIndex(i);
        return;
      }
    }
    throw new AssertionError("Opção não encontrada no formulário: " + trecho);
  }

  private static void clicar(ComprasPanel painel, String chave) {
    JButton botao = acharBotao(painel, SwingSupport.text(chave));
    if (botao == null) throw new AssertionError("Botão não encontrado: " + chave);
    botao.doClick();
  }

  private static JButton acharBotao(java.awt.Container pai, String rotulo) {
    for (java.awt.Component componente : pai.getComponents()) {
      if (componente instanceof JButton botao && rotulo.equals(botao.getText()))
        return botao;
      if (componente instanceof java.awt.Container filho) {
        JButton achado = acharBotao(filho, rotulo);
        if (achado != null) return achado;
      }
    }
    return null;
  }
}
