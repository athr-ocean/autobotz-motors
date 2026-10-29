import autobotz.AuthService;
import autobotz.PerfilUsuario;
import autobotz.UsuarioDAO;
import autobotz.dao.SegurancaDAO;
import autobotz.portal.PortalLauncher;
import autobotz.portal.PortalServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Login, MFA e autorização HTTP reais do portal em MariaDB descartável. */
public final class D15AuthHttpJdbcTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern DESAFIO = Pattern.compile("\"desafioId\":(\\d+)");
    private static final Pattern CODIGO = Pattern.compile("\"codigoDemonstracao\":\"(\\d{6})\"");

    private D15AuthHttpJdbcTest() {}

    public static void main(String[] args) throws Exception {
        String url = System.getenv("AUTOBOTZ_TEST_DB_URL");
        String usuarioDb = System.getenv("AUTOBOTZ_TEST_DB_USER");
        String senhaDb = System.getenv("AUTOBOTZ_TEST_DB_PASSWORD");
        Matcher db = Pattern.compile("^jdbc:(?:mariadb|mysql)://[^/]+/([^/?]+)(?:\\?.*)?$").matcher(url == null ? "" : url);
        if (!db.matches() || !db.group(1).endsWith("_test") || usuarioDb == null || senhaDb == null) {
            throw new IllegalStateException("Defina AUTOBOTZ_TEST_DB_* para um banco descartável terminado em _test.");
        }

        String sufixo = Long.toString(System.currentTimeMillis(), 36);
        String admin = "d15http_a_" + sufixo;
        String vendedor = "d15http_v_" + sufixo;
        var cadastro = new AuthService(new UsuarioDAO(), new SegurancaDAO(), Clock.systemUTC(), AuthService.PERFIS_COM_MFA);
        PortalServer servidor = null;
        try {
            check(cadastro.cadastrar(admin, "DummyAdmin-123", PerfilUsuario.ADMIN), "cadastro ADMIN de teste");
            check(cadastro.cadastrar(vendedor, "DummyVendor-123", PerfilUsuario.VENDEDOR), "cadastro VENDEDOR de teste");

            servidor = PortalLauncher.criarServidor(0, Path.of("portal-treinamento"));
            servidor.iniciar();
            String base = "http://127.0.0.1:" + servidor.porta();
            String metricas = "/api/treinamentos/metricas?inicio=2000-01-01T00%3A00%3A00&fim=2100-01-01T00%3A00%3A00";

            check(get(base + metricas, null).statusCode() == 401, "sem sessão -> HTTP 401");
            String cookieAdmin = loginMfa(base, admin, "DummyAdmin-123");
            var respostaAdmin = get(base + metricas, cookieAdmin);
            check(respostaAdmin.statusCode() == 200, "ADMIN autenticado após MFA -> HTTP 200");
            check(respostaAdmin.body().contains("DADOS_REAIS_DE_EXECUCAO"), "launcher usa backend JDBC persistente");

            String cookieVendedor = loginMfa(base, vendedor, "DummyVendor-123");
            check(get(base + metricas, cookieVendedor).statusCode() == 403, "VENDEDOR autenticado -> HTTP 403");
            check(post(base + "/api/logout", "", cookieAdmin).statusCode() == 204, "logout -> HTTP 204");
            check(get(base + metricas, cookieAdmin).statusCode() == 401, "token ADMIN após logout -> HTTP 401");
        } finally {
            if (servidor != null) servidor.parar();
            limpar(url, usuarioDb, senhaDb, admin, vendedor);
        }
        System.out.println("D15_AUTH_HTTP_JDBC_OK");
    }

    private static String loginMfa(String base, String nome, String senha) throws Exception {
        var login = post(base + "/api/login", "{\"usuario\":\"" + nome + "\",\"senha\":\"" + senha + "\"}", null);
        if (login.statusCode() != 202) {
            throw new AssertionError(nome + " esperava MFA (HTTP 202), recebeu HTTP " + login.statusCode());
        }
        check(true, nome + " senha válida exige MFA");
        String desafio = extrair(DESAFIO, login.body());
        String codigo = extrair(CODIGO, login.body());
        var mfa = post(base + "/api/login/mfa", "{\"desafioId\":" + desafio + ",\"codigo\":\"" + codigo + "\"}", null);
        check(mfa.statusCode() == 200, nome + " MFA válida autentica");
        String setCookie = mfa.headers().firstValue("Set-Cookie").orElse("");
        check(setCookie.startsWith("ABM_SESSAO="), nome + " recebe cookie de sessão após MFA");
        return setCookie.split(";", 2)[0];
    }

    private static String extrair(Pattern pattern, String body) {
        Matcher m = pattern.matcher(body);
        if (!m.find()) throw new AssertionError("resposta MFA sem campo esperado");
        return m.group(1);
    }

    private static HttpResponse<String> get(String url, String cookie) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).GET();
        if (cookie != null) req.header("Cookie", cookie);
        return HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String url, String body, String cookie) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) req.header("Cookie", cookie);
        return HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void limpar(String url, String usuario, String senha, String... nomes) throws Exception {
        try (Connection c = DriverManager.getConnection(url, usuario, senha)) {
            String placeholders = String.join(",", java.util.Collections.nCopies(nomes.length, "?"));
            try (PreparedStatement eventos = c.prepareStatement(
                    "DELETE FROM eventos_seguranca WHERE nome_usuario IN (" + placeholders + ")");
                 PreparedStatement desafios = c.prepareStatement(
                    "DELETE FROM mfa_desafios WHERE id_usuario IN (SELECT id_usuario FROM usuarios WHERE nome_usuario IN (" + placeholders + "))");
                 PreparedStatement usuarios = c.prepareStatement(
                    "DELETE FROM usuarios WHERE nome_usuario IN (" + placeholders + ")")) {
                for (PreparedStatement st : new PreparedStatement[] {eventos, desafios, usuarios}) {
                    for (int i = 0; i < nomes.length; i++) st.setString(i + 1, nomes[i]);
                    st.executeUpdate();
                }
            }
        }
    }

    private static void check(boolean ok, String descricao) {
        System.out.println((ok ? "PASS " : "FAIL ") + descricao);
        if (!ok) throw new AssertionError(descricao);
    }
}
