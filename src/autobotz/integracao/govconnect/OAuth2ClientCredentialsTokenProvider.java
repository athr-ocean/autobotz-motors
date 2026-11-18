package autobotz.integracao.govconnect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obtém e mantém em cache tokens OAuth 2.0 pelo grant client_credentials.
 * O fluxo é máquina-a-máquina; não autentica um usuário humano.
 */
public final class OAuth2ClientCredentialsTokenProvider implements GovConnectIntegracao.AccessTokenProvider {
    private final URI tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final HttpClient http;
    private String tokenCache;
    private Instant expiraEm = Instant.EPOCH;

    public OAuth2ClientCredentialsTokenProvider(URI tokenUri, String clientId,
                                                 String clientSecret, String scope) {
        this.tokenUri = validarTokenUri(tokenUri);
        if (clientId == null || clientId.isBlank()) throw new IllegalArgumentException("clientId obrigatório");
        if (clientSecret == null || clientSecret.isBlank()) throw new IllegalArgumentException("clientSecret obrigatório");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope obrigatório");
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public synchronized String obterToken() throws IOException, InterruptedException {
        if (tokenCache != null && Instant.now().isBefore(expiraEm.minusSeconds(30))) return tokenCache;

        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        String form = "grant_type=client_credentials&scope="
                + URLEncoder.encode(scope, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(tokenUri)
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200)
            throw new IOException("Provedor de identidade recusou a emissão do token (HTTP "
                    + response.statusCode() + ")");

        String accessToken = campo(response.body(), "access_token");
        String tokenType = campo(response.body(), "token_type");
        String expiresIn = campo(response.body(), "expires_in");
        if (accessToken == null || accessToken.isBlank() || tokenType == null
                || !"bearer".equalsIgnoreCase(tokenType) || expiresIn == null)
            throw new IOException("Resposta inválida do provedor de identidade");

        long seconds;
        try {
            seconds = Long.parseLong(expiresIn);
        } catch (NumberFormatException invalid) {
            throw new IOException("expires_in inválido", invalid);
        }
        if (seconds < 60 || seconds > 86400)
            throw new IOException("Validade do token fora do intervalo aceito");

        tokenCache = accessToken;
        expiraEm = Instant.now().plusSeconds(seconds);
        return tokenCache;
    }

    private static URI validarTokenUri(URI uri) {
        Objects.requireNonNull(uri, "tokenUri");
        boolean loopback = "127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                || !("https".equalsIgnoreCase(uri.getScheme())
                || ("http".equalsIgnoreCase(uri.getScheme()) && loopback))) {
            throw new IllegalArgumentException(
                    "Token endpoint exige HTTPS; HTTP só é permitido em localhost para testes");
        }
        return uri;
    }

    private static String campo(String json, String nome) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(nome)
                + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|([^,}\\s]+))");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;
        return matcher.group(1) != null
                ? matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\")
                : matcher.group(2);
    }
}
