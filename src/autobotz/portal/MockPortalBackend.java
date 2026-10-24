package autobotz.portal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Dados de exemplo em memoria (SIMULADO). Trocar pelo TreinamentoService do Higor quando existir. */
public class MockPortalBackend implements PortalBackend {
    private static final Set<String> EVENTOS = Set.of("PORTAL_OPEN", "MATERIAL_VIEW", "VIDEO_VIEW");
    private static final List<Material> MATERIAIS = List.of(
        new Material("guia-venda", "GUIA", "Guia rápido: registrar uma venda",
            "Exemplo de guia. O conteúdo real virá do módulo de conteúdo.", "", true),
        new Material("video-venda", "VIDEO", "Tutorial em vídeo (3 min)",
            "Exemplo de tutorial. O vídeo ainda não foi produzido.", "", true),
        new Material("faq-login", "FAQ", "FAQ: problemas de login",
            "Exemplo de pergunta frequente.", "", true));
    private final AtomicLong seq = new AtomicLong();

    @Override
    public List<Material> catalogo(String tipo) {
        return MATERIAIS.stream()
            .filter(m -> tipo == null || tipo.isBlank() || m.tipo().equalsIgnoreCase(tipo)).toList();
    }

    @Override
    public Material buscarMaterial(String id) {
        return MATERIAIS.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public Evento registrarEvento(String tipo, String materialId, String sessaoId) {
        if (tipo == null || !EVENTOS.contains(tipo)) throw new IllegalArgumentException("tipoEvento invalido.");
        if (sessaoId == null || sessaoId.isBlank() || sessaoId.length() > 64)
            throw new IllegalArgumentException("sessaoId obrigatorio.");
        boolean exigeMaterial = !tipo.equals("PORTAL_OPEN");
        if (exigeMaterial && (materialId == null || buscarMaterial(materialId) == null))
            throw new IllegalArgumentException("materialId invalido.");
        if (!exigeMaterial && materialId != null)
            throw new IllegalArgumentException("PORTAL_OPEN nao usa materialId.");
        return new Evento(seq.incrementAndGet(), LocalDateTime.now().withNano(0).toString());
    }
}
