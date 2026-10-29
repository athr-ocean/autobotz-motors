package autobotz.portal;

import autobotz.service.TreinamentoService;
import autobotz.util.ConexaoBanco;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import javax.sql.DataSource;

/** Inicia o portal de treinamento. Executar a partir da raiz do repositorio. */
public class PortalLauncher {
    public static void main(String[] args) throws Exception {
        int porta = Integer.parseInt(System.getenv().getOrDefault("AUTOBOTZ_PORTAL_PORT", "8085"));
        Path base = Path.of("portal-treinamento");
        CatalogoMateriais catalogo;
        try {
            catalogo = new CatalogoMateriais(base.resolve("content"));
        } catch (IOException e) {
            System.err.println("Nao foi possivel carregar portal-treinamento/content/catalogo.json: " + e.getMessage());
            System.err.println("Execute a partir da raiz do repositorio.");
            return;
        }
        String modo = System.getenv().getOrDefault("AUTOBOTZ_PORTAL_BACKEND", "jdbc");
        PortalBackend backend;
        if (modo.equals("mock")) {
            backend = new MockPortalBackend();
        } else if (modo.equals("jdbc")) {
            DataSource dataSource = ConexaoBanco.getDataSource();
            try (var conexao = dataSource.getConnection()) {
                backend = new TreinamentoPortalBackend(new TreinamentoService(dataSource));
            } catch (SQLException e) {
                System.err.println("Não foi possível iniciar o portal com persistência JDBC: " + e.getMessage());
                return;
            }
        } else {
            System.err.println("AUTOBOTZ_PORTAL_BACKEND deve ser 'jdbc' ou 'mock'.");
            return;
        }
        new PortalServer(porta, backend, catalogo, base.resolve("public")).iniciar();
        System.out.println("Portal de treinamento: http://localhost:" + porta + " (backend: " + modo + ")");
    }
}
