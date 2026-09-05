package autobotz;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private static final String ARQUIVO = "usuarios.txt";

    public void salvar(Usuario usuario) {

        int novoId = proximoId();
        usuario.setId(novoId);

        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(ARQUIVO, true))) {

            writer.write(
                usuario.getId() + ";" +
                usuario.getNomeUsuario() + ";" +
                usuario.getSenhaHash() + ";" +
                usuario.getPerfil().name()
            );

            writer.newLine();

        } catch (IOException e) {
            System.err.println(
                "Erro ao salvar usuário: " + e.getMessage()
            );
        }
    }

    public Usuario buscarPorNome(String nomeUsuario) {

        File file = new File(ARQUIVO);

        if (!file.exists()) {
            return null;
        }

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(ARQUIVO))) {

            String linha;

            while ((linha = reader.readLine()) != null) {

                String[] dados = linha.split(";");

                if (dados.length >= 4 &&
                    dados[1].equals(nomeUsuario)) {

                    int id = Integer.parseInt(dados[0]);

                    String nome = dados[1];
                    String senhaHash = dados[2];

                    PerfilUsuario perfil =
                        PerfilUsuario.valueOf(dados[3]);

                    return new Usuario(
                        id,
                        nome,
                        senhaHash,
                        perfil
                    );
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            System.err.println(
                "Erro ao buscar usuário: " + e.getMessage()
            );
        }

        return null;
    }

    private int proximoId() {

        List<Usuario> usuarios = listar();

        int maiorId = 0;

        for (Usuario usuario : usuarios) {
            if (usuario.getId() > maiorId) {
                maiorId = usuario.getId();
            }
        }

        return maiorId + 1;
    }

    private List<Usuario> listar() {

        List<Usuario> lista = new ArrayList<>();

        File file = new File(ARQUIVO);

        if (!file.exists()) {
            return lista;
        }

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(ARQUIVO))) {

            String linha;

            while ((linha = reader.readLine()) != null) {

                String[] dados = linha.split(";");

                if (dados.length >= 4) {

                    int id = Integer.parseInt(dados[0]);

                    PerfilUsuario perfil =
                        PerfilUsuario.valueOf(dados[3]);

                    lista.add(
                        new Usuario(
                            id,
                            dados[1],
                            dados[2],
                            perfil
                        )
                    );
                }
            }

        } catch (IOException | IllegalArgumentException e) {
            System.err.println(
                "Erro ao listar usuários: " + e.getMessage()
            );
        }

        return lista;
    }
}