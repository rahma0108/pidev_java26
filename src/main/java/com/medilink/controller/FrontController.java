package com.medilink.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class FrontController implements Initializable {

    @FXML
    private FlowPane eventCardsContainer;

    private final List<String> demoEvents = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEvents();
        renderEventCards();
    }

    @FXML
    private void onRefreshEvents() {
        loadEvents();
    }

    private void onParticiper(String selected) {
        if (selected == null || selected.isBlank()) {
            showError("Aucun evenement selectionne.");
            return;
        }
        showInfo("Participation enregistree en mode demo pour: " + selected + " (" + LocalDateTime.now() + ")");
    }

    private void loadEvents() {
        demoEvents.clear();
        demoEvents.add("Journee Don de Sang - Centre Ville");
        demoEvents.add("Collecte Medicaments - Clinique Nord");
        demoEvents.add("Sensibilisation Don d'Organes - Hopital Central");
    }

    private void renderEventCards() {
        eventCardsContainer.getChildren().clear();
        if (demoEvents.isEmpty()) {
            Label empty = new Label("Aucun evenement disponible pour le moment.");
            empty.getStyleClass().add("empty-state");
            eventCardsContainer.getChildren().add(empty);
            return;
        }
        for (String e : demoEvents) {
            eventCardsContainer.getChildren().add(createEventCard(e));
        }
    }

    private VBox createEventCard(String e) {
        VBox card = new VBox(6);
        card.setPrefWidth(300);
        card.getStyleClass().add("event-card");

        Label title = new Label(e);
        title.getStyleClass().add("card-title");
        Label description = new Label("Description: Evenement ajoute en mode demo.");
        Label date = new Label("Date: " + LocalDateTime.now().toLocalDate());
        Label lieu = new Label("Lieu: -");
        Label type = new Label("Type: Don");
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
