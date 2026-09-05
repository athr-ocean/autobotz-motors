package autobotz;

import java.util.Scanner;

public class LoginMenu {

    private final Scanner scanner;
    private final AuthService authService;

    public LoginMenu(
        Scanner scanner,
        AuthService authService
    ) {
        this.scanner = scanner;
        this.authService = authService;
    }

    public boolean executar() {

        System.out.println();
        System.out.println("========== LOGIN ==========");

        System.out.print("Usuário: ");
        String nomeUsuario = scanner.nextLine();

        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        Usuario usuario =
            authService.autenticar(
                nomeUsuario,
                senha
            );

        if (usuario == null) {

            System.out.println(
                "Usuário ou senha inválidos."
            );

            return false;
        }

        SessaoUsuario.getInstancia()
                     .setUsuario(usuario);

        System.out.println();
        System.out.println(
            "Login realizado com sucesso!"
        );

        System.out.println(
            "Usuário: " +
            usuario.getNomeUsuario()
        );

        System.out.println(
            "Perfil: " +
            usuario.getPerfil()
        );

        return true;
    }
}