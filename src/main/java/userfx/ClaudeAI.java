package userfx;


import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;

public class ClaudeAI {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static String API_KEY = loadApiKey();

    private static String loadApiKey() {
        try {
            java.util.Properties props = new java.util.Properties();
            var stream = ClaudeAI.class.getResourceAsStream("/config.properties");
            if (stream == null) {
                System.err.println("config.properties NOT FOUND in resources!");
                return "";
            }
            props.load(stream);
            String key = props.getProperty("openai.api.key");
            return key;
        } catch (Exception e) {
            System.err.println("Could not load API key: " + e.getMessage());
            return "";
        }
    }

    /**
     * Send a prompt to Claude and get a text response
     */
    public static String ask(String systemPrompt, String userMessage) {
        // If no key is provided, use free Pollinations AI as fallback
        if (API_KEY == null || API_KEY.isEmpty()) {
            try {
                String fullPrompt = (systemPrompt != null ? systemPrompt + "\n\n" : "") + userMessage;
                String encodedPrompt = java.net.URLEncoder.encode(fullPrompt, java.nio.charset.StandardCharsets.UTF_8);
                
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(10))
                        .build();
                        
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://text.pollinations.ai/" + encodedPrompt))
                        .GET()
                        .build();
                        
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return response.body().trim();
                }
            } catch (Exception e) {
                System.err.println("Fallback AI failed: " + e.getMessage());
            }
            return "AI unavailable at the moment.";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(8))
                    .build();

            JsonArray messages = new JsonArray();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                JsonObject sysMsg = new JsonObject();
                sysMsg.addProperty("role", "system");
                sysMsg.addProperty("content", systemPrompt);
                messages.add(sysMsg);
            }

            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);

            JsonObject body = new JsonObject();
            body.addProperty("model", "gpt-4o");
            body.addProperty("max_tokens", 300);
            body.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = client.sendAsync(request,
                            HttpResponse.BodyHandlers.ofString())
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (json.has("error")) {
                System.err.println("OpenAI error: " +
                        json.getAsJsonObject("error").get("message").getAsString());
                // Fallback to pollinations even on error if possible
                return ask(null, userMessage); 
            }

            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                return choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString().trim();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "AI unavailable at the moment.";
    }
}