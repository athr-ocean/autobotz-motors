package autobotz.dao;

import autobotz.model.AssinaturaCliente;
import autobotz.util.ConexaoBanco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import autobotz.model.PlanoAssinatura;
import java.time.LocalDate;
import autobotz.model.ModuloOpcional;

public class AssinaturaDAO {
	
	int idPlanoAnterior;
	BigDecimal precoAnterior = null;
	BigDecimal precoNovo = null;

	public int salvar(AssinaturaCliente assinatura)
	        throws SQLException {

	    String sqlAssinatura = """
	        INSERT INTO assinaturas_clientes
	        (id_cliente, id_plano, status, data_inicio)
	        VALUES (?, ?, ?, ?)
	        """;

	    String sqlModulo = """
	        INSERT INTO assinatura_modulos
	        (id_assinatura, id_modulo)
	        VALUES (?, ?)
	        """;

	    String sqlEvento = """
	        INSERT INTO assinatura_eventos
	        (id_assinatura, tipo_evento, descricao, valor_novo)
	        VALUES (?, ?, ?, ?)
	        """;

	    Connection conexao = ConexaoBanco.getConexao();

	    boolean autoCommitOriginal = conexao.getAutoCommit();

	    try {
	        conexao.setAutoCommit(false);

	        int idAssinatura;

	        try (PreparedStatement stmt = conexao.prepareStatement(
	                sqlAssinatura, Statement.RETURN_GENERATED_KEYS)) {

	            stmt.setInt(1, assinatura.getClienteId());
	            stmt.setInt(2, assinatura.getPlano().getId());
	            stmt.setString(3, assinatura.getStatus());
	            stmt.setDate(4, java.sql.Date.valueOf(
	                    assinatura.getDataInicio()));

	            stmt.executeUpdate();

	            try (ResultSet rs = stmt.getGeneratedKeys()) {
	                if (!rs.next()) {
	                    throw new SQLException(
	                        "Não foi possível obter o ID da assinatura."
	                    );
	                }

	                idAssinatura = rs.getInt(1);
	            }
	        }
	        for (ModuloOpcional modulo : assinatura.getModulos()) {

	            try (PreparedStatement stmt =
	                    conexao.prepareStatement(sqlModulo)) {

	                stmt.setInt(1, idAssinatura);
	                stmt.setInt(2, modulo.getId());

	                stmt.executeUpdate();
	            }
	        }
	        try (PreparedStatement stmt =
	                conexao.prepareStatement(sqlEvento)) {

	            stmt.setInt(1, idAssinatura);
	            stmt.setString(2, "CONTRATACAO");
	            stmt.setString(3, "Assinatura criada - dados simulados");
	            stmt.setBigDecimal(4, assinatura.calcularValorMensal());

	            stmt.executeUpdate();
	        }

	        conexao.commit();

	        return idAssinatura;

	    } catch (SQLException | RuntimeException e) {

	        try {
	            conexao.rollback();
	        } catch (SQLException erroRollback) {
	            e.addSuppressed(erroRollback);
	        }

	        throw e;

	    } finally {
	        conexao.setAutoCommit(autoCommitOriginal);
	    }
	}
	private AssinaturaCliente montarAssinatura(ResultSet rs)
	        throws SQLException {

	    PlanoAssinatura plano = new PlanoAssinatura(
	        rs.getInt("id_plano"),
	        rs.getString("nome_plano"),
	        rs.getBigDecimal("preco_base")
	    );

	    Date dataSQL = rs.getDate("data_cancelamento");

	    LocalDate dataCancelamento = null;

	    if (dataSQL != null) {
	        dataCancelamento = dataSQL.toLocalDate();
	    }

	    AssinaturaCliente assinatura = new AssinaturaCliente(
	        rs.getInt("id_assinatura"),
	        rs.getInt("id_cliente"),
	        plano,
	        rs.getDate("data_inicio").toLocalDate(),
	        rs.getString("status"),
	        dataCancelamento,
	        rs.getString("categoria_cancelamento"),
	        rs.getString("motivo_cancelamento")
	    );

	    return assinatura;
	}
	public AssinaturaCliente buscarPorId(int id)
	        throws SQLException {

	    String sql = """
	        SELECT a.*, p.nome AS nome_plano, p.preco_base
	        FROM assinaturas_clientes a
	        JOIN planos_assinatura p
	            ON a.id_plano = p.id_plano
	        WHERE a.id_assinatura = ?
	        """;

	    Connection conexao = ConexaoBanco.getConexao();

	    AssinaturaCliente assinatura = null;

	    try (PreparedStatement stmt =
	            conexao.prepareStatement(sql)) {

	        stmt.setInt(1, id);

	        try (ResultSet rs = stmt.executeQuery()) {

	            if (rs.next()) {
	                assinatura = montarAssinatura(rs);
	            }
	        }
	    }

	    if (assinatura != null) {
	        carregarModulos(assinatura);
	    }

	    return assinatura;
	}
    private void carregarModulos(AssinaturaCliente assinatura)
            throws SQLException {

        String sql = """
            SELECT m.id_modulo, m.nome, m.preco
            FROM assinatura_modulos am
            JOIN modulos_opcionais m
                ON am.id_modulo = m.id_modulo
            WHERE am.id_assinatura = ?
            """;

        Connection conexao = ConexaoBanco.getConexao();

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, assinatura.getId());

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    ModuloOpcional modulo = new ModuloOpcional(
                        rs.getInt("id_modulo"),
                        rs.getString("nome"),
                        rs.getBigDecimal("preco")
                    );

