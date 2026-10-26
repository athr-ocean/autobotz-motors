import autobotz.util.ConexaoBanco;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/** Testa o ciclo de vida do DataSource com um driver fictício, sem acessar banco algum. */
public final class ConexaoBancoDataSourceTest {
    private static final String URL_TESTE = "jdbc:autobotz-test:isolated";

    private ConexaoBancoDataSourceTest() {}

    public static void main(String[] args) throws Exception {
        if (!URL_TESTE.equals(System.getenv("AUTOBOTZ_DB_URL"))) {
            throw new IllegalStateException("O teste exige URL JDBC fictícia; nenhum banco será acessado.");
        }

        FakeDriver driver = new FakeDriver();
        DriverManager.registerDriver(driver);
        try {
            var dataSource = ConexaoBanco.getDataSource();
            Connection primeira = dataSource.getConnection();
            Connection segunda = dataSource.getConnection();

            check(primeira != segunda, "cada chamada entrega uma conexão distinta");
            check(driver.conexoes.size() == 2, "cada chamada abre uma conexão física");

            primeira.close();
            check(primeira.isClosed(), "o chamador pode fechar sua conexão");
            check(!segunda.isClosed(), "fechar uma conexão não fecha a outra");

            segunda.close();
            check(segunda.isClosed(), "a segunda conexão também tem ciclo independente");
            System.out.println("CONEXAO_DATASOURCE_OK");
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    private static void check(boolean condicao, String caso) {
        if (!condicao) {
            throw new AssertionError(caso);
        }
        System.out.println("PASS " + caso);
    }

    private static final class FakeDriver implements Driver {
        private final List<Connection> conexoes = new ArrayList<>();

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            AtomicBoolean fechada = new AtomicBoolean();
            Connection conexao = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[] {Connection.class},
                    (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                        case "close" -> { fechada.set(true); yield null; }
                        case "isClosed" -> fechada.get();
                        case "isValid" -> !fechada.get();
                        case "toString" -> "FakeConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == argumentos[0];
                        default -> throw new SQLFeatureNotSupportedException(metodo.getName());
                    });
            conexoes.add(conexao);
            return conexao;
        }

        @Override public boolean acceptsURL(String url) { return URL_TESTE.equals(url); }
        @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override public int getMajorVersion() { return 1; }
        @Override public int getMinorVersion() { return 0; }
        @Override public boolean jdbcCompliant() { return false; }
        @Override public Logger getParentLogger() { return Logger.getLogger("autobotz.test.jdbc"); }
    }
}
