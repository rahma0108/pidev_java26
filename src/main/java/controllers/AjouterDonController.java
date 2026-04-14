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

public class AjouterDonController implements Initializable {

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DonFormChoices.preparerComboCategorieAjout(cbCategorie);
        DonFormChoices.preparerComboString(cbUnite, DonFormChoices.UNITES, "Unités");
        DonFormChoices.preparerComboString(cbEtat, DonFormChoices.ETATS_DON, "Neuf / Non ouvert");
        DonFormChoices.preparerComboString(cbNiveauUrgence, DonFormChoices.NIVEAUX_URGENCE, "Moyen");

        btnEnregistrer.setOnAction(e -> enregistrer());
        btnAnnuler.setOnAction(e -> retourListe());
    }

    private void enregistrer() {
        try {
            DonFormChoices.CategorieOption cat = cbCategorie.getValue();
            if (cat == null || cat.id() <= 0) {
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

            Don d = new Don(categorieId, desc, quantite, unite, details, etat, urgence, "en_attente", expiration);
            donService.add(d);
            new Alert(Alert.AlertType.INFORMATION, "Don enregistré avec succès.").showAndWait();
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
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/ListeDons.fxml")));
            stage.setScene(new Scene(root, 900, 550));
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Impossible de revenir à la liste : " + ex.getMessage());
            a.showAndWait();
        }
    }
}
