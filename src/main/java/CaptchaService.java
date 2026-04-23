import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CaptchaService {

    // TextCaptcha API — free, no key needed
    private static final String API_URL = "http://api.textcaptcha.com/medilinkcareapp.json";

    private static String currentToken = "";
    private static String[] currentAnswerHashes = {};

    /**
     * Fetch a new CAPTCHA question from the API
     * Returns the question string, or a fallback if API is down
     */
    public static String fetchQuestion() {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            // Extract question and answer hashes
            String question = json.get("q").getAsString();
            currentToken    = json.get("t").getAsString();

            // Answers are MD5 hashes
            var answersArray = json.getAsJsonArray("a");
            currentAnswerHashes = new String[answersArray.size()];
            for (int i = 0; i < answersArray.size(); i++) {
                currentAnswerHashes[i] = answersArray.get(i).getAsString();
            }

            return question;

        } catch (Exception e) {
            System.err.println("CAPTCHA API error: " + e.getMessage());
            // Fallback to simple math if API is down
            return null;
        }
    }

    /**
     * Validate user's answer by MD5 hashing it and comparing
     */
    public static boolean validateAnswer(String userAnswer) {
        if (currentAnswerHashes.length == 0) return false;
        try {
            String hashed = md5(userAnswer.trim().toLowerCase());
            for (String validHash : currentAnswerHashes) {
                if (validHash.equalsIgnoreCase(hashed)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getToken() {
        return currentToken;
    }

    /**
     * MD5 hash a string
     */
    private static String md5(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
