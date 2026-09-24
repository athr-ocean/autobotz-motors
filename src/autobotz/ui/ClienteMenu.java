package autobotz.ui;

import java.sql.SQLException;
import java.util.Scanner;

import autobotz.dao.ClienteDAO;
import autobotz.model.Cliente;
import autobotz.service.ClienteService;
import autobotz.util.I18nUtils;

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
            System.out.println(I18nUtils.getString("cliente.menu.titulo"));
            System.out.println(I18nUtils.getString("cliente.menu.cadastrar"));
            System.out.println(I18nUtils.getString("cliente.menu.listar"));
            System.out.println(I18nUtils.getString("cliente.menu.anonimizar"));
            System.out.println(I18nUtils.getString("comum.voltar"));
            System.out.print(I18nUtils.getString("comum.opcao"));
            try {
                opcao = Integer.parseInt(scanner.nextLine());
                switch (opcao) {
                    case 1 -> cadastrar();
                    case 2 -> listar();
                    case 3 -> anonimizar();
                    case 0 -> { }
                    default -> System.out.println(I18nUtils.getString("comum.opcao_invalida"));
                }
            } catch (SQLException | IllegalArgumentException e) {
                System.out.println(I18nUtils.getString("comum.erro") + e.getMessage());
            }
        }
    }

    private void cadastrar() throws SQLException {
        System.out.print(I18nUtils.getString("cliente.nome"));
        String nome = scanner.nextLine();
        System.out.print(I18nUtils.getString("cliente.cpf"));
        String cpf = scanner.nextLine();
        System.out.print(I18nUtils.getString("cliente.telefone"));
        String telefone = scanner.nextLine();
        System.out.print(I18nUtils.getString("cliente.email"));
        String email = scanner.nextLine();
        clienteDAO.salvar(new Cliente(nome, cpf, telefone, email));
        System.out.println(I18nUtils.getString("cliente.cadastrado"));
    }

    private void listar() throws SQLException {
        for (Cliente cliente : clienteDAO.listar()) {
            System.out.println(
                    I18nUtils.getString("comum.id")
                    + cliente.getId()
                    + " | "
                    + I18nUtils.getString("cliente.nome_label")
                    + cliente.getNome()
                    + " | "
                    + I18nUtils.getString("cliente.cpf_label")
                    + cliente.getCpf()
                    + " | "
                    + I18nUtils.getString("cliente.ativo_label")
                    + (
                        cliente.isAtivo()
                            ? I18nUtils.getString("comum.sim")
                            : I18nUtils.getString("comum.nao")
                    )
            );
        }
    }

    private void anonimizar() throws SQLException {
        System.out.print(I18nUtils.getString("cliente.id_prompt"));
        int id = Integer.parseInt(scanner.nextLine());
        if (clienteService.anonimizar(id)) System.out.println(I18nUtils.getString("cliente.anonimizado"));
        else System.out.println(I18nUtils.getString("cliente.nao_encontrado"));
    }
}
