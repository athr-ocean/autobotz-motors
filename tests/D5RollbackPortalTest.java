import autobotz.portal.CatalogoMateriais;
import autobotz.portal.MockPortalBackend;
import autobotz.portal.PortalServer;
import autobotz.util.MonitorIntegridadeD5;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Simula uma atualização defeituosa dos arquivos do portal e o retorno à versão anterior.
 * Usa cópias temporárias do próprio portal e não modifica a instalação do usuário.
 */
public final class D5RollbackPortalTest {
    private static int verificacoes;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private D5RollbackPortalTest() {
    }

    private static void conferir(boolean condicao, String teste) {
        if (!condicao) {
            throw new AssertionError("FAIL " + teste);
        }
        verificacoes++;
        System.out.println("PASS " + teste);
    }

    private static void copiarDiretorio(Path origem, Path destino) throws IOException {
        try (Stream<Path> stream = Files.walk(origem)) {
            for (Path arquivo : stream.sorted().toList()) {
                Path novo = destino.resolve(origem.relativize(arquivo));
                if (Files.isDirectory(arquivo)) {
                    Files.createDirectories(novo);
                } else if (Files.isRegularFile(arquivo) && !Files.isSymbolicLink(arquivo)) {
                    Files.copy(arquivo, novo, StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    throw new IOException("Arquivo inválido na versão do portal: " + arquivo);
                }
            }
        }
    }

    private static void apagarTemporarios(Path raiz) throws IOException {
        if (raiz == null || !Files.exists(raiz)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(raiz)) {
            for (Path arquivo : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(arquivo);
            }
        }
    }

    private static HttpResponse<String> requisitar(int porta, String rota) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + porta + rota))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return HTTP.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static PortalServer iniciar(Path pastaPublica, Path pastaConteudo) throws IOException {
        PortalServer portal = new PortalServer(0, new MockPortalBackend(),
                new CatalogoMateriais(pastaConteudo), pastaPublica);
        portal.iniciar();
        return portal;
    }

    public static void main(String[] args) throws Exception {
        Path raizPublica = Path.of("portal-treinamento", "public");
        Path conteudo = Path.of("portal-treinamento", "content");
        if (!Files.isRegularFile(raizPublica.resolve("index.html"))
                || !Files.isRegularFile(raizPublica.resolve("app.js"))
                || !Files.isRegularFile(conteudo.resolve("catalogo.json"))) {
            throw new IOException("Execute o teste na raiz do repositório AutoBotz.");
        }

        Path temporarios = Files.createTempDirectory("autobotz-d5-rollback-");
        PortalServer portal = null;
        try {
            Path versaoEstavel = temporarios.resolve("versao-estavel");
            Path versaoComFalha = temporarios.resolve("versao-com-falha");
            copiarDiretorio(raizPublica, versaoEstavel);
            copiarDiretorio(raizPublica, versaoComFalha);
            byte[] indexOriginal = Files.readAllBytes(versaoEstavel.resolve("index.html"));

            Path logs = Files.createDirectory(temporarios.resolve("logs"));
            Files.writeString(logs.resolve("transacoes.log"),
                    "D5;operacao=teste;status=confirmada\n", StandardCharsets.UTF_8);
            Path copiaA = temporarios.resolve("replica-a");
            Path copiaB = temporarios.resolve("replica-b");
            MonitorIntegridadeD5.capturar(logs, copiaA, copiaB);
            conferir(Files.isRegularFile(copiaA.resolve("manifesto.sha256")),
                    "COPIA_DOS_LOGS_ANTES_DA_ATUALIZACAO");

            portal = iniciar(versaoEstavel, conteudo);
            var paginaEstavel = requisitar(portal.porta(), "/");
            conferir(paginaEstavel.statusCode() == 200
                    && paginaEstavel.body().contains("id=\"login-form\""),
                    "PORTAL_ESTAVEL_ABRE_NORMALMENTE");
            conferir(requisitar(portal.porta(), "/app.js").statusCode() == 200,
                    "JAVASCRIPT_DA_VERSAO_ESTAVEL");
            conferir(requisitar(portal.porta(), "/api/treinamentos/materiais").statusCode() == 200,
                    "CATALOGO_DA_VERSAO_ESTAVEL");
            portal.parar();
            portal = null;

            // A atualização de teste perde o index.html; a versão estável fica preservada.
            Files.delete(versaoComFalha.resolve("index.html"));
            conferir(!Files.exists(versaoComFalha.resolve("index.html"))
                    && Files.isRegularFile(versaoEstavel.resolve("index.html")),
                    "ATUALIZACAO_DEFEITUOSA_ISOLADA");

            portal = iniciar(versaoComFalha, conteudo);
            var paginaComFalha = requisitar(portal.porta(), "/");
            conferir(paginaComFalha.statusCode() == 404,
                    "FALHA_DA_NOVA_VERSAO_DETECTADA");
            portal.parar();
            portal = null;

            // A aplicação volta a usar os arquivos guardados antes do deploy.
            portal = iniciar(versaoEstavel, conteudo);
            var paginaRecuperada = requisitar(portal.porta(), "/");
            conferir(paginaRecuperada.statusCode() == 200
                    && paginaRecuperada.body().contains("id=\"login-form\""),
                    "ROLLBACK_RESTAURA_A_INTERFACE");
            conferir(requisitar(portal.porta(), "/app.js").statusCode() == 200,
                    "ROLLBACK_RESTAURA_O_JAVASCRIPT");
            conferir(requisitar(portal.porta(), "/api/treinamentos/materiais").statusCode() == 200,
                    "CATALOGO_RESPONDE_APOS_ROLLBACK");
            conferir(Arrays.equals(indexOriginal,
                    Files.readAllBytes(versaoEstavel.resolve("index.html"))),
                    "ARQUIVO_DA_VERSAO_ESTAVEL_PRESERVADO");

            MonitorIntegridadeD5.verificar(copiaA);
            MonitorIntegridadeD5.verificar(copiaB);
            MonitorIntegridadeD5.compararOrigem(logs, copiaA);
            conferir(Files.readString(logs.resolve("transacoes.log"), StandardCharsets.UTF_8)
                    .contains("status=confirmada"), "LOG_PRESERVADO_APOS_ROLLBACK");

            System.out.println("D5_ROLLBACK_PORTAL_OK checks=" + verificacoes);
        } finally {
            if (portal != null) {
                portal.parar();
            }
            apagarTemporarios(temporarios);
        }
    }
}
