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
    private final OrdemServicoDAO ordemServicoDAO;
    private final ServicoDAO servicoDAO;
    private final ServicoService servicoService;

    public ServicoMenu(Scanner scanner) {
        this.scanner = scanner;

        this.ordemServicoDAO = new OrdemServicoDAO();
        this.servicoDAO = new ServicoDAO();

        this.servicoService =
                new ServicoService(
                        ordemServicoDAO,
                        servicoDAO,
                        new VendaDAO()
                );
    }

    public void executar() {
        boolean sair = false;

        while (!sair) {

            System.out.println();
            System.out.println("===== MENU DA OFICINA =====");
            System.out.println("1. Cadastrar servico");
            System.out.println("2. Abrir Ordem de Servico");
            System.out.println("3. Adicionar item a Ordem de Servico");
            System.out.println("4. Consultar Ordem de Servico");
            System.out.println("5. Calcular total e garantia");
            System.out.println("6. Testar regra de garantia");
            System.out.println("0. Voltar");
            System.out.print("Opcao: ");

            int opcao = lerInteiro();

            try {
                switch (opcao) {
                    case 1 -> cadastrarServico();
                    case 2 -> abrirOrdemServico();
                    case 3 -> adicionarItem();
                    case 4 -> consultarOrdem();
                    case 5 -> calcularOrdem();
                    case 6 -> testarComDataCustomizada();
                    case 0 -> sair = true;
                    default -> System.out.println("Opcao invalida.");
                }

            } catch (SQLException | IllegalArgumentException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private void cadastrarServico() throws SQLException {

        System.out.print("Nome do servico: ");
        String nome = scanner.nextLine().trim();

        if (nome.isBlank()) {
            throw new IllegalArgumentException(
                    "O nome do servico nao pode ser vazio."
            );
        }

        System.out.print("Preco do servico: ");
        double preco = lerDouble();

        if (preco <= 0) {
            throw new IllegalArgumentException(
                    "O preco deve ser maior que zero."
            );
        }

        Servico servico =
                new Servico(nome, preco);

        servicoDAO.inserir(servico);

        System.out.println(
                "Servico cadastrado com sucesso. ID: "
                + servico.getId()
        );
    }

    private void abrirOrdemServico() throws SQLException {

        System.out.print("ID do cliente: ");
        int clienteId = lerInteiro();

        System.out.print("ID do veiculo: ");
        int veiculoId = lerInteiro();

        if (clienteId <= 0 || veiculoId <= 0) {
            throw new IllegalArgumentException(
                    "Cliente e veiculo devem possuir IDs validos."
            );
        }

        OrdemServico ordem =
                new OrdemServico(
                        clienteId,
                        veiculoId,
                        LocalDate.now(),
                        "ABERTA"
                );

        ordemServicoDAO.inserirOrdem(ordem);

        System.out.println(
                "Ordem de Servico aberta com sucesso. ID: "
                + ordem.getId()
        );
    }

    private void adicionarItem() throws SQLException {

        System.out.print("ID da Ordem de Servico: ");
        int ordemId = lerInteiro();

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(ordemId);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    "Ordem de Servico nao encontrada."
            );
        }

        System.out.print("ID do servico: ");
        int servicoId = lerInteiro();

        Servico servico =
                servicoDAO.buscarPorId(servicoId);

        if (servico == null) {
            throw new IllegalArgumentException(
                    "Servico nao encontrado."
            );
        }

        System.out.print("Quantidade: ");
        int quantidade = lerInteiro();

        if (quantidade <= 0) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser maior que zero."
            );
        }

        ItemOS item =
                new ItemOS(
                        ordemId,
                        servicoId,
                        quantidade,
                        servico.getPreco()
                );

        ordemServicoDAO.inserirItem(item);

        System.out.println(
                "Item adicionado com sucesso. ID: "
                + item.getId()
        );
    }

    private void consultarOrdem() throws SQLException {

        System.out.print("ID da Ordem de Servico: ");
        int ordemId = lerInteiro();

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(ordemId);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    "Ordem de Servico nao encontrada."
            );
        }

        System.out.println();
        System.out.println("OS #" + ordem.getId());
        System.out.println("Cliente: " + ordem.getClienteId());
        System.out.println("Veiculo: " + ordem.getVeiculoId());
        System.out.println("Data de abertura: " + ordem.getDataAbertura());
        System.out.println("Status: " + ordem.getStatus());

        List<String> itens =
                ordemServicoDAO.buscarItensDaOrdem(ordemId);

        if (itens.isEmpty()) {
            System.out.println("Nenhum item cadastrado nesta OS.");
            return;
        }

        System.out.println("Itens:");

        for (String item : itens) {
            System.out.println("  " + item);
        }
    }

    private void calcularOrdem() throws SQLException {

        System.out.print("ID da Ordem de Servico: ");
        int ordemId = lerInteiro();

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(ordemId);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    "Ordem de Servico nao encontrada."
            );
        }

        double totalBruto =
                ordemServicoDAO.calcularTotal(ordemId);

        ResultadoOS resultado =
                servicoService.calcularTotalOS(ordemId);

        System.out.printf(
                "Total bruto da OS: R$ %.2f%n",
                totalBruto
        );

        imprimirResultado(resultado);
    }

    private void testarComDataCustomizada() throws SQLException {

        System.out.print(
                "Ha quantos meses o veiculo foi vendido? "
        );

        int meses = lerInteiro();

        System.out.print(
                "ID de um servico de revisao cadastrado: "
        );

        int servicoId = lerInteiro();

        Servico servico =
                servicoDAO.buscarPorId(servicoId);

        if (servico == null) {
            throw new IllegalArgumentException(
                    "Servico nao encontrado."
            );
        }

        LocalDate dataVenda =
                LocalDate.now().minusMonths(meses);

        ItemOS itemRevisao =
                new ItemOS(
                        0,
                        servicoId,
                        1,
                        servico.getPreco()
                );

        ResultadoOS resultado =
                servicoService.calcularTotalOS(
                        List.of(itemRevisao),
                        dataVenda
                );

        imprimirResultado(resultado);
    }

    private void imprimirResultado(ResultadoOS resultado) {

        System.out.println(
                "Garantia ativa: "
                + (resultado.isGarantiaAtiva()
                    ? "SIM"
                    : "NAO")
        );

        System.out.println("Itens:");

        for (String linha : resultado.getDetalhes()) {
            System.out.println("  - " + linha);
        }

        System.out.printf(
                "Total mao de obra a cobrar: R$ %.2f%n",
                resultado.getTotalMaoDeObra()
        );

        System.out.printf(
                "Total de desconto por garantia: R$ %.2f%n",
                resultado.getTotalDesconto()
        );
    }

    private int lerInteiro() {

        while (true) {
            try {
                return Integer.parseInt(
                        scanner.nextLine().trim()
                );

            } catch (NumberFormatException e) {
                System.out.print(
                        "Digite um numero inteiro valido: "
                );
            }
        }
    }

    private double lerDouble() {

        while (true) {
            try {
                String valor =
                        scanner.nextLine()
                                .trim()
                                .replace(',', '.');

                return Double.parseDouble(valor);

            } catch (NumberFormatException e) {
                System.out.print(
                        "Digite um valor numerico valido: "
                );
            }
        }
    }

    public void exibirMenu() {
        executar();
    }
}
