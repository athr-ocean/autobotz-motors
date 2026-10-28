package autobotz.dao;

import autobotz.model.RegistroLedgerContrato;
import autobotz.model.TipoEventoContrato;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** DAO append-only: oferece inserção e consulta; não expõe atualização nem exclusão de eventos. */
public class LedgerContratoDAO {
    public void inserir(Connection c, RegistroLedgerContrato evento) throws SQLException {
        String sql = "INSERT INTO ledger_contratos (contrato_id, sequencia, tipo_evento, payload, hash_conteudo, "
                + "hash_anterior, hash_registro, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, evento.getContratoId());
            ps.setInt(2, evento.getSequencia());
            ps.setString(3, evento.getTipo().name());
            ps.setString(4, evento.getPayload());
            ps.setString(5, evento.getHashConteudo());
            ps.setString(6, evento.getHashAnterior());
            ps.setString(7, evento.getHashRegistro());
            ps.setTimestamp(8, Timestamp.valueOf(evento.getDataHora()));
            ps.executeUpdate();
        }
    }

    public List<RegistroLedgerContrato> listarPorContrato(Connection c, long contratoId) throws SQLException {
        String sql = "SELECT id, contrato_id, sequencia, tipo_evento, payload, hash_conteudo, hash_anterior, "
                + "hash_registro, data_hora FROM ledger_contratos WHERE contrato_id = ? ORDER BY sequencia, id";
        List<RegistroLedgerContrato> eventos = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, contratoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) eventos.add(mapear(rs));
            }
        }
        return eventos;
    }

    public boolean existeEvento(Connection c, long contratoId, TipoEventoContrato tipo) throws SQLException {
        String sql = "SELECT 1 FROM ledger_contratos WHERE contrato_id = ? AND tipo_evento = ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, contratoId);
            ps.setString(2, tipo.name());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static RegistroLedgerContrato mapear(ResultSet rs) throws SQLException {
        return new RegistroLedgerContrato(rs.getLong("id"), rs.getLong("contrato_id"),
                rs.getInt("sequencia"), TipoEventoContrato.valueOf(rs.getString("tipo_evento")),
                rs.getString("payload"), rs.getString("hash_conteudo"), rs.getString("hash_anterior"),
                rs.getString("hash_registro"), rs.getTimestamp("data_hora").toLocalDateTime());
    }
}
