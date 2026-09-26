package autobotz.ui.swing;

import autobotz.AutorizacaoService;
import autobotz.PerfilUsuario;
import autobotz.ProjetoDAO;
import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.dao.AuditoriaDAO;
import autobotz.dao.ClienteDAO;
import autobotz.dao.OrdemServicoDAO;
import autobotz.dao.ServicoDAO;
import autobotz.dao.VeiculoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.Cliente;
import autobotz.model.ItemOS;
import autobotz.model.LogAuditoria;
import autobotz.model.OrdemServico;
import autobotz.model.Servico;
import autobotz.model.Veiculo;
import autobotz.model.Venda;
import autobotz.service.CRMService;
import autobotz.service.ClienteService;
import autobotz.service.RelatorioService;
import autobotz.service.ServicoService;
import autobotz.service.VendaService;
import autobotz.ui.swing.components.AppButton;
import autobotz.ui.swing.components.AppComboBox;
import autobotz.ui.swing.components.AppDialog;
import autobotz.ui.swing.components.AppTable;
import autobotz.ui.swing.theme.AppTheme;
import autobotz.util.I18nUtils;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

/** Janela única do ERP. Páginas são trocadas por CardLayout e consomem o backend existente. */
@SuppressWarnings("serial")
final class AppFrame extends JFrame {
  private final CardLayout cards = new CardLayout();
  private final JPanel content = new JPanel(cards);
  private final JLabel pageTitle = new JLabel();
  private final JLabel profile = new JLabel();
  private final AutorizacaoService auth = new AutorizacaoService(SwingSupport.bundle());
  private final Map<String, JButton> nav = new LinkedHashMap<>();
  AppFrame() {
    SwingSupport.frame(this, "AutoBotz Motors");
    setTitle("AutoBotz Motors");
    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(AppTheme.BACKGROUND);
    root.add(sidebar(), BorderLayout.WEST);
    root.add(workspace(), BorderLayout.CENTER);
    setContentPane(root);
    showPage("dashboard");
  }
  private JPanel sidebar() {
    JPanel side = new JPanel(new BorderLayout());
    side.setBackground(AppTheme.SIDEBAR);
    side.setPreferredSize(new Dimension(225, 0));
    JPanel top = new JPanel();
    top.setOpaque(false);
    top.setBorder(BorderFactory.createEmptyBorder(28, 22, 20, 20));
    top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
    JLabel logo = new JLabel("AutoBotz Motors");
    logo.setForeground(Color.WHITE);
    logo.setFont(AppTheme.LOGO);
    JLabel sub = new JLabel("ERP");
    sub.setForeground(AppTheme.SIDEBAR_MUTED);
    sub.setFont(AppTheme.BODY);
    top.add(logo);
    top.add(sub);
    side.add(top, BorderLayout.NORTH);
    JPanel menu = new JPanel();
    menu.setOpaque(false);
    menu.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
    addNav(menu, "dashboard", "gui.dashboard");
    label(menu, "gui.operations");
    addNav(menu, "vehicles", "gui.vehicles", AutorizacaoService.Permissao.LISTAR_VEICULOS);
    addNav(menu, "clients", "gui.clients", AutorizacaoService.Permissao.LISTAR_CLIENTES);
    addNav(menu, "sales", "gui.sales", AutorizacaoService.Permissao.REALIZAR_VENDA,
        AutorizacaoService.Permissao.LISTAR_VENDAS);
    addNav(menu, "workshop", "gui.workshop", AutorizacaoService.Permissao.ACESSAR_OFICINA);
    label(menu, "gui.management");
    addNav(menu, "projects", "gui.projects", AutorizacaoService.Permissao.CONSULTAR_PROJETOS);
    addNav(menu, "crm", "gui.crm", AutorizacaoService.Permissao.ACESSAR_CRM);
    label(menu, "gui.analysis");
    addNav(menu, "reports", "gui.reports", AutorizacaoService.Permissao.ACESSAR_RELATORIOS);
    addNav(menu, "audit", "gui.audit", AutorizacaoService.Permissao.ACESSAR_RELATORIOS);
    side.add(menu, BorderLayout.CENTER);
    JPanel bottom = new JPanel();
    bottom.setOpaque(false);
    bottom.setBorder(BorderFactory.createEmptyBorder(14, 18, 22, 18));
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
    profile.setForeground(AppTheme.SIDEBAR_TEXT);
    profile.setFont(AppTheme.BODY_BOLD);
    bottom.add(profile);
    JButton logout = new JButton(SwingSupport.text("gui.logout"));
    logout.setAlignmentX(LEFT_ALIGNMENT);
    logout.setForeground(AppTheme.SIDEBAR_MUTED);
    logout.setContentAreaFilled(false);
    logout.setBorderPainted(false);
    logout.addActionListener(e -> logout());
    bottom.add(logout);
    side.add(bottom, BorderLayout.SOUTH);
    return side;
  }
  private void label(JPanel p, String key) {
    JLabel l = new JLabel(SwingSupport.text(key));
    l.setForeground(AppTheme.SIDEBAR_LABEL);
    l.setFont(AppTheme.SMALL_BOLD);
    l.setBorder(BorderFactory.createEmptyBorder(22, 10, 7, 0));
    p.add(l);
  }
  private void addNav(JPanel p, String id, String key,
      AutorizacaoService.Permissao... permissions) {
    boolean permitted = permissions.length == 0;
    for (AutorizacaoService.Permissao permission : permissions)
      permitted |= allowed(permission);
    if (!permitted)
      return;
    JButton b = new JButton(SwingSupport.text(key));
    b.setHorizontalAlignment(SwingConstants.LEFT);
    b.setForeground(AppTheme.SIDEBAR_TEXT);
    b.setBackground(AppTheme.SIDEBAR);
    b.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 12));
    b.setFocusPainted(false);
    b.setContentAreaFilled(false);
    b.addActionListener(e -> showPage(id));
    nav.put(id, b);
    p.add(b);
  }
  private JPanel workspace() {
    JPanel p = new JPanel(new BorderLayout());
    p.setBackground(AppTheme.BACKGROUND);
    JPanel top = new JPanel(new BorderLayout());
    top.setBackground(Color.WHITE);
    top.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, AppTheme.BORDER),
        BorderFactory.createEmptyBorder(18, 28, 18, 28)));
    pageTitle.setFont(AppTheme.TITLE);
    pageTitle.setForeground(AppTheme.TEXT);
    top.add(pageTitle, BorderLayout.WEST);
    JLabel lang = new JLabel(I18nUtils.getCurrentLocale().toLanguageTag());
    lang.setForeground(AppTheme.MUTED);
    top.add(lang, BorderLayout.EAST);
    p.add(top, BorderLayout.NORTH);
    content.setBackground(AppTheme.BACKGROUND);
    p.add(content, BorderLayout.CENTER);
    return p;
  }
  private boolean allowed(AutorizacaoService.Permissao permission) {
    return auth.temPermissao(permission);
  }
  private void showPage(String id) {
    String key = "gui." + id;
    pageTitle.setText(SwingSupport.text(key));
    Usuario currentUser = SessaoUsuario.getInstancia().getUsuario();
    String role = currentUser.getPerfil() == PerfilUsuario.ADMIN ? SwingSupport.text("gui.admin")
                                                                 : SwingSupport.text("gui.seller");
    profile.setText(currentUser.getNomeUsuario() + " · " + role);
    JPanel page = switch (id) {
      case "dashboard" -> dashboard();
      case "vehicles" -> vehicles();
      case "clients" -> clients();
      case "sales" -> sales();
      case "workshop" -> workshop();
      case "projects" -> projects();
      case "crm" -> crm();
      case "reports" -> reports();
      case "audit" -> audit();
      default -> dashboard();
    };
    content.removeAll();
    content.add(page, id);
    cards.show(content, id);
    content.revalidate();
    content.repaint();
    for (var e : nav.entrySet())
      e.getValue().setBackground(
          e.getKey().equals(id) ? AppTheme.SIDEBAR_ACTIVE : AppTheme.SIDEBAR);
  }
  private JPanel page(String subtitle) {
    JPanel p = new JPanel(new BorderLayout(0, 18));
    p.setBackground(AppTheme.BACKGROUND);
    p.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
    JLabel s = AppTheme.muted(subtitle);
    p.add(s, BorderLayout.NORTH);
    return p;
  }
  private JPanel formPanel(LayoutManager layout) {
    JPanel form = new JPanel(layout);
    form.setBackground(AppTheme.CARD);
    form.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    return form;
  }
  private JPanel dashboard() {
    JPanel p = page(SwingSupport.text("gui.dashboard_subtitle"));
    JPanel center = new JPanel();
    center.setBackground(AppTheme.BACKGROUND);
    center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
    JPanel cards = new JPanel();
    cards.setOpaque(false);
    List<JPanel> stats = new ArrayList<>();
    try {
      if (allowed(AutorizacaoService.Permissao.LISTAR_VEICULOS))
        stats.add(stat(SwingSupport.text("gui.vehicles"), new VeiculoDAO().listarTodos().size()));
      if (allowed(AutorizacaoService.Permissao.LISTAR_CLIENTES))
        stats.add(stat(SwingSupport.text("gui.clients"), new ClienteDAO().listar().size()));
      if (allowed(AutorizacaoService.Permissao.LISTAR_VENDAS))
        stats.add(stat(SwingSupport.text("gui.sales"), new VendaDAO().listarVendas().size()));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    if (stats.isEmpty()) {
      cards.setLayout(new BorderLayout());
      cards.add(AppTheme.muted(SwingSupport.text("gui.no_permission")), BorderLayout.CENTER);
    } else {
      cards.setLayout(new GridLayout(1, stats.size(), 16, 0));
      stats.forEach(cards::add);
    }
    center.add(cards);
    center.add(Box.createVerticalStrut(22));
    JPanel quick = AppTheme.card();
    quick.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
    quick.add(AppTheme.section(SwingSupport.text("gui.quick_actions")));
    quick.add(
        button("gui.new_vehicle", "vehicles", AutorizacaoService.Permissao.CADASTRAR_VEICULO));
    quick.add(button("gui.new_client", "clients", AutorizacaoService.Permissao.CADASTRAR_CLIENTE));
    quick.add(button("gui.new_sale", "sales", AutorizacaoService.Permissao.REALIZAR_VENDA));
    quick.add(button("gui.open_order", "workshop", AutorizacaoService.Permissao.ACESSAR_OFICINA));
    center.add(quick);
    p.add(center, BorderLayout.CENTER);
    return p;
  }
  private JPanel stat(String label, int value) {
    JPanel c = AppTheme.card();
    c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
    JLabel n = new JLabel(Integer.toString(value));
    n.setFont(AppTheme.METRIC);
    n.setForeground(AppTheme.PRIMARY);
    c.add(new JLabel(label));
    c.add(Box.createVerticalStrut(12));
    c.add(n);
    return c;
  }
  private JButton button(String key, String page, AutorizacaoService.Permissao permission) {
    AppButton b = new AppButton(SwingSupport.text(key), AppButton.Kind.PRIMARY);
    b.addActionListener(e -> {
      if (permission != null && !allowed(permission)) {
        SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
        return;
      }
      if (key.equals("gui.new_vehicle"))
        vehicleDialog();
      else if (key.equals("gui.new_client"))
        clientDialog();
      else if (key.equals("gui.new_sale"))
        saleDialog();
      else if (key.equals("gui.open_order") || key.equals("gui.new_order"))
        orderDialog();
      else
        showPage(page);
    });
    b.setEnabled(permission == null || allowed(permission));
    return b;
  }
  private void vehicleDialog() {
    vehicleDialog(null);
  }
  private void vehicleDialog(Veiculo vehicle) {
    AutorizacaoService.Permissao permission = vehicle == null
        ? AutorizacaoService.Permissao.CADASTRAR_VEICULO
        : AutorizacaoService.Permissao.ATUALIZAR_VEICULO;
    if (!allowed(permission)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    JTextField plate = new JTextField(vehicle == null ? "" : vehicle.getPlaca());
    JTextField brand = new JTextField(vehicle == null ? "" : vehicle.getMarca());
    JTextField model = new JTextField(vehicle == null ? "" : vehicle.getModelo());
    JTextField year = new JTextField(vehicle == null ? "" : Integer.toString(vehicle.getAno()));
    JTextField price = new JTextField(vehicle == null ? "" : Double.toString(vehicle.getPreco()));
    for (JTextField field : List.of(plate, brand, model, year, price)) SwingSupport.field(field);
    JPanel p = formPanel(new GridLayout(0, 2, 10, 10));
    p.add(new JLabel(SwingSupport.text("gui.plate")));
    p.add(plate);
    p.add(new JLabel(SwingSupport.text("gui.brand")));
    p.add(brand);
    p.add(new JLabel(SwingSupport.text("gui.model")));
    p.add(model);
    p.add(new JLabel(SwingSupport.text("gui.year")));
    p.add(year);
    p.add(new JLabel(SwingSupport.text("gui.price")));
    p.add(price);
    if (!AppDialog.form(this,
            SwingSupport.text(vehicle == null ? "gui.new_vehicle" : "gui.edit_vehicle"),
            SwingSupport.text("gui.vehicle_form_help"), p, SwingSupport.text("gui.save")))
      return;
    try {
      int parsedYear = Integer.parseInt(year.getText().trim());
      double parsedPrice = Double.parseDouble(price.getText().trim().replace(',', '.'));
      if (plate.getText().isBlank() || brand.getText().isBlank() || model.getText().isBlank()
          || parsedYear < 1886 || parsedYear > LocalDate.now().getYear() + 1 || parsedPrice <= 0)
        throw new IllegalArgumentException();
      VeiculoDAO dao = new VeiculoDAO();
      if (vehicle == null) {
        dao.cadastrar(new Veiculo(plate.getText().trim(), model.getText().trim(),
            brand.getText().trim(), parsedYear, parsedPrice));
        SwingSupport.success(this, SwingSupport.text("gui.vehicle_saved"));
      } else {
        vehicle.setPlaca(plate.getText().trim());
        vehicle.setMarca(brand.getText().trim());
        vehicle.setModelo(model.getText().trim());
        vehicle.setAno(parsedYear);
        vehicle.setPreco(parsedPrice);
        dao.atualizar(vehicle);
        SwingSupport.success(this, SwingSupport.text("gui.vehicle_updated"));
      }
      showPage("vehicles");
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void clientDialog() {
    clientDialog(null);
  }
  private void clientDialog(Cliente client) {
    AutorizacaoService.Permissao permission = client == null
        ? AutorizacaoService.Permissao.CADASTRAR_CLIENTE
        : AutorizacaoService.Permissao.ACESSAR_CLIENTE_LGPD;
    if (!allowed(permission)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    JTextField name = new JTextField(client == null ? "" : client.getNome());
    JTextField cpf = new JTextField(client == null ? "" : client.getCpf());
    JTextField phone = new JTextField(client == null ? "" : client.getTelefone());
    JTextField email = new JTextField(client == null ? "" : client.getEmail());
    for (JTextField field : List.of(name, cpf, phone, email)) SwingSupport.field(field);
    JPanel p = formPanel(new GridLayout(0, 2, 10, 10));
    p.add(new JLabel(SwingSupport.text("gui.name")));
    p.add(name);
    p.add(new JLabel(SwingSupport.text("gui.cpf")));
    p.add(cpf);
    p.add(new JLabel(SwingSupport.text("gui.phone")));
    p.add(phone);
    p.add(new JLabel(SwingSupport.text("gui.email")));
    p.add(email);
    if (!AppDialog.form(this,
            SwingSupport.text(client == null ? "gui.new_client" : "gui.edit_client"),
            SwingSupport.text("gui.client_form_help"), p, SwingSupport.text("gui.save")))
      return;
    try {
      if (name.getText().isBlank() || cpf.getText().isBlank())
        throw new IllegalArgumentException();
      ClienteDAO dao = new ClienteDAO();
      if (client == null) {
        dao.salvar(new Cliente(name.getText().trim(), cpf.getText().trim(), phone.getText().trim(),
            email.getText().trim()));
        SwingSupport.success(this, SwingSupport.text("gui.client_saved"));
      } else {
        client = new Cliente(client.getId(), name.getText().trim(), cpf.getText().trim(),
            phone.getText().trim(), email.getText().trim(), client.isAtivo());
        dao.atualizar(client);
        SwingSupport.success(this, SwingSupport.text("gui.client_updated"));
      }
      showPage("clients");
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void deleteVehicle(JTable table) {
    int id = selectedId(table);
    if (id <= 0)
      return;
    if (!allowed(AutorizacaoService.Permissao.EXCLUIR_VEICULO)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    if (!SwingSupport.confirm(this, SwingSupport.text("gui.confirm_delete_vehicle")))
      return;
    try {
      if (!new VeiculoDAO().deletar(id)) {
        SwingSupport.error(this, SwingSupport.text("gui.record_not_found"));
        return;
      }
      SwingSupport.success(this, SwingSupport.text("gui.vehicle_deleted"));
      showPage("vehicles");
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void editVehicle(JTable table) {
    int id = selectedId(table);
    if (id <= 0 || !allowed(AutorizacaoService.Permissao.ATUALIZAR_VEICULO)) {
      if (id > 0)
        SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    try {
      Veiculo selected = new VeiculoDAO().buscarPorId(id);
      if (selected != null)
        vehicleDialog(selected);
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void editClient(JTable table) {
    int id = selectedId(table);
    if (id <= 0 || !allowed(AutorizacaoService.Permissao.ACESSAR_CLIENTE_LGPD)) {
      if (id > 0)
        SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    try {
      Cliente selected = new ClienteDAO().buscarPorId(id);
      if (selected != null)
        clientDialog(selected);
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void anonymizeClient(JTable table) {
    int id = selectedId(table);
    if (id <= 0 || !allowed(AutorizacaoService.Permissao.ACESSAR_CLIENTE_LGPD)) {
      if (id > 0)
        SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    if (!SwingSupport.confirm(this, SwingSupport.text("gui.confirm_anonymize_client")))
      return;
    try {
      if (!new ClienteService().anonimizar(id)) {
        SwingSupport.error(this, SwingSupport.text("gui.record_not_found"));
        return;
      }
      SwingSupport.success(this, SwingSupport.text("gui.client_anonymized"));
      showPage("clients");
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private int selectedId(JTable table) {
    int viewRow = table.getSelectedRow();
    if (viewRow < 0) {
      SwingSupport.error(this, SwingSupport.text("gui.select_record"));
      return -1;
    }
    return Integer.parseInt(table.getValueAt(viewRow, 0).toString());
  }
  private void saleDialog() {
    try {
      List<Cliente> cs = new ClienteDAO().listar();
      cs.removeIf(client -> !client.isAtivo());
      List<Veiculo> vs = new VeiculoDAO().listarTodos();
      if (cs.isEmpty() || vs.stream().noneMatch(v -> !"VENDIDO".equalsIgnoreCase(v.getStatus()))) {
        SwingSupport.error(this, SwingSupport.text("gui.sale_prerequisites"));
        return;
      }
      AppComboBox<String> c = new AppComboBox<>(new String[0]);
      AppComboBox<String> v = new AppComboBox<>(new String[0]);
      for (Cliente x : cs) c.addItem(x.getId() + " · " + x.getNome());
      for (Veiculo x : vs)
        if (!"VENDIDO".equalsIgnoreCase(x.getStatus()))
          v.addItem(x.getId() + " · " + x.getMarca() + " " + x.getModelo());
      JTextField price = new JTextField();
      SwingSupport.field(price);
      JPanel p = formPanel(new GridLayout(0, 2, 10, 10));
      p.add(new JLabel(SwingSupport.text("gui.client")));
      p.add(c);
      p.add(new JLabel(SwingSupport.text("gui.vehicle")));
      p.add(v);
      p.add(new JLabel(SwingSupport.text("gui.price")));
      p.add(price);
      if (!AppDialog.form(this, SwingSupport.text("gui.new_sale"),
              SwingSupport.text("gui.sale_form_help"), p, SwingSupport.text("gui.register_sale")))
        return;
      int ci = Integer.parseInt(c.getSelectedItem().toString().split(" ")[0]),
          vi = Integer.parseInt(v.getSelectedItem().toString().split(" ")[0]);
      double salePrice = Double.parseDouble(price.getText().trim().replace(',', '.'));
      if (salePrice <= 0)
        throw new IllegalArgumentException();
      new VendaService().realizarVenda(ci, vi, salePrice);
      SwingSupport.success(this, SwingSupport.text("gui.sale_saved"));
      showPage("sales");
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void orderDialog() {
    try {
      List<Cliente> cs = new ClienteDAO().listar();
      cs.removeIf(client -> !client.isAtivo());
      List<Veiculo> vs = new VeiculoDAO().listarTodos();
      if (cs.isEmpty() || vs.isEmpty()) {
        SwingSupport.error(this, SwingSupport.text("gui.order_prerequisites"));
        return;
      }
      AppComboBox<String> c = new AppComboBox<>(new String[0]);
      AppComboBox<String> v = new AppComboBox<>(new String[0]);
      for (Cliente x : cs) c.addItem(x.getId() + " · " + x.getNome());
      for (Veiculo x : vs) v.addItem(x.getId() + " · " + x.getMarca() + " " + x.getModelo());
      JPanel p = formPanel(new GridLayout(0, 2, 10, 10));
      p.add(new JLabel(SwingSupport.text("gui.client")));
      p.add(c);
      p.add(new JLabel(SwingSupport.text("gui.vehicle")));
      p.add(v);
      if (!AppDialog.form(this, SwingSupport.text("gui.new_order"),
              SwingSupport.text("gui.order_form_help"), p, SwingSupport.text("gui.open_order")))
        return;
      new OrdemServicoDAO().inserirOrdem(
          new OrdemServico(Integer.parseInt(c.getSelectedItem().toString().split(" ")[0]),
              Integer.parseInt(v.getSelectedItem().toString().split(" ")[0]), LocalDate.now(),
              "ABERTA"));
      SwingSupport.success(this, SwingSupport.text("gui.order_saved"));
      showPage("workshop");
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private JPanel tablePage(String subtitle, JTable table, JComponent actions) {
    JPanel p = page(subtitle);
    actions.setOpaque(false);
    JPanel head = new JPanel(new BorderLayout());
    head.setOpaque(false);
    head.add(actions, BorderLayout.EAST);
    p.add(head, BorderLayout.SOUTH);
    JPanel body = new JPanel(new BorderLayout(0, 8));
    body.setOpaque(false);
    body.add(new JScrollPane(table), BorderLayout.CENTER);
    if (table.getRowCount() == 0) {
      JLabel empty = AppTheme.muted(SwingSupport.text("gui.empty"));
      empty.setHorizontalAlignment(SwingConstants.CENTER);
      body.add(empty, BorderLayout.SOUTH);
    }
    p.add(body, BorderLayout.CENTER);
    return p;
  }
  private JPanel vehicles() {
    JTable t = AppTable.create(
        new String[] {"ID", SwingSupport.text("gui.plate"), SwingSupport.text("gui.brand"),
            SwingSupport.text("gui.model"), SwingSupport.text("gui.year"),
            SwingSupport.text("gui.price"), SwingSupport.text("gui.status")});
    try {
      for (Veiculo v : new VeiculoDAO().listarTodos())
        AppTable.model(t).addRow(new Object[] {v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(),
            v.getAno(), I18nUtils.formatCurrency(v.getPreco()),
            I18nUtils.formatVehicleStatus(v.getStatus())});
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.setOpaque(false);
    if (allowed(AutorizacaoService.Permissao.ATUALIZAR_VEICULO)) {
      JButton edit = new AppButton(SwingSupport.text("gui.edit"), AppButton.Kind.SECONDARY);
      edit.addActionListener(e -> editVehicle(t));
      actions.add(edit);
    }
    if (allowed(AutorizacaoService.Permissao.EXCLUIR_VEICULO)) {
      JButton delete = new AppButton(SwingSupport.text("gui.delete"), AppButton.Kind.DANGER);
      delete.addActionListener(e -> deleteVehicle(t));
      actions.add(delete);
    }
    actions.add(
        button("gui.new_vehicle", "vehicles", AutorizacaoService.Permissao.CADASTRAR_VEICULO));
    return tablePage(SwingSupport.text("gui.vehicles_subtitle"), t, actions);
  }
  private JPanel clients() {
    JTable t = AppTable.create(new String[] {"ID", SwingSupport.text("gui.name"),
        SwingSupport.text("gui.cpf"), SwingSupport.text("gui.phone"),
        SwingSupport.text("gui.email"), SwingSupport.text("gui.status")});
    try {
      for (Cliente c : new ClienteDAO().listar())
        AppTable.model(t).addRow(
            new Object[] {c.getId(), c.getNome(), c.getCpf(), c.getTelefone(), c.getEmail(),
                c.isAtivo() ? SwingSupport.text("gui.active") : SwingSupport.text("gui.inactive")});
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.setOpaque(false);
    if (allowed(AutorizacaoService.Permissao.ACESSAR_CLIENTE_LGPD)) {
      JButton edit = new AppButton(SwingSupport.text("gui.edit"), AppButton.Kind.SECONDARY);
      edit.addActionListener(e -> editClient(t));
      actions.add(edit);
    }
    if (allowed(AutorizacaoService.Permissao.ACESSAR_CLIENTE_LGPD)) {
      JButton anonymize = new AppButton(SwingSupport.text("gui.anonymize"), AppButton.Kind.DANGER);
      anonymize.addActionListener(e -> anonymizeClient(t));
      actions.add(anonymize);
    }
    actions.add(
        button("gui.new_client", "clients", AutorizacaoService.Permissao.CADASTRAR_CLIENTE));
    return tablePage(SwingSupport.text("gui.clients_subtitle"), t, actions);
  }
  private JPanel sales() {
    JTable t = AppTable.create(
        new String[] {"ID", SwingSupport.text("gui.client"), SwingSupport.text("gui.vehicle"),
            SwingSupport.text("gui.price"), SwingSupport.text("gui.date")});
    if (allowed(AutorizacaoService.Permissao.LISTAR_VENDAS)) {
      try {
        Map<Integer, String> clients = new HashMap<>();
        if (allowed(AutorizacaoService.Permissao.LISTAR_CLIENTES))
          for (Cliente client : new ClienteDAO().listar())
            clients.put(client.getId(), client.getNome());
        Map<Integer, String> vehicles = new HashMap<>();
        if (allowed(AutorizacaoService.Permissao.LISTAR_VEICULOS))
          for (Veiculo vehicle : new VeiculoDAO().listarTodos())
            vehicles.put(vehicle.getId(), vehicle.getMarca() + " " + vehicle.getModelo());
        for (Venda sale : new VendaDAO().listarVendas())
          AppTable.model(t).addRow(new Object[] {sale.getId(),
              clients.getOrDefault(sale.getIdCliente(), "#" + sale.getIdCliente()),
              vehicles.getOrDefault(sale.getIdVeiculo(), "#" + sale.getIdVeiculo()),
              I18nUtils.formatCurrency(sale.getValorTotal()),
              I18nUtils.formatDate(sale.getDataVenda())});
      } catch (SQLException e) {
        SwingSupport.error(this, SwingSupport.text("gui.database_error"));
      }
    }
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.setOpaque(false);
    if (allowed(AutorizacaoService.Permissao.REALIZAR_VENDA))
      actions.add(button("gui.new_sale", "sales", AutorizacaoService.Permissao.REALIZAR_VENDA));
    return tablePage(SwingSupport.text("gui.sales_subtitle"), t, actions);
  }
  private JPanel workshop() {
    JPanel page = page(SwingSupport.text("gui.workshop_subtitle"));
    JPanel content = new JPanel(new GridLayout(1, 2, 16, 0));
    content.setOpaque(false);
    JTable services = AppTable.create(
        new String[] {"ID", SwingSupport.text("gui.service"), SwingSupport.text("gui.price")});
    JTable orders = AppTable.create(
        new String[] {"ID", SwingSupport.text("gui.client"), SwingSupport.text("gui.vehicle"),
            SwingSupport.text("gui.date"), SwingSupport.text("gui.status")});
    try {
      for (Servico service : new ServicoDAO().listar())
        AppTable.model(services).addRow(new Object[] {
            service.getId(), service.getNome(), I18nUtils.formatCurrency(service.getPreco())});
      Map<Integer, String> clients = new HashMap<>();
      for (Cliente client : new ClienteDAO().listar())
        clients.put(client.getId(), client.getNome());
      Map<Integer, String> vehicles = new HashMap<>();
      for (Veiculo vehicle : new VeiculoDAO().listarTodos())
        vehicles.put(vehicle.getId(), vehicle.getMarca() + " " + vehicle.getModelo());
      for (OrdemServico order : new OrdemServicoDAO().listar())
        AppTable.model(orders).addRow(new Object[] {order.getId(),
            clients.getOrDefault(order.getClienteId(), "#" + order.getClienteId()),
            vehicles.getOrDefault(order.getVeiculoId(), "#" + order.getVeiculoId()),
            I18nUtils.formatDate(order.getDataAbertura()),
            I18nUtils.formatServiceOrderStatus(order.getStatus())});
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    JPanel servicePanel = AppTheme.card();
    servicePanel.setLayout(new BorderLayout(0, 10));
    servicePanel.add(AppTheme.section(SwingSupport.text("gui.services")), BorderLayout.NORTH);
    servicePanel.add(new JScrollPane(services), BorderLayout.CENTER);
    JPanel serviceActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    serviceActions.setOpaque(false);
    JButton addService =
        new AppButton(SwingSupport.text("gui.new_service"), AppButton.Kind.PRIMARY);
    addService.addActionListener(e -> serviceDialog());
    serviceActions.add(addService);
    servicePanel.add(serviceActions, BorderLayout.SOUTH);
    JPanel orderPanel = AppTheme.card();
    orderPanel.setLayout(new BorderLayout(0, 10));
    orderPanel.add(AppTheme.section(SwingSupport.text("gui.service_orders")), BorderLayout.NORTH);
    orderPanel.add(new JScrollPane(orders), BorderLayout.CENTER);
    JPanel orderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    orderActions.setOpaque(false);
    JButton refresh = new AppButton(SwingSupport.text("gui.refresh"), AppButton.Kind.SECONDARY);
    refresh.addActionListener(e -> showPage("workshop"));
    orderActions.add(refresh);
    if (allowed(AutorizacaoService.Permissao.ACESSAR_OFICINA)) {
      JButton addItem = new AppButton(SwingSupport.text("gui.add_item"), AppButton.Kind.SECONDARY);
      addItem.addActionListener(e -> addOrderItem(orders));
      orderActions.add(addItem);
      JButton total =
          new AppButton(SwingSupport.text("gui.calculate_total"), AppButton.Kind.SECONDARY);
      total.addActionListener(e -> showOrderTotal(orders));
      orderActions.add(total);
      orderActions.add(
          button("gui.new_order", "workshop", AutorizacaoService.Permissao.ACESSAR_OFICINA));
    }
    orderPanel.add(orderActions, BorderLayout.SOUTH);
    content.add(servicePanel);
    content.add(orderPanel);
    page.add(content, BorderLayout.CENTER);
    return page;
  }
  private void serviceDialog() {
    if (!allowed(AutorizacaoService.Permissao.ACESSAR_OFICINA)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    JTextField name = new JTextField();
    JTextField price = new JTextField();
    SwingSupport.field(name);
    SwingSupport.field(price);
    JPanel form = formPanel(new GridLayout(0, 2, 10, 10));
    form.add(new JLabel(SwingSupport.text("gui.service")));
    form.add(name);
    form.add(new JLabel(SwingSupport.text("gui.price")));
    form.add(price);
    if (!AppDialog.form(this, SwingSupport.text("gui.new_service"),
            SwingSupport.text("gui.service_form_help"), form, SwingSupport.text("gui.save")))
      return;
    try {
      double value = Double.parseDouble(price.getText().trim().replace(',', '.'));
      if (name.getText().isBlank() || value <= 0)
        throw new IllegalArgumentException();
      new ServicoDAO().inserir(new Servico(name.getText().trim(), value));
      SwingSupport.success(this, SwingSupport.text("gui.service_saved"));
      showPage("workshop");
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void addOrderItem(JTable orders) {
    int orderId = selectedId(orders);
    if (orderId <= 0)
      return;
    if (!allowed(AutorizacaoService.Permissao.ACESSAR_OFICINA)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    try {
      List<Servico> available = new ServicoDAO().listar();
      if (available.isEmpty()) {
        SwingSupport.error(this, SwingSupport.text("gui.no_services"));
        return;
      }
      AppComboBox<String> serviceList = new AppComboBox<>(new String[0]);
      for (Servico service : available)
        serviceList.addItem(service.getId() + " · " + service.getNome() + " · "
            + I18nUtils.formatCurrency(service.getPreco()));
      JTextField quantity = new JTextField("1");
      SwingSupport.field(quantity);
      JPanel form = formPanel(new GridLayout(0, 2, 10, 10));
      form.add(new JLabel(SwingSupport.text("gui.service")));
      form.add(serviceList);
      form.add(new JLabel(SwingSupport.text("gui.quantity")));
      form.add(quantity);
      if (!AppDialog.form(this, SwingSupport.text("gui.add_item"),
              SwingSupport.text("gui.order_item_help"), form, SwingSupport.text("gui.add_item")))
        return;
      Servico selected = available.get(serviceList.getSelectedIndex());
      int count = Integer.parseInt(quantity.getText().trim());
      if (count <= 0)
        throw new IllegalArgumentException();
      new OrdemServicoDAO().inserirItem(
          new ItemOS(orderId, selected.getId(), count, selected.getPreco()));
      SwingSupport.success(this, SwingSupport.text("gui.item_added"));
      showPage("workshop");
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void showOrderTotal(JTable orders) {
    int orderId = selectedId(orders);
    if (orderId <= 0)
      return;
    if (!allowed(AutorizacaoService.Permissao.ACESSAR_OFICINA)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    try {
      var result = new ServicoService(new OrdemServicoDAO(), new ServicoDAO(), new VendaDAO())
                       .calcularTotalOS(orderId);
      StringBuilder details = new StringBuilder();
      for (String line : result.getDetalhes()) details.append(line).append('\n');
      details.append('\n')
          .append(SwingSupport.text("gui.discount"))
          .append(": ")
          .append(I18nUtils.formatCurrency(result.getTotalDesconto()))
          .append('\n')
          .append(SwingSupport.text("gui.warranty"))
          .append(": ")
          .append(SwingSupport.text(result.isGarantiaAtiva() ? "gui.yes" : "gui.no"))
          .append('\n')
          .append(SwingSupport.text("gui.total"))
          .append(": ")
          .append(I18nUtils.formatCurrency(result.getTotalMaoDeObra()));
      AppDialog.message(this, SwingSupport.text("gui.order_total"), details.toString());
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, e.getMessage() == null || e.getMessage().isBlank()
          ? SwingSupport.text("gui.invalid_form") : e.getMessage());
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private JPanel projects() {
    JTable t = AppTable.create(new String[] {SwingSupport.text("gui.project"),
        SwingSupport.text("gui.owner"), SwingSupport.text("gui.team"),
        SwingSupport.text("gui.status"), SwingSupport.text("gui.members")});
    try {
      for (ProjetoDAO.Resumo p : new ProjetoDAO(SwingSupport.bundle()).listar())
        AppTable.model(t).addRow(new Object[] {p.nome(), p.responsavel(), p.equipe(), p.status(),
            p.membros() == null ? "" : p.membros()});
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    actions.setOpaque(false);
    if (allowed(AutorizacaoService.Permissao.CRIAR_PROJETOS)) {
      JButton create = new AppButton(SwingSupport.text("gui.new_project"), AppButton.Kind.PRIMARY);
      create.addActionListener(e -> projectDialog(null));
      actions.add(create);
    }
    if (allowed(AutorizacaoService.Permissao.ATUALIZAR_PROJETOS)) {
      JButton edit = new AppButton(SwingSupport.text("gui.edit"), AppButton.Kind.SECONDARY);
      edit.addActionListener(e -> projectDialog(selectedProject(t)));
      actions.add(edit);
    }
    if (allowed(AutorizacaoService.Permissao.ATUALIZAR_MEMBROS)) {
      JButton members =
          new AppButton(SwingSupport.text("gui.manage_members"), AppButton.Kind.SECONDARY);
      members.addActionListener(e -> membersDialog(selectedProject(t)));
      actions.add(members);
    }
    if (allowed(AutorizacaoService.Permissao.EXCLUIR_PROJETOS)) {
      JButton delete = new AppButton(SwingSupport.text("gui.delete"), AppButton.Kind.DANGER);
      delete.addActionListener(e -> deleteProject(t));
      actions.add(delete);
    }
    actions.add(refreshButton("projects"));
    return tablePage(SwingSupport.text("gui.projects_subtitle"), t, actions);
  }
  private JButton refreshButton(String page) {
    JButton refresh = new AppButton(SwingSupport.text("gui.refresh"), AppButton.Kind.SECONDARY);
    refresh.addActionListener(e -> showPage(page));
    return refresh;
  }
  private String selectedProject(JTable table) {
    int row = table.getSelectedRow();
    if (row < 0) {
      SwingSupport.error(this, SwingSupport.text("gui.select_record"));
      return null;
    }
    return table.getValueAt(row, 0).toString();
  }
  private void projectDialog(String projectName) {
    if (projectName == null && !allowed(AutorizacaoService.Permissao.CRIAR_PROJETOS)
        || projectName != null && !allowed(AutorizacaoService.Permissao.ATUALIZAR_PROJETOS)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    JTextField name = new JTextField(projectName == null ? "" : projectName);
    JTextField owner = new JTextField();
    JTextField team = new JTextField();
    JTextField status = new JTextField(SwingSupport.text("gui.active_project_status"));
    for (JTextField field : List.of(name, owner, team, status)) SwingSupport.field(field);
    try {
      if (projectName != null) {
        for (ProjetoDAO.Resumo project : new ProjetoDAO(SwingSupport.bundle()).listar()) {
          if (project.nome().equals(projectName)) {
            owner.setText(project.responsavel());
            team.setText(project.equipe());
            status.setText(project.status());
            break;
          }
        }
      }
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
      return;
    }
    if (projectName != null)
      name.setEditable(false);
    JPanel form = formPanel(new GridLayout(0, 2, 10, 10));
    form.add(new JLabel(SwingSupport.text("gui.project")));
    form.add(name);
    form.add(new JLabel(SwingSupport.text("gui.owner")));
    form.add(owner);
    form.add(new JLabel(SwingSupport.text("gui.team")));
    form.add(team);
    form.add(new JLabel(SwingSupport.text("gui.status")));
    form.add(status);
    if (!AppDialog.form(this,
            SwingSupport.text(projectName == null ? "gui.new_project" : "gui.edit_project"),
            SwingSupport.text("gui.project_form_help"), form, SwingSupport.text("gui.save")))
      return;
    if (name.getText().isBlank() || owner.getText().isBlank() || team.getText().isBlank()
        || status.getText().isBlank()) {
      SwingSupport.error(this, SwingSupport.text("gui.invalid_form"));
      return;
    }
    try {
      ProjetoDAO dao = new ProjetoDAO(SwingSupport.bundle());
      if (projectName == null)
        dao.criarProjeto(name.getText().trim(), owner.getText().trim(), team.getText().trim(),
            status.getText().trim());
      else
        dao.atualizarProjeto(
            projectName, owner.getText().trim(), team.getText().trim(), status.getText().trim());
      SwingSupport.success(this,
          SwingSupport.text(projectName == null ? "gui.project_saved" : "gui.project_updated"));
      showPage("projects");
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void membersDialog(String projectName) {
    if (projectName == null)
      return;
    if (!allowed(AutorizacaoService.Permissao.ATUALIZAR_MEMBROS)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    JTextField members = new JTextField();
    try {
      for (ProjetoDAO.Resumo project : new ProjetoDAO(SwingSupport.bundle()).listar())
        if (project.nome().equals(projectName) && project.membros() != null)
          members.setText(project.membros());
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
      return;
    }
    JPanel memberForm = new JPanel(new BorderLayout());
    memberForm.setBackground(AppTheme.CARD);
    SwingSupport.field(members);
    memberForm.add(members, BorderLayout.CENTER);
    if (!AppDialog.form(this, SwingSupport.text("gui.manage_members"),
            SwingSupport.text("gui.members_form_help"), memberForm, SwingSupport.text("gui.save")))
      return;
    try {
      new ProjetoDAO(SwingSupport.bundle()).atualizarMembros(projectName, members.getText().trim());
      SwingSupport.success(this, SwingSupport.text("gui.members_saved"));
      showPage("projects");
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private void deleteProject(JTable table) {
    String name = selectedProject(table);
    if (name == null)
      return;
    if (!allowed(AutorizacaoService.Permissao.EXCLUIR_PROJETOS)) {
      SwingSupport.error(this, SwingSupport.text("gui.no_permission"));
      return;
    }
    if (!SwingSupport.confirm(this, SwingSupport.text("gui.confirm_delete_project")))
      return;
    try {
      new ProjetoDAO(SwingSupport.bundle()).deletarProjeto(name);
      SwingSupport.success(this, SwingSupport.text("gui.project_deleted"));
      showPage("projects");
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
  }
  private JPanel crm() {
    JPanel p = page(SwingSupport.text("gui.crm_subtitle"));
    JPanel card = AppTheme.card();
    card.setLayout(new BorderLayout(0, 12));
    card.add(AppTheme.section(SwingSupport.text("gui.crm")), BorderLayout.NORTH);
    AppComboBox<String> clients = new AppComboBox<>(new String[0]);
    try {
      for (Cliente c : new ClienteDAO().listar()) clients.addItem(c.getId() + " · " + c.getNome());
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    JLabel profile = AppTheme.muted(SwingSupport.text("gui.crm_empty"));
    JTable history = AppTable.create(new String[] {SwingSupport.text("gui.vehicle"),
        SwingSupport.text("gui.price"), SwingSupport.text("gui.date")});
    AppButton view = new AppButton(SwingSupport.text("gui.view"), AppButton.Kind.PRIMARY);
    view.addActionListener(e -> {
      try {
        if (clients.getSelectedItem() == null)
          return;
        int id = Integer.parseInt(clients.getSelectedItem().toString().split(" ")[0]);
        CRMService.Historico h = new CRMService().consultarHistorico(id);
        Map<Integer, String> vehicles = new HashMap<>();
        for (Veiculo vehicle : new VeiculoDAO().listarTodos())
          vehicles.put(vehicle.getId(), vehicle.getMarca() + " " + vehicle.getModelo());
        AppTable.model(history).setRowCount(0);
        for (Venda sale : h.vendas())
          AppTable.model(history).addRow(
              new Object[] {vehicles.getOrDefault(sale.getIdVeiculo(), "#" + sale.getIdVeiculo()),
                  I18nUtils.formatCurrency(sale.getValorTotal()),
                  I18nUtils.formatDate(sale.getDataVenda())});
        String purchaseSummary = h.vendas().isEmpty()
            ? SwingSupport.text("gui.no_purchases")
            : SwingSupport.text("gui.purchases") + ": " + h.vendas().size();
        profile.setText("<html><b>" + h.cliente().getNome() + "</b><br>" + h.cliente().getEmail()
            + " · " + h.cliente().getTelefone() + "<br>" + purchaseSummary + " · "
            + SwingSupport.text("gui.total") + ": " + I18nUtils.formatCurrency(h.total())
            + "</html>");
      } catch (IllegalArgumentException ex) {
        SwingSupport.error(this, ex.getMessage());
      } catch (SQLException ex) {
        SwingSupport.error(this, SwingSupport.text("gui.database_error"));
      }
    });
    JPanel top = new JPanel(new BorderLayout(10, 0));
    top.setOpaque(false);
    top.add(clients, BorderLayout.CENTER);
    top.add(view, BorderLayout.EAST);
    JPanel overview = new JPanel(new BorderLayout(0, 12));
    overview.setOpaque(false);
    overview.add(top, BorderLayout.NORTH);
    overview.add(profile, BorderLayout.CENTER);
    card.add(overview, BorderLayout.NORTH);
    card.add(new JScrollPane(history), BorderLayout.CENTER);
    p.add(card, BorderLayout.CENTER);
    return p;
  }
  private JPanel reports() {
    JPanel p = page(SwingSupport.text("gui.reports_subtitle"));
    JTextArea area = new JTextArea();
    area.setEditable(false);
    area.setBackground(AppTheme.CARD);
    area.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    try {
      RelatorioService r = new RelatorioService();
      StringBuilder s = new StringBuilder();
      s.append(SwingSupport.text("gui.revenue"))
          .append("\n")
          .append(
              String.join("\n", r.faturamento(LocalDate.now().withDayOfMonth(1), LocalDate.now())))
          .append("\n\n")
          .append(SwingSupport.text("gui.inventory"))
          .append("\n")
          .append(String.join("\n", r.curvaEstoque()))
          .append("\n\n")
          .append(SwingSupport.text("gui.vehicle_sales"))
          .append("\n")
          .append(String.join("\n", r.veiculacao()));
      area.setText(s.toString());
    } catch (IllegalArgumentException e) {
      area.setText(e.getMessage() == null ? SwingSupport.text("gui.invalid_form") : e.getMessage());
    } catch (SQLException e) {
      area.setText(SwingSupport.text("gui.report_error"));
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    p.add(new JScrollPane(area), BorderLayout.CENTER);
    return p;
  }
  private JPanel audit() {
    JTable table = AppTable.create(new String[] {SwingSupport.text("gui.date_time"),
        SwingSupport.text("gui.user"), SwingSupport.text("gui.action"),
        SwingSupport.text("gui.entity"), SwingSupport.text("gui.details")});
    try {
      for (LogAuditoria entry : new AuditoriaDAO().listarRecentes(100))
        AppTable.model(table).addRow(new Object[] {I18nUtils.formatDateTime(entry.getDataHora()),
            entry.getNomeUsuario(), entry.getAcao(), entry.getEntidade(), entry.getDetalhes()});
    } catch (SQLException e) {
      SwingSupport.error(this, SwingSupport.text("gui.database_error"));
    }
    return tablePage(SwingSupport.text("gui.audit_subtitle"), table, new JPanel());
  }
  private void logout() {
    SessaoUsuario.getInstancia().encerrarSessao();
    dispose();
    new LanguageFrame().setVisible(true);
  }
}
