// src/autobotz/ConexaoBanco.java
package autobotz.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBanco {
	private static final String URL = "jdbc:mariadb://localhost:3306/autobotz_db";
	private static final String USUARIO = System.getenv().getOrDefault("AUTOBOTZ_DB_USER", "root");
	private static final String SENHA = System.getenv().getOrDefault("AUTOBOTZ_DB_PASSWORD", "joao2160");

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
