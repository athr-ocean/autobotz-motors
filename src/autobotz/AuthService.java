package autobotz;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {

    private final UsuarioDAO usuarioDAO;

    public AuthService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public String gerarHash(String senha) {

        try {

            MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

            byte[] hash =
                digest.digest(
                    senha.getBytes(StandardCharsets.UTF_8)
                );

            StringBuilder hexadecimal = new StringBuilder();

            for (byte b : hash) {
                hexadecimal.append(
                    String.format("%02x", b)
                );
            }

            return hexadecimal.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(
                "SHA-256 não disponível.",
                e
            );
        }
    }

    public boolean cadastrar(
        String nomeUsuario,
        String senha,
        PerfilUsuario perfil
    ) {

        if (usuarioDAO.buscarPorNome(nomeUsuario) != null) {
            return false;
        }

        String senhaHash = gerarHash(senha);

        Usuario usuario =
            new Usuario(
                nomeUsuario,
                senhaHash,
                perfil
            );

        usuarioDAO.salvar(usuario);

        return true;
    }

    public Usuario autenticar(
        String nomeUsuario,
        String senha
    ) {

        Usuario usuario =
            usuarioDAO.buscarPorNome(nomeUsuario);

        if (usuario == null) {
            return null;
        }

        String senhaHashInformada =
            gerarHash(senha);

        if (senhaHashInformada.equals(
                usuario.getSenhaHash())) {

            return usuario;
        }

        return null;
    }
}