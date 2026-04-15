package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import models.Don;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Recherche, filtres combinés et tri multi-niveaux pour la liste publique et l'espace admin.
 */
public final class DonFiltreTriUtil {

    public static final String TRI_AUCUN = "— (aucun) —";
    public static final String ORDRE_CROISSANT = "Croissant";
    public static final String ORDRE_DECROISSANT = "Décroissant";
    public static final String FILTRE_TOUT = "Tous";
    public static final String FILTRE_TOUTES = "Toutes";

    public static final List<String> TRI_NIVEAUX = List.of(
            TRI_AUCUN,
            "ID",
            "Quantité",
            "Statut",
            "Urgence",
            "État",
            "Date de soumission",
            "Description"
    );

    private DonFiltreTriUtil() {
    }

    public static void remplirComboStatuts(ObservableList<Don> masterData, ComboBox<String> statutCombo) {
        String selection = statutCombo.getSelectionModel().getSelectedItem();
        Set<String> ordre = new LinkedHashSet<>();
        ordre.add(FILTRE_TOUT);
        ordre.addAll(masterData.stream()
                .map(Don::getStatut)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        statutCombo.setItems(FXCollections.observableArrayList(ordre));
        if (selection != null && ordre.contains(selection)) {
            statutCombo.getSelectionModel().select(selection);
        } else {
            statutCombo.getSelectionModel().selectFirst();
        }
    }

    public static void preparerCombosTri(ComboBox<String> tri1, ComboBox<String> tri2, ComboBox<String> tri3) {
        for (ComboBox<String> cb : List.of(tri1, tri2, tri3)) {
            cb.getItems().setAll(TRI_NIVEAUX);
        }
        tri1.getSelectionModel().select("Quantité");
        tri2.getSelectionModel().select(TRI_AUCUN);
        tri3.getSelectionModel().select(TRI_AUCUN);
    }

    public static void preparerCombosOrdre(ComboBox<String> o1, ComboBox<String> o2, ComboBox<String> o3) {
        for (ComboBox<String> cb : List.of(o1, o2, o3)) {
            cb.getItems().setAll(ORDRE_CROISSANT, ORDRE_DECROISSANT);
            cb.getSelectionModel().selectFirst();
        }
    }

    public static void remplirFiltresSecondaires(
            ObservableList<Don> masterData,
            ComboBox<String> filtreUrgenceCombo,
            ComboBox<String> filtreEtatCombo,
            ComboBox<String> filtreUniteCombo,
            ComboBox<DonFormChoices.CategorieOption> filtreCategorieCombo) {

        String selU = filtreUrgenceCombo.getSelectionModel().getSelectedItem();
        String selE = filtreEtatCombo.getSelectionModel().getSelectedItem();
        String selUn = filtreUniteCombo.getSelectionModel().getSelectedItem();
        DonFormChoices.CategorieOption selCat = filtreCategorieCombo.getValue();

        Set<String> urgences = new LinkedHashSet<>();
        urgences.add(FILTRE_TOUT);
        urgences.addAll(DonFormChoices.NIVEAUX_URGENCE);
        masterData.stream().map(Don::getNiveauUrgence).filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).forEach(urgences::add);
        filtreUrgenceCombo.setItems(FXCollections.observableArrayList(urgences));
        selectSiPossible(filtreUrgenceCombo, selU, FILTRE_TOUT);

        Set<String> etats = new LinkedHashSet<>();
        etats.add(FILTRE_TOUT);
        etats.addAll(DonFormChoices.ETATS_DON);
        masterData.stream().map(Don::getEtat).filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).forEach(etats::add);
        filtreEtatCombo.setItems(FXCollections.observableArrayList(etats));
        selectSiPossible(filtreEtatCombo, selE, FILTRE_TOUT);

