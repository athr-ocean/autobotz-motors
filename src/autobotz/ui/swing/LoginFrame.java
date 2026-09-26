package autobotz.ui.swing;

import autobotz.AuthService;
import autobotz.PerfilUsuario;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.UsuarioDAO;
import autobotz.ui.swing.components.AppButton;
import autobotz.ui.swing.components.AppComboBox;
import autobotz.ui.swing.components.AppDialog;
import autobotz.ui.swing.components.AppPasswordField;
import autobotz.ui.swing.components.AppTextField;
import autobotz.ui.swing.theme.AppTheme;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

@SuppressWarnings("serial")
final class LoginFrame extends JFrame {
  private final AppTextField user = new AppTextField();
  private final AppPasswordField password = new AppPasswordField();
  private final JLabel feedback = new JLabel(" ");
  LoginFrame() {
    SwingSupport.frame(this, "AutoBotz Motors · " + SwingSupport.text("gui.login"));
    JPanel root = new JPanel(new GridBagLayout());
    root.setBackground(AppTheme.BACKGROUND);
    JPanel card = AppTheme.card();
    card.setLayout(new GridBagLayout());
    card.setPreferredSize(new Dimension(490, 500));
    GridBagConstraints g = new GridBagConstraints();
    g.gridx = 0;
    g.weightx = 1;
    g.fill = GridBagConstraints.HORIZONTAL;
    g.insets = new Insets(0, 36, 12, 36);
    JLabel logo = new JLabel("AutoBotz Motors");
    logo.setFont(AppTheme.BRAND.deriveFont(22f));
    logo.setForeground(AppTheme.TEXT);
    g.gridy = 0;
    card.add(logo, g);
    JLabel title = AppTheme.title(SwingSupport.text("gui.welcome"));
    g.gridy = 1;
    g.insets = new Insets(8, 36, 4, 36);
    card.add(title, g);
    JLabel intro = AppTheme.muted(SwingSupport.text("gui.login_help"));
    g.gridy = 2;
    g.insets = new Insets(0, 36, 24, 36);
    card.add(intro, g);
    g.gridy = 3;
    g.insets = new Insets(0, 36, 5, 36);
    card.add(new JLabel(SwingSupport.text("gui.user")), g);
    g.gridy = 4;
    g.insets = new Insets(0, 36, 14, 36);
    card.add(user, g);
    g.gridy = 5;
    g.insets = new Insets(0, 36, 5, 36);
    card.add(new JLabel(SwingSupport.text("gui.password")), g);
    g.gridy = 6;
    g.insets = new Insets(0, 36, 8, 36);
    card.add(password, g);
    feedback.setForeground(AppTheme.DANGER);
    g.gridy = 7;
    card.add(feedback, g);
    AppButton login = new AppButton(SwingSupport.text("gui.sign_in"), AppButton.Kind.PRIMARY);
    g.gridy = 8;
    g.insets = new Insets(4, 36, 10, 36);
    card.add(login, g);
    JButton register = new JButton(SwingSupport.text("gui.create_account"));
    register.setBorderPainted(false);
    register.setContentAreaFilled(false);
    register.setForeground(AppTheme.PRIMARY);
    register.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    register.setFont(AppTheme.BODY_BOLD);
    register.setFocusPainted(false);
    register.setMargin(new Insets(6, 10, 6, 10));
    register.setRolloverEnabled(true);
    register.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent event) {
        register.setForeground(AppTheme.PRIMARY_HOVER);
      }
      @Override
      public void mouseExited(MouseEvent event) {
        register.setForeground(AppTheme.PRIMARY);
      }
    });
    g.gridy = 9;
    g.insets = new Insets(0, 36, 20, 36);
    card.add(register, g);
    root.add(card);
    setContentPane(root);
    getRootPane().setDefaultButton(login);
    javax.swing.SwingUtilities.invokeLater(user::requestFocusInWindow);
    login.addActionListener(e -> login());
    register.addActionListener(e -> register());
    password.addActionListener(e -> login());
  }
  private void login() {
    if (user.getText().isBlank() || password.getPassword().length == 0) {
      feedback.setText(SwingSupport.text("gui.required_fields"));
      return;
    }
    try {
      Usuario u = new AuthService(new UsuarioDAO())
                      .autenticar(user.getText().trim(), new String(password.getPassword()));
      if (u == null) {
        feedback.setText(SwingSupport.text("gui.invalid_login"));
        return;
      }
      SessaoUsuario.getInstancia().setUsuario(u);
      dispose();
      new AppFrame().setVisible(true);
    } catch (SQLException e) {
      feedback.setText(SwingSupport.text("gui.database_error"));
    }
  }
  private void register() {
    AppTextField name = new AppTextField();
    AppPasswordField pass = new AppPasswordField();
    AppComboBox<PerfilUsuario> role = new AppComboBox<>(PerfilUsuario.values());
    role.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
          JList<?> list, Object value, int index, boolean selected, boolean focused) {
        super.getListCellRendererComponent(list, value, index, selected, focused);
        setFont(AppTheme.BODY);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 10, 7, 10));
        if (!selected) {
          setBackground(AppTheme.CARD);
          setForeground(AppTheme.TEXT);
        }
        if (value instanceof PerfilUsuario perfil)
          setText(SwingSupport.text(perfil == PerfilUsuario.ADMIN ? "gui.admin" : "gui.seller"));
        return this;
      }
    });
    JPanel p = new JPanel(new GridLayout(0, 1, 0, 7));
    p.setBackground(AppTheme.CARD);
    p.add(fieldLabel("gui.user"));
    p.add(name);
    p.add(fieldLabel("gui.password"));
    p.add(pass);
    p.add(fieldLabel("gui.profile"));
    p.add(role);
    if (!AppDialog.form(this, SwingSupport.text("gui.create_account_submit"),
            SwingSupport.text("gui.register_help"), p,
            SwingSupport.text("gui.create_account_submit")))
      return;
    if (name.getText().isBlank() || pass.getPassword().length == 0) {
      SwingSupport.error(this, SwingSupport.text("gui.required_fields"));
      return;
    }
    try {
      if (!new AuthService(new UsuarioDAO())
              .cadastrar(name.getText().trim(), new String(pass.getPassword()),
                  (PerfilUsuario) role.getSelectedItem())) {
        SwingSupport.error(this, SwingSupport.text("gui.user_exists"));
        return;
      }
      SwingSupport.success(this, SwingSupport.text("gui.account_created"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }

  private JLabel fieldLabel(String key) {
    JLabel label = new JLabel(SwingSupport.text(key));
    label.setFont(AppTheme.BODY_BOLD);
    label.setForeground(AppTheme.TEXT);
    return label;
  }
}
