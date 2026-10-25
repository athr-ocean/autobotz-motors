package autobotz.ui.swing.components;

import autobotz.ui.swing.theme.AppTheme;
import autobotz.util.I18nUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Dialogs that keep actions and feedback inside the application's visual language. */
public final class AppDialog {
  private AppDialog() {}

  public static boolean form(
      Component parent, String title, String description, JComponent form, String submitLabel) {
    boolean[] submitted = {false};
    JDialog dialog = create(parent, title);
    JPanel content = content(title, description);
    JPanel body = new JPanel(new BorderLayout());
    body.setBackground(AppTheme.CARD);
    body.add(form, BorderLayout.CENTER);
    content.add(body, BorderLayout.CENTER);

    AppButton cancel = new AppButton(I18nUtils.getString("gui.cancel"), AppButton.Kind.SECONDARY);
    AppButton submit = new AppButton(submitLabel, AppButton.Kind.PRIMARY);
    cancel.addActionListener(event -> dialog.dispose());
    submit.addActionListener(event -> {
      submitted[0] = true;
      dialog.dispose();
    });
    JPanel actions = actions(cancel, submit);
    content.add(actions, BorderLayout.SOUTH);
    dialog.setContentPane(content);
    dialog.getRootPane().setDefaultButton(submit);
    bindEscape(dialog, cancel);
    Component firstInput = firstFocusable(form);
    if (firstInput != null)
      javax.swing.SwingUtilities.invokeLater(firstInput::requestFocusInWindow);
    show(dialog, parent, 460);
    return submitted[0];
  }

  public static void message(Component parent, String title, String message) {
    JDialog dialog = create(parent, title);
    JPanel content = content(title, null);
    JLabel text =
        AppTheme.muted("<html><div style='width:360px'>" + escape(message) + "</div></html>");
    content.add(text, BorderLayout.CENTER);
    AppButton close = new AppButton(I18nUtils.getString("gui.close"), AppButton.Kind.PRIMARY);
    close.addActionListener(event -> dialog.dispose());
    content.add(actions(close), BorderLayout.SOUTH);
    dialog.setContentPane(content);
    dialog.getRootPane().setDefaultButton(close);
    bindEscape(dialog, close);
    show(dialog, parent, 440);
  }

  public static boolean confirm(Component parent, String title, String message) {
    boolean[] confirmed = {false};
    JDialog dialog = create(parent, title);
    JPanel content = content(title, null);
    JLabel text =
        AppTheme.muted("<html><div style='width:360px'>" + escape(message) + "</div></html>");
    content.add(text, BorderLayout.CENTER);
    AppButton cancel = new AppButton(I18nUtils.getString("gui.cancel"), AppButton.Kind.SECONDARY);
    AppButton accept =
        new AppButton(I18nUtils.getString("gui.confirm_action"), AppButton.Kind.DANGER);
    cancel.addActionListener(event -> dialog.dispose());
    accept.addActionListener(event -> {
      confirmed[0] = true;
      dialog.dispose();
    });
    content.add(actions(cancel, accept), BorderLayout.SOUTH);
    dialog.setContentPane(content);
    dialog.getRootPane().setDefaultButton(accept);
    bindEscape(dialog, cancel);
    show(dialog, parent, 440);
    return confirmed[0];
  }

  private static JDialog create(Component parent, String title) {
    Window owner = SwingUtilities.getWindowAncestor(parent);
    JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setResizable(false);
    return dialog;
  }

  private static JPanel content(String title, String description) {
    JPanel panel = new JPanel(new BorderLayout(0, 18));
    panel.setBackground(AppTheme.CARD);
    panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 20, 24));
    JPanel heading = new JPanel(new BorderLayout(0, 6));
    heading.setBackground(AppTheme.CARD);
    heading.add(AppTheme.section(title), BorderLayout.NORTH);
    if (description != null)
      heading.add(AppTheme.muted(description), BorderLayout.CENTER);
    panel.add(heading, BorderLayout.NORTH);
    return panel;
  }

  private static JPanel actions(AppButton... buttons) {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    panel.setBackground(AppTheme.CARD);
    for (AppButton button : buttons) panel.add(button);
    return panel;
  }

  private static void bindEscape(JDialog dialog, AppButton cancel) {
    dialog.getRootPane()
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel-dialog");
    dialog.getRootPane().getActionMap().put("cancel-dialog", new javax.swing.AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent event) {
        cancel.doClick();
      }
    });
  }

  private static void show(JDialog dialog, Component parent, int width) {
    dialog.pack();
    Dimension size = dialog.getSize();
    dialog.setSize(Math.max(width, size.width), Math.max(220, size.height));
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }

  private static Component firstFocusable(Component component) {
    if (component.isFocusable() && component.isEnabled()
        && !(component instanceof Container && component instanceof JPanel))
      return component;
    if (component instanceof Container container) {
      for (Component child : container.getComponents()) {
        Component focusable = firstFocusable(child);
        if (focusable != null)
          return focusable;
      }
    }
    return null;
  }

  private static String escape(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\n", "<br>");
  }
}
