package autobotz.portal;

import java.nio.file.Path;

/** Inicia o portal de treinamento. Executar a partir da raiz do repositorio. */
public class PortalLauncher {
    public static void main(String[] args) throws Exception {
        int porta = Integer.parseInt(System.getenv().getOrDefault("AUTOBOTZ_PORTAL_PORT", "8085"));
        PortalServer servidor = new PortalServer(porta, new MockPortalBackend(),
            Path.of("portal-treinamento", "public"));
        servidor.iniciar();
        System.out.println("Portal de treinamento: http://localhost:" + porta + "  (Ctrl+C para parar)");
    }
}
