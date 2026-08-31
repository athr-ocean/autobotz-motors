package autobotz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {
    
    public void cadastrar(Cliente c) {
        String sql = "INSERT INTO clientes (nome, cpf, telefone, email) VALUES (?, ?, ?, ?)";
        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, c.getNome());
            stmt.setString(2, c.getCpf());
            stmt.setString(3, c.getTelefone());
            stmt.setString(4, c.getEmail());
            stmt.execute();
            System.out.println("Cliente cadastrado com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao cadastrar cliente: " + e.getMessage());
        }
    }

    public List<Cliente> listar() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes";
        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new Cliente(
                    rs.getInt("id_cliente"),
                    rs.getString("nome"),
                    rs.getString("cpf"),
                    rs.getString("telefone"),
                    rs.getString("email")
                ));
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar clientes: " + e.getMessage());
        }
        return lista;
    }
}