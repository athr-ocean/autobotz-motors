package autobotz.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexaoBanco {

    private static final String URL =
            System.getenv().getOrDefault(
                    "AUTOBOTZ_DB_URL",
                    "jdbc:mariadb://localhost:3306/autobotz_db"
            );

    private static final String USUARIO =
            System.getenv().getOrDefault(
                    "AUTOBOTZ_DB_USER",
                    "root"
            );

    private static final String SENHA =
            System.getenv("AUTOBOTZ_DB_PASSWORD");

    private static Connection conexao;

    private ConexaoBanco() {
    }

    public static synchronized Connection getConexao() throws SQLException {

        if (SENHA == null || SENHA.isBlank()) {
            throw new SQLException(
                    "A variavel AUTOBOTZ_DB_PASSWORD nao foi definida."
            );
        }

        if (conexao == null || conexao.isClosed()) {
            try {
                conexao = DriverManager.getConnection(
                        URL,
                        USUARIO,
                        SENHA
                );
            } catch (SQLException e) {
                throw new SQLException(
                        "Nao foi possivel conectar ao MariaDB. "
                        + "Verifique o servico e as variaveis "
                        + "AUTOBOTZ_DB_URL, AUTOBOTZ_DB_USER "
                        + "e AUTOBOTZ_DB_PASSWORD.",
                        e
                );
            }
        }

        return conexao;
    }
}
