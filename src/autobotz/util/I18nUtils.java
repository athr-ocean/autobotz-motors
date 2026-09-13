package autobotz.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18nUtils {

    // Define o idioma padrão inicial como Português do Brasil
    private static Locale currentLocale = Locale.of("pt", "BR");
    private static ResourceBundle bundle = ResourceBundle.getBundle("messages", currentLocale);

    // Método para trocar o idioma do sistema (ex: pt_BR ou en_US)
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    // Busca o texto correspondente à chave no arquivo .properties
    public static String getString(String key) {
        return bundle.getString(key);
    }

    // Formata um valor numérico para o formato de moeda do idioma selecionado
    public static String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(currentLocale);
        return formatter.format(amount);
    }

    // Método principal para testar se a formatação e tradução estão funcionando
    public static void main(String[] args) {
        System.out.println("--- Teste pt_BR ---");
        System.out.println(getString("menu.title"));
        System.out.println(formatCurrency(50000));

        System.out.println("\n--- Teste en_US ---");
        setLocale(Locale.of("en", "US"));
        System.out.println(getString("menu.title"));
        System.out.println(formatCurrency(50000));
    }
}