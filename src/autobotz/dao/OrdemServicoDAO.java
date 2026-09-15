package autobotz.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import autobotz.model.ItemOS;
import autobotz.model.OrdemServico;
import autobotz.util.ConexaoBanco;

public class OrdemServicoDAO {
    public void inserirOrdem(OrdemServico ordem) throws SQLException {
        String sql = "INSERT INTO ordens_servico "
                + "(id_cliente, id_veiculo, data_abertura, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, ordem.getClienteId());
            stmt.setInt(2, ordem.getVeiculoId());
            stmt.setDate(3, java.sql.Date.valueOf(ordem.getDataAbertura()));
            stmt.setString(4, ordem.getStatus());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    ordem.setId(rs.getInt(1));
                }
            }
        }
    }

    public void inserirItem(ItemOS item) throws SQLException {
        String sql = "INSERT INTO itens_os "
                + "(id_ordem, id_servico, quantidade, preco) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getOrdemServicoId());
            stmt.setInt(2, item.getServicoId());
            stmt.setInt(3, item.getQuantidade());
            stmt.setDouble(4, item.getPreco());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        }
    }

    public List<String> buscarItensDaOrdem(int ordemServicoId) throws SQLException {
        String sql = "SELECT os.id_ordem, c.nome AS cliente, v.marca, v.modelo, "
                + "s.nome AS servico, ios.quantidade, ios.preco "
                + "FROM ordens_servico os "
                + "INNER JOIN clientes c ON os.id_cliente = c.id_cliente "
                + "INNER JOIN veiculos v ON os.id_veiculo = v.id "
                + "INNER JOIN itens_os ios ON os.id_ordem = ios.id_ordem "
                + "INNER JOIN servicos s ON ios.id_servico = s.id_servico "
                + "WHERE os.id_ordem = ?";
        List<String> itens = new ArrayList<>();
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, ordemServicoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add("OS: " + rs.getInt("id_ordem")
                            + " | Cliente: " + rs.getString("cliente")
                            + " | Veiculo: " + rs.getString("marca") + " " + rs.getString("modelo")
                            + " | Servico: " + rs.getString("servico")
                            + " | Quantidade: " + rs.getInt("quantidade")
                            + " | Preco: R$ " + rs.getDouble("preco"));
                }
            }
        }
        return itens;
    }

    public double calcularTotal(int ordemServicoId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantidade * preco), 0) AS total "
                + "FROM itens_os WHERE id_ordem = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, ordemServicoId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getDouble("total");
            }
        }
    }
}
