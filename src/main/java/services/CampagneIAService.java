package services;

import models.CampagneDonnee;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Génère le texte d'une campagne d'aide via le même fournisseur que les dons ({@code don.ai.provider}).
 */
public final class CampagneIAService {

    public CampagneIAService() throws IOException {
    }

    private static Properties loadProperties() throws IOException {
        Properties p = new Properties();
        try (InputStream in = CampagneIAService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        return p;
    }

    public CampagneDonnee genererDepuisMessage(String messageUrgence) throws IOException {
        Properties p = loadProperties();
        String provider = p.getProperty("don.ai.provider", "gemini").trim().toLowerCase(Locale.ROOT);
        if ("claude".equals(provider) || "anthropic".equals(provider)) {
            return new ClaudeAPIService().genererCampagneUrgence(messageUrgence);
        }
        return new GeminiAPIService().genererCampagneUrgence(messageUrgence);
    }
}
