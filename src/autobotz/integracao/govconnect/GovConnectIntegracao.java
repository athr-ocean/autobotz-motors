package autobotz.integracao.govconnect;

import autobotz.model.Fornecedor;
import autobotz.model.PedidoCompra;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente REST demonstrativo; contrato e rotas ainda dependem da especificação oficial do órgão. */
public final class GovConnectIntegracao {
    public static final String ROTA_PEDIDOS = "/api/v1/pedidos";
    public static final String HEADER_IDEMPOTENCIA = "Idempotency-Key";
    public static final String HEADER_CORRELACAO = "X-Correlation-ID";
    private static final int MAX_BODY = 16 * 1024;

    private GovConnectIntegracao() { }

    public record PedidoRequest(String referenciaERP, int fornecedorId, String fornecedorNome,
                                BigDecimal valor, String moeda, String descricao) {
        public PedidoRequest {
            if (referenciaERP == null || !referenciaERP.matches("[A-Za-z0-9._-]{1,64}"))
                throw new IllegalArgumentException("referenciaERP inválida");
            if (fornecedorId <= 0) throw new IllegalArgumentException("fornecedorId deve ser positivo");
            fornecedorNome = validarTexto(fornecedorNome, "fornecedorNome", 120);
            if (valor == null || valor.signum() <= 0 || valor.scale() > 2)
                throw new IllegalArgumentException("valor deve ser positivo e ter até 2 casas decimais");
            if (!"BRL".equals(moeda)) throw new IllegalArgumentException("moeda deve ser BRL neste protótipo");
            descricao = validarTexto(descricao, "descricao", 200);
        }

        public static PedidoRequest de(PedidoCompra pedido, Fornecedor fornecedor, String descricao) {
            Objects.requireNonNull(pedido, "pedido");
            Objects.requireNonNull(fornecedor, "fornecedor");
            if (pedido.getId() <= 0) throw new IllegalArgumentException("Pedido precisa estar persistido/aprovado");
            if (pedido.getFornecedorId() != fornecedor.getId())
                throw new IllegalArgumentException("Fornecedor não corresponde ao pedido");
            return new PedidoRequest("AUTOBOTZ-PEDIDO-" + pedido.getId(), fornecedor.getId(),
                    fornecedor.getNome(), pedido.getValor(), "BRL", descricao);
        }
    }

    public record PedidoResponse(String protocolo, String referenciaERP, String status,
                                 String recebidoEm, boolean simulado) { }

    public static final class GovConnectException extends IOException {
        private final int statusHttp;
        private final String codigo;

        public GovConnectException(int statusHttp, String codigo, String mensagem) {
            super(mensagem);
            this.statusHttp = statusHttp;
            this.codigo = codigo;
        }
        public int getStatusHttp() { return statusHttp; }
        public String getCodigo() { return codigo; }
    }

    @FunctionalInterface
    public interface AccessTokenProvider {
        String obterToken() throws IOException, InterruptedException;
    }

    /** Cliente HTTP. Segredos e tokens não são incluídos em logs ou URLs. */
    public static final class Cliente {
        private final URI baseUri;
        private final AccessTokenProvider tokenProvider;
        private final HttpClient http;

        public Cliente(URI baseUri, AccessTokenProvider tokenProvider) {
            this.baseUri = validarBase(baseUri);
            this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
            this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        }

