import autobotz.portal.MockPortalBackend;
import autobotz.portal.PortalAtalho;
import autobotz.portal.PortalAtalho.Resultado;
import autobotz.portal.PortalAtalho.Status;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Testes do PortalAtalho (abertura do portal pelo ERP). Nunca abre navegador de verdade. */
public class D15AtalhoTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        Path tmp = Files.createTempDirectory("d15-atalho-test");
        Path base = tmp.resolve("portal-treinamento");
        Path content = Files.createDirectories(base.resolve("content"));
        Files.writeString(Files.createDirectories(base.resolve("public")).resolve("index.html"), "<h1>ok</h1>");
        Files.writeString(content.resolve("guia.md"), "# Guia");
        Files.writeString(content.resolve("catalogo.json"),
            "{\"materiais\":[{\"slug\":\"guia-rapido\",\"titulo\":\"Guia\",\"tipo\":\"GUIA\","
            + "\"status\":\"PUBLICADO\",\"formato\":\"MARKDOWN\",\"arquivo\":\"guia.md\"}]}");
        try {
            Resultado r = PortalAtalho.abrir(0, tmp.resolve("inexistente"), null);
            check(r.status() == Status.CONTEUDO_INDISPONIVEL && r.url() == null, "sem conteudo -> CONTEUDO_INDISPONIVEL");

            try (ServerSocket ocupada = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
                r = PortalAtalho.abrir(ocupada.getLocalPort(), base, new MockPortalBackend());
                check(r.status() == Status.PORTA_OCUPADA, "porta em uso -> PORTA_OCUPADA");
            }

            r = PortalAtalho.abrir(0, base, new MockPortalBackend());
            check(r.status() == Status.NAVEGADOR_INDISPONIVEL && r.url() != null && r.url().startsWith("http://localhost:"),
                "servidor sobe e devolve a url (sem navegador no teste)");
            check(codigo(r.url()) == 200, "a pagina responde na url devolvida");

            Resultado de2 = PortalAtalho.abrir(0, base, new MockPortalBackend());
            check(de2.url().equals(r.url()), "segunda chamada reaproveita o mesmo servidor");

            PortalAtalho.parar();
            boolean caiu = false;
            try { codigo(r.url()); } catch (IOException e) { caiu = true; }
            check(caiu, "apos parar() o servidor deixa de responder");

            Resultado de3 = PortalAtalho.abrir(0, base, new MockPortalBackend());
            check(de3.url() != null && codigo(de3.url()) == 200, "pode abrir de novo depois de parar()");
        } finally {
            PortalAtalho.parar();
            try (Stream<Path> s = Files.walk(tmp)) { s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
        }
        System.out.println(falhas == 0 ? "TODOS OS TESTES PASSARAM" : falhas + " TESTE(S) FALHARAM");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static int codigo(String url) throws Exception {
        String direto = url.replace("localhost", "127.0.0.1");
        return HTTP.send(HttpRequest.newBuilder(URI.create(direto + "/")).build(),
            HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    private static void check(boolean ok, String nome) {
        System.out.println((ok ? "PASS " : "FAIL ") + nome);
        if (!ok) falhas++;
    }
}
