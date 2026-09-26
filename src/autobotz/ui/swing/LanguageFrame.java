package autobotz.ui.swing;

import autobotz.ui.swing.components.AppButton;
import autobotz.ui.swing.theme.AppTheme;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

@SuppressWarnings("serial")
final class LanguageFrame extends JFrame {
  LanguageFrame() {
    SwingSupport.frame(this, "AutoBotz Motors");
    JPanel root = new JPanel(new GridBagLayout());
    root.setBackground(AppTheme.BACKGROUND);
    JPanel card = AppTheme.card();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setPreferredSize(new Dimension(540, 360));
    JLabel brand = new JLabel("AutoBotz Motors");
    brand.setAlignmentX(CENTER_ALIGNMENT);
    brand.setFont(AppTheme.BRAND);
    brand.setForeground(AppTheme.TEXT);
    JLabel sub = new JLabel(SwingSupport.text("gui.brand_subtitle"));
    sub.setAlignmentX(CENTER_ALIGNMENT);
    sub.setForeground(AppTheme.MUTED);
    JLabel prompt = new JLabel("Selecione seu idioma / Select your language");
    prompt.setAlignmentX(CENTER_ALIGNMENT);
    prompt.setFont(AppTheme.BODY_BOLD);
    prompt.setForeground(AppTheme.TEXT);
    prompt.setBorder(BorderFactory.createEmptyBorder(30, 0, 22, 0));
    JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 12));
    buttons.setOpaque(false);
    buttons.setMaximumSize(new Dimension(320, 110));
    AppButton pt = new AppButton("Português (Brasil)", AppButton.Kind.PRIMARY);
    AppButton en = new AppButton("English (United States)", AppButton.Kind.SECONDARY);
    buttons.add(pt);
    buttons.add(en);
    card.add(Box.createVerticalGlue());
    card.add(brand);
    card.add(Box.createVerticalStrut(5));
    card.add(sub);
    card.add(prompt);
    card.add(buttons);
    card.add(Box.createVerticalGlue());
    root.add(card);
    pt.addActionListener(e -> open(Locale.forLanguageTag("pt-BR")));
    en.addActionListener(e -> open(Locale.US));
    setContentPane(root);
  }
  private void open(Locale locale) {
    SwingSupport.locale(locale);
    dispose();
    new LoginFrame().setVisible(true);
  }
}
