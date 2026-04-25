package models;

public class AINotification {

    // Champs JSON (noms identiques au JSON retourne par l'IA)
    String type_notification;   // "rappel", "urgent", "replanification"
    String moment_envoi;        // "immediat", "24h_avant", "48h_avant", "72h_avant"
    String message;             // texte personnalise pour le patient
    String action_recommandee;  // "confirmer", "replanifier", "annuler", "aucune"
    String niveau_priorite;     // "normal", "eleve", "critique"

    // Champ interne Java uniquement (pas dans le JSON)
    String alerte;
    boolean echec = false;

    // Constructeur vide requis
    public AINotification() {
    }

    // Getters
    public String getTypeNotification() {
        return type_notification;
    }

    public String getMomentEnvoi() {
        return moment_envoi;
    }

    public String getMessage() {
        return message;
    }

    public String getActionRecommandee() {
        return action_recommandee;
    }

    public String getNiveauPriorite() {
        return niveau_priorite;
    }

    public String getAlerte() {
        return alerte;
    }

    public boolean isEchec() {
        return echec;
    }

    // Setters
    public void setTypeNotification(String v) {
        this.type_notification = v;
    }

    public void setMomentEnvoi(String v) {
        this.moment_envoi = v;
    }

    public void setMessage(String v) {
        this.message = v;
    }

    public void setActionRecommandee(String v) {
        this.action_recommandee = v;
    }

    public void setNiveauPriorite(String v) {
        this.niveau_priorite = v;
    }

    public void setAlerte(String v) {
        this.alerte = v;
    }

    public void setEchec(boolean v) {
        this.echec = v;
    }

    // Validation des valeurs IA (defaults surs)
    public String getTypeNotificationSafe() {
        if (type_notification == null) {
            return "rappel";
        }
        return switch (type_notification) {
            case "urgent", "replanification" -> type_notification;
            default -> "rappel";
        };
    }

    public String getNiveauPrioriteSafe() {
        if (niveau_priorite == null) {
            return "normal";
        }
        return switch (niveau_priorite) {
            case "eleve", "critique" -> niveau_priorite;
            default -> "normal";
        };
    }

    public String getMomentEnvoiSafe() {
        if (moment_envoi == null) {
            return "24h_avant";
        }
        return switch (moment_envoi) {
            case "immediat", "48h_avant", "72h_avant" -> moment_envoi;
            default -> "24h_avant";
        };
    }

    public String getActionRecommandeeSafe() {
        if (action_recommandee == null) {
            return "aucune";
        }
        return switch (action_recommandee) {
            case "confirmer", "replanifier", "annuler" -> action_recommandee;
            default -> "aucune";
        };
    }

    @Override
    public String toString() {
        return "AINotification{"
                + "type=" + getTypeNotificationSafe()
                + ", moment=" + getMomentEnvoiSafe()
                + ", priorite=" + getNiveauPrioriteSafe()
                + ", action=" + getActionRecommandeeSafe()
                + ", message=" + message
                + "}";
    }
}
