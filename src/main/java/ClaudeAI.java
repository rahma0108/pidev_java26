import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class ClaudeAI {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(ClaudeAI.class.getResourceAsStream("/config.properties"));
            return props.getProperty("openai.api.key");
        } catch (Exception e) {
            System.err.println("Could not load API key: " + e.getMessage());
            return "";
        }
    }

    /**
     * Send a prompt to Claude and get a text response
     */
    public static String ask(String systemPrompt, String userMessage) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            JsonObject body = new JsonObject();
            body.addProperty("model", "claude-sonnet-4-20250514");
            body.addProperty("max_tokens", 1024);

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                body.addProperty("system", systemPrompt);
            }

            JsonArray messages = new JsonArray();
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);
            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", API_KEY)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

            HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray content = json.getAsJsonArray("content");
            if (content != null && content.size() > 0) {
                return content.get(0).getAsJsonObject()
                    .get("text").getAsString().trim();
            }
        } catch (Exception e) {
            System.err.println("Claude API error: " + e.getMessage());
        }
        return "AI unavailable at the moment.";
    }
}
