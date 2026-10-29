import autobotz.model.ResultadoSimulacao;
import autobotz.ui.swing.SimuladorPlanosPanel;
import java.math.BigDecimal;

public final class D12SimuladorUITest {
  private static int verificacoes;
  public static void main(String[] args) {
    ResultadoSimulacao r = SimuladorPlanosPanel.simular(100,
        new BigDecimal("100.00"), new BigDecimal("80.00"),
        new BigDecimal("150.00"), true, false);
    check(r.getQuantidadeBarato() == 30, "30 por cento no basico");
    check(r.getQuantidadePermanecem() == 50, "50 por cento permanece");
    check(r.getQuantidadeCaro() == 20, "20 por cento no premium");
    check(r.getMrrAntes().compareTo(new BigDecimal("12000.00")) == 0, "MRR inicial com CRM");
    check(r.getMrrDepois().compareTo(new BigDecimal("12400.00")) == 0, "MRR projetado com CRM");
    recusar(0, "100", "80", "150");
    recusar(10001, "100", "80", "150");
    recusar(100, "100", "100", "150");
    recusar(100, "100", "80", "99");
    System.out.println("D12_SIMULADOR_UI_OK checks=" + verificacoes);
  }
  private static void recusar(int n, String atual, String barato, String premium) {
    boolean recusou = false;
    try {
      SimuladorPlanosPanel.simular(n, new BigDecimal(atual),
          new BigDecimal(barato), new BigDecimal(premium), false, false);
    } catch (IllegalArgumentException esperado) {
      recusou = true;
    }
    check(recusou, "entrada invalida recusada");
  }
  private static void check(boolean ok, String descricao) {
    if (!ok) throw new AssertionError(descricao);
    verificacoes++;
    System.out.println("PASS " + descricao);
  }
}
