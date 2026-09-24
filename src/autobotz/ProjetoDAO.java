package autobotz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import autobotz.util.ConexaoBanco;

public class ProjetoDAO {
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

    public void atualizarMembros(String listaMembros) throws SQLException {

    String sql = """
            INSERT INTO membros_projeto (id, lista_membros)
            VALUES (1, ?)
            ON DUPLICATE KEY UPDATE lista_membros = ?
            """;

    try (Connection conexao = ConexaoBanco.getConexao();
         PreparedStatement stmt = conexao.prepareStatement(sql)) {

        stmt.setString(1, listaMembros);
        stmt.setString(2, listaMembros);

        stmt.executeUpdate();

        System.out.println(bundle.getString("ProjetoDAO.05"));
    }
}

public void consultarMembros() throws SQLException {

    String sql = """
            SELECT lista_membros
            FROM membros_projeto
            WHERE id = 1
            """;

    try (Connection conexao = ConexaoBanco.getConexao();
         PreparedStatement stmt = conexao.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

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