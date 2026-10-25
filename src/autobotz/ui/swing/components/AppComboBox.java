package autobotz.ui.swing.components;

import autobotz.ui.swing.theme.AppTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

@SuppressWarnings("serial")
public final class AppComboBox<T> extends JComboBox<T> {
  public AppComboBox(T[] values) {
    super(values);
    setFont(AppTheme.BODY);
    setForeground(AppTheme.TEXT);
    setBackground(AppTheme.CARD);
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
    setRenderer(new DefaultListCellRenderer() {
      @Override
      public java.awt.Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        setFont(AppTheme.BODY);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 10, 7, 10));
        if (!selected) {
          setBackground(Color.WHITE);
          setForeground(AppTheme.TEXT);
        }
        return this;
      }
    });
  }
}
