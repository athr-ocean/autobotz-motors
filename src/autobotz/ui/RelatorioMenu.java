package autobotz.ui;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import autobotz.model.LogAuditoria;
import autobotz.service.RelatorioService;

public class RelatorioMenu {
    private final RelatorioService relatorioService;
    private final Scanner scanner;

    public RelatorioMenu(Scanner scanner) {
        this.relatorioService = new RelatorioService();
        this.scanner = scanner;
    }

    public void exibirMenu() {
        System.out.println("\n=== BI E AUDITORIA ===");
        System.out.println("1. Relatorio de faturamento");
        System.out.println("2. Curva de estoque");
        System.out.println("3. Veiculacao por modelo");
        System.out.println("4. Logs recentes");
        System.out.println("0. Voltar");
        System.out.print("Opcao: ");
        int opcao = Integer.parseInt(scanner.nextLine());
        try {
            switch (opcao) {
                case 1 -> exibirFaturamento();
                case 2 -> imprimir(relatorioService.curvaEstoque());
                case 3 -> imprimir(relatorioService.veiculacao());
                case 4 -> exibirAuditoria();
                case 0 -> { }
                default -> System.out.println("Opcao invalida.");
            }
        } catch (java.sql.SQLException | IllegalArgumentException e) {
            System.out.println("Nao foi possivel gerar o relatorio: " + e.getMessage());
        }
    }

    private void exibirFaturamento() throws java.sql.SQLException {
        System.out.print("Data inicial (AAAA-MM-DD): ");
        LocalDate inicio = LocalDate.parse(scanner.nextLine());
        System.out.print("Data final (AAAA-MM-DD): ");
        LocalDate fim = LocalDate.parse(scanner.nextLine());
        imprimir(relatorioService.faturamento(inicio, fim));
    }

    private void exibirAuditoria() throws java.sql.SQLException {
        System.out.print("Quantidade de logs: ");
        int limite = Integer.parseInt(scanner.nextLine());
        for (LogAuditoria log : relatorioService.auditoriaRecente(limite)) {
            System.out.println(log.getDataHora() + " | " + log.getNomeUsuario() + " | "
                    + log.getAcao() + " | " + log.getEntidade() + " | " + log.getDetalhes());
        }
    }

    private void imprimir(List<String> linhas) {
        if (linhas.isEmpty()) System.out.println("Nenhum dado encontrado.");
        else linhas.forEach(System.out::println);
    }
}
