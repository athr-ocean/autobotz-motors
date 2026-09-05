package autobotz;

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

            System.out.println();
            System.out.println("========== AUTENTICAÇÃO ==========");
            System.out.println("1. Fazer login");
            System.out.println("2. Cadastrar usuário");
            System.out.println("0. Sair");
            System.out.print("Escolha: ");

            int opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {

                case 1:
                    if (fazerLogin()) {
                        return true;
                    }
                    break;

                case 2:
                    cadastrarUsuario();
                    break;

                case 0:
                    return false;

                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private boolean fazerLogin() {

        System.out.println();
        System.out.println("========== LOGIN ==========");

        System.out.print("Usuário: ");
        String nomeUsuario = scanner.nextLine();

        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        Usuario usuario =
                authService.autenticar(nomeUsuario, senha);

        if (usuario == null) {

            System.out.println("Usuário ou senha inválidos.");
            return false;
        }

        SessaoUsuario.getInstancia().setUsuario(usuario);

        System.out.println();
        System.out.println("Login realizado com sucesso!");
        System.out.println("Usuário: " + usuario.getNomeUsuario());
        System.out.println("Perfil: " + usuario.getPerfil());

        return true;
    }

    private void cadastrarUsuario() {

        System.out.println();
        System.out.println("========== CADASTRO ==========");

        System.out.print("Novo usuário: ");
        String nomeUsuario = scanner.nextLine();

        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        System.out.println();
        System.out.println("Escolha o perfil:");
        System.out.println("1. ADMIN");
        System.out.println("2. VENDEDOR");
        System.out.print("Perfil: ");

        int opcaoPerfil = scanner.nextInt();
        scanner.nextLine();

        PerfilUsuario perfil;

        if (opcaoPerfil == 1) {
            perfil = PerfilUsuario.ADMIN;
        } else if (opcaoPerfil == 2) {
            perfil = PerfilUsuario.VENDEDOR;
        } else {
            System.out.println("Perfil inválido.");
            return;
        }

        boolean sucesso =
                authService.cadastrar(
                        nomeUsuario,
                        senha,
                        perfil
                );

        if (sucesso) {
            System.out.println(
                    "Usuário cadastrado com sucesso!"
            );
        } else {
            System.out.println(
                    "Esse usuário já existe."
            );
        }
    }
}