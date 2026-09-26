package autobotz.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import autobotz.model.Cliente;
import autobotz.util.ConexaoBanco;
import autobotz.util.I18nUtils;

public class ClienteDAO {

    public void salvar(Cliente cliente) throws SQLException {
        if (buscarPorCpf(cliente.getCpf()) != null) {
            throw new SQLException(
                    I18nUtils.getString("cliente.cpf_duplicado")
                    + cliente.getCpf()
                    + "."
            );
        }
        String sql = "INSERT INTO clientes (nome, cpf, telefone, email, ativo) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpf());
            stmt.setString(3, cliente.getTelefone());
            stmt.setString(4, cliente.getEmail());
            stmt.setBoolean(5, cliente.isAtivo());
            stmt.executeUpdate();
        }
    }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nome, cpf, telefone, email, ativo FROM clientes ORDER BY id_cliente";
        try (Statement stmt = ConexaoBanco.getConexao().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Cliente buscarPorId(int id) throws SQLException {
        String sql = "SELECT id_cliente, nome, cpf, telefone, email, ativo FROM clientes WHERE id_cliente = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public Cliente buscarPorCpf(String cpf) throws SQLException {
        String sql = "SELECT id_cliente, nome, cpf, telefone, email, ativo FROM clientes WHERE cpf = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    // Restaura atualizar(Cliente), de Higor (0219a18), no schema/JDBC integrado.
    public boolean atualizar(Cliente cliente) throws SQLException {
        if (cliente.getId() <= 0) {
            throw new IllegalArgumentException(I18nUtils.getString("cliente.id_invalido"));
        }
        Cliente atual = buscarPorId(cliente.getId());
        if (atual == null) return false;

        // CPF mascarado pode ser compartilhado por clientes anonimizados.
        if (!java.util.Objects.equals(atual.getCpf(), cliente.getCpf())
                && buscarPorCpf(cliente.getCpf()) != null) {
            throw new SQLException(I18nUtils.getString("cliente.cpf_duplicado") + cliente.getCpf() + ".");
        }

        String sql = "UPDATE clientes SET nome = ?, cpf = ?, telefone = ?, email = ?, ativo = ? "
                + "WHERE id_cliente = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpf());
            stmt.setString(3, cliente.getTelefone());
            stmt.setString(4, cliente.getEmail());
            stmt.setBoolean(5, cliente.isAtivo());
            stmt.setInt(6, cliente.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean anonimizar(int id) throws SQLException {
        String sql = "UPDATE clientes SET nome = ?, cpf = ?, telefone = NULL, email = NULL, ativo = ? "
                + "WHERE id_cliente = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, "ANÔNIMO");
            stmt.setString(2, "000.000.000-00");
            stmt.setBoolean(3, false);
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("id_cliente"), rs.getString("nome"),
                rs.getString("cpf"), rs.getString("telefone"), rs.getString("email"),
                rs.getBoolean("ativo"));
    }
}
