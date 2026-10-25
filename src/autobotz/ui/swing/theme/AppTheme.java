package autobotz.ui.swing.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

public final class AppTheme {
  public static final Color BACKGROUND = Color.decode("#F5F6F8");
  public static final Color SIDEBAR = Color.decode("#111827");
  public static final Color SIDEBAR_HOVER = Color.decode("#1F2937");
  public static final Color SIDEBAR_ACTIVE = Color.decode("#2563EB");
  public static final Color SIDEBAR_TEXT = Color.decode("#D1D5DB");
  public static final Color SIDEBAR_MUTED = Color.decode("#9CA3AF");
  public static final Color SIDEBAR_LABEL = Color.decode("#6B7280");
  public static final Color CARD = Color.WHITE;
  public static final Color BORDER = Color.decode("#E5E7EB");
  public static final Color TABLE_HEADER = Color.decode("#F9FAFB");
  public static final Color TABLE_SELECTION = Color.decode("#DBEAFE");
  public static final Color TEXT = Color.decode("#111827");
  public static final Color MUTED = Color.decode("#6B7280");
  public static final Color PRIMARY = Color.decode("#2563EB");
  public static final Color PRIMARY_HOVER = Color.decode("#1D4ED8");
  public static final Color SUCCESS = Color.decode("#16A34A");
  public static final Color WARNING = Color.decode("#D97706");
  public static final Color DANGER = Color.decode("#DC2626");
  public static final Color DANGER_HOVER = Color.decode("#B91C1C");
  public static final Color BUTTON_HOVER = Color.decode("#F3F4F6");
  private static final String FONT_FAMILY = resolveFontFamily();
  public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
  public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
  public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 26);
  public static final Font SECTION = new Font(FONT_FAMILY, Font.BOLD, 18);
  public static final Font BRAND = new Font(FONT_FAMILY, Font.BOLD, 25);
  public static final Font LOGO = new Font(FONT_FAMILY, Font.BOLD, 18);
  public static final Font SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 10);
  public static final Font METRIC = new Font(FONT_FAMILY, Font.BOLD, 30);
  private AppTheme() {}
  public static void install() {
    try {
      for (UIManager.LookAndFeelInfo lookAndFeel : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(lookAndFeel.getName())) {
          UIManager.setLookAndFeel(lookAndFeel.getClassName());
          break;
        }
      }
    } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
      System.err.println("Não foi possível carregar o tema Nimbus; usando o tema disponível.");
    }
    UIManager.put("Label.font", BODY);
    UIManager.put("Button.font", BODY_BOLD);
    UIManager.put("Button.background", CARD);
    UIManager.put("Button.foreground", TEXT);
    UIManager.put("TextField.font", BODY);
    UIManager.put("TextField.background", CARD);
    UIManager.put("TextField.foreground", TEXT);
    UIManager.put("Table.font", BODY);
    UIManager.put("TableHeader.font", BODY_BOLD);
    UIManager.put("Table.rowHeight", 38);
    UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
    UIManager.put("Panel.background", CARD);
    UIManager.put("ComboBox.background", CARD);
    UIManager.put("ComboBox.foreground", TEXT);
    UIManager.put("ComboBox.font", BODY);
    UIManager.put("PasswordField.background", CARD);
    UIManager.put("PasswordField.foreground", TEXT);
    UIManager.put("PasswordField.font", BODY);
    UIManager.put("swing.aatext", Boolean.TRUE);
    System.setProperty("awt.useSystemAAFontSettings", "on");
  }
  public static Border cardBorder() {
    return new EmptyBorder(20, 20, 20, 20);
  }
  public static Border fieldBorder() {
    return fieldBorder(false);
  }
  public static Border fieldBorder(boolean focused) {
    return new CompoundBorder(
        new LineBorder(focused ? PRIMARY : BORDER, 1, true), new EmptyBorder(8, 10, 8, 10));
  }
  public static JLabel title(String text) {
    JLabel l = new JLabel(text);
    l.setFont(TITLE);
    l.setForeground(TEXT);
    return l;
  }
  public static JLabel section(String text) {
    JLabel l = new JLabel(text);
    l.setFont(SECTION);
    l.setForeground(TEXT);
    return l;
  }
  public static JLabel muted(String text) {
    JLabel l = new JLabel(text);
    l.setForeground(MUTED);
    return l;
  }
  public static JPanel card() {
    JPanel p = new JPanel() {
      @Override
      protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(17, 24, 39, 10));
        g.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, 14, 14);
        g.setColor(CARD);
        g.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 12, 12);
        g.setColor(BORDER);
        g.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 3, 12, 12);
        g.dispose();
        super.paintComponent(graphics);
      }
    };
    p.setOpaque(false);
    p.setBorder(cardBorder());
    return p;
  }

  public static String fontFamily() {
    return FONT_FAMILY;
  }

  private static String resolveFontFamily() {
    Set<String> installed = Set.copyOf(Arrays.asList(
        GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
    for (String candidate :
        new String[] {"Inter", "Segoe UI", "Roboto", "Arial", "Noto Sans", "Cantarell"})
      if (installed.contains(candidate))
        return candidate;
    return "SansSerif";
  }
}
