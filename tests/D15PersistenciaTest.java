import autobotz.model.MetricaTreinamento;
import autobotz.portal.CatalogoMateriais;
import autobotz.portal.PortalServer;
import autobotz.portal.TreinamentoPortalBackend;
import autobotz.service.TreinamentoService;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Teste de persistencia dos eventos do portal (D15) em banco ISOLADO. Recusa qualquer banco cujo
 * nome nao termine em _test. Variaveis: AUTOBOTZ_TEST_DB_URL, AUTOBOTZ_TEST_DB_USER e
 * AUTOBOTZ_TEST_DB_PASSWORD. Executar na raiz do repositorio, com o driver JDBC no classpath.
 */
public class D15PersistenciaTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        String url = System.getenv("AUTOBOTZ_TEST_DB_URL");
        String usuario = System.getenv("AUTOBOTZ_TEST_DB_USER");
        String senha = System.getenv("AUTOBOTZ_TEST_DB_PASSWORD");
        if (url == null || usuario == null || senha == null) {
            System.out.println("PULADO: defina AUTOBOTZ_TEST_DB_URL, AUTOBOTZ_TEST_DB_USER e AUTOBOTZ_TEST_DB_PASSWORD.");
            System.exit(2);
        }
        Matcher nome = Pattern.compile("^jdbc:(?:mariadb|mysql)://[^/]+/([^/?]+)(\\?.*)?$").matcher(url);
        if (!nome.matches() || !nome.group(1).endsWith("_test")) {
            System.out.println("RECUSADO: o banco de teste deve terminar em _test. Nada foi executado.");
            System.exit(3);
        }
        DataSource ds = (DataSource) Proxy.newProxyInstance(D15PersistenciaTest.class.getClassLoader(),
            new Class<?>[] { DataSource.class }, (proxy, metodo, a) -> {
                if (metodo.getName().equals("getConnection") && (a == null || a.length == 0)) {
                    return DriverManager.getConnection(url, usuario, senha);
                }
                throw new UnsupportedOperationException(metodo.getName());
            });

        aplicarMigracao(ds);
        aplicarMigracao(ds);
        check(true, "migration 015 aplica e e idempotente");
        limpar(ds);

        TreinamentoService svc = new TreinamentoService(ds);
        LocalDateTime de = LocalDateTime.now().minusDays(1);
        LocalDateTime ate = LocalDateTime.now().plusDays(1);

        long a = svc.registrarAberturaPortal();
        long b = svc.registrarVisualizacaoMaterial("guia-rapido");
        long c = svc.registrarVisualizacaoVideo("guia-rapido");
        check(a > 0 && b > a && c > b, "eventos gravados no banco com ids crescentes");

        boolean recusou = false;
        try { svc.registrarVisualizacaoMaterial("../x"); } catch (IllegalArgumentException e) { recusou = true; }
        check(recusou, "slug invalido e recusado pelo servico");

        List<MetricaTreinamento> m = svc.consultarMetricas(de, ate);
        check(m.size() == 3, "tres grupos de metricas (abertura, material, video)");
        check(total(m, "PORTAL_OPEN", null) == 1 && total(m, "MATERIAL_VIEW", "guia-rapido") == 1
            && total(m, "VIDEO_VIEW", "guia-rapido") == 1, "totais por tipo e material corretos");
        check(svc.consultarMetricas(de.plusDays(3), ate.plusDays(3)).isEmpty(), "periodo sem eventos devolve vazio");
        recusou = false;
        try { svc.consultarMetricas(ate, de); } catch (IllegalArgumentException e) { recusou = true; }
        check(recusou, "periodo invertido e recusado");

        PortalServer servidor = PortalLauncher.criarServidor(0, Path.of("portal-treinamento"));
        servidor.iniciar();
        String base = "http://127.0.0.1:" + servidor.porta();
        try {
            check(postEvento(base, "{\"tipoEvento\":\"MATERIAL_VIEW\",\"materialSlug\":\"faq\"}") == 201,
                "POST /eventos grava no banco (201)");
            check(postEvento(base, "{\"tipoEvento\":\"MATERIAL_VIEW\",\"materialSlug\":\"nao-existe\"}") == 400,
                "POST com material inexistente -> 400");
            List<MetricaTreinamento> depois = svc.consultarMetricas(de, ate);
            check(total(depois, "MATERIAL_VIEW", "faq") == 1, "evento enviado pelo HTTP aparece nas metricas");
            check(total(depois, "MATERIAL_VIEW", "nao-existe") == 0, "evento recusado nao foi gravado");
        } finally {
            servidor.parar();
            limpar(ds);
        }
        System.out.println(falhas == 0 ? "TODOS OS TESTES PASSARAM" : falhas + " TESTE(S) FALHARAM");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static void aplicarMigracao(DataSource ds) throws Exception {
        StringBuilder sql = new StringBuilder();
        for (String linha : Files.readAllLines(Path.of("db", "migrations", "015_d15_treinamento.sql"))) {
            if (!linha.strip().startsWith("--")) sql.append(linha).append('\n');
        }
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            for (String comando : sql.toString().split(";")) {
                if (!comando.isBlank()) s.execute(comando);
            }
        }
    }

    private static void limpar(DataSource ds) throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("DELETE FROM treinamento_eventos");
        }
    }

    private static long total(List<MetricaTreinamento> lista, String tipo, String slug) {
        return lista.stream()
            .filter(x -> x.getTipoEvento().equals(tipo) && Objects.equals(x.getMaterialSlug(), slug))
            .mapToLong(MetricaTreinamento::getTotal).sum();
    }

    private static int postEvento(String base, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/api/treinamentos/eventos"))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static void check(boolean ok, String nome) {
        System.out.println((ok ? "PASS " : "FAIL ") + nome);
        if (!ok) falhas++;
    }
}
