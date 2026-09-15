package autobotz;

import java.sql.SQLException;
import java.util.Scanner;

public class LoginMenu {
    private final Scanner scanner;
    private final AuthService authService;

    public LoginMenu(Scanner scanner, AuthService authService) {
        this.scanner = scanner;
        this.authService = authService;
    }

    public boolean executar() {
        while (true) {
            System.out.println("\n========== AUTENTICACAO ==========");
            System.out.println("1. Fazer login");
            System.out.println("2. Cadastrar usuario");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");

            try {
                int opcao = Integer.parseInt(scanner.nextLine());
                switch (opcao) {
                    case 1 -> {
                        if (fazerLogin()) {
                            return true;
                        }
                    }
                    case 2 -> cadastrarUsuario();
                    case 0 -> { return false; }
                    default -> System.out.println("Opcao invalida.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Erro: digite uma opcao valida.");
            } catch (SQLException e) {
                System.out.println("Erro no banco de dados: " + e.getMessage());
                return false;
            }
        }
    }

    private boolean fazerLogin() throws SQLException {
        System.out.print("Usuario: ");
        String nomeUsuario = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        Usuario usuario = authService.autenticar(nomeUsuario, senha);
        if (usuario == null) {
            System.out.println("Usuario ou senha invalidos.");
            return false;
        }

        SessaoUsuario.getInstancia().setUsuario(usuario);
        System.out.println("Login realizado com sucesso! Perfil: " + usuario.getPerfil());
        return true;
    }

    private void cadastrarUsuario() throws SQLException {
        System.out.print("Novo usuario: ");
        String nomeUsuario = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();
        System.out.println("1. ADMIN");
        System.out.println("2. VENDEDOR");
        System.out.print("Perfil: ");

        int opcaoPerfil = Integer.parseInt(scanner.nextLine());
        PerfilUsuario perfil;
        if (opcaoPerfil == 1) {
            perfil = PerfilUsuario.ADMIN;
        } else if (opcaoPerfil == 2) {
            perfil = PerfilUsuario.VENDEDOR;
        } else {
            System.out.println("Perfil invalido.");
            return;
        }

        if (authService.cadastrar(nomeUsuario, senha, perfil)) {
            System.out.println("Usuario cadastrado com sucesso.");
        } else {
            System.out.println("Esse usuario ja existe.");
        }
    }
}
