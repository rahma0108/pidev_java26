package com.medilink.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class BackController implements Initializable {

    @FXML
    private TextField txtTitre;
    @FXML
    private TextField txtDescription;
    @FXML
    private DatePicker dpDateEvenement;
    @FXML
    private TextField txtLieu;
    @FXML
    private TextField txtType;
    @FXML
    private TextField txtPhoto;
    @FXML
    private TextField txtSearchEventName;

    @FXML
    private FlowPane eventCardsContainer;

    @FXML
    private FlowPane participationCardsContainer;

    private final List<String> events = new ArrayList<>();
    private final List<String> participations = new ArrayList<>();
    private String selectedEvent;
    private String selectedParticipation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        renderEventCards(events);
    }

    @FXML
    private void onAdd() {
        String titre = txtTitre.getText() == null ? "" : txtTitre.getText().trim();
        if (titre.isEmpty()) {
            showError("Le champ titre est obligatoire.");
            return;
        }
        events.add(titre);
        showInfo("Evenement ajoute en mode demo.");
        clearEventForm();
        renderEventCards(events);
    }

    @FXML
    private void onUpdate() {
        if (selectedEvent == null) {
            showError("Selectionnez un evenement.");
            return;
        }
        String newTitle = txtTitre.getText() == null ? "" : txtTitre.getText().trim();
        if (newTitle.isEmpty()) {
            showError("Le champ titre est obligatoire.");
            return;
        }
        int index = events.indexOf(selectedEvent);
        if (index >= 0) {
            events.set(index, newTitle);
            selectedEvent = newTitle;
        }
        showInfo("Evenement modifie en mode demo.");
        renderEventCards(events);
    }

    @FXML
    private void onDelete() {
        if (selectedEvent == null) {
            showError("Selectionnez un evenement.");
            return;
        }
        events.remove(selectedEvent);
        participations.clear();
        selectedEvent = null;
        selectedParticipation = null;
        showInfo("Evenement supprime en mode demo.");
        clearEventForm();
        renderEventCards(events);
        renderParticipationCards();
    }

    @FXML
    private void onConfirmer() {
        updateParticipationStatus("confirme");
    }

    @FXML
    private void onRefuser() {
        if (selectedParticipation == null) {
            showError("Selectionnez une participation.");
            return;
        }
        selectedParticipation = selectedParticipation + " - refusee";
        showInfo("Participation refusee (mode demo).");
        renderParticipationCards();
    }

    @FXML
    private void onSearchEventName() {
        String keyword = txtSearchEventName.getText() == null ? "" : txtSearchEventName.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            renderEventCards(events);
            return;
        }
        List<String> filtered = new ArrayList<>();
        for (String e : events) {
            if (e.toLowerCase().contains(keyword)) {
                filtered.add(e);
            }
        }
        renderEventCards(filtered);
    }

    @FXML
    private void onResetSearch() {
        txtSearchEventName.clear();
        renderEventCards(events);
    }

    private void updateParticipationStatus(String status) {
        if (selectedParticipation == null) {
            showError("Selectionnez une participation.");
            return;
        }
        showInfo("Participation " + status + " (mode demo).");
    }

    private void clearEventForm() {
        txtTitre.clear();
        txtDescription.clear();
        dpDateEvenement.setValue(null);
        txtLieu.clear();
        txtType.clear();
        txtPhoto.clear();
        selectedEvent = null;
    }

    private void renderEventCards(List<String> source) {
        eventCardsContainer.getChildren().clear();
        if (source == null || source.isEmpty()) {
            Label empty = new Label("Aucun evenement trouve.");
            empty.getStyleClass().add("empty-state");
            eventCardsContainer.getChildren().add(empty);
            return;
        }
        for (String e : source) {
            eventCardsContainer.getChildren().add(createEventCard(e));
        }
    }

    private VBox createEventCard(String e) {
        VBox card = new VBox(6);
        card.setPrefWidth(290);
        card.getStyleClass().add(e == selectedEvent ? "event-card-selected" : "event-card");
        card.setOnMouseClicked(event -> {
            selectedEvent = e;
            txtTitre.setText(e);
            txtDescription.setText("Description demo");
            dpDateEvenement.setValue(LocalDateTime.now().toLocalDate());
            txtLieu.setText("-");
            txtType.setText("Don");
            txtPhoto.setText("");
            participations.clear();
            participations.add("Participation #1 - en_attente");
            renderParticipationCards();
            renderEventCards(events);
        });

        Label title = new Label(e);
        title.getStyleClass().add("card-title");
        Label date = new Label("Date: " + LocalDateTime.now().toLocalDate());
        Label lieu = new Label("Lieu: -");
        date.getStyleClass().add("card-meta");
        lieu.getStyleClass().add("card-meta");
        card.getChildren().addAll(title, date, lieu);
        return card;
    }

    private void renderParticipationCards() {
        participationCardsContainer.getChildren().clear();
        if (participations.isEmpty()) {
            Label empty = new Label("Aucune participation liee a cet evenement.");
            empty.getStyleClass().add("empty-state");
            participationCardsContainer.getChildren().add(empty);
            return;
        }
        for (String p : participations) {
            participationCardsContainer.getChildren().add(createParticipationCard(p));
        }
    }

    private VBox createParticipationCard(String p) {
        VBox card = new VBox(6);
        card.setPrefWidth(290);
        card.getStyleClass().add(p == selectedParticipation ? "event-card-selected" : "event-card");
        card.setOnMouseClicked(event -> {
            selectedParticipation = p;
            renderParticipationCards();
        });

        Label id = new Label(p);
        id.getStyleClass().add("card-title");
        Label user = new Label("User ID: 1");
        Label statut = new Label("Statut: en_attente");
        Label date = new Label("Date: " + LocalDateTime.now());
        user.getStyleClass().add("card-meta");
        statut.getStyleClass().add("card-meta");
        date.getStyleClass().add("card-meta");
        card.getChildren().addAll(id, user, statut, date);
        return card;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
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
