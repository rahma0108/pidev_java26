<<<<<<< HEAD
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
=======
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

import javafx.scene.control.Label;

import javafx.scene.control.ListCell;

import javafx.scene.control.TextArea;

import javafx.scene.control.TextField;

import javafx.application.Platform;

import javafx.scene.layout.VBox;

import javafx.scene.paint.Color;

import javafx.scene.canvas.Canvas;

import javafx.scene.layout.StackPane;

import javafx.stage.Stage;

import models.Don;

import ui.ParticleBackground;

import services.DonIAService;
import services.DonService;

import utils.MediLinkDialogs;



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



    @FXML

    private StackPane rootStack;



    @FXML

    private Canvas particleCanvas;



    @FXML

    private VBox formRoot;



    @FXML

    private Label lblTitre;



    @FXML

    private Label lblCat;



    @FXML

    private Label lblDesc;



    @FXML

    private Label lblQty;



    @FXML

    private Label lblUnite;



    @FXML

    private Label lblDetails;



    @FXML

    private Label lblEtat;



    @FXML

    private Label lblUrg;



    @FXML

    private Label lblExp;



    private ParticleBackground particules;



    private final DonService donService = new DonService();

    /** Initialisé à la demande (nécessite anthropic.api.key ou ANTHROPIC_API_KEY). */
    private DonIAService donIAService;



    @Override

    public void initialize(URL location, ResourceBundle resources) {

        particules = new ParticleBackground(particleCanvas, rootStack, 96);

        particules.play();

        DonFormChoices.preparerComboCategorieAjout(cbCategorie);

        DonFormChoices.preparerComboString(cbUnite, DonFormChoices.UNITES, "Unités");

        DonFormChoices.preparerComboString(cbEtat, DonFormChoices.ETATS_DON, "Neuf / Non ouvert");

        DonFormChoices.preparerComboString(cbNiveauUrgence, DonFormChoices.NIVEAUX_URGENCE, "Moyen");

        appliquerStylesFormulaire();

        btnEnregistrer.setOnAction(e -> enregistrer());

        btnAnnuler.setOnAction(e -> retourListe());

    }



    private static final Color TEXTE_COMBO = Color.web("#e2e8f0");

    private static final Color TEXTE_INVITE_COMBO = Color.web("#94a3b8");

    /** Apparence sombre lisible sur le fond particules — sans feuille CSS externe. */
    private void appliquerStylesFormulaire() {
        rootStack.setStyle("-fx-background-color: transparent;");
        formRoot.setStyle(
                "-fx-background-color: rgba(15,23,42,0.72);"
                        + "-fx-background-radius: 16;"
                        + "-fx-border-color: rgba(148,163,184,0.22);"
                        + "-fx-border-radius: 16;"
                        + "-fx-border-width: 1;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 24, 0, 0, 4);");
        lblTitre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        String lblMuted = "-fx-text-fill: #cbd5e1;";
        lblCat.setStyle(lblMuted);
        lblDesc.setStyle(lblMuted);
        lblQty.setStyle(lblMuted);
        lblUnite.setStyle(lblMuted);
        lblDetails.setStyle(lblMuted);
        lblEtat.setStyle(lblMuted);
        lblUrg.setStyle(lblMuted);
        lblExp.setStyle(lblMuted);
        String champ = "-fx-background-color: rgba(30,41,59,0.85);"
                + "-fx-control-inner-background: rgba(30,41,59,0.92);"
                + "-fx-border-color: rgba(148,163,184,0.35);"
                + "-fx-border-radius: 10; -fx-background-radius: 10;"
                + "-fx-text-fill: #f1f5f9; -fx-prompt-text-fill: #94a3b8; -fx-padding: 8 12;";
        tfDescription.setStyle(champ);
        tfQuantite.setStyle(champ);
        taDetails.setStyle(champ);
        String combo = "-fx-background-color: rgba(30,41,59,0.92);"
                + "-fx-border-color: rgba(148,163,184,0.35);"
                + "-fx-border-radius: 10; -fx-background-radius: 10;"
                + "-fx-text-fill: #e2e8f0;";
        cbCategorie.setStyle(combo);
        cbUnite.setStyle(combo);
        cbEtat.setStyle(combo);
        cbNiveauUrgence.setStyle(combo);
        stylerComboCategorie(cbCategorie);
        stylerComboString(cbUnite);
        stylerComboString(cbEtat);
        stylerComboString(cbNiveauUrgence);
        btnEnregistrer.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 999;"
                        + "-fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
        btnAnnuler.setStyle(
                "-fx-background-color: rgba(51,65,85,0.9); -fx-text-fill: #e2e8f0; -fx-background-radius: 999;"
                        + "-fx-padding: 10 18; -fx-font-weight: bold;"
                        + "-fx-border-color: rgba(148,163,184,0.35); -fx-border-radius: 999; -fx-cursor: hand;");
        Platform.runLater(() -> {
            javafx.scene.Node content = taDetails.lookup(".content");
            if (content != null) {
                content.setStyle("-fx-background-color: rgba(30,41,59,0.92);");
            }
            dpExpiration.setStyle("-fx-background-color: transparent;");
            dpExpiration.getEditor().setStyle(champ);
        });
    }

    private static void stylerComboString(ComboBox<String> cb) {
        cb.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setTextFill(TEXTE_COMBO);
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setTextFill(TEXTE_COMBO);
            }
        });
    }

    private static void stylerComboCategorie(ComboBox<DonFormChoices.CategorieOption> cb) {
        cb.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DonFormChoices.CategorieOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.libelle());
                }
                if (item != null && item.id() <= 0) {
                    setTextFill(TEXTE_INVITE_COMBO);
                } else {
                    setTextFill(TEXTE_COMBO);
                }
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DonFormChoices.CategorieOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.libelle());
                }
                if (item != null && item.id() <= 0) {
                    setTextFill(TEXTE_INVITE_COMBO);
                } else {
                    setTextFill(TEXTE_COMBO);
                }
            }
        });
    }



    private void enregistrer() {

        try {

            DonFormChoices.CategorieOption cat = cbCategorie.getValue();

            if (cat == null || cat.id() <= 0) {

                Alert aw = new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une catégorie de don.");
                MediLinkDialogs.style(aw);
                aw.showAndWait();

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

                Alert aw = new Alert(Alert.AlertType.WARNING, erreur);
                MediLinkDialogs.style(aw);
                aw.showAndWait();

                return;

            }



            Date expiration = dateExp == null ? null : Date.valueOf(dateExp);



            Don d = new Don(categorieId, desc, quantite, unite, details, etat, urgence, "en_attente", expiration);
            d.setTitre(desc);
            d.setCategorie(cat.libelle());
            d.setDescription(details);

            DonIAService ia = donIAService();
            ia.enregistrerDonAvecAnalyse(d);

            Alert ai = new Alert(Alert.AlertType.INFORMATION,
                    "Don enregistré avec succès.\n\n"
                            + "Il sera examiné par un administrateur. L’avis éventuel de l’IA sera visible "
                            + "dans la gestion des dons (dons en attente).");
            MediLinkDialogs.style(ai);
            ai.showAndWait();

            retourListe();

        } catch (NumberFormatException ex) {

            Alert ae = new Alert(Alert.AlertType.ERROR, "La quantité doit être un nombre entier valide.");
            MediLinkDialogs.style(ae);
            ae.showAndWait();

        } catch (SQLException ex) {

            Alert a = new Alert(Alert.AlertType.ERROR);

            a.setTitle("Erreur");

            a.setContentText(ex.getMessage());

            MediLinkDialogs.style(a);

            a.showAndWait();

        } catch (IllegalStateException ex) {

            afficherErreurIaLisible("Configuration IA", "Clé API ou fournisseur IA", ex.getMessage());

        } catch (IOException ex) {

            afficherErreurIaLisible("Analyse IA", "Erreur d’appel au modèle", ex.getMessage());

        }

    }

    /** Alerte large avec texte défilant : le constructeur Alert(type, msg) tronque souvent les longs messages. */
    private static void afficherErreurIaLisible(String titreFenetre, String enTete, String messageComplet) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titreFenetre);
        a.setHeaderText(enTete);
        TextArea ta = new TextArea(messageComplet != null ? messageComplet : "");
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(60);
        a.getDialogPane().setContent(ta);
        a.getDialogPane().setMinWidth(560);
        MediLinkDialogs.style(a);
        a.showAndWait();
    }

    private DonIAService donIAService() throws IOException {
        if (donIAService == null) {
            donIAService = new DonIAService();
        }
        return donIAService;
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

            MediLinkDialogs.style(a);

            a.showAndWait();

        }

    }

}

>>>>>>> gestion-dons-java
