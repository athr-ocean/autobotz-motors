package autobotz.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import autobotz.SessaoUsuario;
import autobotz.Usuario;
import autobotz.model.LogAuditoria;
import autobotz.util.ConexaoBanco;
import autobotz.util.I18nUtils;

public class AuditoriaDAO {
    private static final String INSERT = "INSERT INTO logs_auditoria "
            + "(id_usuario, nome_usuario, acao, entidade, entidade_id, detalhes) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    public long registrar(LogAuditoria log) throws SQLException {
        try (Connection conexao = ConexaoBanco.getConexao();
                PreparedStatement stmt = conexao.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getUsuarioId() == null) stmt.setNull(1, java.sql.Types.INTEGER);
            else stmt.setInt(1, log.getUsuarioId());
            stmt.setString(2, log.getNomeUsuario());
            stmt.setString(3, log.getAcao());
            stmt.setString(4, log.getEntidade());
            if (log.getEntidadeId() == null) stmt.setNull(5, java.sql.Types.INTEGER);
            else stmt.setInt(5, log.getEntidadeId());
            stmt.setString(6, log.getDetalhes());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setId(keys.getLong(1));
                    return log.getId();
                }
            }
        }
        return 0;
    }

    public long registrarAcao(String acao, String entidade, Integer entidadeId, String detalhes)
            throws SQLException {
        SessaoUsuario sessao = SessaoUsuario.getInstancia();
        Usuario usuario = sessao.getUsuario();
        Integer usuarioId = usuario == null ? null : usuario.getId();
        String nomeUsuario = usuario == null ? "SISTEMA" : usuario.getNomeUsuario();
        return registrar(new LogAuditoria(usuarioId, nomeUsuario, acao, entidade, entidadeId, detalhes));
    }

    public List<LogAuditoria> listarRecentes(int limite) throws SQLException {
        if (limite < 1) {
            throw new IllegalArgumentException(
                    I18nUtils.getString("auditoria.limite_invalido")
            );
        }
        String sql = "SELECT id_log, id_usuario, nome_usuario, acao, entidade, entidade_id, detalhes, data_hora "
                + "FROM logs_auditoria ORDER BY data_hora DESC, id_log DESC LIMIT ?";
        List<LogAuditoria> logs = new ArrayList<>();
        try (Connection conexao = ConexaoBanco.getConexao();
                PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) logs.add(mapear(rs));
            }
        }
        return logs;
    }

    private LogAuditoria mapear(ResultSet rs) throws SQLException {
        int idUsuario = rs.getInt("id_usuario");
        Integer usuarioId = rs.wasNull() ? null : idUsuario;
        Timestamp timestamp = rs.getTimestamp("data_hora");
        LocalDateTime dataHora = timestamp == null ? null : timestamp.toLocalDateTime();
        int entidadeId = rs.getInt("entidade_id");
        Integer id = rs.wasNull() ? null : entidadeId;
        return new LogAuditoria(rs.getLong("id_log"), usuarioId, rs.getString("nome_usuario"),
                rs.getString("acao"), rs.getString("entidade"), id, rs.getString("detalhes"), dataHora);
    }
}