                    assinatura.adicionarModulo(modulo);
                }
            }
        }
    }
    public void alterarPlano(int idAssinatura, int idNovoPlano)
            throws SQLException {
    	
        Connection conexao = ConexaoBanco.getConexao();

        boolean autoCommitOriginal = conexao.getAutoCommit();
        BigDecimal totalModulos =
        	    somarModulos(conexao, idAssinatura);

        	precoAnterior = precoAnterior.add(totalModulos);
        	precoNovo = precoNovo.add(totalModulos);

        String sqlConsulta = """
            SELECT a.id_plano, a.status, p.preco_base
            FROM assinaturas_clientes a
            JOIN planos_assinatura p
                ON a.id_plano = p.id_plano
            WHERE a.id_assinatura = ?
            FOR UPDATE
            """;

        String sqlNovoPlano = """
            SELECT preco_base
            FROM planos_assinatura
            WHERE id_plano = ? AND ativo = TRUE
            """;

        String sqlAtualizar = """
            UPDATE assinaturas_clientes
            SET id_plano = ?
            WHERE id_assinatura = ?
            """;

        String sqlEvento = """
            INSERT INTO assinatura_eventos
            (id_assinatura, tipo_evento, descricao,
             valor_anterior, valor_novo)
            VALUES (?, ?, ?, ?, ?)
            """;

        try {
            conexao.setAutoCommit(false);

            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlConsulta)) {

                stmt.setInt(1, idAssinatura);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException("Assinatura não encontrada.");
                    }

                    if (!"ATIVA".equals(rs.getString("status"))) {
                        throw new SQLException(
                            "Não é possível alterar uma assinatura cancelada."
                        );
                    }

                    idPlanoAnterior = rs.getInt("id_plano");
                    precoAnterior = rs.getBigDecimal("preco_base");
                }
            }

            if (idPlanoAnterior == idNovoPlano) {
                throw new SQLException(
                    "O novo plano é igual ao plano atual."
                );
            }

            
            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlNovoPlano)) {

                stmt.setInt(1, idNovoPlano);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Novo plano não encontrado ou inativo."
                        );
                    }

                    precoNovo = rs.getBigDecimal("preco_base");
                }
            }
            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlAtualizar)) {

                stmt.setInt(1, idNovoPlano);
                stmt.setInt(2, idAssinatura);

                stmt.executeUpdate();
            }
            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlEvento)) {

                stmt.setInt(1, idAssinatura);
                stmt.setString(2, "ALTERACAO_PLANO");
                stmt.setString(3,
                    "Plano alterado de ID " + idPlanoAnterior
                    + " para ID " + idNovoPlano);

                stmt.setBigDecimal(4, precoAnterior);
                stmt.setBigDecimal(5, precoNovo);

                stmt.executeUpdate();
            }

            conexao.commit();

        } catch (SQLException | RuntimeException e) {

            try {
                conexao.rollback();
            } catch (SQLException erroRollback) {
                e.addSuppressed(erroRollback);
            }

            throw e;

        } finally {
            conexao.setAutoCommit(autoCommitOriginal);
        }
    }
    public List<AssinaturaCliente> listarAtivas()
            throws SQLException {

        List<AssinaturaCliente> assinaturas =
                new ArrayList<>();

        String sql = """
            SELECT a.*, p.nome AS nome_plano, p.preco_base
            FROM assinaturas_clientes a
            JOIN planos_assinatura p
                ON a.id_plano = p.id_plano
            WHERE a.status = 'ATIVA'
            ORDER BY a.id_assinatura
            """;

        Connection conexao = ConexaoBanco.getConexao();

        try (PreparedStatement stmt =
                conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                assinaturas.add(montarAssinatura(rs));
            }
        }

        // Carregar os módulos após fechar o ResultSet
        for (AssinaturaCliente assinatura : assinaturas) {
            carregarModulos(assinatura);
        }

        return assinaturas;
    	}
    public BigDecimal calcularMRRBanco() throws SQLException {

        BigDecimal total = BigDecimal.ZERO;

        for (AssinaturaCliente assinatura : listarAtivas()) {
            total = total.add(assinatura.calcularValorMensal());
        }

        return total;
    }

    public BigDecimal calcularReceitaPerdida(
            java.time.LocalDate inicio,
            java.time.LocalDate fim) throws SQLException {

        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido.");
        }

        String sql = """
            SELECT COALESCE(SUM(e.valor_anterior), 0) AS total
            FROM assinatura_eventos e
            WHERE e.tipo_evento = 'CANCELAMENTO'
              AND DATE(e.data_evento) BETWEEN ? AND ?
            """;

        Connection conexao = ConexaoBanco.getConexao();

        try (PreparedStatement stmt =
                conexao.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(inicio));
            stmt.setDate(2, java.sql.Date.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        }
    }

    public BigDecimal calcularChurnBanco(
            java.time.LocalDate inicio,
            java.time.LocalDate fim) throws SQLException {

        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Período inválido.");
        }

        String sqlBase = """
            SELECT COUNT(*) AS total
            FROM assinaturas_clientes
            WHERE data_inicio < ?
              AND (data_cancelamento IS NULL
                   OR data_cancelamento >= ?)
            """;

        String sqlCancelamentos = """
            SELECT COUNT(*) AS total
            FROM assinaturas_clientes
            WHERE data_inicio < ?
              AND data_cancelamento BETWEEN ? AND ?
            """;

        Connection conexao = ConexaoBanco.getConexao();

        int baseInicial;
        int cancelamentos;

        try (PreparedStatement stmt =
                conexao.prepareStatement(sqlBase)) {

            stmt.setDate(1, java.sql.Date.valueOf(inicio));
            stmt.setDate(2, java.sql.Date.valueOf(inicio));

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                baseInicial = rs.getInt("total");
            }
        }

        try (PreparedStatement stmt =
                conexao.prepareStatement(sqlCancelamentos)) {

            stmt.setDate(1, java.sql.Date.valueOf(inicio));
            stmt.setDate(2, java.sql.Date.valueOf(inicio));
            stmt.setDate(3, java.sql.Date.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                cancelamentos = rs.getInt("total");
            }
        }

        if (baseInicial == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(cancelamentos)
            .multiply(BigDecimal.valueOf(100))
            .divide(
                BigDecimal.valueOf(baseInicial),
                2,
                java.math.RoundingMode.HALF_UP
            );
    }
    private BigDecimal somarModulos(
            Connection conexao,
            int idAssinatura) throws SQLException {

        String sql = """
            SELECT COALESCE(SUM(m.preco), 0) AS total
            FROM assinatura_modulos am
            JOIN modulos_opcionais m
                ON m.id_modulo = am.id_modulo
            WHERE am.id_assinatura = ?
            """;

        try (PreparedStatement stmt =
                conexao.prepareStatement(sql)) {

            stmt.setInt(1, idAssinatura);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        }
    }
    public void cancelar(
            int idAssinatura,
            String categoria,
            String motivo,
            String feedbackSimulado) throws SQLException {

        if (categoria == null || categoria.isBlank()
                || motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException(
                "Categoria e motivo são obrigatórios."
            );
        }

        String sqlConsulta = """
            SELECT a.status, p.preco_base,
                   COALESCE(SUM(m.preco), 0) AS total_modulos
            FROM assinaturas_clientes a
            JOIN planos_assinatura p
                ON p.id_plano = a.id_plano
            LEFT JOIN assinatura_modulos am
                ON am.id_assinatura = a.id_assinatura
            LEFT JOIN modulos_opcionais m
                ON m.id_modulo = am.id_modulo
            WHERE a.id_assinatura = ?
            GROUP BY a.id_assinatura, a.status, p.preco_base
            FOR UPDATE
            """;

        String sqlAtualizar = """
            UPDATE assinaturas_clientes
            SET status = 'CANCELADA',
                data_cancelamento = CURRENT_DATE,
                categoria_cancelamento = ?,
                motivo_cancelamento = ?,
                feedback_simulado = ?
            WHERE id_assinatura = ? AND status = 'ATIVA'
            """;

        String sqlEvento = """
            INSERT INTO assinatura_eventos
            (id_assinatura, tipo_evento, descricao,
             valor_anterior, valor_novo,
             categoria_motivo, feedback_simulado)
            VALUES (?, 'CANCELAMENTO', ?, ?, ?, ?, ?)
            """;

        Connection conexao = ConexaoBanco.getConexao();
        boolean autoCommitOriginal = conexao.getAutoCommit();

        try {
            conexao.setAutoCommit(false);

            BigDecimal receitaPerdida;

            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlConsulta)) {

                stmt.setInt(1, idAssinatura);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Assinatura não encontrada."
                        );
                    }

                    if (!"ATIVA".equals(rs.getString("status"))) {
                        throw new SQLException(
                            "A assinatura já está cancelada."
                        );
                    }

                    receitaPerdida = rs.getBigDecimal("preco_base")
                        .add(rs.getBigDecimal("total_modulos"));
                }
            }

            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlAtualizar)) {

                stmt.setString(1, categoria);
                stmt.setString(2, motivo);
                stmt.setString(3, feedbackSimulado);
                stmt.setInt(4, idAssinatura);

                if (stmt.executeUpdate() != 1) {
                    throw new SQLException(
                        "Não foi possível cancelar a assinatura."
                    );
                }
            }

            try (PreparedStatement stmt =
                    conexao.prepareStatement(sqlEvento)) {

                stmt.setInt(1, idAssinatura);
                stmt.setString(2, "Cancelamento: " + motivo);
                stmt.setBigDecimal(3, receitaPerdida);
                stmt.setBigDecimal(4, BigDecimal.ZERO);
                stmt.setString(5, categoria);
                stmt.setString(6, feedbackSimulado);

                stmt.executeUpdate();
            }

            conexao.commit();

        } catch (SQLException | RuntimeException e) {

            try {
                conexao.rollback();
            } catch (SQLException erro) {
                e.addSuppressed(erro);
            }

            throw e;

        } finally {
            conexao.setAutoCommit(autoCommitOriginal);
        }
    }
    
    }