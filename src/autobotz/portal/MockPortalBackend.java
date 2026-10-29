package autobotz.portal;

import java.util.ArrayList;
import java.util.List;

/** Guarda eventos so em memoria. Trocar pelo adaptador do TreinamentoService quando a conexao estiver definida. */
public class MockPortalBackend implements PortalBackend {
    private final List<String> eventos = new ArrayList<>();

    @Override public long registrarAberturaPortal() { return registrar("PORTAL_OPEN", null); }
    @Override public long registrarVisualizacaoMaterial(String slug) { return registrar("MATERIAL_VIEW", slug); }
    @Override public long registrarVisualizacaoVideo(String slug) { return registrar("VIDEO_VIEW", slug); }

    public synchronized List<String> eventos() { return List.copyOf(eventos); }

    private synchronized long registrar(String tipo, String slug) {
        eventos.add(tipo + ":" + slug);
        return eventos.size();
    }
}
