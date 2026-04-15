package models;
import java.sql.Date;
public class Don {

        private int id;
        private int categorieId;
        private String articleDescription;
        private int quantite;
        private String unite;
        private String detailsSupplementaires;
        private String etat;
        private String niveauUrgence;
        private String statut;
        private Date dateExpiration;
        private Date dateSoumission;

        public Don() {}


        public Don(int categorieId, String articleDescription, int quantite, String unite,
                   String detailsSupplementaires, String etat, String niveauUrgence,
                   String statut, Date dateExpiration) {

            this.categorieId = categorieId;
            this.articleDescription = articleDescription;
            this.quantite = quantite;
            this.unite = unite;
            this.detailsSupplementaires = detailsSupplementaires;
            this.etat = etat;
            this.niveauUrgence = niveauUrgence;
            this.statut = statut;
            this.dateExpiration = dateExpiration;
        }



        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public int getCategorieId() { return categorieId; }
        public void setCategorieId(int categorieId) { this.categorieId = categorieId; }

        public String getArticleDescription() { return articleDescription; }
        public void setArticleDescription(String articleDescription) { this.articleDescription = articleDescription; }

        public int getQuantite() { return quantite; }
        public void setQuantite(int quantite) { this.quantite = quantite; }

        public String getUnite() { return unite; }
        public void setUnite(String unite) { this.unite = unite; }

        public String getDetailsSupplementaires() { return detailsSupplementaires; }
        public void setDetailsSupplementaires(String detailsSupplementaires) { this.detailsSupplementaires = detailsSupplementaires; }

        public String getEtat() { return etat; }
        public void setEtat(String etat) { this.etat = etat; }

        public String getNiveauUrgence() { return niveauUrgence; }
        public void setNiveauUrgence(String niveauUrgence) { this.niveauUrgence = niveauUrgence; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public Date getDateExpiration() { return dateExpiration; }
        public void setDateExpiration(Date dateExpiration) { this.dateExpiration = dateExpiration; }

        public Date getDateSoumission() { return dateSoumission; }
        public void setDateSoumission(Date dateSoumission) { this.dateSoumission = dateSoumission; }



}
