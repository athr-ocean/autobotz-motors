package autobotz.ui;

import java.time.LocalDate;
import java.util.Scanner;

import autobotz.service.ServicoService;

public class ServicoMenu {
    private final ServicoService servicoService;
    private final Scanner scanner;

    public ServicoMenu() {
        this(new Scanner(System.in));
    }

    public ServicoMenu(Scanner scanner) {
        this.servicoService = new ServicoService();
        this.scanner = scanner;
    }

    public void exibirMenu() {
        System.out.println("\n=== GARANTIA DE SERVICOS ===");
        System.out.print("Data da venda (AAAA-MM-DD): ");
        LocalDate dataVenda = LocalDate.parse(scanner.nextLine());
        System.out.print("Data do servico (AAAA-MM-DD): ");
        LocalDate dataServico = LocalDate.parse(scanner.nextLine());
        System.out.print("Valor da mao de obra: ");
        double valor = Double.parseDouble(scanner.nextLine());
        double valorCobrado = servicoService.calcularMaoDeObra(dataVenda, dataServico, valor);
        System.out.println("Mao de obra cobrada: R$ " + valorCobrado);
    }
}
