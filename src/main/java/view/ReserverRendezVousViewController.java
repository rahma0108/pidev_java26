package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Disponibilite;
import models.RendezVous;
import models.User;
import services.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

public class ReserverRendezVousViewController {

    private static final DateTimeFormatter DATE_HEURE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private TextField motifTextField;
    @FXML
    private FlowPane creneauxCardsContainer;
    @FXML
    private VBox disponibilitesEmptyBox;
    @FXML
    private Button retourButton;
    @FXML
    private Button mesRendezVousButton;
    @FXML
    private Button reserverButton;
    @FXML
    private VBox mesRendezVousListContainer;
    @FXML
    private Label disponibilitesCountLabel;
    @FXML
    private Label mesRendezVousCountLabel;
    @FXML
    private Label recommandationLabel;

    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private UserService userService;
    private Runnable afterReserveCallback;
    private List<Disponibilite> creneauxReservables = new ArrayList<>();
    private Integer demoPatientId;
    private Integer preselectedDisponibiliteId;
    private Disponibilite selectedDisponibilite;

    public void prefillDisponibiliteId(int id) {
        this.preselectedDisponibiliteId = id;
        selectionnerDisponibiliteSiVisible(id);
    }

    public void setAfterReserveCallback(Runnable afterReserveCallback) {
        this.afterReserveCallback = afterReserveCallback;
    }

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
            rendezVousController = new RendezVousController();
            userService = new UserService();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de donnees", e.formatWithCauses());
            return;
        }

        demoPatientId = resoudrePatientId();

        if (retourButton != null) {
            retourButton.setOnAction(e -> retournerAccueil());
        }
        if (mesRendezVousButton != null) {
            mesRendezVousButton.setOnAction(e -> ouvrirPreferences());
        }
        if (reserverButton != null) {
            reserverButton.setOnAction(e -> handleReserver());
        }

        chargerCreneauxLibres();
        chargerMesRendezVous();
        mettreAJourRecommandation();
    }

    private void chargerCreneauxLibres() {
        try {
            creneauxReservables = disponibiliteController.afficherCreneauxReservables().stream()
                    .filter(d -> d.getMedecin() != null && d.getMedecin().getId() > 0)
                    .collect(Collectors.toList());
            renderCreneauxCards(creneauxReservables);
            if (preselectedDisponibiliteId != null) {
                selectionnerDisponibiliteSiVisible(preselectedDisponibiliteId);
            }
            if (disponibilitesCountLabel != null) {
                int count = creneauxReservables.size();
                disponibilitesCountLabel.setText(count + (count > 1 ? " disponibilites" : " disponibilite"));
            }
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void handleReserver() {
        if (demoPatientId == null) {
            ViewAlertUtil.erreur("Configuration", "Aucun patient connecte valide n'a ete trouve.");
            return;
        }
        if (selectedDisponibilite == null) {
            ViewAlertUtil.erreur("Reservation", "Selectionnez une disponibilite a reserver.");
            return;
        }
        try {
            String motif = motifTextField != null ? motifTextField.getText() : null;
            RendezVous rdv = rendezVousController.reserverRendezVous(selectedDisponibilite.getId(), demoPatientId, motif);
            ViewAlertUtil.info("Reservation", "Rendez-vous cree (n " + rdv.getId() + ").");
            if (motifTextField != null) {
                motifTextField.clear();
            }
            selectedDisponibilite = null;
            if (afterReserveCallback != null) {
                afterReserveCallback.run();
            }
            chargerCreneauxLibres();
            chargerMesRendezVous();
            mettreAJourRecommandation();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Reservation", e.formatWithCauses());
        }
    }

    private void renderCreneauxCards(List<Disponibilite> disponibilites) {
        creneauxCardsContainer.getChildren().clear();
        boolean empty = disponibilites == null || disponibilites.isEmpty();
        disponibilitesEmptyBox.setVisible(empty);
        disponibilitesEmptyBox.setManaged(empty);
        if (empty) {
            return;
        }
        for (Disponibilite d : disponibilites) {
            creneauxCardsContainer.getChildren().add(createCreneauCard(d));
        }
    }

    private VBox createCreneauCard(Disponibilite disponibilite) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("availability-card", "patient-dispo-card-item");
        if (selectedDisponibilite != null && selectedDisponibilite.equals(disponibilite)) {
            card.getStyleClass().add("selected");
        }

        Label medecinLabel = new Label(resolveMedecinName(disponibilite));
        medecinLabel.getStyleClass().addAll("card-title", "patient-dispo-doctor");

        Label dateLabel = new Label(formatDate(disponibilite));
        dateLabel.getStyleClass().addAll("card-text", "patient-dispo-date");

        Label heureLabel = new Label(formatTimeRange(disponibilite));
        heureLabel.getStyleClass().addAll("card-text", "patient-dispo-time");

        HBox footer = new HBox(10);
        footer.getStyleClass().add("patient-dispo-footer");

        Label badge = new Label(isTresDemande(disponibilite) ? "Tres demande" : "Disponible");
        badge.getStyleClass().addAll("status-badge", isTresDemande(disponibilite) ? "status-waiting" : "status-available");

        Button reserveButton = new Button("Reserver cette disponibilite");
        reserveButton.getStyleClass().addAll("primary-button", "patient-dispo-book-button");
        reserveButton.setOnAction(e -> {
            selectedDisponibilite = disponibilite;
            renderCreneauxCards(creneauxReservables);
            handleReserver();
        });

        footer.getChildren().addAll(badge, reserveButton);
        card.getChildren().addAll(medecinLabel, dateLabel, heureLabel, footer);

        card.setOnMouseClicked(e -> {
            selectedDisponibilite = disponibilite;
            renderCreneauxCards(creneauxReservables);
        });
        return card;
    }

    private void chargerMesRendezVous() {
        if (mesRendezVousListContainer == null) {
            return;
        }
        mesRendezVousListContainer.getChildren().clear();
        if (demoPatientId == null) {
            renderMesRendezVousEmpty("Aucun patient connecte: impossible de charger les rendez-vous.");
            return;
        }
        try {
            List<RendezVous> rdvs = rendezVousController.listerPourPatient(demoPatientId);
            if (mesRendezVousCountLabel != null) {
                int count = rdvs.size();
                mesRendezVousCountLabel.setText(count + (count > 1 ? " rendez-vous" : " rendez-vous"));
            }
            if (rdvs.isEmpty()) {
                renderMesRendezVousEmpty("Aucun rendez-vous trouve.");
                return;
            }
            for (RendezVous rdv : rdvs) {
                mesRendezVousListContainer.getChildren().add(createRendezVousRow(rdv));
            }
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Rendez-vous", e.formatWithCauses());
        }
    }

    private void renderMesRendezVousEmpty(String message) {
        Label empty = new Label(message);
        empty.getStyleClass().add("rdv-empty-state");
        mesRendezVousListContainer.getChildren().add(empty);
    }

    private HBox createRendezVousRow(RendezVous rdv) {
        HBox row = new HBox(14);
        row.getStyleClass().add("rdv-row");

        Label medecinLabel = createRowCell(resolveMedecinName(rdv.getDisponibilite()), "rdv-cell-label", "patient-col-medecin");
        Label dateHeureLabel = createRowCell(formatDateHeure(rdv.getDateHeure()), "rdv-cell-label", "patient-col-date");

        Label statutBadge = new Label(formatStatut(rdv.getStatut()));
        statutBadge.getStyleClass().addAll("status-badge", statusClassFor(rdv.getStatut()));
        HBox statutBox = new HBox(statutBadge);
        statutBox.getStyleClass().add("patient-col-status");

        HBox calendarBox = new HBox(8);
        calendarBox.getStyleClass().addAll("rdv-actions-box", "patient-col-calendar");

        if (!RendezVous.ANNULE.equalsIgnoreCase(rdv.getStatut())) {
            Button calendrierButton = createActionButton("📅", "Ajouter au calendrier",
                    "secondary-button", "rdv-action-button", "icon-action-button", "calendar-action");
            calendrierButton.setOnAction(e ->
                    ViewAlertUtil.info("Calendrier", "Placeholder calendrier pour le rendez-vous du " + formatDateHeure(rdv.getDateHeure()) + "."));
            calendarBox.getChildren().add(calendrierButton);
        } else {
            Label nonDisponible = new Label("-");
            nonDisponible.getStyleClass().add("rdv-cell-label");
            calendarBox.getChildren().add(nonDisponible);
        }

        row.getChildren().addAll(medecinLabel, dateHeureLabel, statutBox, calendarBox);
        return row;
    }

    private Label createRowCell(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        label.setWrapText(true);
        return label;
    }

    private Button createActionButton(String text, String tooltipText, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClasses);
        if (tooltipText != null && !tooltipText.isBlank()) {
            button.setTooltip(new Tooltip(tooltipText));
        }
        return button;
    }

    private void selectionnerDisponibiliteSiVisible(int disponibiliteId) {
        for (Disponibilite disponibilite : creneauxReservables) {
            if (disponibilite.getId() == disponibiliteId) {
                selectedDisponibilite = disponibilite;
                renderCreneauxCards(creneauxReservables);
                break;
            }
        }
    }

    private void ouvrirPreferences() {
        ViewAlertUtil.info("Mes preferences", "La gestion des preferences patient sera integree ici.");
    }

    private void retournerAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) retourButton.getScene().getWindow();
            stage.setTitle("MediLink - Espace Patient");
            stage.setScene(new Scene(root, 1000, 680));
            stage.centerOnScreen();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Navigation", "Impossible de revenir a l'accueil : " + e.getMessage());
        }
    }

    private void mettreAJourRecommandation() {
        if (recommandationLabel == null) {
            return;
        }
        if (creneauxReservables.isEmpty()) {
            recommandationLabel.setText("Aucune recommandation disponible pour le moment.");
            return;
        }
        Disponibilite first = creneauxReservables.get(0);
        recommandationLabel.setText("Creneau recommande: " + resolveMedecinName(first) + " le " + formatDate(first) + " a " + formatTime(first.getHeureDebut()) + ".");
    }

    private boolean isTresDemande(Disponibilite disponibilite) {
        if (disponibilite == null || disponibilite.getDate() == null) {
            return false;
        }
        return disponibilite.getDate().isEqual(java.time.LocalDate.now().plusDays(1));
    }

    private String resolveMedecinName(Disponibilite disponibilite) {
        if (disponibilite == null || disponibilite.getMedecin() == null || disponibilite.getMedecin().getFullName() == null
                || disponibilite.getMedecin().getFullName().isBlank()) {
            return "Medecin indisponible";
        }
        return disponibilite.getMedecin().getFullName();
    }

    private String formatDate(Disponibilite disponibilite) {
        return disponibilite != null && disponibilite.getDate() != null ? disponibilite.getDate().format(DATE_FMT) : "-";
    }

    private String formatTimeRange(Disponibilite disponibilite) {
        if (disponibilite == null) {
            return "-";
        }
        return formatTime(disponibilite.getHeureDebut()) + " - " + formatTime(disponibilite.getHeureFin());
    }

    private String formatTime(java.time.LocalTime time) {
        return time != null ? time.format(TIME_FMT) : "-";
    }

    private String formatDateHeure(LocalDateTime dt) {
        return dt == null ? "-" : DATE_HEURE_FMT.format(dt);
    }

    private String formatStatut(String statut) {
        if (statut == null || statut.isBlank()) {
            return "Inconnu";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "En attente";
            case RendezVous.CONFIRME -> "Confirme";
            case RendezVous.TERMINE -> "Termine";
            case RendezVous.ANNULE -> "Annule";
            default -> statut;
        };
    }

    private String statusClassFor(String statut) {
        if (statut == null) {
            return "status-default";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "status-waiting";
            case RendezVous.CONFIRME -> "status-confirmed";
            case RendezVous.TERMINE -> "status-done";
            case RendezVous.ANNULE -> "status-cancelled";
            default -> "status-default";
        };
    }

    private Integer chargerConfiguredPatientId() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            String value = properties.getProperty("app.demo.patientId");
            if (value == null || value.isBlank()) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resoudrePatientId() {
        Integer fromSession = SessionContext.getCurrentPatientId();
        if (fromSession != null) {
            return fromSession;
        }
        Integer configuredId = chargerConfiguredPatientId();
        try {
            if (configuredId != null) {
                User configuredUser = userService.findById(configuredId).orElse(null);
                if (configuredUser != null && isPatientUser(configuredUser)) {
                    SessionContext.setCurrentPatientId(configuredId);
                    return configuredId;
                }
            }
            User fallbackUser = userService.findFirstByRole(User.ROLE_PATIENT).orElse(null);
            if (fallbackUser == null) {
                fallbackUser = userService.findFirstByRole("ROLE_USER").orElse(null);
            }
            Integer fallback = fallbackUser != null ? fallbackUser.getId() : null;
            SessionContext.setCurrentPatientId(fallback);
            return fallback;
        } catch (ServiceException e) {
            return null;
        }
    }

    private boolean isPatientUser(User user) {
        return user != null && (user.hasRole(User.ROLE_PATIENT) || user.hasRole("USER"));
    }
}
