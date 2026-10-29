package autobotz.ui.swing;

import autobotz.model.AssinaturaCliente;
import autobotz.model.ModuloOpcional;
import autobotz.model.PlanoAssinatura;
import autobotz.model.ResultadoSimulacao;
import autobotz.service.AssinaturaService;
import autobotz.ui.swing.theme.AppTheme;
import autobotz.util.I18nUtils;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Simulação financeira com dados exclusivamente fictícios; não grava no banco. */
@SuppressWarnings("serial")
public final class SimuladorPlanosPanel extends JPanel {
  private final JTextField clientes = campo("100");
  private final JTextField atual = campo("100.00");
  private final JTextField basico = campo("80.00");
  private final JTextField premium = campo("150.00");
  private final JCheckBox crm = new JCheckBox(SwingSupport.text("gui.plan_crm"));
  private final JCheckBox vendas = new JCheckBox(SwingSupport.text("gui.plan_sales"));
  private final JTextArea saida = new JTextArea(8, 42);

  SimuladorPlanosPanel() {
    setLayout(new BorderLayout(0, 20));
    setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
    setBackground(AppTheme.BACKGROUND);

    JPanel campos = new JPanel(new GridLayout(0, 2, 10, 12));
    campos.setBackground(AppTheme.CARD);
    campos.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));
    adicionar(campos, "gui.plan_clients", clientes);
    adicionar(campos, "gui.plan_current", atual);
    adicionar(campos, "gui.plan_basic", basico);
    adicionar(campos, "gui.plan_premium", premium);
    campos.add(crm);
    campos.add(vendas);
    JButton executar = new JButton(SwingSupport.text("gui.plan_calculate"));
    executar.addActionListener(e -> calcular());
    campos.add(executar);
    campos.add(new JLabel(SwingSupport.text("gui.plan_synthetic")));
    add(campos, BorderLayout.NORTH);

    saida.setEditable(false);
    saida.setLineWrap(true);
    saida.setWrapStyleWord(true);
    saida.setFont(AppTheme.BODY);
    saida.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    add(new JScrollPane(saida), BorderLayout.CENTER);
    calcular();
  }

  private static JTextField campo(String valor) {
    JTextField campo = new JTextField(valor);
    SwingSupport.field(campo);
    return campo;
  }

  private static void adicionar(JPanel painel, String chave, JTextField campo) {
    painel.add(new JLabel(SwingSupport.text(chave)));
    painel.add(campo);
  }

  private void calcular() {
    try {
      int n = Integer.parseInt(clientes.getText().trim());
      BigDecimal precoAtual = preco(atual);
      BigDecimal precoBasico = preco(basico);
      BigDecimal precoPremium = preco(premium);
      ResultadoSimulacao r = simular(n, precoAtual, precoBasico, precoPremium,
          crm.isSelected(), vendas.isSelected());
      var idioma = I18nUtils.getCurrentLocale();
      saida.setText(SwingSupport.text("gui.plan_synthetic") + "\n\n"
          + SwingSupport.text("gui.plan_basic") + ": " + r.getQuantidadeBarato() + " (30%)\n"
          + SwingSupport.text("gui.plan_keep") + ": " + r.getQuantidadePermanecem() + " (50%)\n"
          + SwingSupport.text("gui.plan_premium") + ": " + r.getQuantidadeCaro() + " (20%)\n\n"
          + SwingSupport.text("gui.plan_before") + ": "
          + I18nUtils.formatCurrency(r.getMrrAntes().doubleValue(), idioma) + "\n"
          + SwingSupport.text("gui.plan_after") + ": "
          + I18nUtils.formatCurrency(r.getMrrDepois().doubleValue(), idioma) + "\n"
          + SwingSupport.text("gui.plan_difference") + ": "
          + I18nUtils.formatCurrency(r.getMrrDepois().subtract(r.getMrrAntes()).doubleValue(), idioma));
    } catch (IllegalArgumentException e) {
      saida.setText(SwingSupport.text("gui.plan_invalid"));
    }
  }

  private static BigDecimal preco(JTextField campo) {
    return new BigDecimal(campo.getText().trim().replace(',', '.'));
  }

  public static ResultadoSimulacao simular(int quantidade, BigDecimal atual,
      BigDecimal barato, BigDecimal caro, boolean crm, boolean vendas) {
    if (quantidade < 1 || quantidade > 10000 || atual == null || barato == null || caro == null
        || atual.signum() <= 0 || barato.signum() < 0 || caro.signum() < 0
        || barato.compareTo(atual) >= 0 || caro.compareTo(atual) <= 0) {
      throw new IllegalArgumentException("Parâmetros inválidos para a simulação");
    }
    PlanoAssinatura planoAtual = new PlanoAssinatura(1, "Atual", atual);
    PlanoAssinatura planoBarato = new PlanoAssinatura(2, "Básico", barato);
    PlanoAssinatura planoCaro = new PlanoAssinatura(3, "Premium", caro);
    List<AssinaturaCliente> assinaturas = new ArrayList<>(quantidade);
    for (int i = 1; i <= quantidade; i++) {
      AssinaturaCliente assinatura = new AssinaturaCliente(i, i, planoAtual,
          LocalDate.of(2026, 1, 1), "ATIVA", null, null, null);
      if (crm) assinatura.adicionarModulo(new ModuloOpcional(1, "CRM", new BigDecimal("20.00")));
      if (vendas) assinatura.adicionarModulo(new ModuloOpcional(2, "Vendas avançadas", new BigDecimal("30.00")));
      assinaturas.add(assinatura);
    }
    return new AssinaturaService().simularMigracao(assinaturas, planoBarato, planoCaro);
  }
}
