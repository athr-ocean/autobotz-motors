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
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import java.util.ResourceBundle;

import autobotz.util.I18nUtils;

public class ServicoMenu {

    private final Scanner scanner;
    private final ServicoService servicoService;
    private final ServicoDAO servicoDAO;
    private final OrdemServicoDAO ordemServicoDAO;
    private final ResourceBundle bundle;

    public ServicoMenu(Scanner scanner, ResourceBundle bundle) {
        this.scanner = scanner;
        this.bundle = bundle;
        this.servicoDAO = new ServicoDAO();
        this.ordemServicoDAO = new OrdemServicoDAO();
        this.servicoService = new ServicoService(
                ordemServicoDAO,
                servicoDAO,
                new VendaDAO());
    }

    // Chamado pelo Main.java (Arthur integra isso no case da Oficina/OS)
    public void exibirMenu() {
        executar();
    }

    public void executar() {
        boolean sair = false;
        while (!sair) {
            System.out.println(bundle.getString("oficina.titulo"));
            System.out.println(bundle.getString("oficina.opcao1"));
            System.out.println(bundle.getString("oficina.opcao2"));
            System.out.println(bundle.getString("oficina.opcao3"));
            System.out.println(bundle.getString("oficina.opcao4"));
            System.out.println(bundle.getString("oficina.opcao5"));
            System.out.println(bundle.getString("oficina.opcao0"));
            System.out.print(bundle.getString("oficina.escolha"));

            int opcao = lerInteiro();

            switch (opcao) {
                case 1 -> cadastrarServico();
                case 2 -> abrirOrdemServico();
                case 3 -> adicionarItem();
                case 4 -> consultarOrdemServico();
                case 5 -> calcularTotal();
                case 0 -> sair = true;
                default -> System.out.println(bundle.getString("oficina.opcao_invalida"));
            }
        }
    }

    private void cadastrarServico() {
        System.out.print(bundle.getString("oficina.nome_servico"));
        String nome = scanner.nextLine();
        System.out.print(bundle.getString("oficina.preco"));
        double preco = lerDouble();

        try {
            Servico servico = new Servico(nome, preco);
            servicoDAO.inserir(servico);

            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.servico_cadastrado"),
                            servico.getId()));
        } catch (SQLException e) {

            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.erro_cadastro"),
                            e.getMessage()));
        }
    }

    private void abrirOrdemServico() {
        System.out.print(bundle.getString("oficina.id_cliente"));
        int idCliente = lerInteiro();
        System.out.print(bundle.getString("oficina.id_veiculo"));
        int idVeiculo = lerInteiro();

        try {
            OrdemServico ordem = new OrdemServico(idCliente, idVeiculo, LocalDate.now(), "ABERTA");
            ordemServicoDAO.inserirOrdem(ordem);
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.os_aberta"),
                            ordem.getId()));
        } catch (SQLException e) {
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.erro_abrir_os"),
                            e.getMessage()));

        }
    }

    private void adicionarItem() {
        System.out.print(bundle.getString("oficina.id_ordem"));
        int idOrdem = lerInteiro();
        System.out.print(bundle.getString("oficina.id_servico"));
        int idServico = lerInteiro();
        System.out.print(bundle.getString("oficina.quantidade"));
        int quantidade = lerInteiro();
        System.out.print(bundle.getString("oficina.preco_unitario"));
        double preco = lerDouble();

        try {
            ItemOS item = new ItemOS(idOrdem, idServico, quantidade, preco);
            ordemServicoDAO.inserirItem(item);
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.item_adicionado"),
                            item.getId()));
        } catch (SQLException e) {
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.erro_item"),
                            e.getMessage()));
        }
    }

    private void consultarOrdemServico() {
        System.out.print(bundle.getString("oficina.id_ordem"));
        int idOrdem = lerInteiro();

        try {
            List<String> itens = ordemServicoDAO.buscarItensDaOrdem(idOrdem);
            if (itens.isEmpty()) {
                System.out.println(bundle.getString("oficina.nenhum_item"));
                return;
            }
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.itens_os"),
                            idOrdem));
            for (String linha : itens) {
                System.out.println("  - " + linha);
            }
        } catch (SQLException e) {
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.erro_consulta"),
                            e.getMessage()));
        }
    }

    private void calcularTotal() {
        System.out.print(bundle.getString("oficina.id_ordem"));
        int idOrdem = lerInteiro();

        try {
            ResultadoOS resultado = servicoService.calcularTotalOS(idOrdem);
            imprimirResultado(resultado);
        } catch (IllegalArgumentException e) {
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.erro"),
                            e.getMessage()));
        } catch (SQLException e) {
            System.out.println(
                    MessageFormat.format(
                            bundle.getString("oficina.erro_total"),
                            e.getMessage()));
        }
    }

    private void imprimirResultado(ResultadoOS resultado) {
        String garantia = resultado.isGarantiaAtiva()
                ? bundle.getString("oficina.sim")
                : bundle.getString("oficina.nao");

        System.out.println(
                bundle.getString("oficina.garantia_ativa") + garantia);
        System.out.println(bundle.getString("oficina.itens"));
        for (String linha : resultado.getDetalhes()) {
            System.out.println("  - " + linha);
        }
        System.out.println(
                MessageFormat.format(
                        bundle.getString("oficina.total_mao_obra"),
                        I18nUtils.formatCurrency(
                                resultado.getTotalMaoDeObra(),
                                bundle.getLocale())));
        System.out.println(
                MessageFormat.format(
                        bundle.getString("oficina.total_desconto"),
                        I18nUtils.formatCurrency(
                                resultado.getTotalDesconto(),
                                bundle.getLocale())));
    }

    private int lerInteiro() {
        while (!scanner.hasNextInt()) {
            System.out.print(bundle.getString("oficina.numero_invalido"));
            scanner.next();
        }
        int valor = scanner.nextInt();
        scanner.nextLine();
        return valor;
    }

    private double lerDouble() {
        while (!scanner.hasNextDouble()) {
            System.out.print(bundle.getString("oficina.valor_invalido"));
            scanner.next();
        }
        double valor = scanner.nextDouble();
        scanner.nextLine();
        return valor;
    }
}
