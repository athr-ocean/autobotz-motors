package autobotz.portal;

import java.sql.SQLException;

/** Espelha as operacoes de eventos do TreinamentoService (Higor). */
public interface PortalBackend {
    long registrarAberturaPortal() throws SQLException;

    long registrarVisualizacaoMaterial(String materialSlug) throws SQLException;

    long registrarVisualizacaoVideo(String materialSlug) throws SQLException;
}
