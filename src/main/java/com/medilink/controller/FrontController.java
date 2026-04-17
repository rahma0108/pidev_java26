package com.medilink.controller;

import com.medilink.model.Evenement;
import com.medilink.model.Participation;
import com.medilink.service.EvenementService;
import com.medilink.service.ParticipationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class FrontController implements Initializable {

    private static final int FIXED_USER_ID = 1;

    @FXML
    private FlowPane eventCardsContainer;

    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private final ObservableList<Evenement> evenementList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEvents();
        renderEventCards();
    }

    @FXML
    private void onRefreshEvents() {
        loadEvents();
    }

    private void onParticiper(Evenement selected) {
        String validationError = validateParticipationRequest(selected);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        Participation participation = new Participation(
                selected.getId(),
                "en_attente",
                LocalDateTime.now(),
                FIXED_USER_ID,
                "Demande depuis l'interface front"
        );
        try {
            participationService.insert(participation);
            showInfo("Demande envoyee");
        } catch (SQLException ex) {
            showError("Erreur lors de l'envoi: " + ex.getMessage());
        }
    }

    private void loadEvents() {
        evenementList.clear();
        try {
            evenementList.addAll(evenementService.getAll());
            renderEventCards();
        } catch (SQLException ex) {
            showError("Erreur de chargement des evenements: " + ex.getMessage());
        }
    }

    private void renderEventCards() {
        eventCardsContainer.getChildren().clear();
        if (evenementList.isEmpty()) {
            Label empty = new Label("Aucun événement disponible pour le moment.");
            empty.getStyleClass().add("empty-state");
            eventCardsContainer.getChildren().add(empty);
            return;
        }
        for (Evenement e : evenementList) {
            eventCardsContainer.getChildren().add(createEventCard(e));
        }
    }

    private VBox createEventCard(Evenement e) {
        VBox card = new VBox(6);
        card.setPrefWidth(300);
        card.getStyleClass().add("event-card");

        Label title = new Label(e.getTitre());
        title.getStyleClass().add("card-title");
        Label description = new Label("Description: " + safeText(e.getDescription()));
        Label date = new Label("Date: " + (e.getDateEvenement() != null ? e.getDateEvenement().toString() : "-"));
        Label lieu = new Label("Lieu: " + safeText(e.getLieu()));
        Label type = new Label("Type: " + safeText(e.getType()));
        description.getStyleClass().add("card-meta");
        date.getStyleClass().add("card-meta");
        lieu.getStyleClass().add("card-meta");
        type.getStyleClass().add("card-meta");
        description.setWrapText(true);

        Button participer = new Button("Participer");
        participer.getStyleClass().add("btn-primary");
        participer.setOnAction(event -> onParticiper(e));

        card.getChildren().addAll(title, description, date, lieu, type, participer);
        return card;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String validateParticipationRequest(Evenement selected) {
        if (selected == null) {
            return "Veuillez selectionner un evenement.";
        }
        if (selected.getDateEvenement() == null) {
            return "La date de l'evenement selectionne est invalide.";
        }
        if (selected.getDateEvenement().isBefore(LocalDate.now())) {
            return "Impossible de participer a un evenement deja passe.";
        }
        if (FIXED_USER_ID <= 0) {
            return "Utilisateur invalide pour la participation.";
        }
        return null;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
