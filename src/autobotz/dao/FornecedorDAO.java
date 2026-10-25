package autobotz.dao;

import autobotz.model.Fornecedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistência de fornecedores. Contrato congelado com o D13:
 * {@link #buscarPorId(int)} retorna o fornecedor ou null, e lança SQLException em falha de banco.
 * Os métodos que recebem {@link Connection} participam da transação de quem chamou.
 */
public class FornecedorDAO {

    /**
     * Única linha que decide o banco usado por buscarPorId(int) e listar().
     * Deve apontar para a mesma classe usada em ComprasService.abrirConexao().
     */
    private static Connection abrirConexao() throws SQLException {
        return autobotz.util.ConexaoBanco.getConexao();          // OFICIAL
        // return autobotz.util.FabricaConexao.getConexao();     // TESTES (banco1): use esta linha no lugar da de cima
    }

    public Fornecedor buscarPorId(int id) throws SQLException {
        try (Connection c = abrirConexao()) {
            return buscarPorId(c, id);
        }
    }

    public Fornecedor buscarPorId(Connection c, int id) throws SQLException {
        String sql = "SELECT id, nome, cnpj FROM fornecedores WHERE id = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Fornecedor buscarPorCnpj(Connection c, String cnpj) throws SQLException {
        String sql = "SELECT id, nome, cnpj FROM fornecedores WHERE cnpj = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cnpj);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public List<Fornecedor> listar() throws SQLException {
        String sql = "SELECT id, nome, cnpj FROM fornecedores ORDER BY nome, id";
        List<Fornecedor> lista = new ArrayList<>();
        try (Connection c = abrirConexao();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    /** Insere o fornecedor (o id do argumento é ignorado) e devolve o id gerado. */
    public int inserir(Connection c, Fornecedor f) throws SQLException {
        String sql = "INSERT INTO fornecedores (nome, cnpj) VALUES (?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, f.getNome());
            if (f.getCnpj() == null) {
                ps.setNull(2, Types.CHAR);
            } else {
                ps.setString(2, f.getCnpj());
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                throw new SQLException("INSERT em fornecedores não retornou chave gerada");
            }
        }
    }

    private static Fornecedor mapear(ResultSet rs) throws SQLException {
        return new Fornecedor(rs.getInt("id"), rs.getString("nome"), rs.getString("cnpj"));
    }
}
