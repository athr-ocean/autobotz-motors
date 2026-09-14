package autobotz.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import autobotz.util.ConexaoBanco;

public class RelatorioService {

    // Extrai o faturamento agrupando por ano e mês usando SUM e GROUP BY
    public void gerarRelatorioFaturamentoMensal() {
        // Ajustado para a tabela oficial de vendas da equipe
        String sql = "SELECT YEAR(data_venda) AS ano, MONTH(data_venda) AS mes, SUM(valor_venda) AS total_faturado " +
                     "FROM vendas " +
                     "GROUP BY ano, mes " +
                     "ORDER BY ano DESC, mes DESC";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            System.out.println("\n=== RELATÓRIO DE FATURAMENTO MENSAL ===");
            while (rs.next()) {
                int ano = rs.getInt("ano");
                int mes = rs.getInt("mes");
                double total = rs.getDouble("total_faturado");
                System.out.printf("Mês/Ano: %02d/%d | Faturamento: R$ %.2f\n", mes, ano, total);
            }
            System.out.println("=======================================\n");

        } catch (SQLException e) {
            System.err.println("Erro ao gerar relatório de faturamento: " + e.getMessage());
        }
    }
    
    // Filtra carros no pátio há mais de X dias usando DATEDIFF e HAVING
    public void listarVeiculosEncalhados(int diasTolerancia) {
        String sql = "SELECT marca, modelo, ano, DATEDIFF(NOW(), data_cadastro) AS dias_parados " +
                     "FROM veiculos " +
                     "WHERE status = 'Disponível' " +
                     "HAVING dias_parados >= ? " +
                     "ORDER BY dias_parados DESC";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, diasTolerancia);
            
            try (ResultSet rs = stmt.executeQuery()) {
                System.out.println("\n=== VEÍCULOS ENCALHADOS (+" + diasTolerancia + " dias) ===");
                while (rs.next()) {
                    String marca = rs.getString("marca");
                    String modelo = rs.getString("modelo");
                    int ano = rs.getInt("ano");
                    int dias = rs.getInt("dias_parados");
                    System.out.printf("%s %s (%d) - Parado há %d dias\n", marca, modelo, ano, dias);
                }
                System.out.println("===================================================\n");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao buscar veículos encalhados: " + e.getMessage());
        }
    }
}