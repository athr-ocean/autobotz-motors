package autobotz.ui;

import java.sql.SQLException;
import java.util.Scanner;

import autobotz.dao.ClienteDAO;
import autobotz.model.Cliente;
import autobotz.service.ClienteService;

public class ClienteMenu {
    private final ClienteDAO clienteDAO;
    private final ClienteService clienteService;
    private final Scanner scanner;

    public ClienteMenu(Scanner scanner) {
        this.clienteDAO = new ClienteDAO();
        this.clienteService = new ClienteService(clienteDAO);
        this.scanner = scanner;
    }

    public void exibirMenu() {
        int opcao = -1;
        while (opcao != 0) {
            System.out.println("\n=== CLIENTES E LGPD ===");
            System.out.println("1. Cadastrar cliente");
            System.out.println("2. Listar clientes");
            System.out.println("3. Anonimizar cliente");
            System.out.println("0. Voltar");
            System.out.print("Opcao: ");
            try {
                opcao = Integer.parseInt(scanner.nextLine());
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> listar();
                    case 3 -> anonimizar();
                    case 0 -> { }
                    default -> System.out.println("Opcao invalida.");
                }
            } catch (SQLException | IllegalArgumentException e) {
                System.out.println("Erro: " + e.getMessage());
            }
        }
    }

    private void cadastrar() throws SQLException {
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Telefone: ");
        String telefone = scanner.nextLine();
        System.out.print("Email: ");
        String email = scanner.nextLine();
        clienteDAO.salvar(new Cliente(nome, cpf, telefone, email));
        System.out.println("Cliente cadastrado.");
    }

    private void listar() throws SQLException {
        for (Cliente cliente : clienteDAO.listar()) {
            System.out.println("ID: " + cliente.getId() + " | Nome: " + cliente.getNome()
                    + " | CPF: " + cliente.getCpf() + " | Ativo: " + cliente.isAtivo());
        }
    }

    private void anonimizar() throws SQLException {
        System.out.print("ID do cliente: ");
        int id = Integer.parseInt(scanner.nextLine());
        if (clienteService.anonimizar(id)) System.out.println("Cliente anonimizado.");
        else System.out.println("Cliente nao encontrado.");
    }
}
