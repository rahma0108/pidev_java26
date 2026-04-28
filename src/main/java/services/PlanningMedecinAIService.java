package services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.Disponibilite;
import models.PlanningAnalysis;
import models.RendezVous;

import java.util.ArrayList;
import java.util.List;

public class PlanningMedecinAIService {

    private static final String SYSTEM_PROMPT = """
        Tu es un assistant de planification medicale pour MediLink.
        Tu aides les medecins a organiser et prioriser leurs rendez-vous.

        Regles absolues :
        - Tu ne fais JAMAIS de diagnostic medical
        - Tu ne recommandes JAMAIS de traitement
        - Tu analyses uniquement la charge de travail et l'organisation
        - Tu reponds UNIQUEMENT en JSON valide, sans texte avant ni apres

        Format JSON obligatoire :
        {
          "resume_global": "resume court de la situation en francais",
          "niveau_charge": "normal|charge|surcharge",
          "rdv_prioritaires": [
            {
              "rdv_id": "ID",
              "priorite": "haute|normale|faible",
              "raison": "explication courte"
            }
          ],
          "alertes": ["alerte 1", "alerte 2"],
          "recommandations": ["recommandation 1", "recommandation 2"]
        }
        """;

    public PlanningAnalysis analyserPlanning(
            String nomMedecin,
            List<RendezVous> rendezVous,
            List<Disponibilite> disponibilites
    ) {
        if (rendezVous == null || rendezVous.isEmpty()) {
            return erreur("Aucun rendez-vous a analyser.");
        }

        try {
            String userPrompt = buildUserPrompt(nomMedecin, rendezVous, disponibilites);
            MediLinkClaudeClient client = new MediLinkClaudeClient();
            String reponseJson = client.ask(SYSTEM_PROMPT, userPrompt);

            if (reponseJson == null || reponseJson.isBlank()) {
                return erreur("L'IA n'a pas repondu.");
            }

            return parserReponse(reponseJson);
        } catch (Exception e) {
            return erreur("Erreur : " + e.getMessage());
        }
    }

    private String buildUserPrompt(
            String nomMedecin,
            List<RendezVous> rendezVous,
            List<Disponibilite> disponibilites
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Medecin : ").append(nomMedecin).append("\n\n");

        sb.append("Rendez-vous en attente :\n");
        int enAttente = 0;
        for (RendezVous rdv : rendezVous) {
            if (RendezVous.EN_ATTENTE.equalsIgnoreCase(rdv.getStatut())) {
                enAttente++;
                sb.append("- ID: ").append(rdv.getId())
                        .append(" | Date: ").append(rdv.getDateHeure())
                        .append(" | Statut: ").append(rdv.getStatut());
                if (rdv.getMotif() != null && !rdv.getMotif().isBlank()) {
                    sb.append(" | Motif: ").append(rdv.getMotif());
                }
                sb.append("\n");
            }
        }
        sb.append("Total en attente : ").append(enAttente).append("\n\n");

        sb.append("Tous les rendez-vous (").append(rendezVous.size()).append(") :\n");
        for (RendezVous rdv : rendezVous) {
            sb.append("- ID: ").append(rdv.getId())
                    .append(" | ").append(rdv.getDateHeure())
                    .append(" | ").append(rdv.getStatut()).append("\n");
        }

        sb.append("\nDisponibilites creees : ")
                .append(disponibilites != null ? disponibilites.size() : 0).append("\n");

        sb.append("\nAnalyse la charge de travail, priorise les rendez-vous en attente,");
        sb.append(" identifie les alertes et donne des recommandations d'organisation.");
        sb.append("\nReponds uniquement en JSON valide.");

        return sb.toString();
    }

    private PlanningAnalysis parserReponse(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            PlanningAnalysis result = new PlanningAnalysis();

            if (obj.has("resume_global")) {
                result.setResumeGlobal(obj.get("resume_global").getAsString());
            }
            if (obj.has("niveau_charge")) {
                result.setNiveauCharge(obj.get("niveau_charge").getAsString());
            }

            if (obj.has("rdv_prioritaires")) {
                List<PlanningAnalysis.RdvPrioritaire> liste = new ArrayList<>();
                JsonArray arr = obj.getAsJsonArray("rdv_prioritaires");
                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    PlanningAnalysis.RdvPrioritaire r = new PlanningAnalysis.RdvPrioritaire();
                    r.setRdvId(o.has("rdv_id") ? o.get("rdv_id").getAsString() : "");
                    r.setPriorite(o.has("priorite") ? o.get("priorite").getAsString() : "normale");
                    r.setRaison(o.has("raison") ? o.get("raison").getAsString() : "");
                    liste.add(r);
                }
                result.setRdvPrioritaires(liste);
            }

            if (obj.has("alertes")) {
                List<String> alertes = new ArrayList<>();
                for (JsonElement el : obj.getAsJsonArray("alertes")) {
                    alertes.add(el.getAsString());
                }
                result.setAlertes(alertes);
            }

            if (obj.has("recommandations")) {
                List<String> recommandations = new ArrayList<>();
                for (JsonElement el : obj.getAsJsonArray("recommandations")) {
                    recommandations.add(el.getAsString());
                }
                result.setRecommandations(recommandations);
            }

            return result;
        } catch (Exception e) {
            return erreur("JSON invalide : " + e.getMessage());
        }
    }

    private PlanningAnalysis erreur(String message) {
        PlanningAnalysis p = new PlanningAnalysis();
        p.setAlerte(message);
        p.setEchec(true);
        return p;
    }
}
