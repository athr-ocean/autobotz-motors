package autobotz.integracao;

import autobotz.model.ContratoFornecedor;
import autobotz.model.StatusContrato;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Cliente e endpoint local da prova de conceito de integração do D13.
 * A chamada HTTP usa apenas loopback; o endpoint não é uma blockchain nem realiza pagamentos.
 */
public final class ApiBlockchainSimulada {
    public static final String CAMINHO = "/api/v1/contratos";
    public static final String CABECALHO_CHAVE = "X-Autobotz-Simulation-Key";
    private static final int LIMITE_CORPO = 8192;
    private static final Pattern HASH_SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Set<String> CAMPOS_REQUISICAO = Set.of(
            "contratoId", "fornecedorId", "valor", "vencimento", "estado",
            "notaFiscalValidada", "pagamentoSimuladoLiberado", "sequenciaLedger", "hashLedger");
    private static final Set<String> CAMPOS_RESPOSTA = Set.of(
            "contratoId", "hashLedger", "status", "protocolo", "simulado");

    private ApiBlockchainSimulada() { }

    /** Decisão devolvida pelo contrato inteligente simulado. */
    public enum Status {
        CONTRATO_ENCERRADO,
        AGUARDANDO_NOTA_FISCAL,
        AGUARDANDO_VENCIMENTO,
        PAGAMENTO_SIMULADO_JA_LIBERADO,
        LIBERACAO_SIMULADA_ELEGIVEL
    }

    /** Recibo da plataforma de simulação. O hash enviado identifica a versão do contrato avaliada. */
    public record Resposta(long contratoId, String hashLedger, Status status,
                           String protocolo, boolean simulado) { }

    /** Cliente restrito ao endpoint local, para impedir envio acidental de dados a serviço externo. */
    public static final class Cliente {
        private final URI endpoint;
        private final String chave;
        private final HttpClient http;

        public Cliente(URI endpoint, String chave) {
            this.endpoint = validarEndpoint(endpoint);
            this.chave = validarChave(chave);
            this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        }

        public Resposta enviarContrato(ContratoFornecedor contrato) throws IOException, InterruptedException {
            Objects.requireNonNull(contrato, "contrato");
            Map<String, String> campos = camposContrato(contrato);
            String corpo = codificar(campos);
            HttpRequest pedido = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Accept", "application/x-www-form-urlencoded")
                    .header(CABECALHO_CHAVE, chave)
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resposta = http.send(pedido,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resposta.statusCode() != 200) {
                throw new IOException("API simulada recusou a requisição (HTTP " + resposta.statusCode() + ")");
            }
            Resposta recibo = decodificarResposta(resposta.body());
            if (recibo.contratoId() != contrato.getId()
                    || !recibo.hashLedger().equals(contrato.getUltimoHashLedger()) || !recibo.simulado()) {
                throw new IOException("Resposta da API simulada não corresponde ao contrato enviado");
            }
            return recibo;
        }
    }

    /** Endpoint de teste local que simula a avaliação de um contrato inteligente. */
    public static final class Servidor implements AutoCloseable {
        private final HttpServer servidor;
        private final ExecutorService executor;
        private final String chave;
        private final Clock clock;

        public Servidor(String chave, Clock clock) throws IOException {
            this.chave = validarChave(chave);
            this.clock = Objects.requireNonNull(clock, "clock");
            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            servidor = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
            executor = Executors.newFixedThreadPool(2, tarefa -> {
                Thread thread = new Thread(tarefa, "d13-api-blockchain-simulada");
                thread.setDaemon(true);
                return thread;
            });
            servidor.setExecutor(executor);
            servidor.createContext(CAMINHO, this::atender);
            servidor.start();
        }

        public URI endpoint() {
            return URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + CAMINHO);
        }

        @Override
        public void close() {
            servidor.stop(0);
            executor.shutdownNow();
        }

