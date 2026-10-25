package autobotz.dao;

import autobotz.model.Veiculo;
import autobotz.util.ConexaoBanco;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VeiculoDAO {

    public void cadastrar(Veiculo veiculo) throws SQLException {
        String sql = "INSERT INTO veiculos (placa, modelo, marca, ano, preco, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, veiculo.getPlaca());
            stmt.setString(2, veiculo.getModelo());
            stmt.setString(3, veiculo.getMarca());
            stmt.setInt(4, veiculo.getAno());
            stmt.setDouble(5, veiculo.getPreco());
            stmt.setString(6, veiculo.getStatus());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    veiculo.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void salvar(Veiculo veiculo) throws SQLException {
        cadastrar(veiculo);
    }

    public List<Veiculo> listar() throws SQLException {
        return listarTodos();
    }

    public List<Veiculo> listarTodos() throws SQLException {
        List<Veiculo> veiculos = new ArrayList<>();
        String sql = "SELECT id, placa, modelo, marca, ano, preco, status FROM veiculos ORDER BY id";
        try (Statement stmt = ConexaoBanco.getConexao().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                veiculos.add(mapear(rs));
            }
        }
        return veiculos;
    }

    public Veiculo buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, placa, modelo, marca, ano, preco, status FROM veiculos WHERE id = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public void atualizar(int id, String marca, String modelo, int ano, double preco) throws SQLException {
        String sql = "UPDATE veiculos SET marca = ?, modelo = ?, ano = ?, preco = ? WHERE id = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, marca);
            stmt.setString(2, modelo);
            stmt.setInt(3, ano);
            stmt.setDouble(4, preco);
            stmt.setInt(5, id);
            stmt.executeUpdate();
        }
    }

    public void atualizar(Veiculo veiculo) throws SQLException {
        String sql = "UPDATE veiculos SET placa = ?, modelo = ?, marca = ?, ano = ?, preco = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, veiculo.getPlaca());
            stmt.setString(2, veiculo.getModelo());
            stmt.setString(3, veiculo.getMarca());
            stmt.setInt(4, veiculo.getAno());
            stmt.setDouble(5, veiculo.getPreco());
            stmt.setString(6, veiculo.getStatus());
            stmt.setInt(7, veiculo.getId());
            stmt.executeUpdate();
        }
    }

    public boolean deletar(int id) throws SQLException {
        String sql = "DELETE FROM veiculos WHERE id = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean remover(int id) throws SQLException {
        return deletar(id);
    }

    private Veiculo mapear(ResultSet rs) throws SQLException {
        return new Veiculo(rs.getInt("id"), rs.getString("placa"),
                rs.getString("modelo"), rs.getString("marca"), rs.getInt("ano"),
                rs.getDouble("preco"), rs.getString("status"));
    }
}
