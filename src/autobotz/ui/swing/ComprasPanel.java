package autobotz.ui.swing;

import autobotz.SessaoUsuario;
import autobotz.model.Fornecedor;
import autobotz.model.OrcamentoCompra;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.service.ComprasException;
import autobotz.service.ComprasService;
import autobotz.ui.swing.components.AppButton;
import autobotz.ui.swing.components.AppComboBox;
import autobotz.ui.swing.theme.AppTheme;
import autobotz.util.I18nUtils;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/** Formulário Swing para o fluxo D11. A autorização é fornecida pelo chamador. */
@SuppressWarnings("serial")
final class ComprasPanel extends JPanel {
  private final ComprasService service;

  private final JTextField fornecedorNome = field();
  private final JTextField fornecedorCnpj = field();
  private final JLabel fornecedorResult = resultLabel();
  private final JTextField orcamentoNome = field();
  private final JTextField orcamentoSaldo = field();
  private final JLabel orcamentoResult = resultLabel();

  private final AppComboBox<OpcaoId> orcamentos = new AppComboBox<>(new OpcaoId[0]);
  private final AppComboBox<OpcaoId> fornecedores = new AppComboBox<>(new OpcaoId[0]);
  private final JTextField solicitacaoDescricao = field();
  private final JTextField solicitacaoValor = field();
  private final JTextField solicitante = field();
  private final JLabel solicitacaoResult = resultLabel();

  private final AppComboBox<OpcaoId> solicitacoes = new AppComboBox<>(new OpcaoId[0]);
  private final JTextField aprovador = field();
  private final JTextField motivoRejeicao = field();
  private final JLabel decisaoResult = resultLabel();

  ComprasPanel(ComprasService service) {
    this.service = service;
    setLayout(new BorderLayout(0, 16));
    setBackground(AppTheme.BACKGROUND);
    setBorder(BorderFactory.createEmptyBorder(22, 28, 24, 28));
    add(AppTheme.muted(SwingSupport.text("gui.purchases_subtitle")), BorderLayout.NORTH);

    var tabs = new JTabbedPane();
    tabs.addTab(SwingSupport.text("gui.purchase_setup_tab"), cadastros());
    tabs.addTab(SwingSupport.text("gui.purchase_request_tab"), solicitar());
    tabs.addTab(SwingSupport.text("gui.purchase_approval_tab"), aprovarRejeitarConsultar());
    add(tabs, BorderLayout.CENTER);

    String usuario = SessaoUsuario.getInstancia().getUsuario().getNomeUsuario();
    solicitante.setText(usuario);
    aprovador.setText(usuario);
    recarregarOpcoes(false);
  }

  private JPanel cadastros() {
    JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
    grid.setBackground(AppTheme.BACKGROUND);

    JPanel fornecedorFields = fieldsPanel();
    fornecedorFields.add(fieldRow("gui.purchase_supplier_name", fornecedorNome));
    fornecedorFields.add(fieldRow("gui.purchase_cnpj_optional", fornecedorCnpj));
    AppButton salvarFornecedor = new AppButton(
        SwingSupport.text("gui.purchase_save_supplier"), AppButton.Kind.PRIMARY);
    salvarFornecedor.addActionListener(event -> cadastrarFornecedor());
    grid.add(card(SwingSupport.text("gui.purchase_supplier"), fornecedorFields,
        salvarFornecedor, fornecedorResult));

    JPanel orcamentoFields = fieldsPanel();
    orcamentoFields.add(fieldRow("gui.purchase_budget_name", orcamentoNome));
    orcamentoFields.add(fieldRow("gui.purchase_initial_balance", orcamentoSaldo));
    AppButton salvarOrcamento = new AppButton(
        SwingSupport.text("gui.purchase_save_budget"), AppButton.Kind.PRIMARY);
    salvarOrcamento.addActionListener(event -> criarOrcamento());
    grid.add(card(SwingSupport.text("gui.purchase_budget"), orcamentoFields,
        salvarOrcamento, orcamentoResult));
    return padded(grid);
  }

