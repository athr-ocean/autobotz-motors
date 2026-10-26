package autobotz.portal;

import java.io.IOException;
import java.nio.file.Path;

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
        new PortalServer(porta, new MockPortalBackend(), catalogo, base.resolve("public")).iniciar();
        System.out.println("Portal de treinamento: http://localhost:" + porta
            + "  (eventos so em memoria; Ctrl+C para parar)");
    }
}
