package autobotz.portal;

import java.util.List;

/** Contrato entre o servidor do portal (Mauro) e o servico de conteudo/metricas (Higor). */
public interface PortalBackend {
    record Material(String id, String tipo, String titulo, String descricao, String arquivo, boolean simulado) {}
    record Evento(long id, String registradoEm) {}

    List<Material> catalogo(String tipo);

    /** Retorna null se o material nao existir. */
    Material buscarMaterial(String id);

    /** Lanca IllegalArgumentException se o evento for invalido. */
    Evento registrarEvento(String tipoEvento, String materialId, String sessaoId);
}
