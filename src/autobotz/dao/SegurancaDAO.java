package autobotz.dao;

import autobotz.model.DesafioMfa;
import autobotz.model.EventoSeguranca;
import autobotz.util.ConexaoBanco;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Implementação JDBC (MariaDB) de {@link SegurancaRepositorio}. Só PreparedStatement. */
public class SegurancaDAO implements SegurancaRepositorio {

    @Override
    public void registrarEvento(EventoSeguranca e) throws SQLException {
        String sql = "INSERT INTO eventos_seguranca "
                + "(data_hora, nome_usuario, tipo, sucesso, causa, origem) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(e.dataHora()));
            stmt.setString(2, e.nomeUsuario());
            stmt.setString(3, e.tipo());
            stmt.setBoolean(4, e.sucesso());
            stmt.setString(5, e.causa());
            stmt.setString(6, e.origem());
            stmt.executeUpdate();
        }
    }

    @Override
    public int contarFalhasLogin(String nomeUsuario, Instant janelaInicio) throws SQLException {
        String sql = "SELECT COUNT(*) FROM eventos_seguranca "
                + "WHERE nome_usuario = ? AND tipo = 'LOGIN' AND sucesso = FALSE "
                + "AND data_hora > ? "
                + "AND data_hora > COALESCE((SELECT MAX(data_hora) FROM eventos_seguranca "
                + "WHERE nome_usuario = ? AND tipo = 'LOGIN' AND sucesso = TRUE), ?)";
        Timestamp inicio = Timestamp.from(janelaInicio);
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setString(1, nomeUsuario);
            stmt.setTimestamp(2, inicio);
            stmt.setString(3, nomeUsuario);
            stmt.setTimestamp(4, inicio);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public List<EventoSeguranca> listarEventos(int limite) throws SQLException {
        String sql = "SELECT id_evento, data_hora, nome_usuario, tipo, sucesso, causa, origem "
                + "FROM eventos_seguranca ORDER BY data_hora DESC, id_evento DESC LIMIT ?";
        List<EventoSeguranca> lista = new ArrayList<>();
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limite));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new EventoSeguranca(
                            rs.getLong("id_evento"),
                            rs.getTimestamp("data_hora").toInstant(),
                            rs.getString("nome_usuario"),
                            rs.getString("tipo"),
                            rs.getBoolean("sucesso"),
                            rs.getString("causa"),
                            rs.getString("origem")));
                }
            }
        }
        return lista;
    }

    @Override
    public long criarDesafioMfa(int idUsuario, String codigoHash, Instant criadoEm,
            Instant expiraEm, int maxTentativas) throws SQLException {
        String sql = "INSERT INTO mfa_desafios "
                + "(id_usuario, codigo_hash, criado_em, expira_em, max_tentativas) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = ConexaoBanco.getConexao()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, codigoHash);
            stmt.setTimestamp(3, Timestamp.from(criadoEm));
            stmt.setTimestamp(4, Timestamp.from(expiraEm));
            stmt.setInt(5, maxTentativas);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Desafio MFA nao gerado");
    }

    @Override
    public void invalidarDesafiosPendentes(int idUsuario) throws SQLException {
        String sql = "UPDATE mfa_desafios SET consumido = TRUE "
                + "WHERE id_usuario = ? AND consumido = FALSE";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        }
    }

    @Override
    public DesafioMfa buscarDesafioMfa(long idDesafio) throws SQLException {
        String sql = "SELECT id_desafio, id_usuario, codigo_hash, expira_em, tentativas, "
                + "max_tentativas, consumido FROM mfa_desafios WHERE id_desafio = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setLong(1, idDesafio);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DesafioMfa(
                            rs.getLong("id_desafio"),
                            rs.getInt("id_usuario"),
                            rs.getString("codigo_hash"),
                            rs.getTimestamp("expira_em").toInstant(),
                            rs.getInt("tentativas"),
                            rs.getInt("max_tentativas"),
                            rs.getBoolean("consumido"));
                }
            }
        }
        return null;
    }

    @Override
    public boolean incrementarTentativaMfa(long idDesafio) throws SQLException {
        String sql = "UPDATE mfa_desafios SET tentativas = tentativas + 1 "
                + "WHERE id_desafio = ? AND consumido = FALSE AND tentativas < max_tentativas";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setLong(1, idDesafio);
            return stmt.executeUpdate() == 1;
        }
    }

    @Override
    public boolean consumirDesafioMfa(long idDesafio, Instant agora) throws SQLException {
        String sql = "UPDATE mfa_desafios SET consumido = TRUE "
                + "WHERE id_desafio = ? AND consumido = FALSE AND expira_em > ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setLong(1, idDesafio);
            stmt.setTimestamp(2, Timestamp.from(agora));
            return stmt.executeUpdate() == 1;
        }
    }

    @Override
    public void invalidarDesafio(long idDesafio) throws SQLException {
        String sql = "UPDATE mfa_desafios SET consumido = TRUE WHERE id_desafio = ?";
        try (PreparedStatement stmt = ConexaoBanco.getConexao().prepareStatement(sql)) {
            stmt.setLong(1, idDesafio);
            stmt.executeUpdate();
        }
    }
}
