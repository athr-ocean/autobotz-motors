package autobotz;

import autobotz.util.ConexaoBanco;
import autobotz.util.SenhaHash;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/** Cadastro da primeira conta em uma instalação nova. */
public final class BootstrapAdmin {
    private BootstrapAdmin() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("[A-Za-z0-9._-]{3,50}")) {
            throw new IllegalArgumentException("Uso: BootstrapAdmin NOME_DO_ADMINISTRADOR");
        }
        if (!"1".equals(System.getenv("AUTOBOTZ_ADMIN_SETUP"))) {
            throw new IllegalStateException("Habilite AUTOBOTZ_ADMIN_SETUP=1 somente para iniciar o banco.");
        }
        Console terminal = System.console();
        if (terminal == null) {
            throw new IllegalStateException("Execute em um terminal interativo.");
        }
        char[] senha = terminal.readPassword("Senha do administrador inicial: ");
        if (senha == null || senha.length < 10) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 10 caracteres.");
        }
        try {
            criar(args[0], new String(senha));
        } finally {
            Arrays.fill(senha, '\0');
        }
    }

    private static void criar(String nome, String senha) throws SQLException {
        try (Connection conexao = ConexaoBanco.getDataSource().getConnection()) {
            boolean autoCommitAnterior = conexao.getAutoCommit();
            conexao.setAutoCommit(false);
            try {
                try (Statement stmt = conexao.createStatement();
                     ResultSet lock = stmt.executeQuery("SELECT GET_LOCK('autobotz_bootstrap_admin', 10)")) {
                    if (!lock.next() || lock.getInt(1) != 1) {
                        throw new SQLException("Não foi possível obter o bloqueio do cadastro inicial.");
                    }
                }
                try (Statement stmt = conexao.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                    if (!rs.next() || rs.getInt(1) != 0) {
                        throw new SQLException("O banco já tem usuários. Entre como ADMIN para criar novas contas.");
                    }
                }
                try (PreparedStatement stmt = conexao.prepareStatement(
                        "INSERT INTO usuarios (nome_usuario, senha_hash, perfil) VALUES (?, ?, 'ADMIN')")) {
                    stmt.setString(1, nome);
                    stmt.setString(2, SenhaHash.gerar(senha));
                    stmt.executeUpdate();
                }
                conexao.commit();
                System.out.println("Administrador inicial criado.");
            } catch (SQLException | RuntimeException e) {
                try {
                    conexao.rollback();
                } catch (SQLException falha) {
                    e.addSuppressed(falha);
                }
                throw e;
            } finally {
                try {
                    conexao.setAutoCommit(autoCommitAnterior);
                } catch (SQLException ignored) {
                    // A conexão será fechada ao sair.
                }
            }
        }
    }
}
