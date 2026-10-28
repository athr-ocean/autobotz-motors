import autobotz.model.AssinaturaCliente;
import autobotz.model.ModuloOpcional;
import autobotz.model.PlanoAssinatura;
import autobotz.model.ResultadoSimulacao;
import autobotz.service.AssinaturaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AssinaturaServiceTest {
    public static void main(String[] args) {
        AssinaturaService service = new AssinaturaService();
        PlanoAssinatura base = plano(1, "Base", "100.00");
        PlanoAssinatura barato = plano(2, "Essencial", "80.00");
        PlanoAssinatura premium = plano(3, "Premium", "150.00");

        AssinaturaCliente ativa = assinatura(1, base, "ATIVA",
                LocalDate.of(2026, 1, 1), null);
        ativa.adicionarModulo(new ModuloOpcional(1, "CRM",
                new BigDecimal("20.00")));
        AssinaturaCliente cancelada = assinatura(2, base, "CANCELADA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1));

        assertString("CANCELADA", cancelada.getStatus());
        assertMoney("120.00", service.calcularMRR(
                List.of(ativa, cancelada)));
        assertEquals(50.0, service.calcularChurn(List.of(ativa, cancelada),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));

        ResultadoSimulacao resultado = service.simularMigracao(
                List.of(ativa, cancelada,
                        assinatura(3, base, "ATIVA", LocalDate.of(2026, 1, 1), null),
                        assinatura(4, base, "ATIVA", LocalDate.of(2026, 1, 1), null),
                        assinatura(5, base, "ATIVA", LocalDate.of(2026, 1, 1), null),
                        assinatura(6, base, "ATIVA", LocalDate.of(2026, 1, 1), null)),
                barato, premium);
        assertInt(1, resultado.getQuantidadeBarato());
        assertInt(3, resultado.getQuantidadePermanecem());
        assertInt(1, resultado.getQuantidadeCaro());
        assertMoney("520.00", resultado.getMrrAntes());
        assertMoney("550.00", resultado.getMrrDepois());
        System.out.println("AssinaturaServiceTest: 8 verificações aprovadas");
    }

    private static PlanoAssinatura plano(int id, String nome, String preco) {
        return new PlanoAssinatura(id, nome, new BigDecimal(preco));
    }

    private static AssinaturaCliente assinatura(int id, PlanoAssinatura plano,
            String status, LocalDate inicio, LocalDate cancelamento) {
        return new AssinaturaCliente(id, id, plano, inicio, status,
                cancelamento, null, null);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError("Esperado " + expected + ", recebido " + actual);
        }
    }

    private static void assertEquals(double expected, double actual) {
        if (Double.compare(expected, actual) != 0) {
            throw new AssertionError("Esperado " + expected + ", recebido " + actual);
        }
    }

    private static void assertInt(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Esperado " + expected + ", recebido " + actual);
        }
    }

    private static void assertString(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Esperado " + expected + ", recebido " + actual);
        }
    }
}
