package autobotz.portal;

import autobotz.model.MetricaTreinamento;
import autobotz.service.TreinamentoService;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Liga o servidor do portal ao TreinamentoService (persistencia no MariaDB). */
public class TreinamentoPortalBackend implements PortalBackend {
    private final TreinamentoService servico;

    public TreinamentoPortalBackend(TreinamentoService servico) {
        this.servico = Objects.requireNonNull(servico, "servico");
    }

    @Override
    public long registrarAberturaPortal() throws SQLException {
        return servico.registrarAberturaPortal();
    }

    @Override
    public long registrarVisualizacaoMaterial(String materialSlug) throws SQLException {
        return servico.registrarVisualizacaoMaterial(materialSlug);
    }

    @Override
    public long registrarVisualizacaoVideo(String materialSlug) throws SQLException {
        return servico.registrarVisualizacaoVideo(materialSlug);
    }

    @Override
    public List<MetricaTreinamento> consultarMetricas(LocalDateTime inicio, LocalDateTime fim)
            throws SQLException {
        return servico.consultarMetricas(inicio, fim);
    }

    @Override
    public String origemMetricas() {
        return "DADOS_REAIS_DE_EXECUCAO";
    }
}
