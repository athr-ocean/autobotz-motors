package autobotz.dao;

import autobotz.model.EventoTreinamento;
import autobotz.model.MetricaTreinamento;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Persistência e consulta agregada de eventos do treinamento. */
public final class EventoTreinamentoDAO {
    private final DataSource dataSource;

    /** Recebe o DataSource configurado pela aplicação; não guarda credenciais no código. */
    public EventoTreinamentoDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public long registrar(EventoTreinamento evento) throws SQLException {
        Objects.requireNonNull(evento, "evento");
        String sql = "INSERT INTO treinamento_eventos (tipo_evento, material_slug) VALUES (?, ?)";

        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            comando.setString(1, evento.getTipoEvento());
            comando.setString(2, evento.getMaterialSlug());
            comando.executeUpdate();

            try (ResultSet chaves = comando.getGeneratedKeys()) {
                if (chaves.next()) {
                    return chaves.getLong(1);
                }
            }
        }
        throw new SQLException("O evento foi inserido, mas o banco não retornou o ID gerado.");
    }

    /** Usa início inclusivo e fim exclusivo: [inicio, fim). */
    public List<MetricaTreinamento> consultarMetricas(LocalDateTime inicio, LocalDateTime fim)
            throws SQLException {
        validarIntervalo(inicio, fim);

        String sql = """
                SELECT tipo_evento, material_slug, COUNT(*) AS total
                FROM treinamento_eventos
                WHERE ocorrido_em >= ? AND ocorrido_em < ?
                GROUP BY tipo_evento, material_slug
                ORDER BY tipo_evento, material_slug
                """;
        List<MetricaTreinamento> metricas = new ArrayList<>();

        try (Connection conexao = dataSource.getConnection();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setTimestamp(1, Timestamp.valueOf(inicio));
            comando.setTimestamp(2, Timestamp.valueOf(fim));
            try (ResultSet resultado = comando.executeQuery()) {
                while (resultado.next()) {
                    metricas.add(new MetricaTreinamento(
                            resultado.getString("tipo_evento"),
                            resultado.getString("material_slug"),
                            resultado.getLong("total")));
                }
            }
        }
        return metricas;
    }

    private static void validarIntervalo(LocalDateTime inicio, LocalDateTime fim) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("O fim do período deve ser posterior ao início.");
        }
    }
}
