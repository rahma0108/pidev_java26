package models;

import java.util.List;

public class AIRecommendation {

    // Champs JSON (noms identiques au JSON retourné par Claude)
    String creneau_recommande_id;
    double niveau_confiance;
    String justification;
    List<String> suggestions_secondaires;
    String alerte;

    // Champ interne Java uniquement (pas dans le JSON)
    boolean echec = false;

    // Constructeur vide requis par Gson pour le parsing
    public AIRecommendation() {}

    // ── Getters ───────────────────────────────────────────────────────────

    public String getCreneauRecommandeId() {
        return creneau_recommande_id;
    }

    public double getNiveauConfiance() {
        return niveau_confiance;
    }

    public String getJustification() {
        return justification;
    }

    public List<String> getSuggestionsSecondaires() {
        return suggestions_secondaires;
    }

    public String getAlerte() {
        return alerte;
    }

    public boolean isEchec() {
        return echec;
    }

    // ── Setters ───────────────────────────────────────────────────────────

    public void setAlerte(String alerte) {
        this.alerte = alerte;
    }

    public void setEchec(boolean echec) {
        this.echec = echec;
    }
    public void setCreneauRecommandeId(String id) {
        this.creneau_recommande_id = id;
    }

    public void setNiveauConfiance(double confiance) {
        this.niveau_confiance = confiance;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}