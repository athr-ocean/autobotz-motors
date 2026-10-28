package autobotz.portal;

import autobotz.model.MetricaTreinamento;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Espelha as operacoes de eventos e consulta agregada do TreinamentoService (Higor). */
public interface PortalBackend {
    long registrarAberturaPortal() throws SQLException;

    long registrarVisualizacaoMaterial(String materialSlug) throws SQLException;

    long registrarVisualizacaoVideo(String materialSlug) throws SQLException;

    /** Implementacoes de teste nao afirmam possuir metricas reais persistidas. */
    default List<MetricaTreinamento> consultarMetricas(LocalDateTime inicio, LocalDateTime fim)
            throws SQLException {
        return List.of();
    }

    /** A origem deve refletir a implementacao concreta, nunca a escolha do navegador. */
    default String origemMetricas() {
        return "DADOS_SIMULADOS";
    }
}
