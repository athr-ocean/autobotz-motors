import autobotz.dao.AuditoriaDAO;
import autobotz.model.LogAuditoria;
import autobotz.util.MonitorIntegridadeD5;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exercício de recuperação de logs em banco descartável.
 * Trabalha apenas em autobotz_d5_test, no próprio computador.
 */
public final class D5RecuperacaoJdbcTest {
    private static int verificacoes;
    private static final List<Long> ids = new ArrayList<>();

    private D5RecuperacaoJdbcTest() {
    }

    private static void conferir(boolean condicao, String teste) {
        if (!condicao) {
            throw new AssertionError("FAIL " + teste);
        }
        verificacoes++;
        System.out.println("PASS " + teste);
    }

    private static String conferirAmbiente() {
        String url = System.getenv("AUTOBOTZ_DB_URL");
        if (url == null || !url.matches(
                "jdbc:mariadb://(?:127[.]0[.]0[.]1|localhost):3306/autobotz_d5_test(?:[?].*)?")) {
            throw new IllegalStateException(
                    "O teste só pode usar autobotz_d5_test no MariaDB local, porta 3306.");
        }
        if (System.getenv("AUTOBOTZ_DB_PASSWORD") == null
                || System.getenv("AUTOBOTZ_DB_PASSWORD").isBlank()) {
            throw new IllegalStateException("Defina AUTOBOTZ_DB_PASSWORD para o banco de teste.");
        }
        return url;
    }

    private static Connection novaConexao(String url) throws SQLException {
        String usuario = System.getenv().getOrDefault("AUTOBOTZ_DB_USER", "root");
        return DriverManager.getConnection(url, usuario, System.getenv("AUTOBOTZ_DB_PASSWORD"));
    }

