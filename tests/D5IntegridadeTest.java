import autobotz.util.MonitorIntegridadeD5;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class D5IntegridadeTest {
    private static int verificacoes;

    private D5IntegridadeTest() {
    }

    private static void conferir(boolean condicao, String descricao) {
        if (!condicao) {
            throw new AssertionError(descricao);
        }
        verificacoes++;
        System.out.println("PASS " + descricao);
    }

    private static void esperarErro(Acao acao, String descricao) throws Exception {
        try {
            acao.executar();
        } catch (IOException ex) {
            conferir(true, descricao);
            return;
        }
        throw new AssertionError("Era esperada uma falha: " + descricao);
    }

    @FunctionalInterface
    private interface Acao {
        void executar() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        Path raiz = Files.createTempDirectory("autobotz-d5-test-");
        try {
            Path origem = Files.createDirectory(raiz.resolve("origem"));
            Path logVenda = origem.resolve("vendas.log");
            Path logAuditoria = origem.resolve("auditoria.log");
            Files.writeString(logVenda, "venda=10;valor=100\n", StandardCharsets.UTF_8);
            Files.writeString(logAuditoria, "usuario=teste;acao=salvar\n", StandardCharsets.UTF_8);
            Files.writeString(origem.resolve("anotacoes.txt"), "arquivo fora do escopo");

            Path regiaoA = raiz.resolve("regiao-a").resolve("captura");
            Path regiaoB = raiz.resolve("regiao-b").resolve("captura");
            MonitorIntegridadeD5.capturar(origem, regiaoA, regiaoB);
            MonitorIntegridadeD5.verificar(regiaoA);
            MonitorIntegridadeD5.verificar(regiaoB);
            conferir(Files.isRegularFile(regiaoA.resolve("manifesto.sha256"))
                    && Files.isRegularFile(regiaoB.resolve("manifesto.sha256")),
                    "manifestos das duas copias");
            conferir(Files.readString(regiaoA.resolve("manifesto.sha256"))
                    .equals(Files.readString(regiaoB.resolve("manifesto.sha256"))),
                    "copias com os mesmos hashes");

            MonitorIntegridadeD5.compararOrigem(origem, regiaoB);
            conferir(!Files.exists(regiaoA.resolve("anotacoes.txt")), "captura somente arquivos log");

            Path recuperado = raiz.resolve("recuperado");
            MonitorIntegridadeD5.restaurar(regiaoB, recuperado);
            conferir(Files.readString(recuperado.resolve("vendas.log"))
                    .equals(Files.readString(logVenda)), "restauracao recupera o conteudo");
            esperarErro(() -> MonitorIntegridadeD5.restaurar(regiaoB, recuperado),
                    "nao sobrescreve destino existente");

            Files.writeString(regiaoA.resolve("vendas.log"), "venda adulterada\n");
            esperarErro(() -> MonitorIntegridadeD5.verificar(regiaoA),
                    "detecta conteudo alterado na primeira regiao");
            MonitorIntegridadeD5.verificar(regiaoB);
            conferir(true, "segunda regiao continua valida");

            Files.writeString(logVenda, "venda=10;valor=200\n");
            esperarErro(() -> MonitorIntegridadeD5.compararOrigem(origem, regiaoB),
                    "detecta diferenca entre origem e captura");

            Files.delete(regiaoB.resolve("auditoria.log"));
            esperarErro(() -> MonitorIntegridadeD5.verificar(regiaoB),
                    "detecta arquivo ausente");

            esperarErro(() -> MonitorIntegridadeD5.capturar(origem, origem.resolve("copia"),
                    raiz.resolve("outra-regiao")), "recusa destino dentro da origem");

            Path vazio = Files.createDirectory(raiz.resolve("vazio"));
            esperarErro(() -> MonitorIntegridadeD5.capturar(vazio,
                    raiz.resolve("vazio-a"), raiz.resolve("vazio-b")), "recusa captura vazia");

            Path atalho = origem.resolve("atalho.log");
            Files.createSymbolicLink(atalho, logAuditoria);
            esperarErro(() -> MonitorIntegridadeD5.capturar(origem,
                    raiz.resolve("link-a"), raiz.resolve("link-b")), "recusa link simbolico");

            System.out.println("D5_INTEGRIDADE_OK checks=" + verificacoes);
        } finally {
            try (Stream<Path> paths = Files.walk(raiz)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
