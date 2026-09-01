package autobotz;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        VeiculoDAO veiculoDAO = new VeiculoDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        VendaDAO vendaDAO = new VendaDAO();
        int opcao = -1;

        while (opcao != 0) {
            System.out.println("\n========== AUTOBOTZ MOTORS ==========");
            System.out.println("1. Cadastrar Veículo");
            System.out.println("2. Listar Veículos");
            System.out.println("3. Atualizar Veículo");
            System.out.println("4. Excluir Veículo");
            System.out.println("5. Cadastrar Cliente");
            System.out.println("6. Listar Clientes");
            System.out.println("7. Realizar Venda");
            System.out.println("8. Listar Vendas");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opção: ");
            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Marca: ");
                    String marca = scanner.nextLine();
                    System.out.print("Modelo: ");
                    String modelo = scanner.nextLine();
                    System.out.print("Ano: ");
                    int ano = scanner.nextInt();
                    System.out.print("Preço: ");
                    double preco = scanner.nextDouble();
                    veiculoDAO.cadastrar(new Veiculo(marca, modelo, ano, preco));
                    break;

                case 2:
                    System.out.println("\n--- LISTA DE VEÍCULOS ---");
                    for (Veiculo v : veiculoDAO.listar()) {
                        System.out.println("ID: " + v.getId() + 
                                           " | " + v.getMarca() + " " + v.getModelo() + 
                                           " | Ano: " + v.getAno() + 
                                           " | R$" + v.getPreco() + 
                                           " | Status: " + v.getStatus());
                    }
                    break;

                case 3:
                    System.out.print("ID do Veículo a atualizar: ");
                    int idAlt = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Nova Marca: ");
                    String novaMarca = scanner.nextLine();
                    System.out.print("Novo Modelo: ");
                    String novoModelo = scanner.nextLine();
                    System.out.print("Novo Ano: ");
                    int novoAno = scanner.nextInt();
                    System.out.print("Novo Preço: ");
                    double novoPreco = scanner.nextDouble();
                    veiculoDAO.atualizar(new Veiculo(idAlt, novaMarca, novoModelo, novoAno, novoPreco));
                    break;

                case 4:
                    System.out.print("ID do Veículo a excluir: ");
                    int idDel = scanner.nextInt();
                    veiculoDAO.deletar(idDel);
                    break;

                case 5:
                    System.out.print("Nome: ");
                    String nome = scanner.nextLine();
                    System.out.print("CPF: ");
                    String cpf = scanner.nextLine();
                    System.out.print("Telefone: ");
                    String telefone = scanner.nextLine();
                    System.out.print("Email: ");
                    String email = scanner.nextLine();
                    clienteDAO.salvar(new Cliente(nome, cpf, telefone, email));
                    break;

                case 6:
                    System.out.println("\n--- LISTA DE CLIENTES ---");
                    for (Cliente c : clienteDAO.listar()) {
                        System.out.println("ID: " + c.getId() + 
                                           " | Nome: " + c.getNome() + 
                                           " | CPF: " + c.getCpf() + 
                                           " | Tel: " + c.getTelefone());
                    }
                    break;

                case 7:
                    System.out.print("ID do Cliente: ");
                    int idCliente = scanner.nextInt();
                    System.out.print("ID do Veículo: ");
                    int idVeiculo = scanner.nextInt();
                    System.out.print("Valor Final Negociado: ");
                    double valorFinal = scanner.nextDouble();
                    vendaDAO.registrarVenda(new Venda(idCliente, idVeiculo, valorFinal));
                    break;

                case 8:
                    System.out.println("\n--- HISTÓRICO DE VENDAS ---");
                    for (String v : vendaDAO.listarVendas()) {
                        System.out.println(v);
                    }
                    break;

                case 0:
                    System.out.println("Encerrando o sistema...");
                    break;

                default:
                    System.out.println("Opção inválida!");
            }
        }
        scanner.close();
    }
}