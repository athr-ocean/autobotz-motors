package autobotz;

import autobotz.service.MfaService;
import autobotz.service.ResultadoLogin;
import java.util.Locale;
import java.util.ResourceBundle;
import java.sql.SQLException;
import java.util.Scanner;

public class LoginMenu {
    private final Scanner scanner;
    private final AuthService authService;
    private final ResourceBundle bundle;

    public LoginMenu(Scanner scanner, AuthService authService, ResourceBundle bundle) {
        this.scanner = scanner;
        this.authService = authService;
        this.bundle = bundle;
    }

    public boolean executar() {
        while (true) {
            System.out.println(bundle.getString("login.titulo"));
            System.out.println(bundle.getString("menu.login01"));
            System.out.println(bundle.getString("menu.login02"));
            System.out.println(bundle.getString("menu.login03"));
            System.out.print(bundle.getString("comum.escolha"));

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
                    default -> System.out.println(bundle.getString("comum.opcao_invalida"));
                }
            } catch (NumberFormatException e) {
                System.out.println(bundle.getString("login.opcao_invalida"));
            } catch (SQLException e) {
                System.out.println(bundle.getString("comum.erro_banco") + e.getMessage());
                return false;
            }
        }
    }

    private boolean fazerLogin() throws SQLException {
        System.out.print(bundle.getString("login.usuario"));
        String nomeUsuario = scanner.nextLine();
        System.out.print(bundle.getString("login.senha"));
        String senha = scanner.nextLine();

        ResultadoLogin resultado = authService.iniciarLogin(nomeUsuario, senha);
        if (resultado.getStatus() == ResultadoLogin.Status.MFA_NECESSARIA) {
            resultado = executarMfa(resultado.getDesafio());
        }
        if (resultado.getStatus() == ResultadoLogin.Status.NEGADO && resultado.getMfaStatus() == null) {
            System.out.println(bundle.getString("login.credenciais_invalidas"));
            return false;
        }
        if (resultado.getStatus() != ResultadoLogin.Status.AUTENTICADO) {
            return false;
        }
        Usuario usuario = resultado.getUsuario();

        SessaoUsuario.getInstancia().setUsuario(usuario);
        System.out.println(bundle.getString("login.sucesso") + usuario.getPerfil());
        return true;
    }

    /** Segundo fator simulado: mostra o código rotulado como demonstração e valida a resposta. */
    private ResultadoLogin executarMfa(MfaService.Desafio desafio) throws SQLException {
        System.out.println(bundle.getString("login.mfa_titulo"));
        System.out.println(bundle.getString("login.mfa_demo") + desafio.codigoSimulado());
        while (true) {
            System.out.print(bundle.getString("login.mfa_codigo"));
            ResultadoLogin r = authService.concluirMfa(desafio.id(), scanner.nextLine());
            if (r.getStatus() == ResultadoLogin.Status.AUTENTICADO) {
                return r;
            }
            if (r.getMfaStatus() == MfaService.Status.INVALIDO) {
                System.out.println(bundle.getString("login.mfa_invalido"));
                continue;
            }
            System.out.println(bundle.getString("login.mfa_reiniciar"));
            return r;
        }
    }

    private void cadastrarUsuario() throws SQLException {
        System.out.print(bundle.getString("login.novo_usuario"));
        String nomeUsuario = scanner.nextLine();
        System.out.print(bundle.getString("login.senha"));
        String senha = scanner.nextLine();
        System.out.println(bundle.getString("login.perfil_admin"));
        System.out.println(bundle.getString("login.perfil_vendedor"));
        System.out.print(bundle.getString("login.perfil"));

        int opcaoPerfil = Integer.parseInt(scanner.nextLine());
        PerfilUsuario perfil;
        if (opcaoPerfil == 1) {
            perfil = PerfilUsuario.ADMIN;
        } else if (opcaoPerfil == 2) {
            perfil = PerfilUsuario.VENDEDOR;
        } else {
            System.out.println(bundle.getString("login.perfil_invalido"));
            return;
        }

        if (authService.cadastrar(nomeUsuario, senha, perfil)) {
            System.out.println(bundle.getString("login.usuario_criado"));
        } else {
            System.out.println(bundle.getString("login.usuario_existente"));
        }
    }
}
