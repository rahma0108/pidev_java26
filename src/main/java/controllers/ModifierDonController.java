package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Don;
import services.DonService;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Objects;
import java.util.ResourceBundle;

public class ModifierDonController implements Initializable {

    /** FXML chargé après Enregistrer / Annuler (liste publique ou admin). */
    private String retourFxml = "/ListeDons.fxml";

    @FXML
    private ComboBox<DonFormChoices.CategorieOption> cbCategorie;

    @FXML
    private TextField tfDescription;

    @FXML
    private TextField tfQuantite;

    @FXML
    private ComboBox<String> cbUnite;

    @FXML
    private TextArea taDetails;

    @FXML
    private ComboBox<String> cbEtat;

    @FXML
    private ComboBox<String> cbNiveauUrgence;

    @FXML
    private DatePicker dpExpiration;

    @FXML
    private Button btnEnregistrer;

    @FXML
    private Button btnAnnuler;

    private final DonService donService = new DonService();
    private Don don;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DonFormChoices.preparerComboCategorieModification(cbCategorie);
        DonFormChoices.preparerComboString(cbUnite, DonFormChoices.UNITES, "Unités");
        DonFormChoices.preparerComboString(cbEtat, DonFormChoices.ETATS_DON, "Neuf / Non ouvert");
        DonFormChoices.preparerComboString(cbNiveauUrgence, DonFormChoices.NIVEAUX_URGENCE, "Moyen");

        btnEnregistrer.setOnAction(e -> enregistrer());
        btnAnnuler.setOnAction(e -> retourListe());
    }

    /**
     * Définit l'écran de retour (ex. {@code "/AdminDons.fxml"} ou {@code "/ListeDons.fxml"}).
     */
    public void setRetourFxml(String resourcePath) {
        if (resourcePath != null && !resourcePath.isBlank()) {
            this.retourFxml = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        }
    }

    /**
     * Appelé après le chargement du FXML pour préremplir le formulaire.
     */
    public void setDon(Don don) {
        this.don = don;
        if (don == null) {
            return;
        }
        DonFormChoices.selectionnerCategorie(cbCategorie, don.getCategorieId());
        tfDescription.setText(don.getArticleDescription());
        tfQuantite.setText(String.valueOf(don.getQuantite()));
        DonFormChoices.selectionnerTexteListe(cbUnite, don.getUnite());
        taDetails.setText(videSiNull(don.getDetailsSupplementaires()));
        DonFormChoices.selectionnerTexteListe(cbEtat, don.getEtat());
        DonFormChoices.selectionnerTexteListe(cbNiveauUrgence, don.getNiveauUrgence());
        if (don.getDateExpiration() != null) {
            dpExpiration.setValue(don.getDateExpiration().toLocalDate());
        } else {
            dpExpiration.setValue(null);
        }
    }

    private static String videSiNull(String s) {
        return s == null ? "" : s;
    }

    /**
     * Depuis « Mes dons », un don validé repasse en attente après modification pour être revu par l’admin.
     */
    private String statutApresEnregistrement() {
        if (don == null) {
            return "en_attente";
        }
        if (!"/MesDons.fxml".equals(retourFxml)) {
            return don.getStatut();
        }
        String s = don.getStatut() == null ? "" : don.getStatut().trim();
        if ("valide".equalsIgnoreCase(s)) {
            return "en_attente";
        }
        return don.getStatut() != null ? don.getStatut() : "en_attente";
    }

    private String messageSuccesEnregistrement() {
        if (don != null && "/MesDons.fxml".equals(retourFxml)
                && don.getStatut() != null && "valide".equalsIgnoreCase(don.getStatut().trim())) {
            return "Don mis à jour. Il repasse en « en attente » pour revalidation par l’administrateur.";
        }
        return "Don mis à jour avec succès.";
    }

    private void enregistrer() {
        if (don == null) {
            new Alert(Alert.AlertType.ERROR, "Aucun don chargé.").showAndWait();
            return;
        }
        try {
            DonFormChoices.CategorieOption cat = cbCategorie.getValue();
            if (cat == null) {
                new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une catégorie de don.").showAndWait();
                return;
            }
            int categorieId = cat.id();
            int quantite = Integer.parseInt(tfQuantite.getText().trim());
            String desc = tfDescription.getText() != null ? tfDescription.getText().trim() : "";
            String unite = valeurCombo(cbUnite);
            String details = taDetails.getText() != null ? taDetails.getText().trim() : "";
            String etat = valeurCombo(cbEtat);
            String urgence = valeurCombo(cbNiveauUrgence);
            var dateExp = dpExpiration.getValue();

            String erreur = DonSaisieValidator.validerFormulaireDon(
                    categorieId, desc, quantite, unite, etat, urgence, details, dateExp);
            if (erreur != null) {
                new Alert(Alert.AlertType.WARNING, erreur).showAndWait();
                return;
            }

            Date expiration = dateExp == null ? null : Date.valueOf(dateExp);

            Don misAJour = new Don();
            misAJour.setId(don.getId());
            misAJour.setCategorieId(categorieId);
            misAJour.setArticleDescription(desc);
            misAJour.setQuantite(quantite);
            misAJour.setUnite(unite);
            misAJour.setDetailsSupplementaires(details);
            misAJour.setEtat(etat);
            misAJour.setNiveauUrgence(urgence);
            misAJour.setStatut(statutApresEnregistrement());
            misAJour.setDateExpiration(expiration);
            misAJour.setDateSoumission(don.getDateSoumission());

            donService.update(misAJour);
            new Alert(Alert.AlertType.INFORMATION, messageSuccesEnregistrement()).showAndWait();
            retourListe();
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "La quantité doit être un nombre entier valide.").showAndWait();
        } catch (SQLException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setContentText(ex.getMessage());
            a.showAndWait();
        }
    }

    private static String valeurCombo(ComboBox<String> cb) {
        String v = cb.getValue();
        return v != null ? v.trim() : "";
    }

    private void retourListe() {
        try {
            Stage stage = (Stage) btnAnnuler.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(retourFxml)));
            int w = "/GestionDonsAdmin.fxml".equals(retourFxml) ? 1180 : 960;
            int h = "/GestionDonsAdmin.fxml".equals(retourFxml) ? 780 : 680;
            stage.setScene(new Scene(root, w, h));
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Impossible de revenir : " + ex.getMessage());
            a.showAndWait();
        }
    }
}
