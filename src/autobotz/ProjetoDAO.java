package autobotz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import autobotz.util.ConexaoBanco;

public class ProjetoDAO {
    public record Resumo(String nome, String responsavel, String equipe, String status, String membros) { }

    public java.util.List<Resumo> listar() throws SQLException {
        var projetos = new java.util.ArrayList<Resumo>();
        String sql = "SELECT p.nome_projeto, p.responsavel, p.equipe, p.status, "
                + "(SELECT m.lista_membros FROM membros_projeto m WHERE m.id_projeto = p.id_projeto "
                + "ORDER BY m.id DESC LIMIT 1) AS membros FROM projetos p ORDER BY p.nome_projeto";
        try (var stmt = ConexaoBanco.getConexao().prepareStatement(sql); var rs = stmt.executeQuery()) {
            while (rs.next()) projetos.add(new Resumo(rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)));
        }
        return projetos;
    }

    private final ResourceBundle bundle;

    public ProjetoDAO(ResourceBundle bundle){
        this.bundle = bundle;
    }
    public void criarProjeto(
            String nomeProjeto,
            String responsavel,
            String equipe,
            String status) throws SQLException {

        String sql = """
                INSERT INTO projetos
                (nome_projeto, responsavel, equipe, status)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, nomeProjeto);
            stmt.setString(2, responsavel);
            stmt.setString(3, equipe);
            stmt.setString(4, status);

            stmt.executeUpdate();

            System.out.println(bundle.getString("ProjetoDAO.01"));
        }
    }

    public void atualizarProjeto(
            String nomeProjeto,
            String responsavel,
            String equipe,
            String status) throws SQLException {

        String sql = """
                UPDATE projetos
                SET responsavel = ?,
                    equipe = ?,
                    status = ?
                WHERE nome_projeto = ?
                """;

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, responsavel);
            stmt.setString(2, equipe);
            stmt.setString(3, status);
            stmt.setString(4, nomeProjeto);

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas == 0) {
                System.out.println(bundle.getString("ProjetoDAO.02"));
            } else {
                System.out.println(bundle.getString("ProjetoDAO.03"));
            }
        }
    }

    public void consultarProjetos() throws SQLException {

        String sql = """
                SELECT nome_projeto, responsavel, equipe, status
                FROM projetos
                ORDER BY nome_projeto
                """;

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                System.out.println(
                        bundle.getString("projeto.label") + rs.getString("nome_projeto")
                        + " | " + bundle.getString("projeto.responsavel_label") + rs.getString("responsavel")
                        + " | " + bundle.getString("projeto.equipe_label") + rs.getString("equipe")
                        + " | " + bundle.getString("projeto.status_label") + rs.getString("status")
                );
            }
        }
    }


    public void deletarProjeto(String nomeProjeto) throws SQLException {

    String sql = """
            DELETE FROM projetos
            WHERE nome_projeto = ?
            """;

            try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, nomeProjeto);

            int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas == 0) {
            System.out.println(bundle.getString("ProjetoDAO.02"));
            } else {
            System.out.println(bundle.getString("ProjetoDAO.04"));
            }
        }
    }

    public void atualizarMembros(
            String nomeProjeto,
            String listaMembros) throws SQLException {

        Integer idProjeto = buscarIdProjeto(nomeProjeto);

        if (idProjeto == null) {
            System.out.println(bundle.getString("ProjetoDAO.02"));
            return;
        }

        String sql = """
                UPDATE membros_projeto
                SET lista_membros = ?
                WHERE id_projeto = ?
                """;

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, listaMembros);
            stmt.setInt(2, idProjeto);

            int linhas = stmt.executeUpdate();

            if (linhas == 0) {
                String insert = """
                        INSERT INTO membros_projeto
                        (id_projeto, lista_membros)
                        VALUES (?, ?)
                        """;

                try (PreparedStatement novo =
                             conexao.prepareStatement(insert)) {

                    novo.setInt(1, idProjeto);
                    novo.setString(2, listaMembros);
                    novo.executeUpdate();
                }
            }
        }

        System.out.println(bundle.getString("ProjetoDAO.05"));
    }

    public void consultarMembros(
            String nomeProjeto) throws SQLException {

        Integer idProjeto = buscarIdProjeto(nomeProjeto);

        if (idProjeto == null) {
            System.out.println(bundle.getString("ProjetoDAO.02"));
            return;
        }

        String sql = """
                SELECT lista_membros
                FROM membros_projeto
                WHERE id_projeto = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idProjeto);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    System.out.println(
                            bundle.getString("projeto.membros_label")
                            + rs.getString("lista_membros")
                    );
                } else {
                    System.out.println(bundle.getString("ProjetoDAO.06"));
                }
            }
        }
    }

    private Integer buscarIdProjeto(
            String nomeProjeto) throws SQLException {

        String sql = """
                SELECT id_projeto
                FROM projetos
                WHERE nome_projeto = ?
                """;

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setString(1, nomeProjeto);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt("id_projeto");
                }
            }
        }

        return null;
    }

}
