package controllers;

import java.time.LocalDate;

/**
 * Contrôles de saisie communs (ajout / modification).
 */
public final class DonSaisieValidator {

    public static final int DESCRIPTION_MAX = 255;
    public static final int DETAILS_MAX = 4000;

    private DonSaisieValidator() {
    }

    /**
     * @return message d'erreur affichable, ou {@code null} si tout est valide
     */
    public static String validerFormulaireDon(
            int categorieId,
            String description,
            int quantite,
            String unite,
            String etat,
            String niveauUrgence,
            String detailsSupplementaires,
            LocalDate dateExpiration) {

        if (categorieId <= 0) {
            return "La catégorie est obligatoire.";
        }
        if (description == null || description.isBlank()) {
            return "La description de l'article est obligatoire.";
        }
        if (description.length() > DESCRIPTION_MAX) {
            return "La description ne peut pas dépasser " + DESCRIPTION_MAX + " caractères.";
        }
        if (quantite <= 0) {
            return "La quantité doit être un nombre strictement positif.";
        }
        if (unite == null || unite.isBlank()) {
            return "L'unité de mesure est obligatoire.";
        }
        if (etat == null || etat.isBlank()) {
            return "L'état du don est obligatoire.";
        }
        if (niveauUrgence == null || niveauUrgence.isBlank()) {
            return "Le niveau d'urgence est obligatoire.";
        }
        if (detailsSupplementaires != null && detailsSupplementaires.length() > DETAILS_MAX) {
            return "Les détails ne peuvent pas dépasser " + DETAILS_MAX + " caractères.";
        }
        if (dateExpiration != null) {
            LocalDate today = LocalDate.now();
            if (!dateExpiration.isAfter(today)) {
                return "La date d'expiration doit être postérieure à aujourd'hui, ou laissez le champ vide.";
            }
        }
        return null;
    }
}
