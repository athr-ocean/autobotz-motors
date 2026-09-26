package autobotz.ui.swing.components;

import autobotz.ui.swing.theme.AppTheme;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public final class AppTextField extends JTextField {
  public AppTextField() {
    this("");
  }

  public AppTextField(String value) {
    super(value);
    setFont(AppTheme.BODY);
    setForeground(AppTheme.TEXT);
    setBackground(AppTheme.CARD);
    setCaretColor(AppTheme.PRIMARY);
    setSelectionColor(AppTheme.TABLE_SELECTION);
    setSelectedTextColor(AppTheme.TEXT);
    setBorder(AppTheme.fieldBorder(false));
    setPreferredSize(new Dimension(260, 42));
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent event) {
        setBorder(AppTheme.fieldBorder(true));
      }
      @Override
      public void focusLost(FocusEvent event) {
        setBorder(AppTheme.fieldBorder(false));
      }
    });
  }
}
