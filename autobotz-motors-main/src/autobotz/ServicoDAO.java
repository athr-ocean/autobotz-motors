package autobotz;

import conexao.Conexao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServicoDAO {

    public void inserir(Servico servico) {

        String sql = "INSERT INTO servico (nome, preco) VALUES (?, ?)";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, servico.getNome());
            stmt.setDouble(2, servico.getPreco());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    servico.setId(rs.getInt(1));
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao inserir serviço: " + e.getMessage());
        }
    }

    public Servico buscarPorId(int id) {

        String sql = "SELECT id, nome, preco FROM servico WHERE id = ?";

        try (Connection conn = Conexao.getConexao();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return new Servico(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getDouble("preco")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao buscar serviço: " + e.getMessage());
        }

        return null;
    }
}