    private static int contar(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM logs_auditoria WHERE id_log = ? AND acao LIKE 'D5_IT_%'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static String exportar(Connection c, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id_log, nome_usuario, acao, entidade, detalhes "
                + "FROM logs_auditoria WHERE id_log = ? AND acao LIKE 'D5_IT_%'")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Registro de teste não encontrado: " + id);
                }
                return rs.getLong(1) + ";" + rs.getString(2) + ";"
                        + rs.getString(3) + ";" + rs.getString(4) + ";"
                        + rs.getString(5) + "\n";
            }
        }
    }

    private static void restaurarRegistro(Connection c, String linha) throws SQLException {
        String[] campos = linha.strip().split(";", -1);
        if (campos.length != 5 || !"SISTEMA".equals(campos[1])
                || !campos[2].startsWith("D5_IT_") || !"D5_RECUPERACAO".equals(campos[3])) {
            throw new SQLException("Registro exportado inesperado.");
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO logs_auditoria "
                + "(id_log, nome_usuario, acao, entidade, detalhes) VALUES (?, ?, ?, ?, ?)")) {
            ps.setLong(1, Long.parseLong(campos[0]));
            ps.setString(2, campos[1]);
            ps.setString(3, campos[2]);
            ps.setString(4, campos[3]);
            ps.setString(5, campos[4]);
            conferir(ps.executeUpdate() == 1, "LOG_RESTAURADO_NO_BANCO");
        }
    }

    private static void simularAtualizacaoComFalha(Connection c, long id) throws SQLException {
        boolean autocommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM logs_auditoria WHERE id_log = ? AND acao LIKE 'D5_IT_%'")) {
                ps.setLong(1, id);
                conferir(ps.executeUpdate() == 1, "OPERACAO_ANTES_DA_FALHA");
            }
            // Representa uma alteração inválida da aplicação durante a transação.
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO logs_auditoria (nome_usuario, acao, entidade) "
                    + "VALUES (NULL, 'D5_IT_ERRO', 'D5_RECUPERACAO')")) {
                ps.executeUpdate();
                throw new AssertionError("A alteração inválida deveria ter falhado.");
            } catch (SQLException erroEsperado) {
                c.rollback();
                conferir(contar(c, id) == 1, "ROLLBACK_PRESERVA_LOG");
            }
        } finally {
            c.setAutoCommit(autocommit);
        }
    }

    private static void simularIndisponibilidade(String url, Connection c) throws SQLException {
        String urlIndisponivel =
                "jdbc:mariadb://127.0.0.1:1/autobotz_d5_test?connectTimeout=1000";
        boolean recusado = false;
        try (Connection ignorada = novaConexao(urlIndisponivel)) {
            throw new AssertionError("A conexão de teste deveria falhar.");
        } catch (SQLException esperado) {
            recusado = true;
        }
        conferir(recusado, "INSTANCIA_INDISPONIVEL_DETECTADA");
        try (Connection nova = novaConexao(url)) {
            conferir(nova.isValid(2) && c.isValid(2), "CONEXAO_APOS_FALHA");
        }
    }

    private static void removerArquivos(Path pasta) throws IOException {
        if (pasta == null || !Files.exists(pasta)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(pasta)) {
            for (Path arquivo : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(arquivo);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String url = conferirAmbiente();
        Path temporario = null;
        try (Connection c = novaConexao(url)) {
            try (ResultSet banco = c.createStatement().executeQuery("SELECT DATABASE()")) {
                banco.next();
                conferir("autobotz_d5_test".equals(banco.getString(1)), "BANCO_DESCARTAVEL_CONFIRMADO");
            }
            try (ResultSet tabela = c.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM information_schema.TABLES "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'logs_auditoria'")) {
                tabela.next();
                conferir(tabela.getInt(1) == 1, "TABELA_DE_AUDITORIA_DISPONIVEL");
            }

            temporario = Files.createTempDirectory("autobotz-d5-jdbc-");
            try {
                AuditoriaDAO dao = new AuditoriaDAO();
                long primeiro = dao.registrar(new LogAuditoria(
                        null, "SISTEMA", "D5_IT_ANTES", "D5_RECUPERACAO", null, "registro=1"));
                long segundo = dao.registrar(new LogAuditoria(
                        null, "SISTEMA", "D5_IT_DEPOIS", "D5_RECUPERACAO", null, "registro=2"));
                conferir(primeiro > 0 && segundo > primeiro, "REGISTROS_GRAVADOS_NO_ERP");
                ids.add(primeiro);
                ids.add(segundo);

                simularAtualizacaoComFalha(c, primeiro);
                simularIndisponibilidade(url, c);

                Path origem = Files.createDirectory(temporario.resolve("exportados"));
                String texto = exportar(c, primeiro) + exportar(c, segundo);
                Files.writeString(origem.resolve("auditoria.log"), texto, StandardCharsets.UTF_8);
                Path copiaA = temporario.resolve("copia-a");
                Path copiaB = temporario.resolve("copia-b");
                MonitorIntegridadeD5.capturar(origem, copiaA, copiaB);
                conferir(Files.isRegularFile(copiaA.resolve("manifesto.sha256")),
                        "CAPTURA_COM_HASH");
                MonitorIntegridadeD5.verificar(copiaA);
                MonitorIntegridadeD5.verificar(copiaB);

                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM logs_auditoria WHERE id_log = ? AND acao = 'D5_IT_DEPOIS'")) {
                    ps.setLong(1, segundo);
                    conferir(ps.executeUpdate() == 1 && contar(c, segundo) == 0,
                            "PERDA_DE_LOG_SIMULADA");
                }

                // A restauração só aceita dados da réplica previamente conferida.
                String copia = Files.readString(copiaB.resolve("auditoria.log"), StandardCharsets.UTF_8);
                conferir(copia.equals(texto), "COPIA_CONFERIDA");
                String[] linhas = copia.strip().split("\n");
                conferir(linhas.length == 2, "REGISTROS_PRESENTES_NA_COPIA");
                restaurarRegistro(c, linhas[1]);
                conferir(contar(c, primeiro) == 1 && contar(c, segundo) == 1
                        && exportar(c, segundo).equals(linhas[1] + "\n"), "RECUPERACAO_CONFERIDA");

                Files.writeString(copiaA.resolve("auditoria.log"), "conteudo alterado\n");
                boolean adulteracaoDetectada = false;
                try {
                    MonitorIntegridadeD5.verificar(copiaA);
                } catch (IOException esperado) {
                    adulteracaoDetectada = true;
                }
                conferir(adulteracaoDetectada, "ADULTERACAO_DETECTADA");
                MonitorIntegridadeD5.verificar(copiaB);
                conferir(true, "SEGUNDA_COPIA_PERMANECE_INTEGRA");
                System.out.println("D5_RECUPERACAO_JDBC_OK checks=" + verificacoes);
            } finally {
                for (long id : ids) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM logs_auditoria WHERE id_log = ? AND acao LIKE 'D5_IT_%'")) {
                        ps.setLong(1, id);
                        ps.executeUpdate();
                    }
                }
                removerArquivos(temporario);
            }
        }
    }
}
