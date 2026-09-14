package autobotz.ui;

import autobotz.service.RelatorioService;
import java.util.Scanner;

public class RelatorioMenu {
    private RelatorioService service;

    public RelatorioMenu() {
        this.service = new RelatorioService();
    }

    public void exibirMenu() {
        Scanner scanner = new Scanner(System.in);
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n=== MÓDULO DE BI & AUDITORIA ===");
            System.out.println("1. Relatório de Faturamento Mensal");
            System.out.println("2. Relatório de Veículos Encalhados");
            System.out.println("0. Voltar ao Menu Principal");
            System.out.print("Escolha uma opção: ");
            
            opcao = scanner.nextInt();

            switch (opcao) {
                case 1:
                    service.gerarRelatorioFaturamentoMensal();
                    break;
                case 2:
                    System.out.print("Informe a tolerância de dias (ex: 90): ");
                    int dias = scanner.nextInt();
                    service.listarVeiculosEncalhados(dias);
                    break;
                case 0:
                    System.out.println("Voltando...");
                    break;
                default:
                    System.out.println("Opção inválida!");
            }
        }
    }
}