  private JPanel solicitar() {
    JPanel content = new JPanel(new BorderLayout(0, 14));
    content.setBackground(AppTheme.BACKGROUND);
    JPanel card = AppTheme.card();
    card.setLayout(new BorderLayout(0, 14));
    card.add(AppTheme.section(SwingSupport.text("gui.purchase_new_request")), BorderLayout.NORTH);

    JPanel fields = new JPanel(new GridLayout(0, 2, 12, 12));
    fields.setBackground(AppTheme.CARD);
    fields.add(fieldRow("gui.purchase_budget", orcamentos));
    fields.add(fieldRow("gui.purchase_supplier", fornecedores));
    fields.add(fieldRow("gui.purchase_description", solicitacaoDescricao));
    fields.add(fieldRow("gui.purchase_amount", solicitacaoValor));
    fields.add(fieldRow("gui.purchase_requester", solicitante));
    fields.add(new JPanel());
    card.add(fields, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    actions.setOpaque(false);
    AppButton atualizar = new AppButton(SwingSupport.text("gui.refresh"), AppButton.Kind.SECONDARY);
    atualizar.addActionListener(event -> recarregarOpcoes(true));
    AppButton salvar = new AppButton(SwingSupport.text("gui.purchase_submit_request"), AppButton.Kind.PRIMARY);
    salvar.addActionListener(event -> criarSolicitacao());
    actions.add(atualizar);
    actions.add(salvar);
    card.add(actions, BorderLayout.SOUTH);
    content.add(card, BorderLayout.NORTH);
    content.add(solicitacaoResult, BorderLayout.SOUTH);
    return padded(content);
  }

  private JPanel aprovarRejeitarConsultar() {
    JPanel content = new JPanel(new BorderLayout(0, 14));
    content.setBackground(AppTheme.BACKGROUND);
    JPanel card = AppTheme.card();
    card.setLayout(new BorderLayout(0, 14));
    card.add(AppTheme.section(SwingSupport.text("gui.purchase_decide_request")), BorderLayout.NORTH);

    JPanel fields = new JPanel(new GridLayout(0, 2, 12, 12));
    fields.setBackground(AppTheme.CARD);
    fields.add(fieldRow("gui.purchase_request", solicitacoes));
    fields.add(fieldRow("gui.purchase_approver", aprovador));
    fields.add(fieldRow("gui.purchase_rejection_reason", motivoRejeicao));
    fields.add(new JPanel());
    card.add(fields, BorderLayout.CENTER);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    actions.setOpaque(false);
    AppButton atualizar = new AppButton(SwingSupport.text("gui.refresh"), AppButton.Kind.SECONDARY);
    atualizar.addActionListener(event -> recarregarOpcoes(true));
    AppButton consultar = new AppButton(SwingSupport.text("gui.purchase_consult"), AppButton.Kind.SECONDARY);
    consultar.addActionListener(event -> consultarSolicitacao());
    AppButton rejeitar = new AppButton(SwingSupport.text("gui.purchase_reject"), AppButton.Kind.DANGER);
    rejeitar.addActionListener(event -> rejeitarSolicitacao());
    AppButton aprovar = new AppButton(SwingSupport.text("gui.purchase_approve"), AppButton.Kind.PRIMARY);
    aprovar.addActionListener(event -> aprovarSolicitacao());
    actions.add(atualizar);
    actions.add(consultar);
    actions.add(rejeitar);
    actions.add(aprovar);
    card.add(actions, BorderLayout.SOUTH);

    content.add(card, BorderLayout.NORTH);
    content.add(decisaoResult, BorderLayout.SOUTH);
    return padded(content);
  }

  private void cadastrarFornecedor() {
    try {
      Fornecedor f = service.cadastrarFornecedor(fornecedorNome.getText(), fornecedorCnpj.getText());
      fornecedorResult.setText(SwingSupport.text("gui.purchase_supplier_saved") + " ID: " + f.getId());
      fornecedorNome.setText("");
      fornecedorCnpj.setText("");
      recarregarOpcoes(true);
    } catch (ComprasException e) {
      erroRegra(e);
    } catch (SQLException e) {
      erroBanco();
    }
  }

  private void criarOrcamento() {
    try {
      BigDecimal saldo = valorMonetario(orcamentoSaldo.getText());
      OrcamentoCompra o = service.criarOrcamento(orcamentoNome.getText(), saldo);
      orcamentoResult.setText(SwingSupport.text("gui.purchase_budget_saved") + " ID: " + o.getId()
          + " · " + I18nUtils.formatCurrency(o.getSaldoAtual().doubleValue(), I18nUtils.getCurrentLocale()));
      orcamentoNome.setText("");
      orcamentoSaldo.setText("");
      recarregarOpcoes(true);
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.purchase_invalid_money"));
    } catch (ComprasException e) {
      erroRegra(e);
    } catch (SQLException e) {
      erroBanco();
    }
  }

  private void criarSolicitacao() {
    OpcaoId orcamento = (OpcaoId) orcamentos.getSelectedItem();
    OpcaoId fornecedor = (OpcaoId) fornecedores.getSelectedItem();
    if (orcamento == null || fornecedor == null) {
      SwingSupport.error(this, SwingSupport.text("gui.purchase_select_budget_supplier"));
      return;
    }
    try {
      SolicitacaoCompra s = service.criarSolicitacao(orcamento.id(), fornecedor.id(),
          solicitacaoDescricao.getText(), valorMonetario(solicitacaoValor.getText()), solicitante.getText());
      solicitacaoResult.setText(SwingSupport.text("gui.purchase_request_saved") + " ID: " + s.getId()
          + " · " + s.getStatus());
      solicitacaoDescricao.setText("");
      solicitacaoValor.setText("");
      recarregarOpcoes(true);
      selecionarSolicitacao(s.getId());
    } catch (IllegalArgumentException e) {
      SwingSupport.error(this, SwingSupport.text("gui.purchase_invalid_money"));
    } catch (ComprasException e) {
      erroRegra(e);
    } catch (SQLException e) {
      erroBanco();
    }
  }

  private void aprovarSolicitacao() {
    OpcaoId selecionada = (OpcaoId) solicitacoes.getSelectedItem();
    if (selecionada == null) {
      SwingSupport.error(this, SwingSupport.text("gui.purchase_select_request"));
      return;
    }
    try {
      PedidoCompra pedido = service.aprovar(selecionada.id(), aprovador.getText());
      decisaoResult.setText(SwingSupport.text("gui.purchase_order_created") + " ID: " + pedido.getId());
      recarregarOpcoes(true);
      selecionarSolicitacao(selecionada.id());
      consultarSolicitacao();
    } catch (ComprasException e) {
      erroRegra(e);
    } catch (SQLException e) {
      erroBanco();
    }
  }

  private void rejeitarSolicitacao() {
    OpcaoId selecionada = (OpcaoId) solicitacoes.getSelectedItem();
    if (selecionada == null) {
      SwingSupport.error(this, SwingSupport.text("gui.purchase_select_request"));
      return;
    }
    try {
      SolicitacaoCompra s = service.rejeitar(selecionada.id(), aprovador.getText(), motivoRejeicao.getText());
      decisaoResult.setText(SwingSupport.text("gui.purchase_request_rejected") + " ID: " + s.getId());
      recarregarOpcoes(true);
      selecionarSolicitacao(selecionada.id());
      consultarSolicitacao();
    } catch (ComprasException e) {
      erroRegra(e);
    } catch (SQLException e) {
      erroBanco();
    }
  }

  private void consultarSolicitacao() {
    OpcaoId selecionada = (OpcaoId) solicitacoes.getSelectedItem();
    if (selecionada == null) {
      SwingSupport.error(this, SwingSupport.text("gui.purchase_select_request"));
      return;
    }
    try {
      SolicitacaoCompra s = service.consultarSolicitacao(selecionada.id());
      BigDecimal saldo = service.consultarSaldo(s.getOrcamentoId());
      PedidoCompra pedido = service.consultarPedidoDaSolicitacao(s.getId());
      String order = pedido == null ? SwingSupport.text("gui.purchase_no_order") : "#" + pedido.getId();
      decisaoResult.setText("<html>" + SwingSupport.text("gui.purchase_request") + " #" + s.getId()
          + " · " + s.getStatus() + "<br>" + SwingSupport.text("gui.purchase_current_balance") + ": "
          + I18nUtils.formatCurrency(saldo.doubleValue(), I18nUtils.getCurrentLocale())
          + "<br>" + SwingSupport.text("gui.purchase_order") + ": " + order + "</html>");
    } catch (ComprasException e) {
      erroRegra(e);
    } catch (SQLException e) {
      erroBanco();
    }
  }

  private void recarregarOpcoes(boolean reportFailure) {
    try {
      List<Fornecedor> listaFornecedores = service.listarFornecedores();
      List<OrcamentoCompra> listaOrcamentos = service.listarOrcamentos();
      List<SolicitacaoCompra> listaSolicitacoes = service.listarSolicitacoes();
      fornecedores.removeAllItems();
      for (Fornecedor f : listaFornecedores)
        fornecedores.addItem(new OpcaoId(f.getId(), f.getNome() + (f.getCnpj() == null ? "" : " · " + f.getCnpj())));
      orcamentos.removeAllItems();
      for (OrcamentoCompra o : listaOrcamentos) {
        String saldo = I18nUtils.formatCurrency(o.getSaldoAtual().doubleValue(), I18nUtils.getCurrentLocale());
        orcamentos.addItem(new OpcaoId(o.getId(), o.getNome() + " · " + saldo));
      }
      solicitacoes.removeAllItems();
      for (SolicitacaoCompra s : listaSolicitacoes)
        solicitacoes.addItem(new OpcaoId(s.getId(), "#" + s.getId() + " · " + s.getDescricao() + " · " + s.getStatus()));
    } catch (ComprasException e) {
      if (reportFailure) erroRegra(e);
    } catch (SQLException e) {
      if (reportFailure) erroBanco();
    }
  }

  private void selecionarSolicitacao(int id) {
    for (int i = 0; i < solicitacoes.getItemCount(); i++) {
      OpcaoId opcao = solicitacoes.getItemAt(i);
      if (opcao.id() == id) {
        solicitacoes.setSelectedIndex(i);
        return;
      }
    }
  }

  private void erroRegra(ComprasException e) {
    String mensagem = SwingSupport.text(e.getChaveMensagem());
    if (mensagem.equals(e.getChaveMensagem()))
      mensagem = SwingSupport.text("gui.operation_failed");
    SwingSupport.error(this, mensagem);
  }

  private void erroBanco() {
    SwingSupport.error(this, SwingSupport.text("gui.database_error"));
  }

  private static BigDecimal valorMonetario(String bruto) {
    if (bruto == null)
      throw new IllegalArgumentException("valor ausente");
    String valor = bruto.trim().replace(" ", "");
    if (valor.indexOf(',') >= 0 && valor.indexOf('.') >= 0)
      throw new IllegalArgumentException("não use separador de milhar");
    valor = valor.replace(',', '.');
    if (!valor.matches("\\d{1,10}(\\.\\d{1,2})?"))
      throw new IllegalArgumentException("formato monetário inválido");
    return new BigDecimal(valor).setScale(2);
  }

  private static JPanel padded(JPanel panel) {
    panel.setBorder(BorderFactory.createEmptyBorder(16, 14, 12, 14));
    return panel;
  }

  private static JPanel fieldsPanel() {
    JPanel p = new JPanel(new GridLayout(0, 1, 8, 8));
    p.setBackground(AppTheme.CARD);
    return p;
  }

  private static JPanel fieldRow(String labelKey, java.awt.Component component) {
    JPanel row = new JPanel(new BorderLayout(0, 5));
    row.setBackground(AppTheme.CARD);
    JLabel label = new JLabel(SwingSupport.text(labelKey));
    label.setForeground(AppTheme.TEXT);
    row.add(label, BorderLayout.NORTH);
    row.add(component, BorderLayout.CENTER);
    return row;
  }

  private static JPanel card(String title, JPanel fields, AppButton action, JLabel result) {
    JPanel card = AppTheme.card();
    card.setLayout(new BorderLayout(0, 14));
    card.add(AppTheme.section(title), BorderLayout.NORTH);
    card.add(fields, BorderLayout.CENTER);
    JPanel bottom = new JPanel(new BorderLayout(0, 9));
    bottom.setBackground(AppTheme.CARD);
    bottom.add(result, BorderLayout.NORTH);
    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    actions.setBackground(AppTheme.CARD);
    actions.add(action);
    bottom.add(actions, BorderLayout.SOUTH);
    card.add(bottom, BorderLayout.SOUTH);
    return card;
  }

  private static JTextField field() {
    JTextField field = new JTextField();
    SwingSupport.field(field);
    return field;
  }

  private static JLabel resultLabel() {
    JLabel label = AppTheme.muted(" ");
    label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
    return label;
  }

  private record OpcaoId(int id, String descricao) {
    @Override
    public String toString() {
      return "#" + id + " · " + descricao;
    }
  }
}
