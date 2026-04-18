package view;

import controllers.DisponibiliteController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import models.Disponibilite;
import models.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.UnaryOperator;

public class AjouterDisponibiliteViewController {

    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter DATE_UI_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private TextField medecinIdTextField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField heureDebutTextField;
    @FXML
    private TextField heureFinTextField;
    @FXML
    private FlowPane apercuCardsContainer;
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
    private Disponibilite selectionApercu;
    private Integer medecinIdContexte;

    public void setAfterSaveCallback(Runnable afterSaveCallback) {
        this.afterSaveCallback = afterSaveCallback;
    }

    public void setMedecinIdContexte(Integer medecinIdContexte) {
        this.medecinIdContexte = medecinIdContexte;
        appliquerContexteMedecin();
    }

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }
        configurerAideSaisie();
        appliquerContexteMedecin();

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

    private void appliquerContexteMedecin() {
        if (medecinIdTextField == null) {
            return;
        }
        if (medecinIdContexte != null) {
            medecinIdTextField.setText(Integer.toString(medecinIdContexte));
            medecinIdTextField.setDisable(true);
        } else {
            medecinIdTextField.setDisable(false);
        }
    }

    private void configurerAideSaisie() {
        datePicker.setEditable(true);
        datePicker.getEditor().setPromptText("jj/MM/aaaa");
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_UI_FMT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                String s = text.trim();
                try {
                    return LocalDate.parse(s, DATE_UI_FMT);
                } catch (DateTimeParseException ex) {
                    return LocalDate.parse(s);
                }
            }
        });
        heureDebutTextField.setPromptText("HH:mm (ex: 09:30)");
        heureFinTextField.setPromptText("HH:mm (ex: 10:30)");
        heureDebutTextField.setTextFormatter(creerHeureFormatter());
        heureFinTextField.setTextFormatter(creerHeureFormatter());
    }

    private static TextFormatter<String> creerHeureFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }
            if (!next.matches("\\d{0,2}:?\\d{0,2}")) {
                return null;
            }
            if (next.matches("\\d{2}") && !next.contains(":")) {
                change.setText(change.getText() + ":");
                change.setRange(change.getRangeStart(), change.getRangeEnd());
            }
            return change;
        };
        return new TextFormatter<>(filter);
    }

    private void rafraichirApercu() {
        if (disponibiliteController == null) {
            return;
        }
        try {
            String t = medecinIdTextField.getText();
            if (t == null || t.isBlank()) {
                selectionApercu = null;
                apercuCardsContainer.getChildren().clear();
                return;
            }
            int id = Integer.parseInt(t.trim());
            renderApercuCards(disponibiliteController.afficherDisponibilitesMedecin(id));
        } catch (NumberFormatException e) {
            selectionApercu = null;
            apercuCardsContainer.getChildren().clear();
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
            if (date == null && datePicker.getEditor().getText() != null && !datePicker.getEditor().getText().isBlank()) {
                date = datePicker.getConverter().fromString(datePicker.getEditor().getText().trim());
                datePicker.setValue(date);
            }
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
        if (medecinIdContexte == null) {
            medecinIdTextField.clear();
        }
        datePicker.setValue(null);
        heureDebutTextField.clear();
        heureFinTextField.clear();
        selectionApercu = null;
        apercuCardsContainer.getChildren().clear();
    }

    private void handleSupprimerApercu() {
        Disponibilite sel = selectionApercu;
        if (sel == null) {
            ViewAlertUtil.erreur("Suppression", "Sélectionnez une carte dans l'aperçu.");
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
        Disponibilite sel = selectionApercu;
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

    private void renderApercuCards(List<Disponibilite> disponibilites) {
        apercuCardsContainer.getChildren().clear();
        selectionApercu = null;
        if (disponibilites == null || disponibilites.isEmpty()) {
            Label vide = new Label("Aucune disponibilité trouvée.");
            vide.getStyleClass().add("subtitle");
            apercuCardsContainer.getChildren().add(vide);
            return;
        }
        for (Disponibilite d : disponibilites) {
            apercuCardsContainer.getChildren().add(createApercuCard(d));
        }
    }

    private VBox createApercuCard(Disponibilite d) {
        Label titre = new Label("Disponibilité #" + d.getId());
        titre.getStyleClass().add("card-title");
        Label date = new Label("Date: " + (d.getDate() != null ? d.getDate() : "-"));
        Label heure = new Label("Heure: "
                + (d.getHeureDebut() != null ? d.getHeureDebut() : "-")
                + " - "
                + (d.getHeureFin() != null ? d.getHeureFin() : "-"));
        Label statut = new Label("Statut: " + (d.getStatus() != null ? d.getStatus() : "-"));
        date.getStyleClass().add("card-text");
        heure.getStyleClass().add("card-text");
        statut.getStyleClass().add("card-text");

        VBox card = new VBox(6, titre, date, heure, statut);
        card.setPrefWidth(230);
        applyCardState(card, d.equals(selectionApercu));
        card.setOnMouseClicked(e -> {
            selectionApercu = d;
            refreshApercuSelectionStyle();
        });
        return card;
    }

    private void refreshApercuSelectionStyle() {
        for (javafx.scene.Node node : apercuCardsContainer.getChildren()) {
            if (!(node instanceof VBox box) || box.getChildren().isEmpty()) {
                continue;
            }
            boolean selected = false;
            if (selectionApercu != null && box.getChildren().get(0) instanceof Label label) {
                selected = label.getText().equals("Disponibilité #" + selectionApercu.getId());
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
}
