package autobotz.portal;

import autobotz.service.TreinamentoService;
import autobotz.util.ConexaoBanco;

import java.io.IOException;
import java.nio.file.Path;

/** Inicia o portal de treinamento. Executar a partir da raiz do repositorio. */
public class PortalLauncher {
    public static void main(String[] args) throws Exception {
        int porta = Integer.parseInt(System.getenv().getOrDefault("AUTOBOTZ_PORTAL_PORT", "8085"));
        Path base = Path.of(System.getenv().getOrDefault("AUTOBOTZ_PORTAL_DIR", "portal-treinamento"));
        CatalogoMateriais catalogo;
        try {
            catalogo = new CatalogoMateriais(base.resolve("content"));
        } catch (IOException e) {
            System.err.println("Nao foi possivel carregar " + base.resolve("content/catalogo.json") + ": " + e.getMessage());
            System.err.println("Execute a partir da raiz do repositorio ou defina AUTOBOTZ_PORTAL_DIR.");
            return;
        }

        var backend = new TreinamentoPortalBackend(
            new TreinamentoService(ConexaoBanco.getDataSource()));
        new PortalServer(porta, backend, catalogo, base.resolve("public")).iniciar();
        System.out.println("Portal de treinamento: http://127.0.0.1:" + porta
            + "  (eventos persistidos no banco; Ctrl+C para parar)");
    }
}
