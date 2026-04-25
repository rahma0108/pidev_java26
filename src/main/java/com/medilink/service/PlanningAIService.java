package services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import models.AIRecommendation;
import models.Disponibilite;
import models.RendezVous;

import java.util.List;

public class PlanningAIService {

    private static final String SYSTEM_PROMPT = """
        Tu es un assistant de planification medicale pour l'application MediLink.
        Ton role est uniquement d'aider a choisir le meilleur creneau de rendez-vous.

        Regles absolues :
        - Tu ne fais jamais de diagnostic medical
        - Tu ne recommandes jamais de traitement
        - Tu bases tes decisions uniquement sur : disponibilite, preference horaire,
          urgence organisationnelle, historique de consultation
        - Tu reponds UNIQUEMENT en JSON valide, sans texte avant ni apres
        - Tu n'inventes jamais un creneau qui n'existe pas dans les donnees recues

        Format de reponse JSON obligatoire :
        {
          "creneau_recommande_id": "ID_du_creneau",
          "niveau_confiance": 0.0,
          "justification": "explication courte en francais",
          "suggestions_secondaires": ["ID2", "ID3"],
          "alerte": ""
        }
        """;

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(
            String prenomPatient,
            String preferenceHoraire,
            String urgenceDeclaree,
            List<Disponibilite> disponibilites,
            List<RendezVous> historiqueRdv
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Voici la demande d'un patient :\n\n");
        sb.append("Patient : ").append(prenomPatient).append("\n");
        sb.append("Preference horaire : ").append(preferenceHoraire).append("\n");
        sb.append("Urgence declaree : ").append(urgenceDeclaree).append("\n\n");

        sb.append("Historique rendez-vous patient :\n");
        if (historiqueRdv == null || historiqueRdv.isEmpty()) {
            sb.append("- Aucun historique disponible\n\n");
        } else {
            int limite = Math.min(5, historiqueRdv.size());
            for (int i = 0; i < limite; i++) {
                RendezVous rdv = historiqueRdv.get(i);
                sb.append("- ")
                        .append(rdv.getDateHeure())
                        .append(" | ")
                        .append(rdv.getStatut())
                        .append("\n");
            }
            sb.append("\n");
        }

        sb.append("Creneaux disponibles :\n");
        for (Disponibilite d : disponibilites) {
            sb.append("- ID: ").append(d.getId())
                    .append(" | Date: ").append(d.getDate())
                    .append(" | Heure: ").append(d.getHeureDebut())
                    .append(" -> ").append(d.getHeureFin())
                    .append(" | Medecin: ")
                    .append(d.getMedecin() != null ? d.getMedecin().getFullName() : "Inconnu")
                    .append("\n");
        }

        sb.append("\nAnalyse ces creneaux et recommande le meilleur selon les preferences du patient.");
        sb.append("\nReponds uniquement en JSON valide selon le format demande.");
        return sb.toString();
    }

    public AIRecommendation recommanderCreneau(
            String prenomPatient,
            String preferenceHoraire,
            String urgenceDeclaree,
            List<Disponibilite> disponibilites,
            List<RendezVous> historiqueRdv
    ) {
        System.out.println("PlanningAIService appelé");
        try {
            String userPrompt = buildUserPrompt(
                    prenomPatient, preferenceHoraire, urgenceDeclaree, disponibilites, historiqueRdv
            );

            MediLinkClaudeClient claude = new MediLinkClaudeClient();
            String reponseJson = claude.ask(getSystemPrompt(), userPrompt);

            if (reponseJson == null || reponseJson.isBlank()) {
                return erreur("L'IA n'a pas renvoye de reponse. Reessaie dans un moment.");
            }

            System.out.println("=== REPONSE BRUTE CLAUDE ===");
            System.out.println(reponseJson);
            System.out.println("=== FIN REPONSE ===");

            Gson gson = new Gson();
            com.google.gson.JsonObject obj = gson.fromJson(reponseJson, com.google.gson.JsonObject.class);
            System.out.println("Parsing JSON OK");

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
                return erreur("La reponse de l'IA est incomplete. Reessaie.");
            }
            return recommendation;
        } catch (JsonSyntaxException e) {
            System.out.println("Parsing JSON KO");
            return erreur("L'IA a renvoye une reponse dans un format inattendu.");
        } catch (Exception e) {
            System.out.println("Parsing JSON KO");
            return erreur("Impossible de contacter l'IA : " + e.getMessage());
        }
    }

    private AIRecommendation erreur(String message) {
        AIRecommendation vide = new AIRecommendation();
        vide.setAlerte(message);
        vide.setEchec(true);
        return vide;
    }
}
