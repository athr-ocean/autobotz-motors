package autobotz.dao;

import autobotz.model.ContratoFornecedor;
import autobotz.model.StatusContrato;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Persistência de contratos. A transação pertence ao ContratoService. */
public class ContratoDAO {
    public ContratoFornecedor buscarPorId(Connection c, long id, boolean paraAtualizar) throws SQLException {
        String sql = "SELECT id, fornecedor_id, valor, data_vencimento, estado, nota_fiscal_validada, "
                + "pagamento_simulado_liberado, ledger_ultima_sequencia, ledger_ultimo_hash, criado_em "
                + "FROM contratos_fornecedor WHERE id = ?" + (paraAtualizar ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public long inserir(Connection c, int fornecedorId, BigDecimal valor, LocalDate vencimento) throws SQLException {
        String sql = "INSERT INTO contratos_fornecedor (fornecedor_id, valor, data_vencimento, estado, "
                + "nota_fiscal_validada, pagamento_simulado_liberado, ledger_ultima_sequencia, ledger_ultimo_hash) "
                + "VALUES (?, ?, ?, ?, FALSE, FALSE, 0, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fornecedorId);
            ps.setBigDecimal(2, valor);
            ps.setDate(3, Date.valueOf(vencimento));
            ps.setString(4, StatusContrato.RASCUNHO.name());
            ps.setString(5, ContratoFornecedor.HASH_GENESIS);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("INSERT em contratos_fornecedor não retornou chave");
                return rs.getLong(1);
            }
        }
    }

    public boolean atualizarEstado(Connection c, long id, StatusContrato de, StatusContrato para)
            throws SQLException {
        String sql = "UPDATE contratos_fornecedor SET estado = ? WHERE id = ? AND estado = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, para.name());
            ps.setLong(2, id);
            ps.setString(3, de.name());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean marcarNotaFiscalValidada(Connection c, long id) throws SQLException {
        String sql = "UPDATE contratos_fornecedor SET nota_fiscal_validada = TRUE "
                + "WHERE id = ? AND nota_fiscal_validada = FALSE";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean liberarPagamentoSimulado(Connection c, long id, LocalDate hoje) throws SQLException {
        String sql = "UPDATE contratos_fornecedor SET pagamento_simulado_liberado = TRUE "
                + "WHERE id = ? AND nota_fiscal_validada = TRUE AND pagamento_simulado_liberado = FALSE "
                + "AND data_vencimento <= ? AND estado NOT IN ('CANCELADO','FINALIZADO')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setDate(2, Date.valueOf(hoje));
            return ps.executeUpdate() == 1;
        }
    }

    public boolean atualizarAncoraLedger(Connection c, long id, int sequenciaAnterior, String hashAnterior,
                                         int novaSequencia, String novoHash) throws SQLException {
        String sql = "UPDATE contratos_fornecedor SET ledger_ultima_sequencia = ?, ledger_ultimo_hash = ? "
                + "WHERE id = ? AND ledger_ultima_sequencia = ? AND ledger_ultimo_hash = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, novaSequencia);
            ps.setString(2, novoHash);
            ps.setLong(3, id);
            ps.setInt(4, sequenciaAnterior);
            ps.setString(5, hashAnterior);
            return ps.executeUpdate() == 1;
        }
    }

    public java.util.List<ContratoFornecedor> listarNaJanelaVencimento(Connection c, LocalDate hoje,
                                                                         LocalDate limite) throws SQLException {
        String sql = "SELECT id, fornecedor_id, valor, data_vencimento, estado, nota_fiscal_validada, "
                + "pagamento_simulado_liberado, ledger_ultima_sequencia, ledger_ultimo_hash, criado_em "
                + "FROM contratos_fornecedor WHERE data_vencimento BETWEEN ? AND ? "
                + "AND estado NOT IN ('CANCELADO','FINALIZADO') ORDER BY data_vencimento, id";
        java.util.List<ContratoFornecedor> contratos = new java.util.ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(hoje));
            ps.setDate(2, Date.valueOf(limite));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) contratos.add(mapear(rs));
            }
        }
        return contratos;
    }

    public java.util.List<ContratoFornecedor> listarPendentesPagamento(Connection c, LocalDate hoje)
            throws SQLException {
        String sql = "SELECT id, fornecedor_id, valor, data_vencimento, estado, nota_fiscal_validada, "
                + "pagamento_simulado_liberado, ledger_ultima_sequencia, ledger_ultimo_hash, criado_em "
                + "FROM contratos_fornecedor WHERE data_vencimento <= ? AND nota_fiscal_validada = TRUE "
                + "AND pagamento_simulado_liberado = FALSE AND estado NOT IN ('CANCELADO','FINALIZADO') "
                + "ORDER BY data_vencimento, id";
        java.util.List<ContratoFornecedor> contratos = new java.util.ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(hoje));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) contratos.add(mapear(rs));
            }
        }
        return contratos;
    }

    private static ContratoFornecedor mapear(ResultSet rs) throws SQLException {
        Date vencimento = rs.getDate("data_vencimento");
        java.sql.Timestamp criado = rs.getTimestamp("criado_em");
        return new ContratoFornecedor(rs.getLong("id"), rs.getInt("fornecedor_id"),
                rs.getBigDecimal("valor"), vencimento.toLocalDate(), StatusContrato.valueOf(rs.getString("estado")),
                rs.getBoolean("nota_fiscal_validada"), rs.getBoolean("pagamento_simulado_liberado"),
                rs.getInt("ledger_ultima_sequencia"), rs.getString("ledger_ultimo_hash"),
                criado == null ? (LocalDateTime) null : criado.toLocalDateTime());
    }
}
