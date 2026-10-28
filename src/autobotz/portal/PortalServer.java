package autobotz.portal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Servidor HTTP leve (JDK) do portal. Escuta somente em localhost. Rotas conforme docs/D15_CONTRATO_API.md. */
public class PortalServer {
    private static final String JSON = "application/json; charset=utf-8";
    private static final String BASE = "/api/treinamentos/";
    private final HttpServer server;
    private final PortalBackend backend;
    private final CatalogoMateriais catalogo;
    private final Path raiz;

    public PortalServer(int porta, PortalBackend backend, CatalogoMateriais catalogo, Path raizPublica) throws IOException {
        this.backend = backend;
        this.catalogo = catalogo;
        this.raiz = raizPublica.toAbsolutePath().normalize();
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), porta), 0);
        server.createContext("/api/", this::api);
        server.createContext("/", this::estatico);
    }

    public void iniciar() { server.start(); }
    public void parar() { server.stop(0); }
    public int porta() { return server.getAddress().getPort(); }

    private void api(HttpExchange ex) throws IOException {
        try {
            String m = ex.getRequestMethod();
            String p = ex.getRequestURI().getPath();
            if (m.equals("GET") && p.equals(BASE + "materiais")) {
                listar(ex);
            } else if (m.equals("GET") && p.startsWith(BASE + "materiais/")) {
                material(ex, p.substring((BASE + "materiais/").length()));
            } else if (m.equals("POST") && p.equals(BASE + "eventos")) {
                evento(ex);
            } else if (m.equals("GET") && p.equals(BASE + "metricas")) {
                // Sem sessao administrativa definida (Arthur e Joao), as metricas ficam sempre negadas.
                erro(ex, 401, "NAO_AUTENTICADO", "Autenticação administrativa obrigatória.");
            } else {
                erro(ex, 404, "NAO_ENCONTRADO", "Rota não encontrada.");
            }
        } catch (IllegalArgumentException e) {
            erro(ex, 400, "VALIDACAO", "O parâmetro informado não é válido.");
        } catch (Exception e) {
            System.err.println("Erro no portal: " + e);
            erro(ex, 500, "ERRO_INTERNO", "Erro interno.");
        }
    }

    private void listar(HttpExchange ex) throws IOException {
        String itens = catalogo.publicados().stream()
            .map(x -> "{\"slug\":" + str(x.slug()) + ",\"titulo\":" + str(x.titulo())
                + ",\"tipo\":" + str(x.tipo()) + ",\"status\":" + str(x.status()) + "}")
            .collect(Collectors.joining(",", "[", "]"));
        enviar(ex, 200, JSON, "{\"materiais\":" + itens + "}");
    }

    private void material(HttpExchange ex, String slug) throws IOException {
        if (!CatalogoMateriais.slugValido(slug)) throw new IllegalArgumentException("slug");
        var achado = catalogo.publicado(slug);
        if (achado.isEmpty()) {
            erro(ex, 404, "NAO_ENCONTRADO", "Material não encontrado.");
            return;
        }
        var x = achado.get();
        enviar(ex, 200, JSON, "{\"slug\":" + str(x.slug()) + ",\"titulo\":" + str(x.titulo())
            + ",\"tipo\":" + str(x.tipo()) + ",\"status\":" + str(x.status())
            + ",\"formato\":" + str(x.formato()) + ",\"conteudo\":" + str(catalogo.ler(x)) + "}");
    }

    private void evento(HttpExchange ex) throws Exception {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().startsWith("application/json")) {
            throw new IllegalArgumentException("content-type");
        }
        String corpo = new String(ex.getRequestBody().readNBytes(2048), StandardCharsets.UTF_8);
        String tipo = campo(corpo, "tipoEvento");
        String slug = campo(corpo, "materialSlug");
        long id = switch (tipo == null ? "" : tipo) {
            case "PORTAL_OPEN" -> {
                if (slug != null) throw new IllegalArgumentException("slug");
                yield backend.registrarAberturaPortal();
            }
            case "MATERIAL_VIEW" -> {
                exigirPublicado(slug, false);
                yield backend.registrarVisualizacaoMaterial(slug);
            }
            case "VIDEO_VIEW" -> {
                exigirPublicado(slug, true);
                yield backend.registrarVisualizacaoVideo(slug);
            }
            default -> throw new IllegalArgumentException("tipoEvento");
        };
        enviar(ex, 201, JSON, "{\"id\":" + id + "}");
    }

    /** VIDEO_VIEW so vale para material do tipo VIDEO; MATERIAL_VIEW vale para os demais (ex.: ROTEIRO). */
    private void exigirPublicado(String slug, boolean video) {
        var m = catalogo.publicado(slug).orElseThrow(() -> new IllegalArgumentException("materialSlug"));
        if (m.tipo().equals("VIDEO") != video) throw new IllegalArgumentException("tipo do material");
    }

    private void estatico(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) {
            erro(ex, 405, "METODO_NAO_PERMITIDO", "Use GET.");
            return;
        }
        String p = ex.getRequestURI().getPath();
        Path arq;
        try {
            arq = raiz.resolve(p.equals("/") ? "index.html" : p.substring(1)).normalize();
        } catch (RuntimeException e) {
            arq = null;
        }
        if (arq == null || !arq.startsWith(raiz) || !Files.isRegularFile(arq)) {
            erro(ex, 404, "NAO_ENCONTRADO", "Arquivo não encontrado.");
            return;
        }
        String nome = arq.getFileName().toString();
        String tipo = nome.endsWith(".html") ? "text/html; charset=utf-8"
            : nome.endsWith(".css") ? "text/css; charset=utf-8"
            : nome.endsWith(".js") ? "text/javascript; charset=utf-8" : "application/octet-stream";
        enviar(ex, 200, tipo, Files.readAllBytes(arq));
    }

    private void erro(HttpExchange ex, int status, String codigo, String msg) throws IOException {
        enviar(ex, status, JSON, "{\"erro\":{\"codigo\":" + str(codigo) + ",\"mensagem\":" + str(msg) + "}}");
    }

    private void enviar(HttpExchange ex, int status, String tipo, String corpo) throws IOException {
        enviar(ex, status, tipo, corpo.getBytes(StandardCharsets.UTF_8));
    }

    private void enviar(HttpExchange ex, int status, String tipo, byte[] corpo) throws IOException {
        var h = ex.getResponseHeaders();
        h.set("Content-Type", tipo);
        h.set("X-Content-Type-Options", "nosniff");
        h.set("Content-Security-Policy", "default-src 'self'");
        ex.sendResponseHeaders(status, corpo.length);
        try (var out = ex.getResponseBody()) { out.write(corpo); }
    }

    static String str(String s) {
        if (s == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    /** Le um campo de texto simples de um JSON plano (corpo de /eventos). Ausente ou null devolve null. */
    static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(nome) + "\"\\s*:\\s*(null|\"([^\"\\\\]*)\")").matcher(json);
        return m.find() ? m.group(2) : null;
    }
}
