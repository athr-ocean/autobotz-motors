package autobotz.ui;

import autobotz.dao.OrdemServicoDAO;
import autobotz.dao.ServicoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.ItemOS;
import autobotz.service.ResultadoOS;
import autobotz.service.ServicoService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class ServicoMenu {

    private final Scanner scanner;
    private final ServicoService servicoService;

    public ServicoMenu(Scanner scanner) {

        this.scanner = scanner;

        /*
         * Mantem o menu e a regra de negocio originais
         * usando agora os DAOs reais da equipe.
         */
        this.servicoService =
                new ServicoService(
                        new OrdemServicoDAO(),
                        new ServicoDAO(),
                        new VendaDAO()
                );
    }

    public void executar() {

        boolean sair = false;

        while (!sair) {

            System.out.println(
                    "\n===== MENU DA OFICINA ====="
            );

            System.out.println(
                    "1. Calcular total de uma Ordem de Servico"
            );

            System.out.println(
                    "2. Testar calculo de garantia "
                    + "com data customizada"
            );

            System.out.println(
                    "0. Voltar"
            );

            System.out.print(
                    "Opcao: "
            );

            int opcao = lerInteiro();

            switch (opcao) {

                case 1 ->
                    calcularOrdem();

                case 2 ->
                    testarComDataCustomizada();

                case 0 ->
                    sair = true;

                default ->
                    System.out.println(
                            "Opcao invalida."
                    );
            }
        }
    }

    private void calcularOrdem() {

        System.out.print(
                "Digite o ID da Ordem de Servico: "
        );

        int idOrdem =
                lerInteiro();

        try {

            ResultadoOS resultado =
                    servicoService
                            .calcularTotalOS(
                                    idOrdem
                            );

            imprimirResultado(
                    resultado
            );

        } catch (SQLException
                 | IllegalArgumentException e) {

            System.out.println(
                    "Erro: "
                    + e.getMessage()
            );
        }
    }

    private void testarComDataCustomizada() {

        System.out.print(
                "Ha quantos meses "
                + "o veiculo foi vendido? "
        );

        int meses =
                lerInteiro();

        System.out.print(
                "ID de um servico "
                + "de revisao cadastrado: "
        );

        int idServico =
                lerInteiro();

        LocalDate dataVenda =
                LocalDate.now()
                        .minusMonths(meses);

        /*
         * Mantem o teste criado para validar a regra
         * de garantia, agora usando um servico real.
         */
        ItemOS itemRevisao =
                new ItemOS(
                        0,
                        idServico,
                        1,
                        400.0
                );

        try {

            ResultadoOS resultado =
                    servicoService
                            .calcularTotalOS(
                                    List.of(
                                            itemRevisao
                                    ),
                                    dataVenda
                            );

            imprimirResultado(
                    resultado
            );

        } catch (SQLException
                 | IllegalArgumentException e) {

            System.out.println(
                    "Erro: "
                    + e.getMessage()
            );
        }
    }

    private void imprimirResultado(
            ResultadoOS resultado) {

        System.out.println(
                "Garantia ativa: "
                + (
                    resultado.isGarantiaAtiva()
                        ? "SIM"
                        : "NAO"
                )
        );

        System.out.println(
                "Itens:"
        );

        for (String linha :
                resultado.getDetalhes()) {

            System.out.println(
                    "  - " + linha
            );
        }

        System.out.printf(
                "Total mao de obra a cobrar: "
                + "R$ %.2f%n",
                resultado
                        .getTotalMaoDeObra()
        );

        System.out.printf(
                "Total de desconto (garantia): "
                + "R$ %.2f%n",
                resultado
                        .getTotalDesconto()
        );
    }

    private int lerInteiro() {

        while (!scanner.hasNextInt()) {

            System.out.print(
                    "Digite um numero valido: "
            );

            scanner.next();
        }

        int valor =
                scanner.nextInt();

        scanner.nextLine();

        return valor;
    }

    /*
     * Mantido por compatibilidade com chamadas
     * existentes de versões anteriores.
     */
    public void exibirMenu() {
        executar();
    }
}
