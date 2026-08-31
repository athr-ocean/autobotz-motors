package autobotz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VeiculoDAO {

    public void cadastrar(Veiculo v) {
        String sql = "INSERT INTO veiculos (marca, modelo, ano, preco, status) VALUES (?, ?, ?, ?, 'Disponível')";
        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, v.getMarca());
            stmt.setString(2, v.getModelo());
            stmt.setInt(3, v.getAno());
            stmt.setDouble(4, v.getPreco());
            stmt.execute();
            System.out.println("Veículo salvo com sucesso!");
        } catch (Exception e) {
            System.out.println("Erro ao salvar: " + e.getMessage());
        }
    }

    public List<Veiculo> listar() {
        List<Veiculo> lista = new ArrayList<>();
        String sql = "SELECT * FROM veiculos";
        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Veiculo v = new Veiculo(
                    rs.getInt("id_veiculo"),
                    rs.getString("marca"),
                    rs.getString("modelo"),
                    rs.getInt("ano"),
                    rs.getDouble("preco"),
                    rs.getString("status")
                );
                lista.add(v);
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar: " + e.getMessage());
        }
        return lista;
    }

    public void atualizar(Veiculo v) {
        String sql = "UPDATE veiculos SET marca=?, modelo=?, ano=?, preco=? WHERE id_veiculo=?";
        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, v.getMarca());
            stmt.setString(2, v.getModelo());
            stmt.setInt(3, v.getAno());
            stmt.setDouble(4, v.getPreco());
            stmt.setInt(5, v.getId());
            stmt.execute();
            System.out.println("Veículo atualizado!");
        } catch (Exception e) {
            System.out.println("Erro ao atualizar: " + e.getMessage());
        }
    }

    public void deletar(int id) {
        String sql = "DELETE FROM veiculos WHERE id_veiculo=?";
        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.execute();
            System.out.println("Veículo deletado!");
        } catch (Exception e) {
            System.out.println("Erro ao deletar: " + e.getMessage());
        }
    }
}