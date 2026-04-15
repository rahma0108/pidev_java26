package controllers;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import models.CategorieDons;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Valeurs affichées dans les formulaires (catégorie, unité, état, urgence).
 * Les catégories sont lues depuis la table {@code categories_dons} si possible.
 */
public final class DonFormChoices {

    private DonFormChoices() {
    }

    /** id &lt;= 0 = entrée « non choisi » pour l’écran d’ajout. */
    public record CategorieOption(int id, String libelle) {
        @Override
        public String toString() {
            return libelle;
        }
    }

    public static List<CategorieOption> categoriesAvecPlaceholder() {
        List<CategorieOption> l = new ArrayList<>();
        l.add(new CategorieOption(0, "Sélectionnez une catégorie"));
        l.addAll(categoriesDepuisBaseOuDefaut());
        return l;
    }

    public static List<CategorieOption> categoriesSansPlaceholder() {
        List<CategorieOption> all = categoriesAvecPlaceholder();
        return new ArrayList<>(all.subList(1, all.size()));
    }

    private static List<CategorieOption> categoriesDepuisBaseOuDefaut() {
        List<CategorieOption> categories = new ArrayList<>();
        Connection conn = MyConnection.getInstance().getConn();
        if (conn != null) {
            String req = "SELECT id, nom, description, icone, couleur, created_at FROM categories_dons ORDER BY id";
            try (PreparedStatement ps = conn.prepareStatement(req);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CategorieDons row = CategorieDons.fromResultSet(rs);
                    categories.add(new CategorieOption(row.getId(), row.getNom()));
                }
            } catch (Exception ignored) {
                // fallback ci-dessous
            }
        }
        if (!categories.isEmpty()) {
            return categories;
        }
        // Valeurs de secours si la table est vide/inaccessible.
        categories.add(new CategorieOption(1, "Médicaments"));
        categories.add(new CategorieOption(2, "Matériel médical"));
        categories.add(new CategorieOption(3, "Mobilité et équipement"));
        categories.add(new CategorieOption(4, "Masques et consommables"));
        categories.add(new CategorieOption(5, "Autre"));
        return categories;
    }

    public static final List<String> UNITES = List.of(
            "Boîtes", "Unités", "Pièces", "Flacons", "Seringues", "Autre");

    public static final List<String> ETATS_DON = List.of(
            "Neuf / Non ouvert", "Bon état", "État moyen", "À vérifier");

    public static final List<String> NIVEAUX_URGENCE = List.of(
            "Faible", "Moyen", "Élevé");

    public static void preparerComboString(ComboBox<String> cb, List<String> items, String valeurParDefaut) {
        cb.setItems(FXCollections.observableArrayList(items));
        cb.setEditable(false);
        if (valeurParDefaut != null && items.contains(valeurParDefaut)) {
            cb.setValue(valeurParDefaut);
        } else if (!items.isEmpty()) {
            cb.getSelectionModel().selectFirst();
        }
    }

    public static void preparerComboCategorieAjout(ComboBox<CategorieOption> cb) {
        cb.setItems(FXCollections.observableArrayList(categoriesAvecPlaceholder()));
        cb.setEditable(false);
        cb.getSelectionModel().selectFirst();
    }

    public static void preparerComboCategorieModification(ComboBox<CategorieOption> cb) {
        cb.setItems(FXCollections.observableArrayList(categoriesSansPlaceholder()));
        cb.setEditable(false);
    }

    /** Sélectionne l’id ; si inconnu, garde la première catégorie réelle. */
    public static void selectionnerCategorie(ComboBox<CategorieOption> cb, int categorieId) {
        for (CategorieOption c : cb.getItems()) {
            if (c.id() == categorieId) {
                cb.setValue(c);
                return;
            }
        }
        if (!cb.getItems().isEmpty()) {
            cb.getSelectionModel().selectFirst();
        }
    }

    /**
     * Sélectionne la valeur enregistrée ; si elle n’est pas dans la liste (données anciennes),
     * elle est ajoutée pour rester modifiable.
     */
    public static void selectionnerTexteListe(ComboBox<String> cb, String valeurEnBase) {
        String v = valeurEnBase == null ? "" : valeurEnBase.trim();
        if (v.isEmpty()) {
            cb.getSelectionModel().selectFirst();
            return;
        }
        if (cb.getItems().stream().noneMatch(x -> x.equals(v))) {
            cb.getItems().add(v);
        }
        cb.setValue(v);
    }
}
