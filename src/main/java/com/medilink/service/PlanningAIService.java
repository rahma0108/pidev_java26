package services;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import models.AIRecommendation;
import models.Disponibilite;
import services.MediLinkClaudeClient;
import java.util.List;

public class PlanningAIService {

    // ── 1. Prompt système (fixe) ──────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
        Tu es un assistant de planification médicale pour l'application MediLink.
        Ton rôle est uniquement d'aider à choisir le meilleur créneau de rendez-vous.
        
        Règles absolues :
        - Tu ne fais jamais de diagnostic médical
        - Tu ne recommandes jamais de traitement
        - Tu bases tes décisions uniquement sur : disponibilité, préférence horaire,
          urgence organisationnelle, historique de consultation
        - Tu réponds UNIQUEMENT en JSON valide, sans texte avant ni après
        - Tu n'inventes jamais un créneau qui n'existe pas dans les données reçues
        
        Format de réponse JSON obligatoire :
        {
          "creneau_recommande_id": "ID_du_creneau",
          "niveau_confiance": 0.0,
          "justification": "explication courte en français",
          "suggestions_secondaires": ["ID2", "ID3"],
          "alerte": ""
        }
        """;

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    // ── 2. Construction du prompt utilisateur ─────────────────────────────
    public String buildUserPrompt(
            String prenomPatient,
            String preferenceHoraire,
            String urgenceDeclaree,
            List<Disponibilite> disponibilites
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Voici la demande d'un patient :\n\n");
        sb.append("Patient : ").append(prenomPatient).append("\n");
        sb.append("Préférence horaire : ").append(preferenceHoraire).append("\n");
        sb.append("Urgence déclarée : ").append(urgenceDeclaree).append("\n\n");
        sb.append("Créneaux disponibles :\n");

        for (Disponibilite d : disponibilites) {
            sb.append("- ID: ").append(d.getId())
                    .append(" | Date: ").append(d.getDate())
                    .append(" | Heure: ").append(d.getHeureDebut())
                    .append(" → ").append(d.getHeureFin())
                    .append(" | Médecin: ")
                    .append(d.getMedecin() != null ? d.getMedecin().getFullName() : "Inconnu")
                    .append("\n");
        }

        sb.append("\nAnalyse ces créneaux et recommande le meilleur selon les préférences du patient.");
        sb.append("\nRéponds uniquement en JSON valide selon le format demandé.");
        return sb.toString();
    }

    // ── 3. Appel API + parsing + gestion erreurs ──────────────────────────
    public AIRecommendation recommanderCreneau(
            String prenomPatient,
            String preferenceHoraire,
            String urgenceDeclaree,
            List<Disponibilite> disponibilites
    ) {
        try {
            String systemPrompt = getSystemPrompt();
            String userPrompt   = buildUserPrompt(
                    prenomPatient, preferenceHoraire, urgenceDeclaree, disponibilites
            );

            MediLinkClaudeClient claude = new MediLinkClaudeClient();
            String reponseJson = claude.ask(systemPrompt, userPrompt);

            if (reponseJson == null || reponseJson.isBlank()) {
                return erreur("L'IA n'a pas renvoyé de réponse. Réessaie dans un moment.");
            }

            // TEMPORAIRE - affiche la reponse brute dans la console
            System.out.println("=== REPONSE BRUTE CLAUDE ===");
            System.out.println(reponseJson);
            System.out.println("=== FIN REPONSE ===");

            Gson gson = new Gson();
            com.google.gson.JsonObject obj = gson.fromJson(reponseJson, com.google.gson.JsonObject.class);

            AIRecommendation recommendation = new AIRecommendation();
            recommendation.setCreneauRecommandeId(
                    obj.has("creneau_recommande_id") ? obj.get("creneau_recommande_id").getAsString() : null
            );
            recommendation.setNiveauConfiance(
                    obj.has("niveau_confiance") ? obj.get("niveau_confiance").getAsDouble() : 0.0
            );
            recommendation.setJustification(
                    obj.has("justification") ? obj.get("justification").getAsString() : ""
            );
            recommendation.setAlerte(
                    obj.has("alerte") ? obj.get("alerte").getAsString() : ""
            );

            if (recommendation.getCreneauRecommandeId() == null) {
                return erreur("La réponse de l'IA est incomplète. Réessaie.");
            }

            return recommendation;

        } catch (JsonSyntaxException e) {
            return erreur("L'IA a renvoyé une réponse dans un format inattendu.");
        } catch (Exception e) {
            return erreur("Impossible de contacter l'IA : " + e.getMessage());
        }
    }

    // ── 4. Méthode utilitaire privée ──────────────────────────────────────
    private AIRecommendation erreur(String message) {
        AIRecommendation vide = new AIRecommendation();
        vide.setAlerte(message);
        vide.setEchec(true);
        return vide;
    }
}