        private void atender(HttpExchange exchange) throws IOException {
            try (exchange) {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    responderErro(exchange, 405, "Metodo nao permitido");
                    return;
                }
                if (!CAMINHO.equals(exchange.getRequestURI().getPath())) {
                    responderErro(exchange, 404, "Rota nao encontrada");
                    return;
                }
                if (!chaveCorreta(exchange.getRequestHeaders().getFirst(CABECALHO_CHAVE), chave)) {
                    responderErro(exchange, 401, "Credencial de simulacao invalida");
                    return;
                }
                String tipo = exchange.getRequestHeaders().getFirst("Content-Type");
                if (tipo == null || !tipo.toLowerCase(java.util.Locale.ROOT)
                        .startsWith("application/x-www-form-urlencoded")) {
                    responderErro(exchange, 415, "Formato de requisicao nao suportado");
                    return;
                }
                Map<String, String> campos = decodificarForm(lerLimitado(exchange.getRequestBody()));
                if (!campos.keySet().equals(CAMPOS_REQUISICAO)) {
                    responderErro(exchange, 400, "Campos da requisicao invalidos");
                    return;
                }
                ContratoRecebido contrato = validarContrato(campos);
                Status status = decidirStatus(contrato, LocalDate.now(clock));
                String protocolo = sha256(contrato.canonico() + "|status=" + status
                        + "|avaliadoEm=" + LocalDate.now(clock));
                Map<String, String> recibo = new LinkedHashMap<>();
                recibo.put("contratoId", Long.toString(contrato.id()));
                recibo.put("hashLedger", contrato.hashLedger());
                recibo.put("status", status.name());
                recibo.put("protocolo", protocolo);
                recibo.put("simulado", "true");
                responderForm(exchange, 200, recibo);
            } catch (IllegalArgumentException e) {
                responderErro(exchange, 400, "Dados do contrato invalidos");
            } catch (IOException e) {
                responderErro(exchange, 400, "Requisicao invalida");
            }
        }
    }

    private record ContratoRecebido(long id, int fornecedorId, BigDecimal valor, LocalDate vencimento,
                                    StatusContrato estado, boolean notaFiscalValidada,
                                    boolean pagamentoSimuladoLiberado, int sequencia,
                                    String hashLedger) {
        String canonico() {
            return id + "|" + fornecedorId + "|" + valor.toPlainString() + "|" + vencimento
                    + "|" + estado.name() + "|" + notaFiscalValidada + "|"
                    + pagamentoSimuladoLiberado + "|" + sequencia + "|" + hashLedger;
        }
    }

    private static Status decidirStatus(ContratoRecebido c, LocalDate hoje) {
        if (c.estado().isTerminal()) return Status.CONTRATO_ENCERRADO;
        if (c.pagamentoSimuladoLiberado()) return Status.PAGAMENTO_SIMULADO_JA_LIBERADO;
        if (!c.notaFiscalValidada()) return Status.AGUARDANDO_NOTA_FISCAL;
        if (c.vencimento().isAfter(hoje)) return Status.AGUARDANDO_VENCIMENTO;
        return Status.LIBERACAO_SIMULADA_ELEGIVEL;
    }

    private static Map<String, String> camposContrato(ContratoFornecedor c) {
        if (c.getId() <= 0 || c.getFornecedorId() <= 0 || c.getValor().signum() <= 0
                || c.getValor().stripTrailingZeros().scale() > 2 || c.getUltimaSequenciaLedger() <= 0
                || c.getUltimoHashLedger() == null || !HASH_SHA256.matcher(c.getUltimoHashLedger()).matches()) {
            throw new IllegalArgumentException("Contrato persistido e ancorado é obrigatório para envio");
        }
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("contratoId", Long.toString(c.getId()));
        campos.put("fornecedorId", Integer.toString(c.getFornecedorId()));
        campos.put("valor", c.getValor().setScale(2).toPlainString());
        campos.put("vencimento", c.getDataVencimento().toString());
        campos.put("estado", c.getEstado().name());
        campos.put("notaFiscalValidada", Boolean.toString(c.isNotaFiscalValidada()));
        campos.put("pagamentoSimuladoLiberado", Boolean.toString(c.isPagamentoSimuladoLiberado()));
        campos.put("sequenciaLedger", Integer.toString(c.getUltimaSequenciaLedger()));
        campos.put("hashLedger", c.getUltimoHashLedger());
        return campos;
    }

    private static ContratoRecebido validarContrato(Map<String, String> m) {
        long id = Long.parseLong(m.get("contratoId"));
        int fornecedorId = Integer.parseInt(m.get("fornecedorId"));
        BigDecimal valor = new BigDecimal(m.get("valor"));
        LocalDate vencimento = LocalDate.parse(m.get("vencimento"));
        StatusContrato estado = StatusContrato.valueOf(m.get("estado"));
        boolean nf = booleano(m.get("notaFiscalValidada"));
        boolean pagamento = booleano(m.get("pagamentoSimuladoLiberado"));
        int sequencia = Integer.parseInt(m.get("sequenciaLedger"));
        String hash = m.get("hashLedger");
        if (id <= 0 || fornecedorId <= 0 || valor.signum() <= 0 || valor.stripTrailingZeros().scale() > 2
                || sequencia <= 0 || !HASH_SHA256.matcher(hash).matches()) {
            throw new IllegalArgumentException("Valores fora dos limites");
        }
        return new ContratoRecebido(id, fornecedorId, valor.setScale(2), vencimento, estado,
                nf, pagamento, sequencia, hash);
    }

    private static Resposta decodificarResposta(String corpo) throws IOException {
        try {
            Map<String, String> m = decodificarForm(corpo.getBytes(StandardCharsets.UTF_8));
            if (!m.keySet().equals(CAMPOS_RESPOSTA)) throw new IllegalArgumentException("Campos inesperados");
            long id = Long.parseLong(m.get("contratoId"));
            String hash = m.get("hashLedger");
            String protocolo = m.get("protocolo");
            Status status = Status.valueOf(m.get("status"));
            if (id <= 0 || !HASH_SHA256.matcher(hash).matches() || !HASH_SHA256.matcher(protocolo).matches()) {
                throw new IllegalArgumentException("Recibo inválido");
            }
            return new Resposta(id, hash, status, protocolo, booleano(m.get("simulado")));
        } catch (IllegalArgumentException e) {
            throw new IOException("Resposta inválida da API simulada", e);
        }
    }

    private static URI validarEndpoint(URI uri) {
        Objects.requireNonNull(uri, "endpoint");
        if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || !"127.0.0.1".equals(uri.getHost()) || uri.getPort() <= 0
                || !CAMINHO.equals(uri.getPath()) || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("A API de demonstração aceita somente endpoint HTTP local 127.0.0.1");
        }
        return uri;
    }

    private static String validarChave(String chave) {
        if (chave == null || chave.length() < 16 || chave.length() > 256) {
            throw new IllegalArgumentException("A chave da API de simulação deve ter de 16 a 256 caracteres");
        }
        return chave;
    }

    private static boolean chaveCorreta(String recebida, String esperada) {
        return recebida != null && MessageDigest.isEqual(
                recebida.getBytes(StandardCharsets.UTF_8), esperada.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean booleano(String valor) {
        if ("true".equals(valor)) return true;
        if ("false".equals(valor)) return false;
        throw new IllegalArgumentException("Booleano inválido");
    }

    private static String codificar(Map<String, String> campos) {
        StringBuilder resultado = new StringBuilder();
        campos.forEach((chave, valor) -> {
            if (resultado.length() > 0) resultado.append('&');
            resultado.append(URLEncoder.encode(chave, StandardCharsets.UTF_8))
                    .append('=').append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
        });
        return resultado.toString();
    }

    private static Map<String, String> decodificarForm(byte[] bytes) throws IOException {
        if (bytes.length > LIMITE_CORPO) throw new IOException("Corpo excede o limite permitido");
        String corpo = new String(bytes, StandardCharsets.UTF_8);
        Map<String, String> campos = new LinkedHashMap<>();
        if (corpo.isEmpty()) throw new IllegalArgumentException("Corpo vazio");
        for (String par : corpo.split("&", -1)) {
            int separador = par.indexOf('=');
            if (separador <= 0) throw new IllegalArgumentException("Campo malformado");
            String chave = URLDecoder.decode(par.substring(0, separador), StandardCharsets.UTF_8);
            String valor = URLDecoder.decode(par.substring(separador + 1), StandardCharsets.UTF_8);
            if (campos.putIfAbsent(chave, valor) != null) throw new IllegalArgumentException("Campo duplicado");
        }
        return campos;
    }

    private static byte[] lerLimitado(InputStream entrada) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bloco = new byte[1024];
        int total = 0;
        int lido;
        while ((lido = entrada.read(bloco)) != -1) {
            total += lido;
            if (total > LIMITE_CORPO) throw new IOException("Corpo excede o limite permitido");
            buffer.write(bloco, 0, lido);
        }
        return buffer.toByteArray();
    }

    private static void responderForm(HttpExchange exchange, int status, Map<String, String> campos)
            throws IOException {
        byte[] corpo = codificar(campos).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, corpo.length);
        exchange.getResponseBody().write(corpo);
    }

    private static void responderErro(HttpExchange exchange, int status, String mensagem) throws IOException {
        byte[] corpo = mensagem.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, corpo.length);
        exchange.getResponseBody().write(corpo);
    }

    private static String sha256(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível no JDK", e);
        }
    }
}
