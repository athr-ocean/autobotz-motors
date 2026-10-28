package autobotz.portal;

import autobotz.service.TreinamentoService;
import autobotz.util.ConexaoBanco;

import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Ponto de entrada para o ERP (Swing ou console) abrir o portal de treinamento. Inicia o servidor
 * uma unica vez, em thread daemon (nao impede a JVM de encerrar) e abre o navegador. Nunca lanca
 * excecao: devolve um Status, que a interface traduz para mensagens dos bundles.
 */
public final class PortalAtalho {
    public enum Status { ABERTO, NAVEGADOR_INDISPONIVEL, PORTA_OCUPADA, CONTEUDO_INDISPONIVEL, ERRO }

    /** url e null quando o servidor nao subiu. Em NAVEGADOR_INDISPONIVEL a interface deve mostrar a url. */
    public record Resultado(Status status, String url) {}

    private static PortalServer servidor;
    private static String url;

    private PortalAtalho() {}

    /** Porta: AUTOBOTZ_PORTAL_PORT (padrao 8085). Pasta: AUTOBOTZ_PORTAL_DIR (padrao portal-treinamento). */
    public static Resultado abrir() {
        int porta = 8085;
        try {
            porta = Integer.parseInt(System.getenv().getOrDefault("AUTOBOTZ_PORTAL_PORT", "8085"));
        } catch (NumberFormatException e) {
            // mantem a porta padrao
        }
        Path base = Path.of(System.getenv().getOrDefault("AUTOBOTZ_PORTAL_DIR", "portal-treinamento"));
        return abrir(porta, base, null);
    }

    /** Versao com parametros, usada nos testes. Porta 0 escolhe uma porta livre. Backend nulo usa o padrao. */
    public static synchronized Resultado abrir(int porta, Path base, PortalBackend backend) {
        if (servidor == null) {
            CatalogoMateriais catalogo;
            try {
                catalogo = new CatalogoMateriais(base.resolve("content"));
            } catch (IOException e) {
                return new Resultado(Status.CONTEUDO_INDISPONIVEL, null);
            }
            try {
                PortalServer novo = new PortalServer(porta, backend != null ? backend : backendPadrao(),
                    catalogo, base.resolve("public"));
                iniciarComoDaemon(novo);
                servidor = novo;
                url = "http://localhost:" + novo.porta();
            } catch (BindException e) {
                return new Resultado(Status.PORTA_OCUPADA, "http://localhost:" + porta);
            } catch (Exception e) {
                System.err.println("Portal: falha ao iniciar: " + e);
                return new Resultado(Status.ERRO, null);
            }
        }
        return abrirNavegador(url);
    }

    /** Opcional: o servidor e daemon e nao impede o encerramento da aplicacao. */
    public static synchronized void parar() {
        if (servidor != null) {
            servidor.parar();
            servidor = null;
            url = null;
        }
    }

    /** Usa o servico real e conexoes independentes do banco configurado no ERP. */
    private static PortalBackend backendPadrao() throws SQLException {
        return new TreinamentoPortalBackend(new TreinamentoService(ConexaoBanco.getDataSource()));
    }

    /** A thread do dispatcher herda o estado daemon de quem chama start(). */
    private static void iniciarComoDaemon(PortalServer s) throws InterruptedException {
        Thread t = new Thread(s::iniciar, "portal-inicio");
        t.setDaemon(true);
        t.start();
        t.join();
    }

    private static Resultado abrirNavegador(String endereco) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(endereco));
                return new Resultado(Status.ABERTO, endereco);
            }
        } catch (Exception e) {
            // cai no retorno abaixo
        }
        return new Resultado(Status.NAVEGADOR_INDISPONIVEL, endereco);
    }
}
