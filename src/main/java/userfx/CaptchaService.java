package userfx;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;
import java.util.Random;

public class CaptchaService {

    // We use api.yagura.net — free image CAPTCHA API, no key needed
    private static final String API_URL = "https://api.yagura.net/captcha/v1/image";

    private static String sessionId = "";
    private static byte[] lastImageBytes = null;
    private static final Random rand = new Random();

    // Fallback math captcha
    private static int mathAnswer = -1;
    private static boolean usingFallback = false;

    /**
     * Fetch a CAPTCHA image from the API
     * Returns the image bytes, or null if API fails
     */
    public static byte[] fetchCaptchaImage() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            com.google.gson.JsonObject json =
                    com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();

            sessionId = json.get("session_id").getAsString();
            String base64Image = json.get("image").getAsString();

            // Remove data:image/png;base64, prefix if present
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }

            lastImageBytes = Base64.getDecoder().decode(base64Image);
            usingFallback = false;
            return lastImageBytes;

        } catch (Exception e) {
            System.err.println("CAPTCHA API error: " + e.getMessage());
            usingFallback = true;
            return null;
        }
    }

    public static boolean validate(String userAnswer) {
        if (usingFallback) {
            try {
                return Integer.parseInt(userAnswer.trim()) == mathAnswer;
            } catch (Exception e) { return false; }
        }

        try {
            String body = "session_id=" + sessionId + "&answer=" + userAnswer.trim();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.yagura.net/captcha/v1/verify"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.body().contains("\"success\":true");
        } catch (Exception e) {
            System.err.println("CAPTCHA validate error: " + e.getMessage());
            return false;
        }
    }

    // Generate math fallback
    public static String generateMathFallback() {
        usingFallback = true;
        int a = 1 + rand.nextInt(9);
        int b = 1 + rand.nextInt(9);
        int op = rand.nextInt(2);
        if (op == 0) {
            mathAnswer = a + b;
            return a + " + " + b + " = ?";
        } else {
            if (a < b) { int t = a; a = b; b = t; }
            mathAnswer = a - b;
            return a + " - " + b + " = ?";
        }
    }

    public static boolean isUsingFallback() { return usingFallback; }
}