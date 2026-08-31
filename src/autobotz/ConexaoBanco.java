package autobotz;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConexaoBanco {
    public static Connection obterConexao() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/autobotz_db", "root", "");
        } catch (Exception e) {
            throw new RuntimeException("Erro na conexão: " + e.getMessage());
        }
    }
}