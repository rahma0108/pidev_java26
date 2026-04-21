package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MediLinkClaudeClient {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";

    public String ask(String systemPrompt, String userMessage) {
        try {
            String apiKey = System.getenv("GROQ_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                System.err.println("=== ERREUR : GROQ_API_KEY non trouvée ===");
                return null;
            }

            System.out.println("=== CLÉ GROQ trouvée : " + apiKey.substring(0, 10) + "... ===");

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

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("=== STATUS HTTP : " + response.statusCode() + " ===");
            System.out.println("=== BODY : " + response.body() + " ===");

            // Extraire le contenu texte de la réponse Groq (format OpenAI)
            // Extraire le contenu via Gson (gère les caractères échappés)
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
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