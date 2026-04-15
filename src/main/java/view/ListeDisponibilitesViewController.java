package view;

import controllers.DisponibiliteController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Disponibilite;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ListeDisponibilitesViewController {

    @FXML
    private TextField filtreMedecinTextField;
    @FXML
    private DatePicker filtreDatePicker;
    @FXML
    private Button rafraichirButton;
    @FXML
    private Label selectionInfoLabel;
    @FXML
    private FlowPane cardsFlowPane;
    @FXML
    private Button ajouterButton;
    @FXML
    private Button supprimerButton;
    @FXML
    private Button retourButton;

    private DisponibiliteController disponibiliteController;
    private Disponibilite selectionCourante;

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }

        rafraichirButton.setOnAction(e -> chargerDonnees());
        ajouterButton.setOnAction(e -> ouvrirFenetreAjout());
        supprimerButton.setOnAction(e -> supprimerSelection());
        retourButton.setOnAction(e -> retournerAccueil());

        chargerDonnees();
    }

    private void chargerDonnees() {
        if (disponibiliteController == null) {
            return;
        }
        try {
            List<Disponibilite> list;
            String mid = filtreMedecinTextField.getText();
            if (mid != null && !mid.isBlank()) {
                list = disponibiliteController.afficherDisponibilitesMedecin(Integer.parseInt(mid.trim()));
            } else {
                list = disponibiliteController.afficherDisponibilites();
            }
            LocalDate dateMin = filtreDatePicker.getValue();
            if (dateMin != null) {
                list = list.stream()
                        .filter(d -> d.getDate() != null && !d.getDate().isBefore(dateMin))
                        .collect(Collectors.toList());
            }
            renderCards(list);
        } catch (NumberFormatException e) {
            ViewAlertUtil.erreur("Filtre", "Identifiant médecin invalide (nombre entier attendu).");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void renderCards(List<Disponibilite> disponibilites) {
        cardsFlowPane.getChildren().clear();
        selectionCourante = null;
        selectionInfoLabel.setText("Aucune disponibilité sélectionnée.");

        if (disponibilites == null || disponibilites.isEmpty()) {
            Label vide = new Label("Aucune disponibilité trouvée.");
            vide.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            cardsFlowPane.getChildren().add(vide);
            return;
        }

        for (Disponibilite d : disponibilites) {
            VBox card = createCard(d);
            cardsFlowPane.getChildren().add(card);
        }
    }

    private VBox createCard(Disponibilite d) {
        Label titre = new Label("Disponibilité #" + d.getId());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label date = new Label("Date: " + (d.getDate() != null ? d.getDate() : "-"));
        Label heure = new Label("Heure: "
                + (d.getHeureDebut() != null ? d.getHeureDebut() : "-")
                + " - "
                + (d.getHeureFin() != null ? d.getHeureFin() : "-"));
        Label statut = new Label("Statut: " + (d.getStatus() != null ? d.getStatus() : "-"));
        Label medecin = new Label("Médecin: "
                + (d.getMedecin() != null && d.getMedecin().getFullName() != null ? d.getMedecin().getFullName() : "-"));

        HBox row = new HBox(statut);
        HBox.setHgrow(statut, Priority.ALWAYS);

        VBox card = new VBox(8, titre, date, heure, medecin, row);
        card.setPadding(new Insets(12));
        card.setPrefWidth(250);
        card.setStyle(styleCard(false));

        card.setOnMouseClicked(e -> {
            selectionCourante = d;
            refreshSelectionStyles();
            selectionInfoLabel.setText("Sélection: disponibilité #" + d.getId());
        });
        return card;
    }

    private void refreshSelectionStyles() {
        for (javafx.scene.Node node : cardsFlowPane.getChildren()) {
            if (!(node instanceof VBox box) || box.getChildren().isEmpty()) {
                continue;
            }
            boolean selected = false;
            if (selectionCourante != null && box.getChildren().get(0) instanceof Label l) {
                selected = l.getText().equals("Disponibilité #" + selectionCourante.getId());
            }
            box.setStyle(styleCard(selected));
        }
    }

    private String styleCard(boolean selected) {
        if (selected) {
            return "-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 2; "
                    + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
        }
        return "-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-width: 1; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;";
    }

    private void ouvrirFenetreAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterDisponibiliteView.fxml"));
            Parent root = loader.load();
            AjouterDisponibiliteViewController ctrl = loader.getController();
            ctrl.setAfterSaveCallback(this::chargerDonnees);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(ajouterButton.getScene().getWindow());
            stage.setTitle("Ajouter une disponibilité");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void supprimerSelection() {
        if (selectionCourante == null) {
            ViewAlertUtil.erreur("Suppression", "Sélectionnez une carte.");
            return;
        }
        if (!ViewAlertUtil.confirmer("Suppression", "Supprimer la disponibilité n° " + selectionCourante.getId() + " ?")) {
            return;
        }
        try {
            disponibiliteController.supprimerDisponibilite(selectionCourante.getId());
            chargerDonnees();
            ViewAlertUtil.info("Suppression", "Disponibilité supprimée.");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Suppression", e.formatWithCauses());
        }
    }

    private void retournerAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) retourButton.getScene().getWindow();
            stage.setTitle("MediLink - Accueil");
            stage.setScene(new Scene(root, 600, 380));
            stage.centerOnScreen();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible de revenir à l'accueil : " + e.getMessage());
        }
    }
}
