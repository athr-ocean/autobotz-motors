package autobotz.ui;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import autobotz.model.LogAuditoria;
import autobotz.service.RelatorioService;
import autobotz.util.I18nUtils;

public class RelatorioMenu {
    private final RelatorioService relatorioService;
    private final Scanner scanner;

    public RelatorioMenu(Scanner scanner) {
        this.relatorioService = new RelatorioService();
        this.scanner = scanner;
    }

    public void exibirMenu() {
        System.out.println(I18nUtils.getString("relatorio.titulo"));
        System.out.println(I18nUtils.getString("relatorio.opcao1"));
        System.out.println(I18nUtils.getString("relatorio.opcao2"));
        System.out.println(I18nUtils.getString("relatorio.opcao3"));
        System.out.println(I18nUtils.getString("relatorio.opcao4"));
        System.out.println(I18nUtils.getString("comum.voltar"));
        System.out.print(I18nUtils.getString("comum.opcao"));
        try {
            int opcao = Integer.parseInt(scanner.nextLine());

            switch (opcao) {
                case 1 -> exibirFaturamento();
                case 2 -> imprimir(relatorioService.curvaEstoque());
                case 3 -> imprimir(relatorioService.veiculacao());
                case 4 -> exibirAuditoria();
                case 0 -> { }
                default -> System.out.println(I18nUtils.getString("comum.opcao_invalida"));
            }
        } catch (java.sql.SQLException | IllegalArgumentException e) {
            System.out.println(I18nUtils.getString("relatorio.erro") + e.getMessage());
        }
    }

    private void exibirFaturamento() throws java.sql.SQLException {
        System.out.print(I18nUtils.getString("relatorio.data_inicial"));
        LocalDate inicio = LocalDate.parse(scanner.nextLine());
        System.out.print(I18nUtils.getString("relatorio.data_final"));
        LocalDate fim = LocalDate.parse(scanner.nextLine());
        imprimir(relatorioService.faturamento(inicio, fim));
    }

    private void exibirAuditoria() throws java.sql.SQLException {
        System.out.print(I18nUtils.getString("relatorio.quantidade_logs"));
        int limite = Integer.parseInt(scanner.nextLine());
        for (LogAuditoria log : relatorioService.auditoriaRecente(limite)) {
            System.out.println(log.getDataHora() + " | " + log.getNomeUsuario() + " | "
                    + log.getAcao() + " | " + log.getEntidade() + " | " + log.getDetalhes());
        }
    }

    private void imprimir(List<String> linhas) {
        if (linhas.isEmpty()) System.out.println(I18nUtils.getString("relatorio.sem_dados"));
        else linhas.forEach(System.out::println);
    }
}
