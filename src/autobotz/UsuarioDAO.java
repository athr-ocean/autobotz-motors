package autobotz;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import autobotz.util.ConexaoBanco;

public class UsuarioDAO {

    public void salvar(Usuario usuario) throws SQLException {

        String sql =
                "INSERT INTO usuarios " +
                "(nome_usuario, senha_hash, perfil) " +
                "VALUES (?, ?, ?)";

        try (PreparedStatement stmt =
                     ConexaoBanco.getConexao()
                             .prepareStatement(
                                     sql,
                                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNomeUsuario());
            stmt.setString(2, usuario.getSenhaHash());
            stmt.setString(3, usuario.getPerfil().name());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {

                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
        }
    }

    public Usuario buscarPorNome(String nomeUsuario)
            throws SQLException {

        String sql =
                "SELECT id_usuario, nome_usuario, senha_hash, perfil " +
                "FROM usuarios WHERE nome_usuario = ?";

        try (PreparedStatement stmt =
                     ConexaoBanco.getConexao()
                             .prepareStatement(sql)) {

            stmt.setString(1, nomeUsuario);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return new Usuario(
                            rs.getInt("id_usuario"),
                            rs.getString("nome_usuario"),
                            rs.getString("senha_hash"),
                            PerfilUsuario.valueOf(
                                    rs.getString("perfil")
                            )
                    );
                }
            }
        }

        return null;
    }
}