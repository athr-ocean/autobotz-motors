package autobotz.ui;

import autobotz.dao.OrdemServicoDAO;
import autobotz.dao.ServicoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.ItemOS;
import autobotz.model.OrdemServico;
import autobotz.model.Servico;
import autobotz.service.ResultadoOS;
import autobotz.service.ServicoService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class ServicoMenu {

    private final Scanner scanner;
    private final ServicoService servicoService;
    private final ServicoDAO servicoDAO;
    private final OrdemServicoDAO ordemServicoDAO;

    public ServicoMenu(Scanner scanner) {
        this.scanner = scanner;
        this.servicoDAO = new ServicoDAO();
        this.ordemServicoDAO = new OrdemServicoDAO();
        this.servicoService = new ServicoService(
                ordemServicoDAO,
                servicoDAO,
                new VendaDAO()
        );
    }

    // Chamado pelo Main.java (Arthur integra isso no case da Oficina/OS)
    public void exibirMenu() {
        executar();
    }

    public void executar() {
        boolean sair = false;
        while (!sair) {
            System.out.println("\n===== MENU DA OFICINA / OS =====");
            System.out.println("1. Cadastrar novo tipo de servico");
            System.out.println("2. Abrir nova Ordem de Servico (OS)");
            System.out.println("3. Adicionar item a uma OS");
            System.out.println("4. Consultar itens de uma OS");
            System.out.println("5. Calcular total da OS (considera garantia)");
            System.out.println("0. Voltar");
            System.out.print("Opcao: ");

            int opcao = lerInteiro();

            switch (opcao) {
                case 1 -> cadastrarServico();
                case 2 -> abrirOrdemServico();
                case 3 -> adicionarItem();
                case 4 -> consultarOrdemServico();
                case 5 -> calcularTotal();
                case 0 -> sair = true;
                default -> System.out.println("Opcao invalida.");
            }
        }
    }

    private void cadastrarServico() {
        System.out.print("Nome do servico: ");
        String nome = scanner.nextLine();
        System.out.print("Preco (ex: 150.00): ");
        double preco = lerDouble();

        try {
            Servico servico = new Servico(nome, preco);
            servicoDAO.inserir(servico);
            System.out.println("Servico cadastrado com ID: " + servico.getId());
        } catch (SQLException e) {
            System.out.println("Erro ao cadastrar servico: " + e.getMessage());
        }
    }

    private void abrirOrdemServico() {
        System.out.print("ID do cliente: ");
        int idCliente = lerInteiro();
        System.out.print("ID do veiculo: ");
        int idVeiculo = lerInteiro();

        try {
            OrdemServico ordem = new OrdemServico(idCliente, idVeiculo, LocalDate.now(), "ABERTA");
            ordemServicoDAO.inserirOrdem(ordem);
            System.out.println("OS aberta com ID: " + ordem.getId());
        } catch (SQLException e) {
            System.out.println("Erro ao abrir OS: " + e.getMessage());
        }
    }

    private void adicionarItem() {
        System.out.print("ID da Ordem de Servico: ");
        int idOrdem = lerInteiro();
        System.out.print("ID do servico: ");
        int idServico = lerInteiro();
        System.out.print("Quantidade: ");
        int quantidade = lerInteiro();
        System.out.print("Preco unitario (ex: 150.00): ");
        double preco = lerDouble();

        try {
            ItemOS item = new ItemOS(idOrdem, idServico, quantidade, preco);
            ordemServicoDAO.inserirItem(item);
            System.out.println("Item adicionado com ID: " + item.getId());
        } catch (SQLException e) {
            System.out.println("Erro ao adicionar item: " + e.getMessage());
        }
    }

    private void consultarOrdemServico() {
        System.out.print("ID da Ordem de Servico: ");
        int idOrdem = lerInteiro();

        try {
            List<String> itens = ordemServicoDAO.buscarItensDaOrdem(idOrdem);
            if (itens.isEmpty()) {
                System.out.println("Nenhum item encontrado para essa OS.");
                return;
            }
            System.out.println("Itens da OS " + idOrdem + ":");
            for (String linha : itens) {
                System.out.println("  - " + linha);
            }
        } catch (SQLException e) {
            System.out.println("Erro ao consultar OS: " + e.getMessage());
        }
    }

    private void calcularTotal() {
        System.out.print("ID da Ordem de Servico: ");
        int idOrdem = lerInteiro();

        try {
            ResultadoOS resultado = servicoService.calcularTotalOS(idOrdem);
            imprimirResultado(resultado);
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Erro ao calcular total: " + e.getMessage());
        }
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

    private double lerDouble() {
        while (!scanner.hasNextDouble()) {
            System.out.print("Digite um valor valido: ");
            scanner.next();
        }
        double valor = scanner.nextDouble();
        scanner.nextLine();
        return valor;
    }
}