        public PedidoResponse enviarPedido(PedidoRequest pedido) throws IOException, InterruptedException {
            Objects.requireNonNull(pedido, "pedido");
            String json = serializar(pedido);
            HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(ROTA_PEDIDOS))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + tokenProvider.obterToken())
                    .header(HEADER_IDEMPOTENCIA, pedido.referenciaERP())
                    .header(HEADER_CORRELACAO, UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 && response.statusCode() != 201) throw erro(response);
            PedidoResponse resultado = lerResposta(response.body());
            if (!pedido.referenciaERP().equals(resultado.referenciaERP()))
                throw new IOException("Resposta GovConnect não corresponde à referência enviada");
            return resultado;
        }

        public PedidoResponse consultar(String protocolo) throws IOException, InterruptedException {
            if (protocolo == null || !protocolo.matches("[A-Za-z0-9-]{8,80}"))
                throw new IllegalArgumentException("protocolo inválido");
            HttpRequest request = HttpRequest.newBuilder(
                    baseUri.resolve(ROTA_PEDIDOS + "/" + protocolo))
                    .timeout(Duration.ofSeconds(8))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + tokenProvider.obterToken())
                    .GET().build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) throw erro(response);
            return lerResposta(response.body());
        }

        private static GovConnectException erro(HttpResponse<String> response) {
            String codigo = campo(response.body(), "codigo");
            String mensagem = campo(response.body(), "mensagem");
            if (codigo == null) codigo = "HTTP_" + response.statusCode();
            if (mensagem == null) mensagem = "Falha na integração (HTTP " + response.statusCode() + ")";
            return new GovConnectException(response.statusCode(), codigo, mensagem);
        }
    }

    /**
     * Mock usado exclusivamente pelo teste automatizado. Escuta apenas em 127.0.0.1,
     * guarda dados em memória e não representa um serviço oficial do governo.
     */
    public static final class ServidorSimulado implements AutoCloseable {
        private final HttpServer server;
        private final String bearerToken;
        private final Map<String, PedidoResponse> porProtocolo = new ConcurrentHashMap<>();
        private final Map<String, PedidoResponse> porIdempotencia = new ConcurrentHashMap<>();

        public ServidorSimulado(String bearerToken) throws IOException {
            if (bearerToken == null || bearerToken.isBlank())
                throw new IllegalArgumentException("Token obrigatório");
            this.bearerToken = bearerToken;
            server = HttpServer.create(new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"), 0), 0);
            server.setExecutor(Executors.newFixedThreadPool(2, tarefa -> {
                Thread thread = new Thread(tarefa, "govconnect-mock");
                thread.setDaemon(true);
                return thread;
            }));
            server.createContext(ROTA_PEDIDOS, this::atender);
            server.start();
        }

        public URI baseUri() {
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        }

        @Override public void close() { server.stop(0); }

        private void atender(HttpExchange exchange) throws IOException {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                String received = authorization != null && authorization.startsWith("Bearer ")
                        ? authorization.substring(7) : "";
                if (!MessageDigest.isEqual(received.getBytes(StandardCharsets.UTF_8),
                        bearerToken.getBytes(StandardCharsets.UTF_8))) {
                    responder(exchange, 401, erroJson("NAO_AUTENTICADO", "Bearer token ausente ou inválido"));
                    return;
                }

                if ("GET".equals(exchange.getRequestMethod()) && path.startsWith(ROTA_PEDIDOS + "/")) {
                    String protocolo = path.substring((ROTA_PEDIDOS + "/").length());
                    PedidoResponse found = porProtocolo.get(protocolo);
                    if (found == null) responder(exchange, 404, erroJson("NAO_ENCONTRADO", "Pedido não encontrado"));
                    else responder(exchange, 200, serializar(found));
                    return;
                }
                if (!ROTA_PEDIDOS.equals(path)) {
                    responder(exchange, 404, erroJson("ROTA_NAO_ENCONTRADA", "Rota não encontrada"));
                    return;
                }
                if (!"POST".equals(exchange.getRequestMethod())) {
                    responder(exchange, 405, erroJson("METODO_NAO_PERMITIDO", "Use POST para registrar pedido"));
                    return;
                }
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
                    responder(exchange, 415, erroJson("FORMATO_NAO_SUPORTADO", "Envie application/json"));
                    return;
                }
                String key = exchange.getRequestHeaders().getFirst(HEADER_IDEMPOTENCIA);
                if (key == null || key.isBlank()) {
                    responder(exchange, 400, erroJson("IDEMPOTENCIA_OBRIGATORIA", "Informe Idempotency-Key"));
                    return;
                }
                String body = new String(exchange.getRequestBody().readNBytes(MAX_BODY + 1),
                        StandardCharsets.UTF_8);
                if (body.length() > MAX_BODY) {
                    responder(exchange, 413, erroJson("CORPO_MUITO_GRANDE", "Corpo excede 16 KiB"));
                    return;
                }
                PedidoRequest pedido;
                try { pedido = lerPedido(body); }
                catch (RuntimeException invalid) {
                    responder(exchange, 400, erroJson("DADOS_INVALIDOS", "Campos do pedido inválidos"));
                    return;
                }
                if (!key.equals(pedido.referenciaERP())) {
                    responder(exchange, 409, erroJson("CHAVE_DIVERGENTE", "Idempotency-Key não corresponde à referênciaERP"));
                    return;
                }
                PedidoResponse existing = porIdempotencia.get(key);
                if (existing != null) {
                    responder(exchange, 200, serializar(existing));
                    return;
                }
                PedidoResponse created = new PedidoResponse(UUID.randomUUID().toString(),
                        pedido.referenciaERP(), "RECEBIDO", Instant.now().toString(), true);
                PedidoResponse raced = porIdempotencia.putIfAbsent(key, created);
                PedidoResponse result = raced == null ? created : raced;
                porProtocolo.put(result.protocolo(), result);
                responder(exchange, raced == null ? 201 : 200, serializar(result));
            } catch (IOException failure) {
                responder(exchange, 400, erroJson("REQUISICAO_INVALIDA", "Não foi possível ler a requisição"));
            }
        }
    }

    private static URI validarBase(URI uri) {
        Objects.requireNonNull(uri, "baseUri");
        boolean loopback = "127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost());
        boolean https = "https".equalsIgnoreCase(uri.getScheme());
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme()) && loopback;
        if ((!https && !localHttp) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("GovConnect exige HTTPS; HTTP só é permitido em localhost para testes");
        return URI.create(uri.toString().replaceAll("/$", ""));
    }

    private static String validarTexto(String value, String field, int limit) {
        if (value == null || value.isBlank() || value.trim().length() > limit)
            throw new IllegalArgumentException(field + " inválido");
        return value.trim();
    }

    private static PedidoRequest lerPedido(String json) {
        return new PedidoRequest(campo(json, "referenciaERP"),
                Integer.parseInt(campo(json, "fornecedorId")),
                campo(json, "fornecedorNome"),
                new BigDecimal(campo(json, "valor")),
                campo(json, "moeda"), campo(json, "descricao"));
    }

    private static String serializar(PedidoRequest pedido) {
        return "{\"referenciaERP\":" + quote(pedido.referenciaERP())
                + ",\"fornecedorId\":" + pedido.fornecedorId()
                + ",\"fornecedorNome\":" + quote(pedido.fornecedorNome())
                + ",\"valor\":" + pedido.valor().toPlainString()
                + ",\"moeda\":" + quote(pedido.moeda())
                + ",\"descricao\":" + quote(pedido.descricao()) + "}";
    }

    private static String serializar(PedidoResponse response) {
        return "{\"protocolo\":" + quote(response.protocolo())
                + ",\"referenciaERP\":" + quote(response.referenciaERP())
                + ",\"status\":" + quote(response.status())
                + ",\"recebidoEm\":" + quote(response.recebidoEm())
                + ",\"simulado\":" + response.simulado() + "}";
    }

    private static PedidoResponse lerResposta(String json) throws IOException {
        try {
            return new PedidoResponse(campo(json, "protocolo"), campo(json, "referenciaERP"),
                    campo(json, "status"), campo(json, "recebidoEm"),
                    Boolean.parseBoolean(campo(json, "simulado")));
        } catch (RuntimeException invalid) {
            throw new IOException("Resposta GovConnect malformada", invalid);
        }
    }

    private static String campo(String json, String name) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^,}\\s]+))");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;
        if (matcher.group(1) != null) {
            return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\")
                    .replace("\\n", "\n");
        }
        return matcher.group(2);
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (c < 0x20) result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
                }
            }
        }
        return result.append('"').toString();
    }

    private static String erroJson(String code, String message) {
        return "{\"codigo\":" + quote(code) + ",\"mensagem\":" + quote(message) + "}";
    }

    private static void responder(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
