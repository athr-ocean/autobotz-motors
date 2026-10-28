package autobotz.portal;

import autobotz.model.MetricaTreinamento;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Servidor HTTP leve (JDK) do portal. Escuta somente em localhost. */
public class PortalServer {
    private static final String JSON = "application/json; charset=utf-8";
    private static final String BASE = "/api/treinamentos/";
    private final HttpServer server;
    private final PortalBackend backend;
    private final CatalogoMateriais catalogo;
    private final Path raiz;
    private final MetricasAuthorizer autorizadorMetricas;
    private Object sessoesWeb;

    @FunctionalInterface
    public interface MetricasAuthorizer {
        int autorizar(HttpExchange exchange) throws Exception;
    }

    public PortalServer(int porta, PortalBackend backend, CatalogoMateriais catalogo, Path raizPublica)
            throws IOException {
        this(porta, backend, catalogo, raizPublica, null);
    }

    /** Sobrecarga para testar 401/403/200 sem forjar identidade no pedido HTTP. */
    public PortalServer(int porta, PortalBackend backend, CatalogoMateriais catalogo, Path raizPublica,
            MetricasAuthorizer autorizadorMetricas) throws IOException {
        this.backend = backend;
        this.catalogo = catalogo;
        this.raiz = raizPublica.toAbsolutePath().normalize();
        this.autorizadorMetricas = autorizadorMetricas == null ? this::autorizarComSessaoWeb : autorizadorMetricas;
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
            } else if (m.equals("POST") && p.equals("/api/login")) {
                login(ex);
            } else if (m.equals("POST") && p.equals("/api/login/mfa")) {
                loginMfa(ex);
            } else if (m.equals("POST") && p.equals("/api/logout")) {
                logout(ex);
            } else if (m.equals("POST") && p.equals(BASE + "eventos")) {
                evento(ex);
            } else if (m.equals("GET") && p.equals(BASE + "metricas")) {
                metricas(ex);
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

    /**
     * Usa a sessão D14, validada no servidor. A reflexão mantém este PR compilável
     * enquanto a branch-base ainda não contém SessaoWebService; nesse caso nega acesso.
     */
    private synchronized int autorizarComSessaoWeb(HttpExchange ex) throws Exception {
        final String token;
        try {
            token = tokenPedido(ex);
            Object servico = servicoSessoesWeb();
            Object decisao = chamar(servico, "autorizarMetricas",
                    new Class<?>[] { String.class }, token);
            return (int) chamar(decisao, "httpStatus", new Class<?>[0]);
        } catch (ClassNotFoundException e) {
            return 401; // falha fechada até D14 estar disponível na base integrada
        }
    }

    private synchronized Object servicoSessoesWeb() throws Exception {
        if (sessoesWeb == null) {
            Class<?> tipo = Class.forName("autobotz.service.SessaoWebService");
            sessoesWeb = tipo.getConstructor().newInstance();
        }
        return sessoesWeb;
    }

    private static Object chamar(Object alvo, String nome, Class<?>[] tipos, Object... argumentos)
            throws Exception {
        try {
            return alvo.getClass().getMethod(nome, tipos).invoke(alvo, argumentos);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getCause();
            if (causa instanceof Exception exCause) throw exCause;
            throw e;
        }
    }

    private static String tokenPedido(HttpExchange ex) throws Exception {
        Class<?> tipo = Class.forName("autobotz.service.SessaoWebService");
        Method extrair = tipo.getMethod("extrairToken", String.class, String.class);
        return (String) extrair.invoke(null, ex.getRequestHeaders().getFirst("Authorization"),
                ex.getRequestHeaders().getFirst("Cookie"));
    }

    private void login(HttpExchange ex) throws Exception {
        exigirJson(ex);
        String corpo = new String(ex.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
        String usuario = campo(corpo, "usuario");
        String senha = campo(corpo, "senha");
        if (usuario == null || senha == null) throw new IllegalArgumentException("credenciais");
        final Object resultado;
        try {
            resultado = chamar(servicoSessoesWeb(), "iniciarLogin",
                    new Class<?>[] { String.class, String.class }, usuario, senha);
        } catch (ClassNotFoundException e) {
            erro(ex, 503, "AUTENTICACAO_INDISPONIVEL", "Autenticação indisponível.");
            return;
        }
        String status = String.valueOf(chamar(resultado, "status", new Class<?>[0]));
        if (status.equals("AUTENTICADO")) {
            definirCookie(ex, String.valueOf(chamar(resultado, "token", new Class<?>[0])));
            enviar(ex, 200, JSON, "{\"status\":\"AUTENTICADO\"}");
        } else if (status.equals("MFA_NECESSARIA")) {
            Object desafio = chamar(resultado, "desafio", new Class<?>[0]);
            String id = String.valueOf(chamar(desafio, "id", new Class<?>[0]));
            String expira = String.valueOf(chamar(desafio, "expiraEm", new Class<?>[0]));
            String codigo = String.valueOf(chamar(desafio, "codigoSimulado", new Class<?>[0]));
            enviar(ex, 202, JSON, "{\"status\":\"MFA_NECESSARIA\",\"desafioId\":" + id
                + ",\"expiraEm\":" + str(expira) + ",\"codigoDemonstracao\":" + str(codigo)
                + ",\"aviso\":\"MFA SIMULADO — código de demonstração, não enviado por SMS ou e-mail.\"}");
        } else if (status.equals("BLOQUEADO")) {
            Object segundos = chamar(resultado, "segundosRestantes", new Class<?>[0]);
            long espera = Long.parseLong(String.valueOf(segundos));
            ex.getResponseHeaders().set("Retry-After", Long.toString(espera));
            erro(ex, 429, "LOGIN_BLOQUEADO", "Não foi possível autenticar. Tente novamente mais tarde.");
        } else {
            erro(ex, 401, "NEGADO", "Usuário ou senha inválidos.");
        }
    }

    private void loginMfa(HttpExchange ex) throws Exception {
        exigirJson(ex);
        String corpo = new String(ex.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
        String idTexto = campo(corpo, "desafioId");
        String codigo = campo(corpo, "codigo");
        if (idTexto == null || codigo == null) throw new IllegalArgumentException("mfa");
        final long id;
        try {
            id = Long.parseLong(idTexto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("desafioId");
        }
        final Object resultado;
        try {
            resultado = chamar(servicoSessoesWeb(), "concluirMfa",
                    new Class<?>[] { long.class, String.class }, id, codigo);
        } catch (ClassNotFoundException e) {
            erro(ex, 503, "AUTENTICACAO_INDISPONIVEL", "Autenticação indisponível.");
            return;
        }
        String status = String.valueOf(chamar(resultado, "status", new Class<?>[0]));
        if (status.equals("AUTENTICADO")) {
            definirCookie(ex, String.valueOf(chamar(resultado, "token", new Class<?>[0])));
            enviar(ex, 200, JSON, "{\"status\":\"AUTENTICADO\"}");
        } else {
            erro(ex, 401, "MFA_NEGADO", "O segundo fator não foi validado. Inicie o login novamente se o desafio expirou.");
        }
    }

    private void logout(HttpExchange ex) throws Exception {
        String token;
        try {
            token = tokenPedido(ex);
            chamar(servicoSessoesWeb(), "logout", new Class<?>[] { String.class }, token);
        } catch (ClassNotFoundException e) {
            // Não existe sessão utilizável quando o serviço de sessão não está disponível.
        }
        ex.getResponseHeaders().set("Set-Cookie",
                "ABM_SESSAO=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    private static void exigirJson(HttpExchange ex) {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().startsWith("application/json")) {
            throw new IllegalArgumentException("content-type");
        }
    }

    private static void definirCookie(HttpExchange ex, String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token");
        ex.getResponseHeaders().set("Set-Cookie",
                "ABM_SESSAO=" + token + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800");
    }

    private void metricas(HttpExchange ex) throws Exception {
        int status = autorizadorMetricas.autorizar(ex);
        if (status != 200) {
            int seguro = status == 403 ? 403 : 401;
            erro(ex, seguro, seguro == 403 ? "ACESSO_PROIBIDO" : "NAO_AUTENTICADO",
                    seguro == 403 ? "Acesso negado." : "Autenticação obrigatória.");
            return;
        }
        Map<String, String> q = parametros(ex.getRequestURI().getRawQuery());
        String inicioTexto = q.get("inicio");
        String fimTexto = q.get("fim");
        if (inicioTexto == null || fimTexto == null) throw new IllegalArgumentException("periodo");
        LocalDateTime inicio;
        LocalDateTime fim;
        try {
            inicio = LocalDateTime.parse(inicioTexto);
            fim = LocalDateTime.parse(fimTexto);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("periodo");
        }
        if (!fim.isAfter(inicio)) throw new IllegalArgumentException("periodo");

        List<MetricaTreinamento> linhas = backend.consultarMetricas(inicio, fim);
        String itens = linhas.stream()
            .map(x -> "{\"tipoEvento\":" + str(x.getTipoEvento())
                + ",\"materialSlug\":" + str(x.getMaterialSlug())
                + ",\"total\":" + x.getTotal() + "}")
            .collect(Collectors.joining(",", "[", "]"));
        String corpo = "{\"inicio\":" + str(inicio.toString())
            + ",\"fim\":" + str(fim.toString())
            + ",\"metricas\":" + itens
            + ",\"origem\":" + str(backend.origemMetricas()) + "}";
        enviar(ex, 200, JSON, corpo);
    }

    private static Map<String, String> parametros(String query) {
        Map<String, String> r = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return r;
        for (String parte : query.split("&")) {
            String[] kv = parte.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length == 2 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            if (r.putIfAbsent(k, v) != null) throw new IllegalArgumentException("parametro repetido");
        }
        return r;
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

    /** VIDEO_VIEW so vale para material do tipo VIDEO; MATERIAL_VIEW vale para os demais. */
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

    /** Lê string ou número de um campo JSON plano, tratando escapes de string. */
    static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(nome)
                + "\"\\s*:\\s*(?:null|\"((?:[^\"\\\\]|\\\\.)*)\"|(-?\\d+))").matcher(json);
        if (!m.find()) return null;
        return m.group(1) != null ? desescaparJson(m.group(1)) : m.group(2);
    }

    private static String desescaparJson(String valor) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c != '\\\\') {
                b.append(c);
                continue;
            }
            if (++i >= valor.length()) throw new IllegalArgumentException("escape JSON");
            char e = valor.charAt(i);
            switch (e) {
                case '"' -> b.append('"');
                case '\\\\' -> b.append('\\\\');
                case '/' -> b.append('/');
                case 'b' -> b.append('\\b');
                case 'f' -> b.append('\\f');
                case 'n' -> b.append('\\n');
                case 'r' -> b.append('\\r');
                case 't' -> b.append('\\t');
                case 'u' -> {
                    if (i + 4 >= valor.length()) throw new IllegalArgumentException("escape Unicode");
                    try {
                        b.append((char) Integer.parseInt(valor.substring(i + 1, i + 5), 16));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("escape Unicode");
                    }
                    i += 4;
                }
                default -> throw new IllegalArgumentException("escape JSON");
            }
        }
        return b.toString();
    }
}
