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
        PortalServer servidor;
        try {
            servidor = criarServidor(porta, base);
        } catch (IOException e) {
            System.err.println("Nao foi possivel carregar " + base.resolve("content/catalogo.json") + ": " + e.getMessage());
            System.err.println("Execute a partir da raiz do repositorio ou defina AUTOBOTZ_PORTAL_DIR.");
            return;
        }
        servidor.iniciar();
        System.out.println("Portal de treinamento: http://127.0.0.1:" + porta
            + " (eventos persistidos no banco; Ctrl+C para parar)");
    }

    /**
     * Constrói a mesma configuração persistente usada pelo launcher normal.
     * Publico para que o teste JDBC possa verificar o caminho padrão em porta efêmera.
     */
    public static PortalServer criarServidor(int porta, Path base) throws IOException {
        CatalogoMateriais catalogo = new CatalogoMateriais(base.resolve("content"));
        var backend = new TreinamentoPortalBackend(
            new TreinamentoService(ConexaoBanco.getDataSource()));
        return new PortalServer(porta, backend, catalogo, base.resolve("public"));
    }
}
