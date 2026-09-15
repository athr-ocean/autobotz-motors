package autobotz.ui;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import autobotz.dao.VeiculoDAO;
import autobotz.model.Veiculo;

public class VeiculoMenu {
    private final VeiculoDAO veiculoDAO;
    private final Scanner scanner;

    public VeiculoMenu() {
        this.veiculoDAO = new VeiculoDAO();
        this.scanner = new Scanner(System.in);
    }

    public void exibirMenu() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n=== GESTAO DE VEICULOS ===");
            System.out.println("1. Cadastrar Veiculo");
            System.out.println("2. Listar Veiculos");
            System.out.println("3. Buscar Veiculo por ID");
            System.out.println("0. Sair");
            System.out.print("Escolha uma opcao: ");

            try {
                opcao = Integer.parseInt(scanner.nextLine());
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> listar();
                    case 3 -> buscarPorId();
                    case 0 -> System.out.println("Encerrando...");
                    default -> System.out.println("Opcao invalida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Erro: digite um numero valido.");
            } catch (SQLException e) {
                System.out.println("Erro no banco de dados: " + e.getMessage());
            }
        }
    }

    private void cadastrar() throws SQLException {
        System.out.print("Placa: ");
        String placa = scanner.nextLine();
        System.out.print("Modelo: ");
        String modelo = scanner.nextLine();
        System.out.print("Marca: ");
        String marca = scanner.nextLine();
        System.out.print("Ano: ");
        int ano = Integer.parseInt(scanner.nextLine());
        System.out.print("Preco: ");
        double preco = Double.parseDouble(scanner.nextLine());

        Veiculo veiculo = new Veiculo(placa, modelo, marca, ano, preco);
        veiculoDAO.salvar(veiculo);
        System.out.println("Veiculo gravado com sucesso! ID: " + veiculo.getId());
    }

    private void listar() throws SQLException {
        List<Veiculo> veiculos = veiculoDAO.listarTodos();
        if (veiculos.isEmpty()) {
            System.out.println("Nenhum veiculo encontrado.");
            return;
        }
        veiculos.forEach(System.out::println);
    }

    private void buscarPorId() throws SQLException {
        System.out.print("ID do veiculo: ");
        int id = Integer.parseInt(scanner.nextLine());
        Veiculo veiculo = veiculoDAO.buscarPorId(id);
        System.out.println(veiculo == null ? "Veiculo nao encontrado." : veiculo);
    }
}
