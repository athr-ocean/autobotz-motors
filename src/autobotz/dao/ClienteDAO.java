package autobotz.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import autobotz.model.Cliente;
import autobotz.util.ConexaoBanco;
import autobotz.util.CriptografiaDadosPessoais;
import autobotz.util.I18nUtils;

public class ClienteDAO {
    private static final String CAMPOS_CLIENTE =
            "SELECT id_cliente, nome, cpf, cpf_hash, telefone, email, ativo FROM clientes ";

    public void salvar(Cliente cliente) throws SQLException {
        if (buscarPorCpf(cliente.getCpf()) != null) {
            throw new SQLException(
                    I18nUtils.getString("cliente.cpf_duplicado")
                    + cliente.getCpf()
                    + "."
            );
        }

        String sql = "INSERT INTO clientes "
                + "(nome, cpf, cpf_hash, telefone, email, ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String cpfHash = CriptografiaDadosPessoais.fingerprintCpf(cliente.getCpf());
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, CriptografiaDadosPessoais.proteger(cliente.getNome(), "nome"));
            stmt.setString(2, CriptografiaDadosPessoais.proteger(cliente.getCpf(), "cpf"));
            stmt.setString(3, cpfHash);
            stmt.setString(4, CriptografiaDadosPessoais.proteger(cliente.getTelefone(), "telefone"));
            stmt.setString(5, CriptografiaDadosPessoais.proteger(cliente.getEmail(), "email"));
            stmt.setBoolean(6, cliente.isAtivo());
            stmt.executeUpdate();
        }
    }

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = CAMPOS_CLIENTE + "ORDER BY id_cliente";
        try (Statement stmt = ConexaoBanco.getConexao().createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Cliente buscarPorId(int id) throws SQLException {
        String sql = CAMPOS_CLIENTE + "WHERE id_cliente = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    public Cliente buscarPorCpf(String cpf) throws SQLException {
        if (cpf == null) {
            return null;
        }

        String hash = CriptografiaDadosPessoais.fingerprintCpf(cpf);
        String sqlProtegido = CAMPOS_CLIENTE
                + "WHERE cpf_hash = ? ORDER BY id_cliente LIMIT 1";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sqlProtegido)) {
            stmt.setString(1, hash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }

        // Leitura temporária dos registros anteriores à migração.
        // A migração explícita remove essa necessidade depois de concluída.
        String sqlLegado = CAMPOS_CLIENTE
                + "WHERE cpf = ? AND (cpf_hash IS NULL OR cpf_hash = '') "
                + "ORDER BY id_cliente LIMIT 1";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sqlLegado)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    // Preserva o contrato original de Higor, compatibilizado com o schema integrado.
    public boolean atualizar(Cliente cliente) throws SQLException {
        if (cliente.getId() <= 0) {
            throw new IllegalArgumentException(I18nUtils.getString("cliente.id_invalido"));
        }
        Cliente atual = buscarPorId(cliente.getId());
        if (atual == null) {
            return false;
        }

        // CPF mascarado pode ser compartilhado por clientes anonimizados.
        if (!Objects.equals(atual.getCpf(), cliente.getCpf())
                && buscarPorCpf(cliente.getCpf()) != null) {
            throw new SQLException(
                    I18nUtils.getString("cliente.cpf_duplicado") + cliente.getCpf() + ".");
        }

        String sql = "UPDATE clientes SET nome = ?, cpf = ?, cpf_hash = ?, "
                + "telefone = ?, email = ?, ativo = ? WHERE id_cliente = ?";
        String cpfHash = CriptografiaDadosPessoais.fingerprintCpf(cliente.getCpf());
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, CriptografiaDadosPessoais.proteger(cliente.getNome(), "nome"));
            stmt.setString(2, CriptografiaDadosPessoais.proteger(cliente.getCpf(), "cpf"));
            stmt.setString(3, cpfHash);
            stmt.setString(4, CriptografiaDadosPessoais.proteger(cliente.getTelefone(), "telefone"));
            stmt.setString(5, CriptografiaDadosPessoais.proteger(cliente.getEmail(), "email"));
            stmt.setBoolean(6, cliente.isAtivo());
            stmt.setInt(7, cliente.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean anonimizar(int id) throws SQLException {
        String cpfMascarado = "000.000.000-00";
        String sql = "UPDATE clientes SET nome = ?, cpf = ?, cpf_hash = ?, "
                + "telefone = NULL, email = NULL, ativo = ? WHERE id_cliente = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, CriptografiaDadosPessoais.proteger("ANÔNIMO", "nome"));
            stmt.setString(2, CriptografiaDadosPessoais.proteger(cpfMascarado, "cpf"));
            stmt.setString(3, CriptografiaDadosPessoais.fingerprintCpf(cpfMascarado));
            stmt.setBoolean(4, false);
            stmt.setInt(5, id);
            return stmt.executeUpdate() > 0;
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id_cliente"),
                CriptografiaDadosPessoais.revelar(rs.getString("nome"), "nome"),
                CriptografiaDadosPessoais.revelar(rs.getString("cpf"), "cpf"),
                CriptografiaDadosPessoais.revelar(rs.getString("telefone"), "telefone"),
                CriptografiaDadosPessoais.revelar(rs.getString("email"), "email"),
                rs.getBoolean("ativo"));
    }
}
