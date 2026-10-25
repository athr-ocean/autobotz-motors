package autobotz;

import conexao.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class OrdemServicoDAO {

    public void inserirOrdem(OrdemServico ordem) {

        String sql = "INSERT INTO ordem_servico " +
                     "(cliente_id, veiculo_id, data_abertura, status) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

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

        } catch (SQLException e) {
            System.out.println("Erro ao inserir ordem de serviço: " + e.getMessage());
        }
    }

    public void inserirItem(ItemOS item) {

        String sql = "INSERT INTO item_os " +
                     "(ordem_servico_id, servico_id, quantidade, preco) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

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

        } catch (SQLException e) {
            System.out.println("Erro ao inserir item da OS: " + e.getMessage());
        }
    }

    public void buscarItensDaOrdem(int ordemServicoId) {

        String sql = """
            SELECT
                os.id AS ordem_id,
                c.nome AS cliente,
                v.marca,
                v.modelo,
                s.nome AS servico,
                ios.quantidade,
                ios.preco
            FROM ordem_servico os
            INNER JOIN cliente c
                ON os.cliente_id = c.id
            INNER JOIN veiculo v
                ON os.veiculo_id = v.id
            INNER JOIN item_os ios
                ON os.id = ios.ordem_servico_id
            INNER JOIN servico s
                ON ios.servico_id = s.id
            WHERE os.id = ?
        """;

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ordemServicoId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    System.out.println(
                        "OS: " + rs.getInt("ordem_id") +
                        " | Cliente: " + rs.getString("cliente") +
                        " | Veículo: " + rs.getString("marca") +
                        " " + rs.getString("modelo") +
                        " | Serviço: " + rs.getString("servico") +
                        " | Quantidade: " + rs.getInt("quantidade") +
                        " | Preço: R$ " + rs.getDouble("preco")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao buscar ordem de serviço: " + e.getMessage());
        }
    }

    public double calcularTotal(int ordemServicoId) {

        String sql = """
            SELECT SUM(quantidade * preco) AS total
            FROM item_os
            WHERE ordem_servico_id = ?
        """;

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, ordemServicoId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao calcular total: " + e.getMessage());
        }

        return 0.0;
        
    }
}