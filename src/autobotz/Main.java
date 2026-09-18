package autobotz;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;

import autobotz.dao.ClienteDAO;
import autobotz.dao.VeiculoDAO;
import autobotz.dao.VendaDAO;
import autobotz.model.Cliente;
import autobotz.model.Veiculo;
import autobotz.model.Venda;
import autobotz.ui.RelatorioMenu;
import autobotz.ui.ClienteMenu;
import autobotz.service.VendaService;
import autobotz.util.I18nUtils;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Select Language / Selecione o Idioma:");
        System.out.println("1. Portugues (BR)");
        System.out.println("2. English (US)");
        System.out.print("Option / Opcao: ");

        int langOpcao = scanner.nextInt();
        scanner.nextLine();

        Locale locale = (langOpcao == 2)
                ? Locale.of("en", "US")
                : Locale.of("pt", "BR");

        ResourceBundle bundle = I18nUtils.getBundle(locale);

        /*
         * ==============================
         * AUTENTICAÇÃO
         * ==============================
         */

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        AuthService authService = new AuthService(usuarioDAO);
        AutorizacaoService autorizacaoService = new AutorizacaoService();
        LoginMenu loginMenu = new LoginMenu(scanner, authService);

        if (!loginMenu.executar()) {
            System.out.println("Acesso encerrado.");
            scanner.close();
            return;
        }

        /*
         * ==============================
         * DAOs E SERVIÇOS
         * ==============================
         */

        VeiculoDAO veiculoDAO = new VeiculoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        VendaDAO vendaDAO = new VendaDAO();

        VendaService vendaService = new VendaService();

        RelatorioMenu relatorioMenu = new RelatorioMenu(scanner);
        ClienteMenu clienteMenu = new ClienteMenu(scanner);

        int opcao = -1;

        while (opcao != 0) {

            System.out.println(bundle.getString("menu.titulo"));
            System.out.println(bundle.getString("menu.opcao1"));
            System.out.println(bundle.getString("menu.opcao2"));
            System.out.println(bundle.getString("menu.opcao3"));
            System.out.println(bundle.getString("menu.opcao4"));
            System.out.println(bundle.getString("menu.opcao5"));
            System.out.println(bundle.getString("menu.opcao6"));
            System.out.println(bundle.getString("menu.opcao7"));
            System.out.println(bundle.getString("menu.opcao8"));
            System.out.println(bundle.getString("menu.opcao9"));
            System.out.println(bundle.getString("menu.opcao10"));
            System.out.println(bundle.getString("menu.opcao11"));
            System.out.println(bundle.getString("menu.opcao0"));

            System.out.print(bundle.getString("menu.escolha"));

            opcao = scanner.nextInt();
            scanner.nextLine();

            try {

                switch (opcao) {

                    case 1:
                        if (!autorizacaoService.exigirPermissao( AutorizacaoService.Permissao.CADASTRAR_VEICULO)) {
                        break;
                        }


                        System.out.print(bundle.getString("veiculo.placa"));
                        String placa = scanner.nextLine();

                        System.out.print(bundle.getString("veiculo.marca"));
                        String marca = scanner.nextLine();

                        System.out.print(bundle.getString("veiculo.modelo"));
                        String modelo = scanner.nextLine();

                        System.out.print(bundle.getString("veiculo.ano"));
                        int ano = scanner.nextInt();

                        System.out.print(bundle.getString("veiculo.preco"));
                        double preco = scanner.nextDouble();

                        veiculoDAO.cadastrar(
                                new Veiculo(
                                        placa,
                                        modelo,
                                        marca,
                                        ano,
                                        preco
                                )
                        );

                        break;

                    case 2:

                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.LISTAR_VEICULOS)) {
                        break;
                        }

                        System.out.println(
                                bundle.getString("veiculo.lista_titulo")
                        );

                        for (Veiculo v : veiculoDAO.listar()) {

                            System.out.println(
                                    "ID: " + v.getId()
                                    + " | " + v.getMarca()
                                    + " " + v.getModelo()
                                    + " | "
                                    + bundle.getString("veiculo.ano_label")
                                    + v.getAno()
                                    + " | "
                                    + I18nUtils.formatCurrency(
                                            v.getPreco(),
                                            locale
                                    )
                                    + " | "
                                    + bundle.getString("veiculo.status")
                                    + v.getStatus()
                            );
                        }

                        break;

                    case 3:

                        if (!autorizacaoService.exigirPermissao(
                            AutorizacaoService.Permissao.ATUALIZAR_VEICULO)) {
                         break;
                        }

                        System.out.print(
                                bundle.getString("veiculo.id_atualizar")
                        );

                        int idAlt = scanner.nextInt();
                        scanner.nextLine();

                        System.out.print(
                                bundle.getString("veiculo.nova_marca")
                        );

                        String novaMarca = scanner.nextLine();

                        System.out.print(
                                bundle.getString("veiculo.novo_modelo")
                        );

                        String novoModelo = scanner.nextLine();

                        System.out.print(
                                bundle.getString("veiculo.novo_ano")
                        );

                        int novoAno = scanner.nextInt();

                        System.out.print(
                                bundle.getString("veiculo.novo_preco")
                        );

                        double novoPreco = scanner.nextDouble();

                        veiculoDAO.atualizar(
                                idAlt,
                                novaMarca,
                                novoModelo,
                                novoAno,
                                novoPreco
                        );

                        break;

                    case 4:
                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.EXCLUIR_VEICULO)) {
                        break;
                        }

                        System.out.print(
                                bundle.getString("veiculo.id_excluir")
                        );

                        int idDel = scanner.nextInt();

                        veiculoDAO.deletar(idDel);

                        break;

                    case 5:

                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.CADASTRAR_CLIENTE)) {
                        break;
                        }

                        System.out.print(
                                bundle.getString("cliente.nome")
                        );

                        String nome = scanner.nextLine();

                        System.out.print(
                                bundle.getString("cliente.cpf")
                        );

                        String cpf = scanner.nextLine();

                        System.out.print(
                                bundle.getString("cliente.telefone")
                        );

                        String telefone = scanner.nextLine();

                        System.out.print(
                                bundle.getString("cliente.email")
                        );

                        String email = scanner.nextLine();

                        clienteDAO.salvar(
                                new Cliente(
                                        nome,
                                        cpf,
                                        telefone,
                                        email
                                )
                        );

                        break;

                    case 6:
                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.LISTAR_CLIENTES)) {
                        break;
                        }

                        System.out.println(
                                bundle.getString("cliente.lista_titulo")
                        );

                        for (Cliente c : clienteDAO.listar()) {

                            System.out.println(
                                    "ID: " + c.getId()
                                    + " | "
                                    + bundle.getString("cliente.nome_label")
                                    + c.getNome()
                                    + " | CPF: "
                                    + c.getCpf()
                                    + " | "
                                    + bundle.getString("cliente.tel_label")
                                    + c.getTelefone()
                            );
                        }

                        break;

                    case 7:

                        if (!autorizacaoService.exigirPermissao(
                            AutorizacaoService.Permissao.REALIZAR_VENDA)) {
                            break;
                        }


                        System.out.print(
                                bundle.getString("venda.id_cliente")
                        );

                        int idCliente = scanner.nextInt();

                        System.out.print(
                                bundle.getString("venda.id_veiculo")
                        );

                        int idVeiculo = scanner.nextInt();

                        System.out.print(
                                bundle.getString("venda.valor_final")
                        );

                        double valorFinal = scanner.nextDouble();

                        vendaService.realizarVenda(
                                idCliente,
                                idVeiculo,
                                valorFinal
                        );

                        break;

                    case 8:

                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.LISTAR_VENDAS)) {
                        break;
                        }

                        System.out.println(
                                bundle.getString("venda.historico_titulo")
                        );

                        for (Venda venda : vendaDAO.listarVendas()) {

                            System.out.println(
                                    "Cliente: "
                                    + venda.getIdCliente()
                                    + " | Veiculo: "
                                    + venda.getIdVeiculo()
                                    + " | Valor: "
                                    + I18nUtils.formatCurrency(
                                            venda.getValorTotal(),
                                            locale
                                    )
                            );
                        }

                        break;

                    case 9:
                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.ACESSAR_OFICINA)) {
                            break;
                        }

                        System.out.println(
                                "Modulo da oficina indisponivel no momento."
                        );

                        break;

                    case 10:
                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.ACESSAR_RELATORIOS)) {
                            break;
                        }

                        relatorioMenu.exibirMenu();

                        break;

                    case 11:
                        if (!autorizacaoService.exigirPermissao(
                        AutorizacaoService.Permissao.ACESSAR_CLIENTE_LGPD)) {
                        break;
                        }

                        clienteMenu.exibirMenu();

                        break;

                    case 0:

                    SessaoUsuario.getInstancia().encerrarSessao();

                        System.out.println(
                                bundle.getString("sistema.encerrando")
                        );

                        break;

                    default:

                        System.out.println(
                                bundle.getString("sistema.opcao_invalida")
                        );
                }

            } catch (SQLException | IllegalArgumentException e) {

                System.out.println(
                        "Erro: " + e.getMessage()
                );
            }
        }

        scanner.close();
    }
}