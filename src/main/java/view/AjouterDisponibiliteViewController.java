package view;

import controllers.DisponibiliteController;
import exceptions.ServiceException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Disponibilite;
import models.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AjouterDisponibiliteViewController {

    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("H:mm");

    @FXML
    private TextField medecinIdTextField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField heureDebutTextField;
    @FXML
    private TextField heureFinTextField;
    @FXML
    private TableView<Disponibilite> apercuTableView;
    @FXML
    private TableColumn<Disponibilite, Integer> colonneApercuId;
    @FXML
    private TableColumn<Disponibilite, LocalDate> colonneApercuDate;
    @FXML
    private TableColumn<Disponibilite, String> colonneApercuDebut;
    @FXML
    private TableColumn<Disponibilite, String> colonneApercuFin;
    @FXML
    private TableColumn<Disponibilite, String> colonneApercuStatut;
    @FXML
    private Button ajouterButton;
    @FXML
    private Button supprimerButton;
    @FXML
    private Button reserverButton;
    @FXML
    private Button reinitialiserButton;

    private DisponibiliteController disponibiliteController;
    private Runnable afterSaveCallback;

    public void setAfterSaveCallback(Runnable afterSaveCallback) {
        this.afterSaveCallback = afterSaveCallback;
    }

    @FXML
    private void initialize() {
        colonneApercuId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colonneApercuDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colonneApercuDebut.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getHeureDebut() != null ? c.getValue().getHeureDebut().toString() : ""));
        colonneApercuFin.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getHeureFin() != null ? c.getValue().getHeureFin().toString() : ""));
        colonneApercuStatut.setCellValueFactory(new PropertyValueFactory<>("status"));

        apercuTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        try {
            disponibiliteController = new DisponibiliteController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }

        medecinIdTextField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                rafraichirApercu();
            }
        });

        ajouterButton.setOnAction(e -> handleAjouter());
        reinitialiserButton.setOnAction(e -> viderFormulaire());
        supprimerButton.setOnAction(e -> handleSupprimerApercu());
        reserverButton.setOnAction(e -> ouvrirReservation());
    }

    private void rafraichirApercu() {
        if (disponibiliteController == null) {
            return;
        }
        try {
            String t = medecinIdTextField.getText();
            if (t == null || t.isBlank()) {
                apercuTableView.getItems().clear();
                return;
            }
            int id = Integer.parseInt(t.trim());
            apercuTableView.getItems().setAll(disponibiliteController.afficherDisponibilitesMedecin(id));
        } catch (NumberFormatException e) {
            apercuTableView.getItems().clear();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Aperçu", e.formatWithCauses());
        }
    }

    private void handleAjouter() {
        if (disponibiliteController == null) {
            return;
        }
        try {
            int medId = Integer.parseInt(medecinIdTextField.getText().trim());
            LocalDate date = datePicker.getValue();
            if (date == null) {
                ViewAlertUtil.erreur("Saisie", "Choisissez une date.");
                return;
            }
            LocalTime debut = parseHeure(heureDebutTextField.getText());
            LocalTime fin = parseHeure(heureFinTextField.getText());

            User medecin = new User();
            medecin.setId(medId);

            Disponibilite d = new Disponibilite();
            d.setMedecin(medecin);
            d.setDate(date);
            d.setHeureDebut(debut);
            d.setHeureFin(fin);

            disponibiliteController.ajouterDisponibilite(d);
            ViewAlertUtil.info("Succès", "Disponibilité ajoutée.");
            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }
            rafraichirApercu();
        } catch (NumberFormatException e) {
            ViewAlertUtil.erreur("Saisie", "Identifiant médecin invalide (nombre entier).");
        } catch (IllegalArgumentException e) {
            ViewAlertUtil.erreur("Saisie", e.getMessage());
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Ajout", e.formatWithCauses());
        }
    }

    private static LocalTime parseHeure(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Indiquez une heure (ex. 09:00 ou 9:30).");
        }
        String s = raw.trim();
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException e1) {
            try {
                return LocalTime.parse(s, HEURE_FMT);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Heure invalide : « " + s + " » (ex. 09:30).");
            }
        }
    }

    private void viderFormulaire() {
        medecinIdTextField.clear();
        datePicker.setValue(null);
        heureDebutTextField.clear();
        heureFinTextField.clear();
        apercuTableView.getItems().clear();
    }

    private void handleSupprimerApercu() {
        Disponibilite sel = apercuTableView.getSelectionModel().getSelectedItem();
        if (sel == null) {
            ViewAlertUtil.erreur("Suppression", "Sélectionnez une ligne dans l'aperçu.");
            return;
        }
        if (!ViewAlertUtil.confirmer("Suppression", "Supprimer la disponibilité n° " + sel.getId() + " ?")) {
            return;
        }
        try {
            disponibiliteController.supprimerDisponibilite(sel.getId());
            ViewAlertUtil.info("Suppression", "Disponibilité supprimée.");
            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }
            rafraichirApercu();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Suppression", e.formatWithCauses());
        }
    }

    private void ouvrirReservation() {
        Disponibilite sel = apercuTableView.getSelectionModel().getSelectedItem();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReserverRendezVousView.fxml"));
            Parent root = loader.load();
            ReserverRendezVousViewController ctrl = loader.getController();
            if (sel != null) {
                ctrl.prefillDisponibiliteId(sel.getId());
            }
            ctrl.setAfterReserveCallback(() -> {
                if (afterSaveCallback != null) {
                    afterSaveCallback.run();
                }
                rafraichirApercu();
            });

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(reserverButton.getScene().getWindow());
            stage.setTitle("Réserver un rendez-vous");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible d'ouvrir la réservation : " + e.getMessage());
        }
    }
}
