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
    private Integer medecinIdContexte;

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }
        medecinIdContexte = SessionContext.getCurrentMedecinId();

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
            List<Disponibilite> list = medecinIdContexte == null
                    ? disponibiliteController.afficherDisponibilites()
                    : disponibiliteController.afficherDisponibilitesMedecin(medecinIdContexte);
            LocalDate dateMin = filtreDatePicker.getValue();
            if (dateMin != null) {
                list = list.stream()
                        .filter(d -> d.getDate() != null && !d.getDate().isBefore(dateMin))
                        .collect(Collectors.toList());
            }
            renderCards(list);
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
            vide.getStyleClass().add("subtitle");
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
        titre.getStyleClass().add("card-title");

        Label date = new Label("Date: " + (d.getDate() != null ? d.getDate() : "-"));
        Label heure = new Label("Heure: "
                + (d.getHeureDebut() != null ? d.getHeureDebut() : "-")
                + " - "
                + (d.getHeureFin() != null ? d.getHeureFin() : "-"));
        Label statut = new Label("Statut: " + (d.getStatus() != null ? d.getStatus() : "-"));
        Label medecin = new Label("Médecin: "
                + (d.getMedecin() != null && d.getMedecin().getFullName() != null ? d.getMedecin().getFullName() : "-"));
        date.getStyleClass().add("card-text");
        heure.getStyleClass().add("card-text");
        statut.getStyleClass().add("card-text");
        medecin.getStyleClass().add("card-text");

        HBox row = new HBox(statut);
        HBox.setHgrow(statut, Priority.ALWAYS);

        VBox card = new VBox(8, titre, date, heure, medecin, row);
        card.setPadding(new Insets(14));
        card.setPrefWidth(280);
        applyCardState(card, false);

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
            applyCardState(box, selected);
        }
    }

    private static void applyCardState(VBox card, boolean selected) {
        card.getStyleClass().setAll("availability-card");
        if (selected) {
            card.getStyleClass().add("selected");
        }
    }

    private void ouvrirFenetreAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterDisponibiliteView.fxml"));
            Parent root = loader.load();
            AjouterDisponibiliteViewController ctrl = loader.getController();
            ctrl.setMedecinIdContexte(medecinIdContexte);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) retourButton.getScene().getWindow();
            stage.setTitle("MediLink - Espace Médecin");
            stage.setScene(new Scene(root, 960, 640));
            stage.centerOnScreen();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible de revenir à l'accueil : " + e.getMessage());
        }
    }
}
