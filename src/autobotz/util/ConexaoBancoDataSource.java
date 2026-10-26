package autobotz.util;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * DataSource sem pool que usa a configuração central de {@link ConexaoBanco}.
 * Cada chamada abre uma conexão física nova; o chamador é responsável por fechá-la.
 */
public final class ConexaoBancoDataSource implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
        return ConexaoBanco.abrirNovaConexao();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return ConexaoBanco.abrirNovaConexao(username, password);
    }

    @Override
    public PrintWriter getLogWriter() {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger("autobotz.jdbc");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Este DataSource não expõe o wrapper solicitado.");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface != null && iface.isInstance(this);
    }
}
