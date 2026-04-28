package models;

import java.util.ArrayList;
import java.util.List;

public class PlanningAnalysis {

    private String resume_global;
    private String niveau_charge;
    private List<RdvPrioritaire> rdv_prioritaires = new ArrayList<>();
    private List<String> alertes = new ArrayList<>();
    private List<String> recommandations = new ArrayList<>();
    private String alerte;
    private boolean echec = false;

    public PlanningAnalysis() {}

    public static class RdvPrioritaire {
        private String rdv_id;
        private String priorite;
        private String raison;

        public String getRdvId() {
            return rdv_id;
        }

        public void setRdvId(String rdvId) {
            this.rdv_id = rdvId;
        }

        public String getPriorite() {
            return priorite;
        }

        public void setPriorite(String priorite) {
            this.priorite = priorite;
        }

        public String getRaison() {
            return raison;
        }

        public void setRaison(String raison) {
            this.raison = raison;
        }
    }

    public String getResumeGlobal() {
        return resume_global;
    }

    public void setResumeGlobal(String resumeGlobal) {
        this.resume_global = resumeGlobal;
    }

    public String getNiveauCharge() {
        return niveau_charge;
    }

    public void setNiveauCharge(String niveauCharge) {
        this.niveau_charge = niveauCharge;
    }

    public List<RdvPrioritaire> getRdvPrioritaires() {
        return rdv_prioritaires;
    }

    public void setRdvPrioritaires(List<RdvPrioritaire> rdvPrioritaires) {
        this.rdv_prioritaires = rdvPrioritaires != null ? rdvPrioritaires : new ArrayList<>();
    }

    public List<String> getAlertes() {
        return alertes;
    }

    public void setAlertes(List<String> alertes) {
        this.alertes = alertes != null ? alertes : new ArrayList<>();
    }

    public List<String> getRecommandations() {
        return recommandations;
    }

    public void setRecommandations(List<String> recommandations) {
        this.recommandations = recommandations != null ? recommandations : new ArrayList<>();
    }

    public String getAlerte() {
        return alerte;
    }

    public void setAlerte(String alerte) {
        this.alerte = alerte;
    }

    public boolean isEchec() {
        return echec;
    }

    public void setEchec(boolean echec) {
        this.echec = echec;
    }
}
