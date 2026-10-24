package autobotz.portal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Servidor HTTP leve (JDK) do portal. Escuta somente em localhost. */
public class PortalServer {
    private static final String JSON = "application/json; charset=utf-8";
    private final HttpServer server;
    private final PortalBackend backend;
    private final Path raiz;

    public PortalServer(int porta, PortalBackend backend, Path raiz) throws IOException {
        this.backend = backend;
        this.raiz = raiz.toAbsolutePath().normalize();
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 0);
        server.createContext("/api/", this::api);
        server.createContext("/", this::estatico);
    }

    public void iniciar() { server.start(); }
    public void parar() { server.stop(0); }

    private void api(HttpExchange ex) throws IOException {
        try {
            String m = ex.getRequestMethod();
            String p = ex.getRequestURI().getPath();
            if (m.equals("GET") && p.equals("/api/catalogo")) {
                String tipo = consulta(ex.getRequestURI().getRawQuery(), "tipo");
                enviar(ex, 200, JSON, backend.catalogo(tipo).stream().map(PortalServer::json)
                    .collect(Collectors.joining(",", "[", "]")));
            } else if (m.equals("GET") && p.startsWith("/api/catalogo/")) {
                var mat = backend.buscarMaterial(p.substring("/api/catalogo/".length()));
                if (mat == null) erro(ex, 404, "MATERIAL_NAO_ENCONTRADO", "Material nao encontrado.");
                else enviar(ex, 200, JSON, json(mat));
            } else if (m.equals("POST") && p.equals("/api/eventos")) {
                String corpo = new String(ex.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
                var ev = backend.registrarEvento(campo(corpo, "tipoEvento"), campo(corpo, "materialId"),
                    campo(corpo, "sessaoId"));
                enviar(ex, 201, JSON, "{\"id\":" + ev.id() + ",\"registradoEm\":" + str(ev.registradoEm()) + "}");
            } else if (p.equals("/api/metricas")) {
                erro(ex, 403, "ACESSO_NEGADO", "Metricas indisponiveis ate a regra de permissao ser definida.");
            } else {
                erro(ex, 404, "ROTA_NAO_ENCONTRADA", "Rota nao encontrada.");
            }
        } catch (IllegalArgumentException e) {
            erro(ex, 400, "EVENTO_INVALIDO", e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro no portal: " + e);
            erro(ex, 500, "ERRO_INTERNO", "Erro interno.");
        }
    }

    private void estatico(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { erro(ex, 405, "METODO_NAO_PERMITIDO", "Use GET."); return; }
        String p = ex.getRequestURI().getPath();
        Path arq;
        try {
            arq = raiz.resolve(p.equals("/") ? "index.html" : p.substring(1)).normalize();
        } catch (RuntimeException e) { arq = null; }
        if (arq == null || !arq.startsWith(raiz) || !Files.isRegularFile(arq)) {
            erro(ex, 404, "ARQUIVO_NAO_ENCONTRADO", "Arquivo nao encontrado.");
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
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(status, corpo.length);
        try (var out = ex.getResponseBody()) { out.write(corpo); }
    }

    private static String json(PortalBackend.Material m) {
        return "{\"id\":" + str(m.id()) + ",\"tipo\":" + str(m.tipo()) + ",\"titulo\":" + str(m.titulo())
            + ",\"descricao\":" + str(m.descricao()) + ",\"arquivo\":" + str(m.arquivo())
            + ",\"simulado\":" + m.simulado() + "}";
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

    /** Le um campo de texto simples de um JSON plano (suficiente para o corpo de /api/eventos). */
    static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(nome) + "\"\\s*:\\s*(null|\"([^\"\\\\]*)\")").matcher(json);
        return m.find() ? m.group(2) : null;
    }

    private static String consulta(String q, String nome) {
        if (q == null) return null;
        for (String par : q.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2 && kv[0].equals(nome)) return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }
}
