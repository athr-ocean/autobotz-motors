package autobotz;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private static final String URL =
            "jdbc:mysql://localhost:3306/concessionaria";

    private static final String USUARIO = "root";
    private static final String SENHA = "1234";

    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    // Cadastrar cliente
    public void salvar(Cliente cliente) throws SQLException {

        if (buscarPorCpf(cliente.getCpf()) != null) {
            throw new SQLException(
                    "Ja existe um cliente cadastrado com o CPF "
                    + cliente.getCpf() + ".");
        }

        String sql = """
                INSERT INTO clientes
                (nome, cpf, telefone, email, ativo)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpf());
            stmt.setString(3, cliente.getTelefone());
            stmt.setString(4, cliente.getEmail());
            stmt.setBoolean(5, cliente.isAtivo());

            stmt.executeUpdate();
        }
    }

    // Listar clientes
    public List<Cliente> listar() throws SQLException {

        List<Cliente> clientes = new ArrayList<>();

        String sql = """
                SELECT id, nome, cpf, telefone, email, ativo
                FROM clientes
                ORDER BY id
                """;

        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clientes.add(mapear(rs));
            }
        }

        return clientes;
    }

    // Buscar cliente pelo ID
    public Cliente buscarPorId(int id) throws SQLException {

        String sql = """
                SELECT id, nome, cpf, telefone, email, ativo
                FROM clientes
                WHERE id = ?
                """;

        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }

        return null;
    }

    // Buscar cliente pelo CPF
    public Cliente buscarPorCpf(String cpf) throws SQLException {

        String sql = """
                SELECT id, nome, cpf, telefone, email, ativo
                FROM clientes
                WHERE cpf = ?
                """;

        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }

        return null;
    }

    // Atualizar cliente
    public boolean atualizar(Cliente cliente) throws SQLException {

        String sql = """
                UPDATE clientes
                SET nome = ?, cpf = ?, telefone = ?, email = ?, ativo = ?
                WHERE id = ?
                """;

        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpf());
            stmt.setString(3, cliente.getTelefone());
            stmt.setString(4, cliente.getEmail());
            stmt.setBoolean(5, cliente.isAtivo());
            stmt.setInt(6, cliente.getId());

            return stmt.executeUpdate() > 0;
        }
    }

    // Anonimização LGPD
    // NÃO utiliza DELETE
    public boolean anonimizar(int id) throws SQLException {

        String sql = """
                UPDATE clientes
                SET nome = ?,
                    cpf = ?,
                    telefone = ?,
                    email = ?,
                    ativo = ?
                WHERE id = ?
                """;

        try (Connection conn = conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "ANÔNIMO");
            stmt.setString(2, "000.000.000-00");
            stmt.setString(3, "00000000000");
            stmt.setString(4, "anonimo@anonimo.com");
            stmt.setBoolean(5, false);
            stmt.setInt(6, id);

            return stmt.executeUpdate() > 0;
        }
    }

    // Converte o resultado do banco em objeto Cliente
    private Cliente mapear(ResultSet rs) throws SQLException {

        return new Cliente(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cpf"),
                rs.getString("telefone"),
                rs.getString("email"),
                rs.getBoolean("ativo")
        );
    }
}
