package autobotz.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18nUtils {
    private I18nUtils() {
    }

    public static ResourceBundle getBundle(Locale locale) {
        Locale idioma = locale == null ? Locale.getDefault() : locale;
        return ResourceBundle.getBundle("messages", idioma);
    }

    public static String formatCurrency(double valor, Locale locale) {
        Locale idioma = locale == null ? Locale.getDefault() : locale;
        return NumberFormat.getCurrencyInstance(idioma).format(valor);
    }

    public static String formatDate(LocalDate data, Locale locale) {
        if (data == null) return "";
        Locale idioma = locale == null ? Locale.getDefault() : locale;
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", idioma));
    }
}
