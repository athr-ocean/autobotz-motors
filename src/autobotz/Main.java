package autobotz;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Seleção de Idioma Inicial
        System.out.println("Select Language / Selecione o Idioma:");
        System.out.println("1. Português (BR)");
        System.out.println("2. English (US)");
        System.out.print("Option / Opção: ");
        int langOpcao = scanner.nextInt();
        scanner.nextLine();

        // Correção aplicada: uso de Locale.of() em vez do construtor depreciado new Locale()
        Locale locale = (langOpcao == 2) ? Locale.of("en", "US") : Locale.of("pt", "BR");
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        /*
         * ==========================================================
         * AUTENTICAÇÃO - FRENTE 7
         * ==========================================================
         */

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        AuthService authService = new AuthService(usuarioDAO);

        LoginMenu loginMenu =
                new LoginMenu(scanner, authService);

        boolean loginSucesso = loginMenu.executar();

        if (!loginSucesso) {
            System.out.println("Acesso negado.");
            scanner.close();
            return;
        }

        /*
         * ==========================================================
         * MENU ORIGINAL DO ERP
         * ==========================================================
         */


        VeiculoDAO veiculoDAO = new VeiculoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        VendaDAO vendaDAO = new VendaDAO();
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
            System.out.println(bundle.getString("menu.opcao0"));
            System.out.print(bundle.getString("menu.escolha"));
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print(bundle.getString("veiculo.marca"));
                    String marca = scanner.nextLine();
                    System.out.print(bundle.getString("veiculo.modelo"));
                    String modelo = scanner.nextLine();
                    System.out.print(bundle.getString("veiculo.ano"));
                    int ano = scanner.nextInt();
                    System.out.print(bundle.getString("veiculo.preco"));
                    double preco = scanner.nextDouble();
                    veiculoDAO.cadastrar(new Veiculo(marca, modelo, ano, preco));
                    break;

                case 2:
                    System.out.println(bundle.getString("veiculo.lista_titulo"));
                    for (Veiculo v : veiculoDAO.listar()) {
                        System.out.println("ID: " + v.getId() + 
                                           " | " + v.getMarca() + " " + v.getModelo() + 
                                           " | " + bundle.getString("veiculo.ano_label") + v.getAno() + 
                                           " | R$" + v.getPreco() + 
                                           " | " + bundle.getString("veiculo.status") + v.getStatus());
                    }
                    break;

                case 3:
                    System.out.print(bundle.getString("veiculo.id_atualizar"));
                    int idAlt = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print(bundle.getString("veiculo.nova_marca"));
                    String novaMarca = scanner.nextLine();
                    System.out.print(bundle.getString("veiculo.novo_modelo"));
                    String novoModelo = scanner.nextLine();
                    System.out.print(bundle.getString("veiculo.novo_ano"));
                    int novoAno = scanner.nextInt();
                    System.out.print(bundle.getString("veiculo.novo_preco"));
                    double novoPreco = scanner.nextDouble();
                    veiculoDAO.atualizar(new Veiculo(idAlt, novaMarca, novoModelo, novoAno, novoPreco));
                    break;

                case 4:
                    System.out.print(bundle.getString("veiculo.id_excluir"));
                    int idDel = scanner.nextInt();
                    veiculoDAO.deletar(idDel);
                    break;

                case 5:
                    System.out.print(bundle.getString("cliente.nome"));
                    String nome = scanner.nextLine();
                    System.out.print(bundle.getString("cliente.cpf"));
                    String cpf = scanner.nextLine();
                    System.out.print(bundle.getString("cliente.telefone"));
                    String telefone = scanner.nextLine();
                    System.out.print(bundle.getString("cliente.email"));
                    String email = scanner.nextLine();
                    clienteDAO.salvar(new Cliente(nome, cpf, telefone, email));
                    break;

                case 6:
                    System.out.println(bundle.getString("cliente.lista_titulo"));
                    for (Cliente c : clienteDAO.listar()) {
                        System.out.println("ID: " + c.getId() + 
                                           " | " + bundle.getString("cliente.nome_label") + c.getNome() + 
                                           " | CPF: " + c.getCpf() + 
                                           " | " + bundle.getString("cliente.tel_label") + c.getTelefone());
                    }
                    break;

                case 7:
                    System.out.print(bundle.getString("venda.id_cliente"));
                    int idCliente = scanner.nextInt();
                    System.out.print(bundle.getString("venda.id_veiculo"));
                    int idVeiculo = scanner.nextInt();
                    System.out.print(bundle.getString("venda.valor_final"));
                    double valorFinal = scanner.nextDouble();
                    vendaDAO.registrarVenda(new Venda(idCliente, idVeiculo, valorFinal));
                    break;

                case 8:
                    System.out.println(bundle.getString("venda.historico_titulo"));
                    for (String v : vendaDAO.listarVendas()) {
                        System.out.println(v);
                    }
                    break;

                case 0:
                    System.out.println(bundle.getString("sistema.encerrando"));
                    break;

                default:
                    System.out.println(bundle.getString("sistema.opcao_invalida"));
            }
        }
        scanner.close();
    }
}