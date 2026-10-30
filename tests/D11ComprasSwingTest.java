package autobotz.ui.swing;

import autobotz.PerfilUsuario;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.service.ComprasService;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Exercita o formulário real sem exigir display; não usa banco fora de *_test. */
public final class D11ComprasSwingTest {
  private D11ComprasSwingTest() {}

  public static void main(String[] args) throws Exception {
    String url = System.getenv("AUTOBOTZ_DB_URL");
    String database = url == null ? "" : url.split("\\?", 2)[0].replaceFirst("^.*/", "");
    if (!database.matches("autobotz_[a-zA-Z0-9_]*_test"))
      throw new IllegalStateException("Use apenas autobotz_*_test");
    String nome = "d11ui_" + Long.toString(System.nanoTime(), 36);
    final AtomicReference<Throwable> erro = new AtomicReference<>();
    try {
      SessaoUsuario.getInstancia().setUsuario(new Usuario(0, "d11ui_admin", "", PerfilUsuario.ADMIN));
      ComprasService servico = new ComprasService(acao -> true);
      SwingUtilities.invokeAndWait(() -> {
        try {
          ComprasPanel painel = new ComprasPanel(servico);
          JTextField campo = (JTextField) campo(painel, "fornecedorNome");
          campo.setText(nome);
          JButton salvar = acharBotao(painel, SwingSupport.text("gui.purchase_save_supplier"));
          if (salvar == null) throw new AssertionError("Botão Salvar fornecedor ausente");
          salvar.doClick();
        } catch (Throwable e) {
          erro.set(e);
        }
      });
      if (erro.get() != null) throw new AssertionError("Falha no formulário", erro.get());
      boolean salvou = servico.listarFornecedores().stream()
          .anyMatch(f -> nome.equals(f.getNome()));
      if (!salvou) throw new AssertionError("O botão não persistiu o fornecedor informado");
      System.out.println("D11_COMPRAS_SWING_OK=2 (preenchimento, clique/persistência)");
    } finally {
      try (Connection conexao = DriverManager.getConnection(url,
          System.getenv("AUTOBOTZ_DB_USER"), System.getenv("AUTOBOTZ_DB_PASSWORD"));
          PreparedStatement stmt = conexao.prepareStatement(
              "DELETE FROM fornecedores WHERE nome = ?")) {
        stmt.setString(1, nome);
        stmt.executeUpdate();
      }
      SessaoUsuario.getInstancia().encerrarSessao();
    }
  }

  private static Object campo(ComprasPanel painel, String nome) throws ReflectiveOperationException {
    Field f = ComprasPanel.class.getDeclaredField(nome);
    f.setAccessible(true);
    return f.get(painel);
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
