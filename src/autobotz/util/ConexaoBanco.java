// src/autobotz/ConexaoBanco.java
package autobotz.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBanco {
	private static final String URL = "jdbc:mysql://localhost:3306/autobotz_db?useTimezone=true&serverTimezone=UTC";
	private static final String USUARIO = "root";
	private static final String SENHA = ""; // TODO: ajustar para a senha real do seu MySQL local

	private static Connection conexao;

	private ConexaoBanco() {
	}

	public static Connection getConexao() throws SQLException {
		if (conexao == null || conexao.isClosed()) {
			try {
				conexao = DriverManager.getConnection(URL, USUARIO, SENHA);
			} catch (SQLException e) {
				throw new SQLException("Nao foi possivel conectar ao banco autobotz_db. "
						+ "Verifique se o MySQL esta rodando e se USUARIO/SENHA em ConexaoBanco.java estao corretos.", e);
			}
		}
		return conexao;
	}
}