package autobotz.dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import autobotz.util.ConexaoBanco;

public class AuditoriaDAO {
    public void registrar(String acao, int usuarioId) {
        String sql = "INSERT INTO log_auditoria (acao, usuario_id) VALUES (?, ?)";
        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, acao);
            stmt.setInt(2, usuarioId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro na auditoria: " + e.getMessage());
        }
    }
}