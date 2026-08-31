package autobotz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VendaDAO {

    public void registrarVenda(Venda v) {
        String sqlVenda = "INSERT INTO vendas (id_cliente, id_veiculo, valor_final) VALUES (?, ?, ?)";
        String sqlStatus = "UPDATE veiculos SET status = 'Vendido' WHERE id_veiculo = ?";

        try (Connection con = ConexaoBanco.obterConexao()) {
            // Registra a venda
            try (PreparedStatement stmtVenda = con.prepareStatement(sqlVenda)) {
                stmtVenda.setInt(1, v.getIdCliente());
                stmtVenda.setInt(2, v.getIdVeiculo());
                stmtVenda.setDouble(3, v.getValorFinal());
                stmtVenda.execute();
            }

            // Atualiza o status do veiculo para Vendido
            try (PreparedStatement stmtStatus = con.prepareStatement(sqlStatus)) {
                stmtStatus.setInt(1, v.getIdVeiculo());
                stmtStatus.execute();
            }

            System.out.println("Venda registrada e veículo atualizado para 'Vendido'!");

        } catch (Exception e) {
            System.out.println("Erro ao registrar venda: " + e.getMessage());
        }
    }

    public List<String> listarVendas() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT v.id_venda, c.nome AS cliente, ve.marca, ve.modelo, v.valor_final " +
                     "FROM vendas v " +
                     "JOIN clientes c ON v.id_cliente = c.id_cliente " +
                     "JOIN veiculos ve ON v.id_veiculo = ve.id_veiculo";

        try (Connection con = ConexaoBanco.obterConexao();
             PreparedStatement stmt = con.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String linha = "Venda #" + rs.getInt("id_venda") +
                               " | Cliente: " + rs.getString("cliente") +
                               " | Veículo: " + rs.getString("marca") + " " + rs.getString("modelo") +
                               " | Valor Final: R$" + rs.getDouble("valor_final");
                lista.add(linha);
            }

        } catch (Exception e) {
            System.out.println("Erro ao listar vendas: " + e.getMessage());
        }

        return lista;
    }
}