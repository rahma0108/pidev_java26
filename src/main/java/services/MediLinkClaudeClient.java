package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MediLinkClaudeClient {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    public String ask(String systemPrompt, String userMessage) {
        try {
            String apiKey = System.getenv("GROQ_API_KEY");
            boolean hasApiKey = apiKey != null && !apiKey.isBlank();
            System.out.println("Clé API détectée : " + (hasApiKey ? "oui" : "non"));

            if (!hasApiKey) {
                System.err.println("Erreur : GROQ_API_KEY non trouvee.");
                return null;
            }

            String body = """
                {
                  "model": "%s",
                  "messages": [
                    { "role": "system", "content": %s },
                    { "role": "user",   "content": %s }
                  ],
                  "temperature": 0.3
                }
                """.formatted(
                    MODEL,
                    toJsonString(systemPrompt),
                    toJsonString(userMessage)
            );

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            System.out.println("Appel API envoyé");
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("HTTP status = " + response.statusCode());
            System.out.println("Réponse API reçue : "
                    + ((response.body() != null && !response.body().isBlank()) ? "oui" : "non"));
            System.out.println("=== BODY BRUT API ===");
            System.out.println(response.body());
            System.out.println("=== FIN BODY BRUT API ===");

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            System.out.println("Parsing JSON OK");
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            System.out.println("Parsing JSON KO");
            System.err.println("Erreur appel Groq : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String toJsonString(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }
}
