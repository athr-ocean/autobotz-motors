package autobotz.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Confere cópias de logs exportados. Não acessa nem restaura diretamente o MariaDB.
 */
public final class MonitorIntegridadeD5 {
    private static final String MANIFESTO = "manifesto.sha256";
    private static final Pattern NOME_LOG = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]{0,149}\\.log");
    private static final Pattern LINHA = Pattern.compile("([0-9a-f]{64})  ([A-Za-z0-9][A-Za-z0-9_.-]{0,149}\\.log)");

    private MonitorIntegridadeD5() {
    }

    public static void capturar(Path origem, Path regiaoA, Path regiaoB) throws IOException {
        Path fonte = normalizar(origem);
        Path a = normalizar(regiaoA);
        Path b = normalizar(regiaoB);
        if (a.equals(b) || a.startsWith(fonte) || b.startsWith(fonte)
                || fonte.startsWith(a) || fonte.startsWith(b)
                || a.startsWith(b) || b.startsWith(a)) {
            throw new IOException("Origem e destinos devem ser caminhos separados.");
        }
        if (Files.exists(a, LinkOption.NOFOLLOW_LINKS)
                || Files.exists(b, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("O destino da cópia já existe. Informe novos caminhos.");
        }
        List<Path> arquivos = listarLogs(fonte);
        if (arquivos.isEmpty()) {
            throw new IOException("Nenhum arquivo .log encontrado na origem.");
        }

        Path temporarioA = null;
        Path temporarioB = null;
        try {
            temporarioA = criarTemporario(a);
            for (Path arquivo : arquivos) {
                Files.copy(arquivo, temporarioA.resolve(arquivo.getFileName()));
            }
            String conteudo = gerarManifesto(temporarioA);
            Files.writeString(temporarioA.resolve(MANIFESTO), conteudo, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            verificar(temporarioA);

            temporarioB = criarTemporario(b);
            for (Path arquivo : listarLogs(temporarioA)) {
                Files.copy(arquivo, temporarioB.resolve(arquivo.getFileName()));
            }
            Files.copy(temporarioA.resolve(MANIFESTO), temporarioB.resolve(MANIFESTO));
            verificar(temporarioB);

            mover(temporarioA, a);
            temporarioA = null;
            mover(temporarioB, b);
            temporarioB = null;
        } finally {
            limparTemporario(temporarioA);
            limparTemporario(temporarioB);
        }
    }

    public static void verificar(Path replica) throws IOException {
        Path pasta = normalizar(replica);
        Map<String, String> esperado = lerManifesto(pasta);
        List<Path> logs = listarLogs(pasta);
        if (logs.size() != esperado.size()) {
            throw new IOException("Quantidade de logs diferente da registrada no manifesto.");
        }
        try (Stream<Path> stream = Files.list(pasta)) {
            for (Path arquivo : stream.toList()) {
                if (!arquivo.getFileName().toString().equals(MANIFESTO)
                        && !arquivo.getFileName().toString().endsWith(".log")) {
                    throw new IOException("Arquivo inesperado na réplica: " + arquivo.getFileName());
                }
            }
        }
        compararArquivos(logs, esperado);
    }

    public static void compararOrigem(Path origem, Path replica) throws IOException {
        Path pasta = normalizar(origem);
        Map<String, String> esperado = lerManifesto(normalizar(replica));
        List<Path> logs = listarLogs(pasta);
        if (logs.size() != esperado.size()) {
            throw new IOException("A quantidade de logs mudou desde a captura.");
        }
        compararArquivos(logs, esperado);
    }

    public static void restaurar(Path replica, Path destino) throws IOException {
        Path copia = normalizar(replica);
        Path saida = normalizar(destino);
        if (saida.startsWith(copia) || copia.startsWith(saida)) {
            throw new IOException("A recuperação deve usar um destino separado.");
        }
        verificar(copia);
        if (Files.exists(saida, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("O destino já existe. A recuperação não sobrescreve arquivos.");
        }
        Path temporario = null;
        try {
            temporario = criarTemporario(saida);
            for (Path arquivo : listarLogs(copia)) {
                Files.copy(arquivo, temporario.resolve(arquivo.getFileName()));
            }
            compararOrigem(temporario, copia);
            mover(temporario, saida);
            temporario = null;
        } finally {
            limparTemporario(temporario);
        }
    }

    private static Path normalizar(Path caminho) {
        return caminho.toAbsolutePath().normalize();
    }

    private static Path criarTemporario(Path destino) throws IOException {
        Path pai = destino.getParent();
        Files.createDirectories(pai);
        if (Files.isSymbolicLink(pai)) {
            throw new IOException("Não é permitido usar diretórios simbólicos como destino.");
        }
        return Files.createTempDirectory(pai, ".d5-parcial-");
    }

    private static void mover(Path de, Path para) throws IOException {
        try {
            Files.move(de, para, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(de, para);
        }
    }

    private static void limparTemporario(Path temporario) throws IOException {
        if (temporario == null || !Files.exists(temporario, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> stream = Files.list(temporario)) {
            for (Path arquivo : stream.toList()) {
                Files.delete(arquivo);
            }
        }
        Files.delete(temporario);
    }

    private static List<Path> listarLogs(Path pasta) throws IOException {
        if (!Files.isDirectory(pasta, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Diretório não encontrado: " + pasta);
        }
        List<Path> arquivos = new ArrayList<>();
        try (Stream<Path> stream = Files.list(pasta)) {
            for (Path arquivo : stream.toList()) {
                if (Files.isSymbolicLink(arquivo)) {
                    throw new IOException("Link simbólico não permitido: " + arquivo.getFileName());
                }
                String nome = arquivo.getFileName().toString();
                if (nome.endsWith(".log")) {
                    if (!NOME_LOG.matcher(nome).matches()
                            || !Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Nome ou tipo de log inválido: " + nome);
                    }
                    arquivos.add(arquivo);
                }
            }
        }
        arquivos.sort(Comparator.comparing(x -> x.getFileName().toString()));
        return arquivos;
    }

    private static String gerarManifesto(Path pasta) throws IOException {
        StringBuilder texto = new StringBuilder();
        for (Path arquivo : listarLogs(pasta)) {
            texto.append(hash(arquivo)).append("  ")
                    .append(arquivo.getFileName()).append("\n");
        }
        return texto.toString();
    }

    private static Map<String, String> lerManifesto(Path replica) throws IOException {
        Path manifesto = replica.resolve(MANIFESTO);
        if (!Files.isRegularFile(manifesto, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Manifesto de integridade não encontrado.");
        }
        List<String> linhas = Files.readAllLines(manifesto, StandardCharsets.UTF_8);
        if (linhas.isEmpty()) {
            throw new IOException("Manifesto vazio.");
        }
        Map<String, String> hashes = new TreeMap<>();
        for (String linha : linhas) {
            Matcher matcher = LINHA.matcher(linha);
            if (!matcher.matches() || hashes.putIfAbsent(matcher.group(2), matcher.group(1)) != null) {
                throw new IOException("Manifesto inválido ou com arquivo repetido.");
            }
        }
        return hashes;
    }

    private static void compararArquivos(List<Path> logs, Map<String, String> esperado) throws IOException {
        for (Path arquivo : logs) {
            String nome = arquivo.getFileName().toString();
            if (!hash(arquivo).equals(esperado.get(nome))) {
                throw new IOException("Log ausente ou alterado: " + nome);
            }
        }
    }

    private static String hash(Path arquivo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bloco = new byte[8192];
            try (InputStream leitura = Files.newInputStream(arquivo)) {
                int n;
                while ((n = leitura.read(bloco)) != -1) {
                    digest.update(bloco, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 não está disponível.", ex);
        }
    }

    public static void main(String[] args) {
        try {
            if (args.length == 4 && "capturar".equals(args[0])) {
                capturar(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
                System.out.println("D5_OK: duas cópias verificadas.");
            } else if (args.length == 2 && "verificar".equals(args[0])) {
                verificar(Path.of(args[1]));
                System.out.println("D5_OK: cópia íntegra.");
            } else if (args.length == 3 && "comparar".equals(args[0])) {
                compararOrigem(Path.of(args[1]), Path.of(args[2]));
                System.out.println("D5_OK: arquivos iguais aos da captura.");
            } else if (args.length == 3 && "restaurar".equals(args[0])) {
                restaurar(Path.of(args[1]), Path.of(args[2]));
                System.out.println("D5_OK: logs recuperados no novo diretório.");
            } else if (args.length == 3 && "monitorar".equals(args[0])) {
                int segundos = Integer.parseInt(args[2]);
                if (segundos < 1 || segundos > 86400) {
                    throw new IllegalArgumentException("O intervalo deve ser de 1 a 86400 segundos.");
                }
                while (true) {
                    verificar(Path.of(args[1]));
                    System.out.println(Instant.now() + " D5_OK: integridade confirmada.");
                    Thread.sleep(segundos * 1000L);
                }
            } else {
                throw new IllegalArgumentException("Uso: capturar ORIGEM REGIAO_A REGIAO_B | verificar REPLICA | "
                        + "comparar ORIGEM REPLICA | restaurar REPLICA NOVO_DESTINO | "
                        + "monitorar REPLICA INTERVALO_SEGUNDOS");
            }
        } catch (IOException | IllegalArgumentException ex) {
            System.err.println("D5_ERRO: " + ex.getMessage());
            System.exit(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("Monitoramento interrompido.");
            System.exit(1);
        }
    }
}
