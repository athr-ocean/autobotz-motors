package autobotz.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import autobotz.dao.AuditoriaDAO;
import autobotz.model.LogAuditoria;
import autobotz.util.ConexaoBanco;
import autobotz.util.I18nUtils;

public class RelatorioService {
    public List<String> faturamento(LocalDate inicio, LocalDate fim) throws SQLException {
        validarPeriodo(inicio, fim);
        String sql = "SELECT DATE_FORMAT(data_venda, '%Y-%m') AS periodo, "
                + "SUM(valor_final) AS faturamento, COUNT(*) AS quantidade "
                + "FROM vendas WHERE data_venda BETWEEN ? AND ? "
                + "GROUP BY DATE_FORMAT(data_venda, '%Y-%m') "
                + "HAVING SUM(valor_final) > 0 ORDER BY periodo";
        List<String> linhas = new ArrayList<>();
        try (Connection conexao = ConexaoBanco.getConexao();
                PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(inicio));
            stmt.setDate(2, Date.valueOf(fim));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    linhas.add(
                            String.format(
                                    I18nUtils.getCurrentLocale(),
                                    I18nUtils.getString(
                                            "relatorio.linha_faturamento"
                                    ),
                                    rs.getString("periodo"),
                                    rs.getInt("quantidade"),
                                    I18nUtils.formatCurrency(
                                            rs.getDouble("faturamento")
                                    )
                            )
                    );
                }
            }
        }
        return linhas;
    }

    public List<String> curvaEstoque() throws SQLException {
        String sql = "SELECT status, COUNT(*) AS quantidade, SUM(preco) AS valor_total "
                + "FROM veiculos GROUP BY status HAVING COUNT(*) > 0 ORDER BY quantidade DESC";
        List<String> linhas = new ArrayList<>();
        try (Connection conexao = ConexaoBanco.getConexao();
                PreparedStatement stmt = conexao.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                linhas.add(
                        String.format(
                                I18nUtils.getCurrentLocale(),
                                I18nUtils.getString(
                                        "relatorio.linha_estoque"
                                ),
                                I18nUtils.formatVehicleStatus(
                                        rs.getString("status")
                                ),
                                rs.getInt("quantidade"),
                                I18nUtils.formatCurrency(
                                        rs.getDouble("valor_total")
                                )
                        )
                );
            }
        }
        return linhas;
    }

    public List<String> veiculacao() throws SQLException {
        String sql = "SELECT ve.marca, ve.modelo, COUNT(v.id_venda) AS vendas, "
                + "SUM(v.valor_final) AS faturamento, "
                + "AVG(DATEDIFF(CURRENT_DATE, v.data_venda)) AS dias_medio "
                + "FROM vendas v JOIN veiculos ve ON ve.id = v.id_veiculo "
                + "GROUP BY ve.marca, ve.modelo HAVING COUNT(v.id_venda) > 0 "
                + "ORDER BY vendas DESC, faturamento DESC";
        List<String> linhas = new ArrayList<>();
        try (Connection conexao = ConexaoBanco.getConexao();
                PreparedStatement stmt = conexao.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                linhas.add(
                        String.format(
                                I18nUtils.getCurrentLocale(),
                                I18nUtils.getString(
                                        "relatorio.linha_veiculacao"
                                ),
                                rs.getString("marca"),
                                rs.getString("modelo"),
                                rs.getInt("vendas"),
                                I18nUtils.formatCurrency(
                                        rs.getDouble("faturamento")
                                ),
                                rs.getDouble("dias_medio")
                        )
                );
            }
        }
        return linhas;
    }

    public List<LogAuditoria> auditoriaRecente(int limite) throws SQLException {
        return new AuditoriaDAO().listarRecentes(limite);
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException(
                    I18nUtils.getString(
                            "relatorio.periodo_invalido"
                    )
            );
        }
    }
}
