package autobotz.ui.swing.components;

import autobotz.ui.swing.theme.AppTheme;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JButton;

@SuppressWarnings("serial")
public final class AppButton extends JButton {
  private final boolean outlined;
  private final Kind kind;
  private final Color enabledForeground;

  public enum Kind { PRIMARY, SECONDARY, DANGER, GHOST }
  public AppButton(String text, Kind kind) {
    super(text);
    this.kind = kind;
    outlined = kind == Kind.SECONDARY;
    setFocusPainted(false);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setOpaque(false);
    setMargin(new Insets(9, 16, 9, 16));
    setFont(AppTheme.BODY_BOLD);
    setMinimumSize(new Dimension(0, 40));
    switch (kind) {
      case PRIMARY -> {
        setBackground(AppTheme.PRIMARY);
        setForeground(Color.WHITE);
      }
      case DANGER -> {
        setBackground(AppTheme.DANGER);
        setForeground(Color.WHITE);
      }
      case SECONDARY -> {
        setBackground(Color.WHITE);
        setForeground(AppTheme.PRIMARY);
      }
      case GHOST -> {
        setBackground(AppTheme.SIDEBAR);
        setForeground(Color.WHITE);
      }
    }
    enabledForeground = getForeground();
    addPropertyChangeListener("enabled",
        event -> setForeground((Boolean) event.getNewValue() ? enabledForeground : AppTheme.MUTED));
  }
  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    Color fill = getBackground();
    if (!isEnabled())
      fill = AppTheme.BORDER;
    else if (getModel().isPressed() || getModel().isRollover()) {
      fill = switch (kind) {
        case PRIMARY -> AppTheme.PRIMARY_HOVER;
        case DANGER -> AppTheme.DANGER_HOVER;
        case SECONDARY -> AppTheme.BUTTON_HOVER;
        case GHOST -> AppTheme.SIDEBAR_HOVER;
      };
    }
    g2.setColor(fill);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
    if (outlined) {
      g2.setColor(AppTheme.BORDER);
      g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
    }
    if (isFocusOwner()) {
      g2.setColor(AppTheme.PRIMARY);
      g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 6, 6);
    }
    g2.dispose();
    super.paintComponent(g);
  }
}
