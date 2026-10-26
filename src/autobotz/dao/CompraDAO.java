package autobotz.dao;

import autobotz.model.OrcamentoCompra;
import autobotz.model.PedidoCompra;
import autobotz.model.SolicitacaoCompra;
import autobotz.model.StatusSolicitacao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * SQL do fluxo de compra. Todos os métodos recebem a {@link Connection} do chamador:
 * a fronteira transacional pertence ao {@code ComprasService}, nunca ao DAO.
 * Métodos "paraAtualizar" usam SELECT ... FOR UPDATE (bloqueio de linha no InnoDB).
 */
public class CompraDAO {

    // ---------- orçamento ----------

    public int inserirOrcamento(Connection c, OrcamentoCompra o) throws SQLException {
        String sql = "INSERT INTO orcamentos_compra (nome, saldo_inicial, saldo_atual) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getNome());
            ps.setBigDecimal(2, o.getSaldoInicial());
            ps.setBigDecimal(3, o.getSaldoAtual());
            ps.executeUpdate();
            return chaveGerada(ps);
        }
    }

    public OrcamentoCompra buscarOrcamento(Connection c, int id, boolean paraAtualizar) throws SQLException {
        String sql = "SELECT id, nome, saldo_inicial, saldo_atual FROM orcamentos_compra WHERE id = ?"
                + (paraAtualizar ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new OrcamentoCompra(rs.getInt("id"), rs.getString("nome"),
                        rs.getBigDecimal("saldo_inicial"), rs.getBigDecimal("saldo_atual"));
            }
        }
    }

    /** Baixa condicional: só debita se o saldo cobrir o valor. Retorna true se debitou. */
    public boolean debitarSaldo(Connection c, int orcamentoId, BigDecimal valor) throws SQLException {
        String sql = "UPDATE orcamentos_compra SET saldo_atual = saldo_atual - ? WHERE id = ? AND saldo_atual >= ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, valor);
            ps.setInt(2, orcamentoId);
            ps.setBigDecimal(3, valor);
            return ps.executeUpdate() == 1;
        }
    }

    // ---------- solicitação ----------

    public int inserirSolicitacao(Connection c, SolicitacaoCompra s) throws SQLException {
        String sql = "INSERT INTO solicitacoes_compra "
                + "(orcamento_id, fornecedor_id, descricao, valor, status, solicitante) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getOrcamentoId());
            ps.setInt(2, s.getFornecedorId());
            ps.setString(3, s.getDescricao());
            ps.setBigDecimal(4, s.getValor());
            ps.setString(5, s.getStatus().name());
            ps.setString(6, s.getSolicitante());
            ps.executeUpdate();
            return chaveGerada(ps);
        }
    }

    public SolicitacaoCompra buscarSolicitacao(Connection c, int id, boolean paraAtualizar) throws SQLException {
        String sql = "SELECT id, orcamento_id, fornecedor_id, descricao, valor, status, solicitante, "
                + "aprovador, motivo_rejeicao FROM solicitacoes_compra WHERE id = ?"
                + (paraAtualizar ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new SolicitacaoCompra(rs.getInt("id"), rs.getInt("orcamento_id"),
                        rs.getInt("fornecedor_id"), rs.getString("descricao"), rs.getBigDecimal("valor"),
                        StatusSolicitacao.valueOf(rs.getString("status")), rs.getString("solicitante"),
                        rs.getString("aprovador"), rs.getString("motivo_rejeicao"));
            }
        }
    }

    /** Registra a decisão (aprovador, motivo, data) e muda o status, só se o status atual for {@code de}. */
    public boolean registrarDecisao(Connection c, int solicitacaoId, StatusSolicitacao de, StatusSolicitacao para,
                                    String aprovador, String motivoRejeicao) throws SQLException {
        String sql = "UPDATE solicitacoes_compra SET status = ?, aprovador = ?, motivo_rejeicao = ?, "
                + "decidido_em = CURRENT_TIMESTAMP WHERE id = ? AND status = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, para.name());
            ps.setString(2, aprovador);
            if (motivoRejeicao == null) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, motivoRejeicao);
            }
            ps.setInt(4, solicitacaoId);
            ps.setString(5, de.name());
            return ps.executeUpdate() == 1;
        }
    }

    /** Muda apenas o status, só se o status atual for {@code de}. */
    public boolean avancarStatus(Connection c, int solicitacaoId, StatusSolicitacao de, StatusSolicitacao para)
            throws SQLException {
        String sql = "UPDATE solicitacoes_compra SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, para.name());
            ps.setInt(2, solicitacaoId);
            ps.setString(3, de.name());
            return ps.executeUpdate() == 1;
        }
    }

    // ---------- pedido ----------

    public int inserirPedido(Connection c, PedidoCompra p) throws SQLException {
        String sql = "INSERT INTO pedidos_compra (solicitacao_id, orcamento_id, fornecedor_id, valor) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getSolicitacaoId());
            ps.setInt(2, p.getOrcamentoId());
            ps.setInt(3, p.getFornecedorId());
            ps.setBigDecimal(4, p.getValor());
            ps.executeUpdate();
            return chaveGerada(ps);
        }
    }

    public PedidoCompra buscarPedidoPorSolicitacao(Connection c, int solicitacaoId) throws SQLException {
        String sql = "SELECT id, solicitacao_id, orcamento_id, fornecedor_id, valor "
                + "FROM pedidos_compra WHERE solicitacao_id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, solicitacaoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new PedidoCompra(rs.getInt("id"), rs.getInt("solicitacao_id"),
                        rs.getInt("orcamento_id"), rs.getInt("fornecedor_id"), rs.getBigDecimal("valor"));
            }
        }
    }

    private static int chaveGerada(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("INSERT não retornou chave gerada");
        }
    }
}
