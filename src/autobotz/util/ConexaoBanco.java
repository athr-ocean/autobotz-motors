package autobotz.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;

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

    private static final DataSource DATA_SOURCE = new ConexaoBancoDataSource();
    private static Connection conexao;

    private ConexaoBanco() {
    }

    public static synchronized Connection getConexao() throws SQLException {

        if (SENHA == null || SENHA.isBlank()) {
            throw new SQLException(
                    I18nUtils.getString("db.password_ausente")
            );
        }

        if (conexao == null || conexao.isClosed()) {
            conexao = abrirConexao(URL, USUARIO, SENHA);
        }

        return conexao;
    }

    public static DataSource getDataSource() {
        return DATA_SOURCE;
    }

    static Connection abrirNovaConexao() throws SQLException {
        if (SENHA == null || SENHA.isBlank()) {
            throw new SQLException(I18nUtils.getString("db.password_ausente"));
        }
        return abrirConexao(URL, USUARIO, SENHA);
    }

    static Connection abrirNovaConexao(String usuario, String senha) throws SQLException {
        if (usuario == null || senha == null) {
            throw new SQLException("Usuário e senha JDBC são obrigatórios.");
        }
        return abrirConexao(URL, usuario, senha);
    }

    private static Connection abrirConexao(String url, String usuario, String senha) throws SQLException {
        try {
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            throw new SQLException(I18nUtils.getString("db.conexao_falhou"), e);
        }
    }
}
