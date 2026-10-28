import autobotz.portal.CatalogoMateriais;
import autobotz.portal.MockPortalBackend;
import autobotz.portal.PortalServer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Testes das rotas do portal D15. Sem banco: backend em memoria e catalogo em pasta temporaria. */
public class D15PortalTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String base;
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        Path tmp = Files.createTempDirectory("d15-portal-test");
        Path content = Files.createDirectory(tmp.resolve("content"));
        Path pub = Files.createDirectory(tmp.resolve("public"));
        Files.writeString(pub.resolve("index.html"), "<h1>ok</h1>");
        Files.writeString(content.resolve("guia.md"), "# Guia\nPasso 1");
        Files.writeString(content.resolve("rascunho.md"), "texto em preparacao");
        Files.writeString(tmp.resolve("segredo.txt"), "fora da pasta de conteudo");
        Files.writeString(content.resolve("catalogo.json"), """
            {"materiais":[
              {"slug":"guia-rapido","titulo":"Guia rápido","tipo":"GUIA","status":"PUBLICADO","formato":"MARKDOWN","arquivo":"guia.md"},
              {"slug":"futuro","titulo":"Em preparação","tipo":"VIDEO","status":"EM_PREPARACAO","formato":"MARKDOWN","arquivo":"rascunho.md"},
              {"slug":"tutorial","titulo":"Tutorial","tipo":"VIDEO","status":"PUBLICADO","formato":"MARKDOWN","arquivo":"guia.md"},
              {"slug":"roteiro","titulo":"Roteiro","tipo":"ROTEIRO","status":"PUBLICADO","formato":"MARKDOWN","arquivo":"guia.md"}
            ]}""");

        MockPortalBackend backend = new MockPortalBackend();
        PortalServer servidor = new PortalServer(0, backend, new CatalogoMateriais(content), pub);
        servidor.iniciar();
        base = "http://127.0.0.1:" + servidor.porta();
        try {
            var r = get("/api/treinamentos/materiais");
            check(r.statusCode() == 200 && r.body().contains("guia-rapido"), "lista inclui material publicado");
            check(!r.body().contains("futuro") && !r.body().contains("guia.md"),
                "lista nao expoe rascunho nem nome de arquivo");

            r = get("/api/treinamentos/materiais/guia-rapido");
            check(r.statusCode() == 200 && r.body().contains("Passo 1"), "material publicado devolve conteudo");
            check(!r.body().contains("guia.md") && !r.body().contains("arquivo"), "resposta nao expoe caminho interno");

            check(get("/api/treinamentos/materiais/nao-existe").statusCode() == 404, "slug desconhecido -> 404");
            check(get("/api/treinamentos/materiais/futuro").statusCode() == 404, "material nao publicado -> 404");
            check(get("/api/treinamentos/materiais/..%2Fsegredo").statusCode() == 400, "slug com barra -> 400");
            check(get("/api/treinamentos/materiais/Guia-Rapido").statusCode() == 400, "slug com maiuscula -> 400");

            check(post("{\"tipoEvento\":\"PORTAL_OPEN\"}", true).statusCode() == 201, "PORTAL_OPEN -> 201");
            check(post("{\"tipoEvento\":\"MATERIAL_VIEW\",\"materialSlug\":\"guia-rapido\"}", true).statusCode() == 201,
                "MATERIAL_VIEW valido -> 201");
            check(backend.eventos().size() == 2, "dois eventos validos registrados");

            check(post("{\"tipoEvento\":\"MATERIAL_VIEW\",\"materialSlug\":\"futuro\"}", true).statusCode() == 400,
                "evento de material nao publicado -> 400");
            check(post("{\"tipoEvento\":\"MATERIAL_VIEW\"}", true).statusCode() == 400, "MATERIAL_VIEW sem slug -> 400");
            check(post("{\"tipoEvento\":\"PORTAL_OPEN\",\"materialSlug\":\"guia-rapido\"}", true).statusCode() == 400,
                "PORTAL_OPEN com slug -> 400");
            check(post("{\"tipoEvento\":\"HACK\"}", true).statusCode() == 400, "tipo de evento invalido -> 400");
            check(post("{\"tipoEvento\":\"PORTAL_OPEN\"}", false).statusCode() == 400, "sem Content-Type JSON -> 400");
            check(backend.eventos().size() == 2, "eventos recusados nao foram registrados");

            check(post("{\"tipoEvento\":\"VIDEO_VIEW\",\"materialSlug\":\"tutorial\"}", true).statusCode() == 201,
                "VIDEO_VIEW em material do tipo VIDEO -> 201");
            check(post("{\"tipoEvento\":\"MATERIAL_VIEW\",\"materialSlug\":\"roteiro\"}", true).statusCode() == 201,
                "MATERIAL_VIEW em ROTEIRO -> 201");
            check(post("{\"tipoEvento\":\"VIDEO_VIEW\",\"materialSlug\":\"roteiro\"}", true).statusCode() == 400,
                "VIDEO_VIEW em ROTEIRO (nao e video) -> 400");
            check(post("{\"tipoEvento\":\"VIDEO_VIEW\",\"materialSlug\":\"guia-rapido\"}", true).statusCode() == 400,
                "VIDEO_VIEW em GUIA -> 400");
            check(post("{\"tipoEvento\":\"MATERIAL_VIEW\",\"materialSlug\":\"tutorial\"}", true).statusCode() == 400,
                "MATERIAL_VIEW em material do tipo VIDEO -> 400");
            check(backend.eventos().size() == 4, "so os dois eventos coerentes foram registrados");

            check(get("/api/treinamentos/metricas").statusCode() == 401, "metricas sem sessao -> 401");
            check(get("/api/treinamentos/outra").statusCode() == 404, "rota desconhecida -> 404");
            check(get("/").statusCode() == 200, "pagina inicial -> 200");
            check(get("/..%2Fsegredo.txt").statusCode() == 404, "arquivo fora da pasta publica -> 404");
            Files.createSymbolicLink(pub.resolve("atalho-segredo.txt"), tmp.resolve("segredo.txt"));
            check(get("/atalho-segredo.txt").statusCode() == 404, "symlink externo fora da pasta publica -> 404");

            Path ruim = Files.createDirectory(tmp.resolve("ruim"));
            Files.writeString(ruim.resolve("catalogo.json"),
                "{\"materiais\":[{\"slug\":\"x\",\"titulo\":\"T\",\"tipo\":\"GUIA\",\"status\":\"PUBLICADO\","
                + "\"formato\":\"MARKDOWN\",\"arquivo\":\"../segredo.txt\"}]}");
            try {
                new CatalogoMateriais(ruim);
                check(false, "catalogo com caminho fora da pasta e recusado");
            } catch (IOException e) {
                check(true, "catalogo com caminho fora da pasta e recusado");
            }
        } finally {
            servidor.parar();
            try (Stream<Path> s = Files.walk(tmp)) { s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
        }
        System.out.println(falhas == 0 ? "TODOS OS TESTES PASSARAM" : falhas + " TESTE(S) FALHARAM");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String json, boolean comJson) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/api/treinamentos/eventos"))
            .POST(HttpRequest.BodyPublishers.ofString(json));
        if (comJson) b.header("Content-Type", "application/json");
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void check(boolean ok, String nome) {
        System.out.println((ok ? "PASS " : "FAIL ") + nome);
        if (!ok) falhas++;
    }
}
