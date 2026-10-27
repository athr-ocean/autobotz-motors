import autobotz.portal.CatalogoMateriais;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/**
 * Garante que os materiais do portal sao copias fieis dos textos oficiais em docs/ e que todo
 * material publicado tem conteudo. Executar na raiz do repositorio.
 */
public class D15ConteudoTest {
    private static final Path CONTENT = Path.of("portal-treinamento", "content");
    private static final Map<String, String> OFICIAL = Map.of(
        "guia-rapido.md", "docs/D15_GUIA_RAPIDO.md",
        "faq.md", "docs/D15_FAQ.md");
    private static int falhas = 0;

    public static void main(String[] args) throws Exception {
        for (var par : OFICIAL.entrySet()) {
            Path copia = CONTENT.resolve(par.getKey());
            Path oficial = Path.of(par.getValue());
            boolean igual = Files.isRegularFile(copia) && Files.isRegularFile(oficial)
                && Arrays.equals(Files.readAllBytes(copia), Files.readAllBytes(oficial));
            check(igual, copia + " e igual a " + oficial);
        }
        CatalogoMateriais catalogo = new CatalogoMateriais(CONTENT);
        for (var m : catalogo.publicados()) {
            check(!catalogo.ler(m).isBlank(), "material publicado tem conteudo: " + m.slug());
        }
        System.out.println(falhas == 0 ? "TODOS OS TESTES PASSARAM" : falhas + " TESTE(S) FALHARAM");
        System.exit(falhas == 0 ? 0 : 1);
    }

    private static void check(boolean ok, String nome) {
        System.out.println((ok ? "PASS " : "FAIL ") + nome);
        if (!ok) falhas++;
    }
}
