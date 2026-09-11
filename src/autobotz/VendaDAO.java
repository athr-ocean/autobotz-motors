// src/autobotz/VendaDAO.java
package autobotz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {

	private final ClienteDAO clienteDAO = new ClienteDAO();
	private final VeiculoDAO veiculoDAO = new VeiculoDAO();

	public Venda registrarVenda(int idCliente, int idVeiculo, double valorFinal) throws SQLException {
		if (clienteDAO.buscarPorId(idCliente) == null) {
			throw new SQLException("Cliente " + idCliente + " nao encontrado.");
		}
		Veiculo veiculo = veiculoDAO.buscarPorId(idVeiculo);
		if (veiculo == null) {
			throw new SQLException("Veiculo " + idVeiculo + " nao encontrado.");
		}
		if (!"Disponivel".equals(veiculo.getStatus())) {
			throw new SQLException("Veiculo " + idVeiculo + " nao esta disponivel para venda (status atual: " + veiculo.getStatus() + ").");
		}

		Connection conexao = ConexaoBanco.getConexao();
		boolean autoCommitOriginal = conexao.getAutoCommit();
		try {
			conexao.setAutoCommit(false);

			String sql = "INSERT INTO vendas (id_cliente, id_veiculo, valor_final) VALUES (?, ?, ?)";
			int idGerado;
			try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setInt(1, idCliente);
				stmt.setInt(2, idVeiculo);
				stmt.setDouble(3, valorFinal);
				stmt.executeUpdate();
				try (ResultSet rs = stmt.getGeneratedKeys()) {
					rs.next();
					idGerado = rs.getInt(1);
				}
			}

			veiculoDAO.atualizarStatus(idVeiculo, "Vendido", conexao);

			conexao.commit();
			return new Venda(idGerado, idCliente, idVeiculo, valorFinal);
		} catch (SQLException e) {
			conexao.rollback();
			throw e;
		} finally {
			conexao.setAutoCommit(autoCommitOriginal);
		}
	}

	public List<String> listarVendas() throws SQLException {
		List<String> lista = new ArrayList<>();
		String sql = "SELECT v.id_venda, c.nome, ve.marca, ve.modelo, v.valor_final, v.data_venda "
				+ "FROM vendas v JOIN clientes c ON v.id_cliente = c.id_cliente "
				+ "JOIN veiculos ve ON v.id_veiculo = ve.id_veiculo ORDER BY v.id_venda";
		try (Statement stmt = ConexaoBanco.getConexao().createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				lista.add("Venda #" + rs.getInt("id_venda") + " | Cliente: " + rs.getString("nome")
						+ " | Veiculo: " + rs.getString("marca") + " " + rs.getString("modelo")
						+ " | Valor: R$" + rs.getDouble("valor_final")
						+ " | Data: " + rs.getTimestamp("data_venda"));
			}
		}
		return lista;
	}
}