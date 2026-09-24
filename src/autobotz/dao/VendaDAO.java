package autobotz.dao;

import autobotz.model.Venda;
import autobotz.util.ConexaoBanco;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;

public class VendaDAO {
    public void registrarVenda(Venda venda) throws SQLException {
        validarVenda(venda);
        String verificarCliente = "SELECT 1 FROM clientes WHERE id_cliente = ?";
        String bloquearVeiculo = "SELECT status FROM veiculos WHERE id = ? FOR UPDATE";
        String baixarVeiculo = "UPDATE veiculos SET status = 'VENDIDO' WHERE id = ?";
        String inserirVenda = "INSERT INTO vendas (id_cliente, id_veiculo, valor_final, data_venda) "
                + "VALUES (?, ?, ?, ?)";

        try (Connection conexao = ConexaoBanco.getConexao()) {
            conexao.setAutoCommit(false);
            try (PreparedStatement clienteStmt = conexao.prepareStatement(verificarCliente);
                    PreparedStatement veiculoStmt = conexao.prepareStatement(bloquearVeiculo);
                    PreparedStatement baixaStmt = conexao.prepareStatement(baixarVeiculo);
                    PreparedStatement vendaStmt = conexao.prepareStatement(inserirVenda,
                            Statement.RETURN_GENERATED_KEYS)) {
                clienteStmt.setInt(1, venda.getIdCliente());
                try (ResultSet rs = clienteStmt.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Cliente nao encontrado.");
                }

                veiculoStmt.setInt(1, venda.getIdVeiculo());
                try (ResultSet rs = veiculoStmt.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Veiculo nao encontrado.");
                    String status = rs.getString("status");
                    if (!disponivel(status)) throw new IllegalArgumentException("Veiculo nao esta disponivel.");
                }

                baixaStmt.setInt(1, venda.getIdVeiculo());
                baixaStmt.executeUpdate();
                vendaStmt.setInt(1, venda.getIdCliente());
                vendaStmt.setInt(2, venda.getIdVeiculo());
                vendaStmt.setDouble(3, venda.getValorTotal());
                vendaStmt.setDate(4, Date.valueOf(venda.getDataVenda()));
                vendaStmt.executeUpdate();
                try (ResultSet keys = vendaStmt.getGeneratedKeys()) {
                    if (keys.next()) venda.setId(keys.getInt(1));
                }
                conexao.commit();
            } catch (SQLException | RuntimeException e) {
                try { conexao.rollback(); } catch (SQLException rollbackError) { e.addSuppressed(rollbackError); }
                throw e;
            } finally {
                try { conexao.setAutoCommit(true); } catch (SQLException ignored) { }
            }
        }
    }

    public void registrarVenda(int idVeiculo, int idCliente, double valor) throws SQLException {
        registrarVenda(new Venda(idVeiculo, idCliente, valor));
    }

    public List<Venda> listarVendas() throws SQLException {
        List<Venda> vendas = new ArrayList<>();
        String sql = "SELECT id_venda, id_veiculo, id_cliente, valor_final, data_venda "
                + "FROM vendas ORDER BY id_venda";
        try (Connection conexao = ConexaoBanco.getConexao();
                Statement stmt = conexao.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                vendas.add(new Venda(rs.getInt("id_venda"), rs.getInt("id_veiculo"),
                        rs.getInt("id_cliente"), rs.getDouble("valor_final"),
                        rs.getDate("data_venda").toLocalDate()));
            }
        }
        return vendas;
    }

    public List<Venda> listarVendasPorCliente(int idCliente) throws SQLException {
        List<Venda> vendas = new ArrayList<>();

        String sql = "SELECT id_venda, id_veiculo, id_cliente, valor_final, data_venda "
                   + "FROM vendas WHERE id_cliente = ? ORDER BY data_venda";

        try (Connection conexao = ConexaoBanco.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vendas.add(new Venda(
                        rs.getInt("id_venda"),
                        rs.getInt("id_veiculo"),
                        rs.getInt("id_cliente"),
                        rs.getDouble("valor_final"),
                        rs.getDate("data_venda").toLocalDate()
                    ));
                }
            }
        }

        return vendas;
    }
    
    private void validarVenda(Venda venda) {
        if (venda == null || venda.getIdCliente() <= 0 || venda.getIdVeiculo() <= 0) {
            throw new IllegalArgumentException("Cliente e veiculo devem ser validos.");
        }
        if (venda.getValorTotal() <= 0 || venda.getDataVenda() == null) {
            throw new IllegalArgumentException("Valor e data da venda devem ser validos.");
        }
    }

    private boolean disponivel(String status) {
        return status != null && (status.equalsIgnoreCase("DISPONIVEL")
                || status.equalsIgnoreCase("Disponivel") || status.equalsIgnoreCase("Disponível"));
    }
}
