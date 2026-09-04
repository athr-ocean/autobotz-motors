// src/autobotz/ClienteDAO.java
package autobotz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

	public void salvar(Cliente cliente) throws SQLException {
		if (buscarPorCpf(cliente.getCpf()) != null) {
			throw new SQLException("Ja existe um cliente cadastrado com o CPF " + cliente.getCpf() + ".");
		}
		String sql = "INSERT INTO clientes (nome, cpf, telefone, email) VALUES (?, ?, ?, ?)";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setString(1, cliente.getNome());
			stmt.setString(2, cliente.getCpf());
			stmt.setString(3, cliente.getTelefone());
			stmt.setString(4, cliente.getEmail());
			stmt.executeUpdate();
		}
	}

	public List<Cliente> listar() throws SQLException {
		List<Cliente> lista = new ArrayList<>();
		String sql = "SELECT id_cliente, nome, cpf, telefone, email FROM clientes ORDER BY id_cliente";
		try (Statement stmt = ConexaoBanco.getConexao().createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) lista.add(mapear(rs));
		}
		return lista;
	}

	public Cliente buscarPorId(int id) throws SQLException {
		String sql = "SELECT id_cliente, nome, cpf, telefone, email FROM clientes WHERE id_cliente = ?";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setInt(1, id);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) return mapear(rs);
			}
		}
		return null;
	}

	public Cliente buscarPorCpf(String cpf) throws SQLException {
		String sql = "SELECT id_cliente, nome, cpf, telefone, email FROM clientes WHERE cpf = ?";
		try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
			stmt.setString(1, cpf);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) return mapear(rs);
			}
		}
		return null;
	}

	private Cliente mapear(ResultSet rs) throws SQLException {
		return new Cliente(rs.getInt("id_cliente"), rs.getString("nome"),
				rs.getString("cpf"), rs.getString("telefone"), rs.getString("email"));
	}
}