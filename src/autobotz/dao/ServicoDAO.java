package autobotz.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import autobotz.model.Servico;
import autobotz.util.ConexaoBanco;
import autobotz.util.I18nUtils;

public class ServicoDAO {
    public java.util.List<Servico> listar() throws SQLException {
        var lista = new java.util.ArrayList<Servico>();
        try (var stmt = ConexaoBanco.getConexao().prepareStatement(
                "SELECT id_servico, nome, preco FROM servicos ORDER BY id_servico");
             var rs = stmt.executeQuery()) {
            while (rs.next()) lista.add(new Servico(rs.getInt(1), rs.getString(2), rs.getDouble(3)));
        }
        return lista;
    }

    public void inserir(Servico servico) throws SQLException {
        if (servico == null || !Double.isFinite(servico.getPreco()) || servico.getPreco() <= 0) {
            throw new IllegalArgumentException(I18nUtils.getString("oficina.preco_invalido"));
        }
        String sql = "INSERT INTO servicos (nome, preco) VALUES (?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, servico.getNome());
            stmt.setDouble(2, servico.getPreco());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    servico.setId(rs.getInt(1));
                }
            }
        }
    }

    public Servico buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_servico, nome, preco FROM servicos WHERE id_servico = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Servico(rs.getInt("id_servico"),
                            rs.getString("nome"), rs.getDouble("preco"));
                }
            }
        }
        return null;
    }
}
