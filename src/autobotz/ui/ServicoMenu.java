package autobotz.ui;

import autobotz.mock.MockItemOSDAO;
import autobotz.mock.MockOrdemServicoDAO;
import autobotz.mock.MockServicoDAO;
import autobotz.mock.MockVendaDAO;
import autobotz.model.ItemOS;
import autobotz.service.ResultadoOS;
import autobotz.service.ServicoService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Scanner;

public class ServicoMenu {

    private final Scanner scanner;
    private final ServicoService servicoService;

    public ServicoMenu(Scanner scanner) {
        this.scanner = scanner;
        // TODO: quando o Alexandre terminar os DAOs reais, troque os Mock*
        // pelos DAOs reais aqui.
        this.servicoService = new ServicoService(
                new MockOrdemServicoDAO(),
                new MockItemOSDAO(),
                new MockServicoDAO(),
                new MockVendaDAO()
        );
    }

    public void executar() {
        boolean sair = false;
        while (!sair) {
            System.out.println("\n===== MENU DA OFICINA =====");
            System.out.println("1. Calcular total de uma Ordem de Servico (dados de exemplo)");
            System.out.println("2. Testar calculo de garantia com data customizada");
            System.out.println("0. Voltar");
            System.out.print("Opcao: ");

            int opcao = lerInteiro();

            switch (opcao) {
                case 1 -> calcularOrdemExemplo();
                case 2 -> testarComDataCustomizada();
                case 0 -> sair = true;
                default -> System.out.println("Opcao invalida.");
            }
        }
    }

    private void calcularOrdemExemplo() {
        System.out.print("Digite o ID da Ordem de Servico (1 ou 2 nos dados de teste): ");
        int idOrdem = lerInteiro();

        try {
            ResultadoOS resultado = servicoService.calcularTotalOS(idOrdem);
            imprimirResultado(resultado);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private void testarComDataCustomizada() {
        System.out.print("Ha quantos meses o veiculo foi vendido? ");
        int meses = lerInteiro();
        LocalDate dataVenda = LocalDate.now().minusMonths(meses);

        // Simula uma OS com um item de revisao de R$ 400,00
        ItemOS itemRevisao = new ItemOS(99, 99, 1, 1, 400.0);

        ResultadoOS resultado = servicoService.calcularTotalOS(Arrays.asList(itemRevisao), dataVenda);
        imprimirResultado(resultado);
    }

    private void imprimirResultado(ResultadoOS resultado) {
        System.out.println("Garantia ativa: " + (resultado.isGarantiaAtiva() ? "SIM" : "NAO"));
        System.out.println("Itens:");
        for (String linha : resultado.getDetalhes()) {
            System.out.println("  - " + linha);
        }
        System.out.printf("Total mao de obra a cobrar: R$ %.2f%n", resultado.getTotalMaoDeObra());
        System.out.printf("Total de desconto (garantia): R$ %.2f%n", resultado.getTotalDesconto());
    }

    private int lerInteiro() {
        while (!scanner.hasNextInt()) {
            System.out.print("Digite um numero valido: ");
            scanner.next();
        }
        int valor = scanner.nextInt();
        scanner.nextLine();
        return valor;
    }

	public void exibirMenu() {
		// TODO Auto-generated method stub
		
	}
}