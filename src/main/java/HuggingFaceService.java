import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HuggingFaceService {

    private static final String API_URL = "https://text.pollinations.ai/";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private final HttpClient httpClient;

    public HuggingFaceService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .proxy(ProxySelector.getDefault())
                .build();
    }

    public String generateEventDescription(String eventName) throws IOException, InterruptedException {
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Nom de l'événement vide");
        }

        String prompt = "Rédige une description professionnelle et attrayante de 3 ou 4 lignes pour un événement intitulé : " + eventName;
        String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + encodedPrompt))
                .timeout(TIMEOUT)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)") 
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur API (" + response.statusCode() + ") : " + response.body());
        }

        String result = response.body();
        if (result == null || result.isBlank()) {
            throw new RuntimeException("L'IA n'a pas pu générer de texte.");
        }

        return result.trim();
    }
}
