import autobotz.dao.AssinaturaDAO;
import autobotz.model.AssinaturaCliente;
import autobotz.model.ModuloOpcional;
import autobotz.model.PlanoAssinatura;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** Exercita o DAO D12 contra MariaDB descartável terminado em _test. */
public final class AssinaturaDAOJdbcTest {
    private static int verificacoes;

    private AssinaturaDAOJdbcTest() {}

    public static void main(String[] args) throws Exception {
        String url = System.getenv("AUTOBOTZ_DB_URL");
        String usuario = System.getenv("AUTOBOTZ_DB_USER");
        String senha = System.getenv("AUTOBOTZ_DB_PASSWORD");
        if (url == null || !Pattern.matches(
                "^jdbc:(?:mariadb|mysql)://[^/]+/[^/?]*_test(?:\\?.*)?$", url)
                || usuario == null || senha == null) {
            throw new IllegalStateException(
                    "Defina AUTOBOTZ_DB_* para banco descartável terminado em _test.");
        }

        int clienteId;
        int planoAtualId;
        int planoNovoId;
        int planoInativoId;
        int moduloId;
        try (var conexao = DriverManager.getConnection(url, usuario, senha);
             var stmt = conexao.createStatement()) {
            stmt.executeUpdate("INSERT INTO clientes (nome, cpf) VALUES ('D12 JDBC', 'D12-JDBC-TEST')");
            try (var rs = stmt.executeQuery("SELECT LAST_INSERT_ID()")) {
                rs.next();
                clienteId = rs.getInt(1);
            }
            stmt.executeUpdate("INSERT INTO planos_assinatura (nome, preco_base) VALUES ('D12 base', 100.00), ('D12 novo', 150.00)");
            try (var rs = stmt.executeQuery("SELECT MIN(id_plano), MAX(id_plano) FROM planos_assinatura")) {
                rs.next();
                planoAtualId = rs.getInt(1);
                planoNovoId = rs.getInt(2);
            }
            stmt.executeUpdate("INSERT INTO planos_assinatura (nome, preco_base, ativo) VALUES ('D12 inativo', 90.00, FALSE)");
            try (var rs = stmt.executeQuery("SELECT id_plano FROM planos_assinatura WHERE nome = 'D12 inativo'")) {
                rs.next();
                planoInativoId = rs.getInt(1);
            }
            stmt.executeUpdate("INSERT INTO modulos_opcionais (nome, preco) VALUES ('D12 modulo', 20.00)");
            try (var rs = stmt.executeQuery("SELECT MAX(id_modulo) FROM modulos_opcionais")) {
                rs.next();
                moduloId = rs.getInt(1);
            }
        }

        var planoAtual = new PlanoAssinatura(planoAtualId, "D12 base", new BigDecimal("100.00"));
        var assinatura = new AssinaturaCliente(0, clienteId, planoAtual,
                LocalDate.now(), "ATIVA", null, null, null);
        assinatura.adicionarModulo(new ModuloOpcional(moduloId, "D12 modulo", new BigDecimal("20.00")));
        AssinaturaDAO dao = new AssinaturaDAO();
        int assinaturaId = dao.salvar(assinatura);
        check(assinaturaId > 0, "assinatura e modulo persistidos");

        dao.alterarPlano(assinaturaId, planoNovoId);
        try (var conexao = DriverManager.getConnection(url, usuario, senha);
             var stmt = conexao.prepareStatement(
                     "SELECT a.id_plano, e.valor_anterior, e.valor_novo "
                     + "FROM assinaturas_clientes a JOIN assinatura_eventos e "
                     + "ON e.id_assinatura = a.id_assinatura "
                     + "WHERE a.id_assinatura = ? AND e.tipo_evento = 'ALTERACAO_PLANO'")) {
            stmt.setInt(1, assinaturaId);
            try (var rs = stmt.executeQuery()) {
                check(rs.next(), "evento de alteração persistido");
                check(rs.getInt("id_plano") == planoNovoId, "plano novo persistido");
                check(new BigDecimal("120.00").compareTo(rs.getBigDecimal("valor_anterior")) == 0,
                        "valor anterior inclui módulo");
                check(new BigDecimal("170.00").compareTo(rs.getBigDecimal("valor_novo")) == 0,
                        "valor novo inclui módulo");
            }
        }

        boolean recusouPlanoInativo = false;
        try {
            dao.alterarPlano(assinaturaId, planoInativoId);
        } catch (java.sql.SQLException esperado) {
            recusouPlanoInativo = esperado.getMessage().contains("inativo");
        }
        check(recusouPlanoInativo, "plano inativo recusado");
        check(dao.buscarPorId(assinaturaId).getPlano().getId() == planoNovoId,
                "falha preserva plano e transacao");

        dao.cancelar(assinaturaId, "OUTROS", "Teste D12", "feedback ficticio");
        check("CANCELADA".equals(dao.buscarPorId(assinaturaId).getStatus()),
                "status cancelado preservado ao carregar do banco");
        System.out.println("AssinaturaDAOJdbcTest: " + verificacoes + " verificações aprovadas");
    }

    private static void check(boolean condicao, String nome) {
        if (!condicao) {
            throw new AssertionError(nome);
        }
        verificacoes++;
        System.out.println("PASS " + nome);
    }
}
