// src/autobotz/VeiculoDAO.java
package autobotz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VeiculoDAO {

	public void cadastrar(Veiculo veiculo) throws SQLException {
		String sql = "INSERT INTO veiculos (marca, modelo, ano, preco, status) VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setString(1, veiculo.getMarca());
			stmt.setString(2, veiculo.getModelo());
			stmt.setInt(3, veiculo.getAno());
			stmt.setDouble(4, veiculo.getPreco());
			stmt.setString(5, veiculo.getStatus());
			stmt.executeUpdate();
		}
	}

	public List<Veiculo> listar() throws SQLException {
		List<Veiculo> lista = new ArrayList<>();
		String sql = "SELECT id_veiculo, marca, modelo, ano, preco, status FROM veiculos ORDER BY id_veiculo";
		try (Statement stmt = ConexaoBanco.getConexao().createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) lista.add(mapear(rs));
		}
		return lista;
	}

	public Veiculo buscarPorId(int id) throws SQLException {
		String sql = "SELECT id_veiculo, marca, modelo, ano, preco, status FROM veiculos WHERE id_veiculo = ?";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setInt(1, id);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) return mapear(rs);
			}
		}
		return null;
	}

	public void atualizar(int id, String marca, String modelo, int ano, double preco) throws SQLException {
		String sql = "UPDATE veiculos SET marca = ?, modelo = ?, ano = ?, preco = ? WHERE id_veiculo = ?";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setString(1, marca);
			stmt.setString(2, modelo);
			stmt.setInt(3, ano);
			stmt.setDouble(4, preco);
			stmt.setInt(5, id);
			stmt.executeUpdate();
		}
	}

	public void atualizarStatus(int id, String status, Connection conexaoExterna) throws SQLException {
		String sql = "UPDATE veiculos SET status = ? WHERE id_veiculo = ?";
		try (PreparedStatement stmt = conexaoExterna.prepareStatement(sql)) {
			stmt.setString(1, status);
			stmt.setInt(2, id);
			stmt.executeUpdate();
		}
	}

	public void deletar(int id) throws SQLException {
		String sql = "DELETE FROM veiculos WHERE id_veiculo = ?";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setInt(1, id);
			stmt.executeUpdate();
		}
	}

	private Veiculo mapear(ResultSet rs) throws SQLException {
		return new Veiculo(rs.getInt("id_veiculo"), rs.getString("marca"), rs.getString("modelo"),
				rs.getInt("ano"), rs.getDouble("preco"), rs.getString("status"));
	}
}