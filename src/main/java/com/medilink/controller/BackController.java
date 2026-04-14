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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
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

    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private final ObservableList<Evenement> evenementList = FXCollections.observableArrayList();
    private final ObservableList<Evenement> filteredEventList = FXCollections.observableArrayList();
    private final ObservableList<Participation> participationList = FXCollections.observableArrayList();
    private Evenement selectedEvent;
    private Participation selectedParticipation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEvents();
        renderEventCards(evenementList);
    }

    @FXML
    private void onAdd() {
        String validationError = validateEventForm();
        if (validationError != null) {
            showError(validationError);
            return;
        }
        Evenement e = new Evenement(
                txtTitre.getText().trim(),
                txtDescription.getText().trim(),
                dpDateEvenement.getValue(),
                txtLieu.getText().trim(),
                txtType.getText().trim(),
                txtPhoto.getText().trim()
        );
        try {
            evenementService.insert(e);
            showInfo("Evenement ajoute.");
            clearEventForm();
            loadEvents();
        } catch (SQLException ex) {
            showError("Erreur ajout evenement: " + ex.getMessage());
        }
    }

    @FXML
    private void onUpdate() {
        if (selectedEvent == null) {
            showError("Selectionnez un evenement.");
            return;
        }
        String validationError = validateEventForm();
        if (validationError != null) {
            showError(validationError);
            return;
        }
        selectedEvent.setTitre(txtTitre.getText().trim());
        selectedEvent.setDescription(txtDescription.getText().trim());
        selectedEvent.setDateEvenement(dpDateEvenement.getValue());
        selectedEvent.setLieu(txtLieu.getText().trim());
        selectedEvent.setType(txtType.getText().trim());
        selectedEvent.setPhoto(txtPhoto.getText().trim());
        try {
            evenementService.update(selectedEvent);
            showInfo("Evenement modifie.");
            loadEvents();
            renderEventCards(evenementList);
            loadParticipationsByEvent(selectedEvent.getId());
        } catch (SQLException ex) {
            showError("Erreur modification evenement: " + ex.getMessage());
        }
    }

    @FXML
    private void onDelete() {
        if (selectedEvent == null) {
            showError("Selectionnez un evenement.");
            return;
        }
        try {
            // Supprimer d'abord les participations liees pour respecter la contrainte FK.
            for (Participation p : participationService.getByEvent(selectedEvent.getId())) {
                participationService.delete(p.getId());
            }
            evenementService.delete(selectedEvent.getId());
            showInfo("Evenement supprime.");
            clearEventForm();
            loadEvents();
            renderEventCards(evenementList);
            participationList.clear();
            renderParticipationCards();
            selectedEvent = null;
            selectedParticipation = null;
        } catch (SQLException ex) {
            showError("Erreur suppression evenement: " + ex.getMessage());
        }
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
        try {
            participationService.updateStatut(selectedParticipation.getId(), "refuse");
            showInfo("Participation refusee.");
            refreshParticipationForSelectedEvent();
        } catch (SQLException ex) {
            showError("Erreur refus participation: " + ex.getMessage());
        }
    }

    @FXML
    private void onSearchEventName() {
        String keyword = txtSearchEventName.getText() == null ? "" : txtSearchEventName.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            renderEventCards(evenementList);
            return;
        }

        filteredEventList.clear();
        for (Evenement e : evenementList) {
            String titre = e.getTitre() == null ? "" : e.getTitre().toLowerCase();
            if (titre.contains(keyword)) {
                filteredEventList.add(e);
            }
        }
        renderEventCards(filteredEventList);
    }

    @FXML
    private void onResetSearch() {
        txtSearchEventName.clear();
        renderEventCards(evenementList);
    }

    private void updateParticipationStatus(String status) {
        if (selectedParticipation == null) {
            showError("Selectionnez une participation.");
            return;
        }
        try {
            participationService.updateStatut(selectedParticipation.getId(), status);
            showInfo("Participation " + status + ".");
            refreshParticipationForSelectedEvent();
        } catch (SQLException ex) {
            showError("Erreur mise a jour statut: " + ex.getMessage());
        }
    }

    private void loadEvents() {
        evenementList.clear();
        try {
            evenementList.addAll(evenementService.getAll());
            renderEventCards(evenementList);
        } catch (SQLException ex) {
            showError("Erreur chargement evenements: " + ex.getMessage());
        }
    }

    private void loadParticipationsByEvent(int eventId) {
        participationList.clear();
        try {
            participationList.addAll(participationService.getByEvent(eventId));
            renderParticipationCards();
        } catch (SQLException ex) {
            showError("Erreur chargement participations: " + ex.getMessage());
        }
    }

    private void refreshParticipationForSelectedEvent() {
        if (selectedEvent != null) {
            loadParticipationsByEvent(selectedEvent.getId());
        }
    }

    private void fillEventForm(Evenement e) {
        txtTitre.setText(e.getTitre());
        txtDescription.setText(e.getDescription());
        dpDateEvenement.setValue(e.getDateEvenement());
        txtLieu.setText(e.getLieu());
        txtType.setText(e.getType());
        txtPhoto.setText(e.getPhoto());
    }

    private String validateEventForm() {
        String titre = txtTitre.getText().trim();
        String description = txtDescription.getText().trim();
        LocalDate dateEvenement = dpDateEvenement.getValue();
        String lieu = txtLieu.getText().trim();
        String type = txtType.getText().trim();
        String photo = txtPhoto.getText().trim();

        if (titre.isEmpty()) {
            return "Le champ titre est obligatoire.";
        }
        if (titre.length() < 3) {
            return "Le titre doit contenir au moins 3 caracteres.";
        }
        if (description.isEmpty()) {
            return "Le champ description est obligatoire.";
        }
        if (description.length() < 10) {
            return "La description doit contenir au moins 10 caracteres.";
        }
        if (dateEvenement == null) {
            return "La date de l'evenement est obligatoire.";
        }
        if (dateEvenement.isBefore(LocalDate.now())) {
            return "La date de l'evenement ne peut pas etre dans le passe.";
        }
        if (lieu.isEmpty()) {
            return "Le champ lieu est obligatoire.";
        }
        if (lieu.length() < 2) {
            return "Le lieu doit contenir au moins 2 caracteres.";
        }
        if (type.isEmpty()) {
            return "Le champ type est obligatoire.";
        }
        if (type.length() < 3) {
            return "Le type doit contenir au moins 3 caracteres.";
        }
        if (!photo.isEmpty() && photo.length() < 4) {
            return "Le chemin ou nom de photo est invalide.";
        }

        return null;
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

    private void renderEventCards(ObservableList<Evenement> source) {
        eventCardsContainer.getChildren().clear();
        for (Evenement e : source) {
            eventCardsContainer.getChildren().add(createEventCard(e));
        }
    }

    private VBox createEventCard(Evenement e) {
        VBox card = new VBox(6);
        card.setPrefWidth(290);
        card.setStyle(baseCardStyle(e == selectedEvent));

        Label title = new Label(e.getTitre());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label date = new Label("Date: " + (e.getDateEvenement() != null ? e.getDateEvenement().toString() : "-"));
        Label lieu = new Label("Lieu: " + safeText(e.getLieu()));
        Label type = new Label("Type: " + safeText(e.getType()));
        Label desc = new Label("Description: " + safeText(e.getDescription()));

        Button select = new Button("Selectionner");
        select.setOnAction(event -> {
            selectedEvent = e;
            fillEventForm(e);
            loadParticipationsByEvent(e.getId());
            renderEventCards(evenementList);
        });

        card.getChildren().addAll(title, date, lieu, type, desc, select);
        return card;
    }

    private void renderParticipationCards() {
        participationCardsContainer.getChildren().clear();
        for (Participation p : participationList) {
            participationCardsContainer.getChildren().add(createParticipationCard(p));
        }
    }

    private VBox createParticipationCard(Participation p) {
        VBox card = new VBox(6);
        card.setPrefWidth(290);
        card.setStyle(baseCardStyle(p == selectedParticipation));

        Label id = new Label("Participation #" + p.getId());
        id.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label user = new Label("User ID: " + p.getUserId());
        Label statut = new Label("Statut: " + safeText(p.getStatut()));
        Label date = new Label("Date: " + (p.getDateInscription() != null ? p.getDateInscription().toString() : "-"));

        Button select = new Button("Selectionner");
        select.setOnAction(event -> {
            selectedParticipation = p;
            renderParticipationCards();
        });

        card.getChildren().addAll(id, user, statut, date, select);
        return card;
    }

    private String baseCardStyle(boolean selected) {
        String border = selected ? "#1e5fcc" : "#c5d7f3";
        return "-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: " + border + "; -fx-border-width: 2;";
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
