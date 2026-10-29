package autobotz.portal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Catalogo de materiais lido de catalogo.json. O nome do arquivo de cada material vem
 * so do catalogo (nunca do navegador) e a leitura fica restrita a pasta de conteudo.
 * Leitor simplificado: cada material e um objeto plano, sem chaves dentro dos textos.
 */
public final class CatalogoMateriais {
    public record Material(String slug, String titulo, String tipo, String status, String formato, String arquivo) {}

    private static final Pattern SLUG = Pattern.compile("[a-z0-9][a-z0-9-]{0,99}");
    private static final Pattern ARQUIVO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");
    private static final Pattern OBJETO = Pattern.compile("\\{[^{}]*\\}");

    private final Path pasta;
    private final Map<String, Material> porSlug = new LinkedHashMap<>();

    public CatalogoMateriais(Path pastaConteudo) throws IOException {
        pasta = pastaConteudo.toAbsolutePath().normalize();
        String json = Files.readString(pasta.resolve("catalogo.json"), StandardCharsets.UTF_8);
        Matcher obj = OBJETO.matcher(json);
        while (obj.find()) {
            String o = obj.group();
            Material m = new Material(campo(o, "slug"), campo(o, "titulo"), campo(o, "tipo"),
                campo(o, "status"), campo(o, "formato"), campo(o, "arquivo"));
            if (!slugValido(m.slug()) || m.titulo() == null || m.tipo() == null || m.status() == null
                || m.formato() == null || m.arquivo() == null || !ARQUIVO.matcher(m.arquivo()).matches()
                || porSlug.putIfAbsent(m.slug(), m) != null) {
                throw new IOException("Entrada invalida no catalogo.json: " + o);
            }
        }
    }

    public static boolean slugValido(String slug) {
        return slug != null && SLUG.matcher(slug).matches();
    }

    public List<Material> publicados() {
        List<Material> r = new ArrayList<>();
        for (Material m : porSlug.values()) if (m.status().equals("PUBLICADO")) r.add(m);
        return r;
    }

    /** Vazio se o slug for invalido, desconhecido ou nao estiver PUBLICADO. */
    public Optional<Material> publicado(String slug) {
        if (!slugValido(slug)) return Optional.empty();
        return Optional.ofNullable(porSlug.get(slug)).filter(m -> m.status().equals("PUBLICADO"));
    }

    public String ler(Material m) throws IOException {
        Path real = pasta.resolve(m.arquivo()).toRealPath();
        if (!real.startsWith(pasta.toRealPath()) || !Files.isRegularFile(real)) {
            throw new IOException("Arquivo do material indisponivel.");
        }
        return Files.readString(real, StandardCharsets.UTF_8);
    }

    private static String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(nome) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }
}
