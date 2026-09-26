package autobotz.ui.swing;

import autobotz.ui.swing.components.AppDialog;
import autobotz.ui.swing.theme.AppTheme;
import autobotz.util.I18nUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.JFrame;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

final class SwingSupport {
  private SwingSupport() {}
  static ResourceBundle bundle() {
    return I18nUtils.getBundle(I18nUtils.getCurrentLocale());
  }
  static String text(String key) {
    try {
      return bundle().getString(key);
    } catch (Exception e) {
      return key;
    }
  }
  static void frame(JFrame f, String title) {
    f.setTitle(title);
    f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    f.setMinimumSize(new Dimension(1100, 680));
    f.setSize(1360, 820);
    f.setLocationRelativeTo(null);
  }
  static void field(JTextField field) {
    field.setFont(AppTheme.BODY);
    field.setForeground(AppTheme.TEXT);
    field.setBackground(AppTheme.CARD);
    field.setCaretColor(AppTheme.PRIMARY);
    field.setBorder(AppTheme.fieldBorder());
    field.setPreferredSize(new Dimension(240, 40));
    field.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent event) {
        field.setBorder(AppTheme.fieldBorder(true));
      }
      @Override
      public void focusLost(FocusEvent event) {
        field.setBorder(AppTheme.fieldBorder(false));
      }
    });
  }
  static void field(JPasswordField field) {
    field.setFont(AppTheme.BODY);
    field.setForeground(AppTheme.TEXT);
    field.setBackground(AppTheme.CARD);
    field.setCaretColor(AppTheme.PRIMARY);
    field.setBorder(AppTheme.fieldBorder());
    field.setPreferredSize(new Dimension(240, 40));
    field.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent event) {
        field.setBorder(AppTheme.fieldBorder(true));
      }
      @Override
      public void focusLost(FocusEvent event) {
        field.setBorder(AppTheme.fieldBorder(false));
      }
    });
  }
  static void error(Component parent, String msg) {
    AppDialog.message(parent, text("gui.operation_failed"), msg);
  }
  static void success(Component parent, String msg) {
    AppDialog.message(parent, text("gui.success"), msg);
  }
  static boolean confirm(Component parent, String msg) {
    return AppDialog.confirm(parent, text("gui.confirm"), msg);
  }
  static void locale(Locale locale) {
    I18nUtils.setLocale(locale);
  }
}
