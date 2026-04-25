package services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.AINotification;
import models.RendezVous;
import models.User;

import java.util.List;

public class NotificationAIService {

    // Prompt systeme (fixe)
    private static final String SYSTEM_PROMPT = """
        Tu es un assistant de gestion de notifications medicales pour MediLink.
        Ton role est de decider quand et comment notifier un patient
        concernant ses rendez-vous medicaux.

        Regles absolues :
        - Tu ne fais JAMAIS de diagnostic medical
        - Tu ne recommandes JAMAIS de traitement
        - Tu ne prends JAMAIS de decision finale (tu proposes seulement)
        - Tu bases tes decisions uniquement sur : statut du rendez-vous,
          historique du patient, proximite temporelle, comportement passe
        - Tu reponds UNIQUEMENT en JSON valide, sans texte avant ni apres

        Format JSON obligatoire :
        {
          "type_notification": "rappel|urgent|replanification",
          "moment_envoi": "immediat|24h_avant|48h_avant|72h_avant",
          "message": "message personnalise pour le patient en francais",
          "action_recommandee": "confirmer|replanifier|annuler|aucune",
          "niveau_priorite": "normal|eleve|critique"
        }
        """;

    public AINotification genererNotification(
            User patient,
            RendezVous rendezVous,
            List<RendezVous> historique
    ) {
        if (patient == null) {
            return erreur("Patient manquant.");
        }
        if (rendezVous == null) {
            return erreur("Rendez-vous manquant.");
        }

        try {
            String userPrompt = buildUserPrompt(patient, rendezVous, historique);

            MediLinkClaudeClient client = new MediLinkClaudeClient();
            String reponseJson = client.ask(SYSTEM_PROMPT, userPrompt);

            if (reponseJson == null || reponseJson.isBlank()) {
                return erreur("L'IA n'a pas renvoye de reponse.");
            }

            System.out.println("=== NOTIFICATION IA ===");
            System.out.println(reponseJson);
            System.out.println("=== FIN ===");

            return parserReponse(reponseJson);
        } catch (Exception e) {
            return erreur("Erreur inattendue : " + e.getMessage());
        }
    }

    private String buildUserPrompt(
            User patient,
            RendezVous rendezVous,
            List<RendezVous> historique
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("Patient : ")
                .append(patient.getFullName() != null ? patient.getFullName() : "Inconnu")
                .append("\n");

        if (patient.getPreferredTime() != null) {
            String pref = patient.getPreferredTime().getHour() < 12 ? "matin" : "apres-midi";
            sb.append("Preference horaire : ").append(pref).append("\n");
        }

        sb.append("\nRendez-vous concerne :\n");
        sb.append("- Statut : ").append(rendezVous.getStatut()).append("\n");
        sb.append("- Date   : ").append(rendezVous.getDateHeure()).append("\n");

        if (rendezVous.getDisponibilite() != null
                && rendezVous.getDisponibilite().getMedecin() != null) {
            sb.append("- Medecin : ")
                    .append(rendezVous.getDisponibilite().getMedecin().getFullName())
                    .append("\n");
        }

        sb.append("\nHistorique des rendez-vous :\n");
        if (historique == null || historique.isEmpty()) {
            sb.append("- Aucun historique disponible\n");
        } else {
            int annulations = 0;
            List<RendezVous> recent = historique.stream().limit(5).toList();
            for (RendezVous rdv : recent) {
                sb.append("- ").append(rdv.getDateHeure())
                        .append(" | ").append(rdv.getStatut()).append("\n");
                if (RendezVous.ANNULE.equalsIgnoreCase(rdv.getStatut())) {
                    annulations++;
                }
            }
            sb.append("Annulations passees : ").append(annulations).append("\n");
        }

        sb.append("\nDecide du type de notification adapte a ce contexte.");
        sb.append("\nReponds uniquement en JSON valide selon le format demande.");

        return sb.toString();
    }

    private AINotification parserReponse(String reponseJson) {
        try {
            JsonObject obj = JsonParser.parseString(reponseJson).getAsJsonObject();

            AINotification notif = new AINotification();
            notif.setTypeNotification(
                    obj.has("type_notification")
                            ? obj.get("type_notification").getAsString() : "rappel"
            );
            notif.setMomentEnvoi(
                    obj.has("moment_envoi")
                            ? obj.get("moment_envoi").getAsString() : "24h_avant"
            );
            notif.setMessage(
                    obj.has("message")
                            ? obj.get("message").getAsString() : ""
            );
            notif.setActionRecommandee(
                    obj.has("action_recommandee")
                            ? obj.get("action_recommandee").getAsString() : "aucune"
            );
            notif.setNiveauPriorite(
                    obj.has("niveau_priorite")
                            ? obj.get("niveau_priorite").getAsString() : "normal"
            );

            return notif;
        } catch (Exception e) {
            return erreur("JSON invalide : " + e.getMessage());
        }
    }

    private AINotification erreur(String message) {
        AINotification n = new AINotification();
        n.setAlerte(message);
        n.setEchec(true);
        return n;
    }
}
