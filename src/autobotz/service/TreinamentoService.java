package autobotz.service;

import autobotz.dao.EventoTreinamentoDAO;
import autobotz.model.EventoTreinamento;
import autobotz.model.MetricaTreinamento;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Regras e validações para registro de eventos e consulta das métricas D15. */
public final class TreinamentoService {
    private static final Pattern SLUG_VALIDO = Pattern.compile("[a-z0-9][a-z0-9._-]{0,99}");
    private final EventoTreinamentoDAO eventoDAO;

    public TreinamentoService(DataSource dataSource) {
        this.eventoDAO = new EventoTreinamentoDAO(dataSource);
    }

    public long registrarAberturaPortal() throws SQLException {
        return eventoDAO.registrar(new EventoTreinamento(EventoTreinamento.PORTAL_OPEN, null));
    }

    public long registrarVisualizacaoMaterial(String materialSlug) throws SQLException {
        validarSlug(materialSlug);
        return eventoDAO.registrar(new EventoTreinamento(EventoTreinamento.MATERIAL_VIEW, materialSlug));
    }

    public long registrarVisualizacaoVideo(String materialSlug) throws SQLException {
        validarSlug(materialSlug);
        return eventoDAO.registrar(new EventoTreinamento(EventoTreinamento.VIDEO_VIEW, materialSlug));
    }

    /** A camada chamadora deve validar a permissão administrativa antes de expor as métricas. */
    public List<MetricaTreinamento> consultarMetricas(LocalDateTime inicio, LocalDateTime fim)
            throws SQLException {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O fim do período deve ser posterior ao início.");
        }
        return eventoDAO.consultarMetricas(inicio, fim);
    }

    private static void validarSlug(String slug) {
        if (slug == null || !SLUG_VALIDO.matcher(slug).matches()) {
            throw new IllegalArgumentException(
                    "O slug do material deve conter de 1 a 100 caracteres: letras minúsculas, números, ponto, hífen ou sublinhado.");
        }
    }
}
