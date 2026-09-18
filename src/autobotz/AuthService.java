package autobotz;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class AuthService {
    private final UsuarioDAO usuarioDAO;

    public AuthService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public String gerarHash(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte b : hash) {
                hexadecimal.append(String.format("%02x", b));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel.", e);
        }
    }

    public boolean cadastrar(String nomeUsuario, String senha, PerfilUsuario perfil) throws SQLException {
        if (usuarioDAO.buscarPorNome(nomeUsuario) != null) {
            return false;
        }
        usuarioDAO.salvar(new Usuario(nomeUsuario, gerarHash(senha), perfil));
        return true;
    }

    public Usuario autenticar(String nomeUsuario, String senha) throws SQLException {
        Usuario usuario = usuarioDAO.buscarPorNome(nomeUsuario);
        if (usuario != null && gerarHash(senha).equals(usuario.getSenhaHash())) {
            return usuario;
        }
        return null;
    }
}
