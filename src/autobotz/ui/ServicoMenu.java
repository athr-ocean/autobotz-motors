package autobotz.ui;

import autobotz.dao.OrdemServicoDAO;
import autobotz.dao.ServicoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.ItemOS;
import autobotz.model.OrdemServico;
import autobotz.model.Servico;
import autobotz.service.ResultadoOS;
import autobotz.service.ServicoService;
import autobotz.util.I18nUtils;

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
            System.out.println(I18nUtils.getString("oficina.titulo"));
            System.out.println(I18nUtils.getString("oficina.opcao1"));
            System.out.println(I18nUtils.getString("oficina.opcao2"));
            System.out.println(I18nUtils.getString("oficina.opcao3"));
            System.out.println(I18nUtils.getString("oficina.opcao4"));
            System.out.println(I18nUtils.getString("oficina.opcao5"));
            System.out.println(I18nUtils.getString("oficina.opcao6"));
            System.out.println(I18nUtils.getString("comum.voltar"));
            System.out.print(I18nUtils.getString("comum.opcao"));

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
                    default -> System.out.println(I18nUtils.getString("comum.opcao_invalida"));
                }

            } catch (SQLException | IllegalArgumentException e) {
                System.out.println(I18nUtils.getString("comum.erro") + e.getMessage());
            }
        }
    }

    private void cadastrarServico() throws SQLException {

        System.out.print(I18nUtils.getString("oficina.nome_servico"));
        String nome = scanner.nextLine().trim();

        if (nome.isBlank()) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.nome_vazio")
            );
        }

        System.out.print(I18nUtils.getString("oficina.preco_servico"));
        double preco = lerDouble();

        if (preco <= 0) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.preco_invalido")
            );
        }

        Servico servico =
                new Servico(nome, preco);

        servicoDAO.inserir(servico);

        System.out.println(
                I18nUtils.getString("oficina.servico_cadastrado")
                + servico.getId()
        );
    }

    private void abrirOrdemServico() throws SQLException {

        System.out.print(I18nUtils.getString("oficina.id_cliente"));
        int clienteId = lerInteiro();

        System.out.print(I18nUtils.getString("oficina.id_veiculo"));
        int veiculoId = lerInteiro();

        if (clienteId <= 0 || veiculoId <= 0) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.ids_invalidos")
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
                I18nUtils.getString("oficina.os_aberta")
                + ordem.getId()
        );
    }

    private void adicionarItem() throws SQLException {

        System.out.print(I18nUtils.getString("oficina.id_os"));
        int ordemId = lerInteiro();

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(ordemId);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.os_nao_encontrada")
            );
        }

        System.out.print(I18nUtils.getString("oficina.id_servico"));
        int servicoId = lerInteiro();

        Servico servico =
                servicoDAO.buscarPorId(servicoId);

        if (servico == null) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.servico_nao_encontrado")
            );
        }

        System.out.print(I18nUtils.getString("oficina.quantidade"));
        int quantidade = lerInteiro();

        if (quantidade <= 0) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.quantidade_invalida")
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
                I18nUtils.getString("oficina.item_adicionado")
                + item.getId()
        );
    }

    private void consultarOrdem() throws SQLException {

        System.out.print(I18nUtils.getString("oficina.id_os"));
        int ordemId = lerInteiro();

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(ordemId);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.os_nao_encontrada")
            );
        }

        System.out.println();
        System.out.println(I18nUtils.getString("oficina.os_prefixo") + ordem.getId());
        System.out.println(I18nUtils.getString("oficina.cliente_label") + ordem.getClienteId());
        System.out.println(I18nUtils.getString("oficina.veiculo_label") + ordem.getVeiculoId());
        System.out.println(I18nUtils.getString("oficina.data_label") + ordem.getDataAbertura());
        System.out.println(I18nUtils.getString("oficina.status_label") + I18nUtils.formatServiceOrderStatus(
                        ordem.getStatus()
                ));

        List<String> itens =
                ordemServicoDAO.buscarItensDaOrdem(ordemId);

        if (itens.isEmpty()) {
            System.out.println(I18nUtils.getString("oficina.sem_itens"));
            return;
        }

        System.out.println(I18nUtils.getString("oficina.itens"));

        for (String item : itens) {
            System.out.println(item);
        }
    }

    private void calcularOrdem() throws SQLException {

        System.out.print(I18nUtils.getString("oficina.id_os"));
        int ordemId = lerInteiro();

        OrdemServico ordem =
                ordemServicoDAO.buscarPorId(ordemId);

        if (ordem == null) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.os_nao_encontrada")
            );
        }

        double totalBruto =
                ordemServicoDAO.calcularTotal(ordemId);

        ResultadoOS resultado =
                servicoService.calcularTotalOS(ordemId);

        System.out.println(
                I18nUtils.getString("oficina.total_bruto")
                + I18nUtils.formatCurrency(totalBruto)
        );

        imprimirResultado(resultado);
    }

    private void testarComDataCustomizada() throws SQLException {

        System.out.print(
                I18nUtils.getString("oficina.meses_venda")
        );

        int meses = lerInteiro();

        System.out.print(
                I18nUtils.getString("oficina.id_revisao")
        );

        int servicoId = lerInteiro();

        Servico servico =
                servicoDAO.buscarPorId(servicoId);

        if (servico == null) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("oficina.servico_nao_encontrado")
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
                I18nUtils.getString("oficina.garantia_ativa")
                + (resultado.isGarantiaAtiva()
                    ? I18nUtils.getString("comum.sim")
                    : I18nUtils.getString("comum.nao"))
        );

        System.out.println(I18nUtils.getString("oficina.itens"));

        for (String linha : resultado.getDetalhes()) {
            System.out.println(linha);
        }

        System.out.println(
                I18nUtils.getString("oficina.total_mao_obra")
                + I18nUtils.formatCurrency(
                        resultado.getTotalMaoDeObra()
                )
        );

        System.out.println(
                I18nUtils.getString("oficina.total_desconto")
                + I18nUtils.formatCurrency(
                        resultado.getTotalDesconto()
                )
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
                        I18nUtils.getString("comum.inteiro_invalido")
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
                        I18nUtils.getString("comum.decimal_invalido")
                );
            }
        }
    }

    public void exibirMenu() {
        executar();
    }
}