        Set<String> unites = new LinkedHashSet<>();
        unites.add(FILTRE_TOUT);
        unites.addAll(DonFormChoices.UNITES);
        masterData.stream().map(Don::getUnite).filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty()).forEach(unites::add);
        filtreUniteCombo.setItems(FXCollections.observableArrayList(unites));
        selectSiPossible(filtreUniteCombo, selUn, FILTRE_TOUT);

        List<DonFormChoices.CategorieOption> cats = Stream.concat(
                Stream.of(new DonFormChoices.CategorieOption(-1, FILTRE_TOUTES)),
                DonFormChoices.categoriesSansPlaceholder().stream()
        ).collect(Collectors.toList());
        filtreCategorieCombo.setItems(FXCollections.observableArrayList(cats));
        if (selCat != null && cats.stream().anyMatch(c -> c.id() == selCat.id())) {
            filtreCategorieCombo.getSelectionModel().select(
                    cats.stream().filter(c -> c.id() == selCat.id()).findFirst().orElse(cats.get(0)));
        } else {
            filtreCategorieCombo.getSelectionModel().selectFirst();
        }
    }

    private static void selectSiPossible(ComboBox<String> cb, String previous, String defaut) {
        if (previous != null && cb.getItems().contains(previous)) {
            cb.getSelectionModel().select(previous);
        } else {
            cb.getSelectionModel().select(defaut);
        }
    }

    public static Integer parseIntOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Predicate<Don> creerPredicate(
            String q,
            String statutFiltre,
            String urgenceF,
            String etatF,
            String uniteF,
            DonFormChoices.CategorieOption catF,
            Integer qMin,
            Integer qMax) {
        return don -> {
            if (!matchStatut(don, statutFiltre)) {
                return false;
            }
            if (!matchCombo(don.getNiveauUrgence(), urgenceF)) {
                return false;
            }
            if (!matchCombo(don.getEtat(), etatF)) {
                return false;
            }
            if (!matchCombo(don.getUnite(), uniteF)) {
                return false;
            }
            if (catF != null && catF.id() > 0 && don.getCategorieId() != catF.id()) {
                return false;
            }
            if (qMin != null && don.getQuantite() < qMin) {
                return false;
            }
            if (qMax != null && don.getQuantite() > qMax) {
                return false;
            }
            return matchRechercheTexte(don, q);
        };
    }

    private static boolean matchStatut(Don don, String statutFiltre) {
        return statutFiltre == null
                || FILTRE_TOUT.equals(statutFiltre)
                || (don.getStatut() != null && don.getStatut().equals(statutFiltre));
    }

    private static boolean matchCombo(String valeurDon, String filtre) {
        if (filtre == null || FILTRE_TOUT.equals(filtre)) {
            return true;
        }
        return valeurDon != null && valeurDon.trim().equalsIgnoreCase(filtre.trim());
    }

    private static boolean matchRechercheTexte(Don don, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.trim().toLowerCase();
        if (needle.matches("\\d+")) {
            int idNeedle = Integer.parseInt(needle);
            if (don.getId() == idNeedle) {
                return true;
            }
        }
        return contientIgnoreCase(don.getArticleDescription(), needle)
                || contientIgnoreCase(don.getDetailsSupplementaires(), needle)
                || contientIgnoreCase(don.getUnite(), needle)
                || contientIgnoreCase(don.getEtat(), needle)
                || contientIgnoreCase(don.getNiveauUrgence(), needle)
                || contientIgnoreCase(don.getStatut(), needle)
                || contientIgnoreCase(String.valueOf(don.getCategorieId()), needle);
    }

    private static boolean contientIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    public static Comparator<Don> comparateurMultiNiveaux(
            String tri1Champ, String tri1Ordre,
            String tri2Champ, String tri2Ordre,
            String tri3Champ, String tri3Ordre) {
        Comparator<Don> result = null;
        result = ajouterNiveauTri(result, tri1Champ, tri1Ordre);
        result = ajouterNiveauTri(result, tri2Champ, tri2Ordre);
        result = ajouterNiveauTri(result, tri3Champ, tri3Ordre);
        if (result == null) {
            result = Comparator.comparingInt(Don::getId);
        } else {
            result = result.thenComparingInt(Don::getId);
        }
        return result;
    }

    private static Comparator<Don> ajouterNiveauTri(Comparator<Don> base, String champLabel, String ordreLabel) {
        if (champLabel == null || TRI_AUCUN.equals(champLabel)) {
            return base;
        }
        boolean asc = ORDRE_CROISSANT.equals(ordreLabel);
        Comparator<Don> next = comparateurPourLibelle(champLabel);
        if (!asc) {
            next = next.reversed();
        }
        return base == null ? next : base.thenComparing(next);
    }

    private static Comparator<Don> comparateurPourLibelle(String champ) {
        return switch (champ) {
            case "ID" -> Comparator.comparingInt(Don::getId);
            case "Quantité" -> Comparator.comparingInt(Don::getQuantite);
            case "Statut" -> Comparator
                    .comparingInt((Don d) -> ordreStatut(d.getStatut()))
                    .thenComparing(d -> Objects.toString(d.getStatut(), ""), String.CASE_INSENSITIVE_ORDER);
            case "Urgence" -> Comparator
                    .comparingInt((Don d) -> ordreUrgence(d.getNiveauUrgence()))
                    .thenComparing(d -> Objects.toString(d.getNiveauUrgence(), ""), String.CASE_INSENSITIVE_ORDER);
            case "État" -> Comparator.comparing(
                    d -> Objects.toString(d.getEtat(), ""),
                    String.CASE_INSENSITIVE_ORDER);
            case "Date de soumission" -> Comparator.comparing(
                    Don::getDateSoumission,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case "Description" -> Comparator.comparing(
                    d -> Objects.toString(d.getArticleDescription(), ""),
                    String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingInt(Don::getId);
        };
    }

    private static int ordreStatut(String s) {
        if (s == null) {
            return 9;
        }
        return switch (s) {
            case "en_attente" -> 0;
            case "valide" -> 1;
            case "rejete" -> 2;
            default -> 3;
        };
    }

    private static int ordreUrgence(String u) {
        if (u == null || u.isBlank()) {
            return 9;
        }
        String x = u.trim();
        if (x.equalsIgnoreCase("Faible")) {
            return 0;
        }
        if (x.equalsIgnoreCase("Moyen")) {
            return 1;
        }
        if (x.equalsIgnoreCase("Élevé") || x.equalsIgnoreCase("Eleve")) {
            return 2;
        }
        return 3;
    }
}
