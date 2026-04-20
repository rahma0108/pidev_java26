package services;

import models.AnalyseDonIA;
import models.CampagneDonnee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Appelle l’API Messages Anthropic (Claude) via {@link HttpURLConnection}.
 */
public final class ClaudeAPIService implements DonAnalyseLLM {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;

    public ClaudeAPIService() throws IOException {
        this(loadProperties());
    }

    private ClaudeAPIService(Properties p) {
        this.apiKey = firstNonBlank(
                System.getenv("ANTHROPIC_API_KEY"),
                p.getProperty("anthropic.api.key", ""));
        this.model = p.getProperty("anthropic.model", "claude-sonnet-4-20250514").trim();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Clé API Anthropic absente : définissez la variable d’environnement ANTHROPIC_API_KEY "
                            + "ou la propriété anthropic.api.key dans application.properties.");
        }
    }

    public ClaudeAPIService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model != null && !model.isBlank() ? model : "claude-sonnet-4-20250514";
    }

    private static Properties loadProperties() throws IOException {
        Properties p = new Properties();
        try (var in = ClaudeAPIService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        return p;
    }

    @Override
    public AnalyseDonIA analyserDon(
            String titre,
            String categorie,
            String description,
            int quantite) throws IOException {

        String userPrompt = DonIAPrompt.construirePromptUtilisateur(titre, categorie, description, quantite);
        String requestBody = "{"
                + "\"model\":\"" + DonIAPrompt.escapeJson(model) + "\","
                + "\"max_tokens\":1024,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + DonIAPrompt.escapeJson(userPrompt) + "\"}]"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(60_000);
        conn.setReadTimeout(120_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);

        byte[] payload = requestBody.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(payload.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int code = conn.getResponseCode();
        String body = lireCorps(conn, code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new IOException("Claude API HTTP " + code + " : " + body);
        }

        String texteModele = extraireTextePremierBlocAnthropic(body);
        if (texteModele == null || texteModele.isBlank()) {
            throw new IOException("Réponse Claude sans bloc texte exploitable : " + body);
        }
        return DonIAPrompt.parserJsonAnalyse(DonIAPrompt.extraireJsonObjet(texteModele));
    }

    public CampagneDonnee genererCampagneUrgence(String messageUrgence) throws IOException {
        String userPrompt = CampagneIAPrompt.construirePromptCampagne(messageUrgence);
        String requestBody = "{"
                + "\"model\":\"" + DonIAPrompt.escapeJson(model) + "\","
                + "\"max_tokens\":1024,"
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + DonIAPrompt.escapeJson(userPrompt) + "\"}]"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(60_000);
        conn.setReadTimeout(120_000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("x-api-key", apiKey);
        conn.setRequestProperty("anthropic-version", ANTHROPIC_VERSION);

        byte[] payload = requestBody.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(payload.length));
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
        }

        int code = conn.getResponseCode();
        String body = lireCorps(conn, code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        if (code < 200 || code >= 300) {
            throw new IOException("Claude API HTTP " + code + " : " + body);
        }

        String texteModele = extraireTextePremierBlocAnthropic(body);
        if (texteModele == null || texteModele.isBlank()) {
            throw new IOException("Réponse Claude sans bloc texte exploitable : " + body);
        }
        return CampagneIAPrompt.parserCampagne(DonIAPrompt.extraireJsonObjet(texteModele));
    }

    private static String extraireTextePremierBlocAnthropic(String responseJson) {
        int contentIdx = responseJson.indexOf("\"content\"");
        if (contentIdx < 0) {
            return null;
        }
        int typeText = responseJson.indexOf("\"type\":\"text\"", contentIdx);
        if (typeText < 0) {
            typeText = responseJson.indexOf("\"type\": \"text\"", contentIdx);
        }
        if (typeText < 0) {
            return null;
        }
        int textKey = responseJson.indexOf("\"text\"", typeText);
        if (textKey < 0) {
            return null;
        }
        int colon = responseJson.indexOf(':', textKey);
        if (colon < 0) {
            return null;
        }
        int startQuote = responseJson.indexOf('"', colon + 1);
        if (startQuote < 0) {
            return null;
        }
        return DonIAPrompt.unescapeJsonString(responseJson, startQuote + 1);
    }

    private static String lireCorps(HttpURLConnection conn, java.io.InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }
}
