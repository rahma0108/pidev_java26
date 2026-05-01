package userfx;


import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Properties;

public class TurnstileService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static String SECRET_KEY = "";

    static {
        try {
            Properties props = new Properties();
            props.load(TurnstileService.class.getResourceAsStream("/config.properties"));
            SECRET_KEY = props.getProperty("turnstile.secret.key", "");
        } catch (Exception e) {
            System.err.println("Could not load Turnstile secret: " + e.getMessage());
        }
    }

    /**
     * Verify the token with Cloudflare's API
     * Returns true if valid
     */
    public static boolean verify(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            String body = "secret=" + SECRET_KEY + "&response=" + token;

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VERIFY_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            String resp = response.body();
            System.out.println("Turnstile response: " + resp);

            // Parse success field
            return resp.contains("\"success\":true");

        } catch (Exception e) {
            System.err.println("Turnstile verify error: " + e.getMessage());
            return false;
        }
    }
}
