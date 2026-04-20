package models;

/**
 * Résultat structuré de l’analyse Claude (décision modération + traduction).
 */
public record AnalyseDonIA(String decision, String raison, String traduction) {

    public static AnalyseDonIA vide() {
        return new AnalyseDonIA("", "", "");
    }
}
