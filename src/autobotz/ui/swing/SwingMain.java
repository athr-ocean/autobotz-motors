package autobotz.ui.swing;

import autobotz.ui.swing.theme.AppTheme;
import javax.swing.SwingUtilities;

public final class SwingMain {
  private SwingMain() {}
  public static void main(String[] args) {
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    SwingUtilities.invokeLater(() -> {
      AppTheme.install();
      new LanguageFrame().setVisible(true);
    });
  }
}
