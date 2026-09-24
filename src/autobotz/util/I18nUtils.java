package autobotz.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public final class I18nUtils {

    private static Locale currentLocale =
            Locale.of("pt", "BR");

    private static ResourceBundle bundle =
            ResourceBundle.getBundle(
                    "messages",
                    currentLocale
            );

    private I18nUtils() {
    }

    public static synchronized void setLocale(
            Locale locale) {

        currentLocale =
                locale == null
                        ? Locale.getDefault()
                        : locale;

        bundle =
                ResourceBundle.getBundle(
                        "messages",
                        currentLocale
                );
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static String getString(String key) {
        return bundle.getString(key);
    }

    public static ResourceBundle getBundle(
            Locale locale) {

        Locale idioma =
                locale == null
                        ? Locale.getDefault()
                        : locale;

        return ResourceBundle.getBundle(
                "messages",
                idioma
        );
    }

    public static String formatCurrency(
            double valor,
            Locale locale) {

        Locale idioma =
                locale == null
                        ? Locale.getDefault()
                        : locale;

        return NumberFormat
                .getCurrencyInstance(idioma)
                .format(valor);
    }

    public static String formatCurrency(
            double valor) {

        return formatCurrency(
                valor,
                currentLocale
        );
    }

    public static String formatDate(
            LocalDate data,
            Locale locale) {

        if (data == null) {
            return "";
        }

        Locale idioma =
                locale == null
                        ? Locale.getDefault()
                        : locale;

        return data.format(
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy",
                        idioma
                )
        );
    }

    public static String formatDate(
            LocalDate data) {

        return formatDate(
                data,
                currentLocale
        );
    }

    public static String formatVehicleStatus(String status) {

        if (status == null) {
            return "";
        }

        if (status.equalsIgnoreCase("VENDIDO")) {
            return getString("status.veiculo.vendido");
        }

        if (status.equalsIgnoreCase("DISPONIVEL")
                || status.equalsIgnoreCase("Disponivel")
                || status.equalsIgnoreCase("Disponível")) {

            return getString("status.veiculo.disponivel");
        }

        return status;
    }

    public static String formatServiceOrderStatus(String status) {

        if (status == null) {
            return "";
        }

        if (status.equalsIgnoreCase("ABERTA")) {
            return getString("status.os.aberta");
        }

        return status;
    }

}