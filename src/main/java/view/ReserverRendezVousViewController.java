package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.AIRecommendation;
import models.Disponibilite;
import models.RendezVous;
import models.User;
import services.AppointmentMailerService;
import services.DisponibiliteService;
import services.PlanningAIService;
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

    private static final String CARD_BASE_STYLE =
            "-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16; "
                    + "-fx-border-color: #e0e0e0; -fx-border-width: 0.5; -fx-padding: 16;";
    private static final String CARD_SELECTED_STYLE =
            "-fx-background-color: #f7faff; -fx-background-radius: 16; -fx-border-radius: 16; "
                    + "-fx-border-color: #2d6ecf; -fx-border-width: 1.5; -fx-padding: 16;";
    private static final String META_CHIP_STYLE =
            "-fx-background-color: #eef4ff; -fx-text-fill: #0c447c; -fx-background-radius: 10; "
                    + "-fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: 700;";
    private static final String ICON_BUTTON_PRIMARY_STYLE =
            "-fx-background-color: #185FA5; -fx-text-fill: white; -fx-background-radius: 8; "
                    + "-fx-padding: 8 14; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent;";
    private static final String ICON_BUTTON_SECONDARY_STYLE =
            "-fx-background-color: #534AB7; -fx-text-fill: white; -fx-background-radius: 8; "
                    + "-fx-padding: 8 14; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent;";
    private static final String ICON_BUTTON_DANGER_STYLE =
            "-fx-background-color: #d54836; -fx-text-fill: white; -fx-background-radius: 8; "
                    + "-fx-padding: 8 14; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent;";

    @FXML private TextField motifTextField;
    @FXML private FlowPane creneauxListContainer;
    @FXML private VBox disponibilitesEmptyBox;
    @FXML private Button retourButton;
    @FXML private Button mesRendezVousButton;
    @FXML private Button btnTrouverIA;
    @FXML private FlowPane mesRendezVousListContainer;
    @FXML private Label disponibilitesCountLabel;
    @FXML private Label mesRendezVousCountLabel;
    @FXML private Label recommandationLabel;
    @FXML private Label labelResultatIA;
    @FXML private ComboBox<String> comboPreferenceHoraire;
    @FXML private ComboBox<String> comboUrgence;

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

        System.out.println("[SESSION][Reserver.initialize] currentPatientId(SessionContext)="
                + SessionContext.getCurrentPatientId());
        demoPatientId = resoudrePatientId();
        System.out.println("[SESSION][Reserver.initialize] demoPatientId(resolu)=" + demoPatientId);

        if (retourButton != null) {
            retourButton.setOnAction(e -> retournerAccueil());
        }
        if (mesRendezVousButton != null) {
            mesRendezVousButton.setOnAction(e -> ouvrirPreferences());
        }
        if (comboPreferenceHoraire != null) {
            comboPreferenceHoraire.getItems().addAll("matin", "apres-midi");
            comboPreferenceHoraire.setValue(resolvePatientPreferenceHoraire());
        }
        if (comboUrgence != null) {
            comboUrgence.getItems().addAll("normale", "moderee", "haute");
            comboUrgence.setValue("normale");
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
            try {
                new AppointmentMailerService().sendReservationEmail(rdv);
            } catch (Exception ignored) {}
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

    @FXML
    private void handleTrouverAvecIA() {
        System.out.println("Bouton IA cliqué");
        System.out.println("[SESSION][Reserver.handleTrouverAvecIA] currentPatientId(SessionContext)="
                + SessionContext.getCurrentPatientId()
                + " | demoPatientId=" + demoPatientId);

        List<Disponibilite> creneaux;
        try {
            DisponibiliteService disponibiliteService = new DisponibiliteService();
            creneaux = disponibiliteService.listerReservables();
        } catch (ServiceException e) {
            if (labelResultatIA != null) {
                labelResultatIA.setText("Impossible de charger les creneaux disponibles.");
            }
            ViewAlertUtil.erreur("Disponibilites", e.formatWithCauses());
            return;
        }

        if (creneaux == null || creneaux.isEmpty()) {
            labelResultatIA.setText("Aucun creneau disponible pour le moment.");
            return;
        }

        btnTrouverIA.setDisable(true);
        labelResultatIA.setText("L'IA analyse les creneaux...");

        final User patientIA = resolvePatientForIA();
        if (patientIA == null) {
            btnTrouverIA.setDisable(false);
            labelResultatIA.setText("Aucun patient connecte. Connectez-vous puis reessayez.");
            return;
        }

        final String prenomPatient = resolvePatientDisplayName(patientIA);
        final String preferenceHoraire = comboPreferenceHoraire != null && comboPreferenceHoraire.getValue() != null
                ? comboPreferenceHoraire.getValue()
                : resolvePatientPreferenceHoraireForAI(patientIA);
        final String urgenceDeclaree = comboUrgence != null && comboUrgence.getValue() != null
                ? comboUrgence.getValue()
                : "normale";
        final List<RendezVous> historique = loadHistoriquePatientIA();

        Task<AIRecommendation> task = new Task<>() {
            @Override
            protected AIRecommendation call() {
                PlanningAIService aiService = new PlanningAIService();
                return aiService.recommanderCreneau(
                        prenomPatient,
                        preferenceHoraire,
                        urgenceDeclaree,
                        creneaux,
                        historique
                );
            }
        };

        task.setOnSucceeded(event -> {
            AIRecommendation result = task.getValue();
            btnTrouverIA.setDisable(false);

            if (result == null) {
                labelResultatIA.setText("Une erreur inattendue s'est produite.");
                return;
            }

            if (result.isEchec()) {
                labelResultatIA.setText(result.getAlerte());
                return;
            }

            Disponibilite recommande = trouverCreneauParId(result.getCreneauRecommandeId());
            if (recommande == null) {
                labelResultatIA.setText("Creneau recommande introuvable. Reessaie.");
                return;
            }

            labelResultatIA.setText("");
            afficherPopupRecommandation(recommande, result);
        });

        task.setOnFailed(event -> {
            btnTrouverIA.setDisable(false);
            labelResultatIA.setText("Une erreur inattendue s'est produite.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void renderCreneauxCards(List<Disponibilite> disponibilites) {
        creneauxListContainer.getChildren().clear();
        boolean empty = disponibilites == null || disponibilites.isEmpty();
        disponibilitesEmptyBox.setVisible(empty);
        disponibilitesEmptyBox.setManaged(empty);
        if (empty) {
            return;
        }
        for (Disponibilite d : disponibilites) {
            creneauxListContainer.getChildren().add(createCreneauCard(d));
        }
    }

    private VBox createCreneauCard(Disponibilite disponibilite) {
        VBox card = new VBox(12);
        boolean isSelected = selectedDisponibilite != null && selectedDisponibilite.equals(disponibilite);
        card.setStyle(isSelected ? CARD_SELECTED_STYLE : CARD_BASE_STYLE);
        card.setPrefWidth(360);
        card.setMaxWidth(360);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = buildAvatar(resolveInitials(resolveMedecinName(disponibilite)), avatarBackgroundFor(disponibilite), "#27466f");
        Label title = new Label(resolveMedecinName(disponibilite));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label subtitle = new Label(formatDate(disponibilite) + " • " + formatTimeRange(disponibilite));
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7b8aa0;");
        VBox titleBox = new VBox(2, title, subtitle);
        header.getChildren().addAll(avatar, titleBox);

        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getChildren().addAll(
                buildMetaChip("MEDECIN", resolveShortRoleLabel(disponibilite)),
                buildStatusChip(isTresDemande(disponibilite) ? "TRES DEMANDE" : "DISPONIBLE"),
                buildSelectionChip(isSelected ? "CHOISI" : "LIBRE")
        );

        VBox details = new VBox(6);
        details.getChildren().addAll(
                buildSimpleInfo("Patient", resolvePatientDisplayName()),
                buildSimpleInfo("Preference", comboPreferenceHoraire != null && comboPreferenceHoraire.getValue() != null
                        ? comboPreferenceHoraire.getValue()
                        : resolvePatientPreferenceHoraire()),
                buildSimpleInfo("Creneau", "#" + disponibilite.getId())
        );

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button chooseButton = new Button("Choisir");
        chooseButton.setStyle(ICON_BUTTON_SECONDARY_STYLE);
        chooseButton.setOnAction(e -> ViewAlertUtil.info(
                "Disponibilite",
                "Medecin : " + resolveMedecinName(disponibilite) + "\n"
                        + "Date : " + formatDate(disponibilite) + "\n"
                        + "Heure : " + formatTimeRange(disponibilite) + "\n"
                        + "Statut : " + (isTresDemande(disponibilite) ? "Tres demande" : "Disponible")
        ));

        Button editButton = new Button("Selectionner");
        editButton.setStyle(ICON_BUTTON_PRIMARY_STYLE);
        editButton.setOnAction(e -> {
            selectedDisponibilite = disponibilite;
            renderCreneauxCards(creneauxReservables);
        });

        Button reserveButton = new Button("Reserver");
        reserveButton.setStyle(ICON_BUTTON_DANGER_STYLE);
        reserveButton.setOnAction(e -> {
            selectedDisponibilite = disponibilite;
            renderCreneauxCards(creneauxReservables);
            handleReserver();
        });

        actions.getChildren().addAll(chooseButton, editButton, reserveButton);
        card.getChildren().addAll(header, separator, chips, details, actions);
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
                mesRendezVousCountLabel.setText(count + " rendez-vous");
            }
            if (rdvs.isEmpty()) {
                renderMesRendezVousEmpty("Aucun rendez-vous trouve.");
                return;
            }
            for (RendezVous rdv : rdvs) {
                mesRendezVousListContainer.getChildren().add(createRendezVousCard(rdv));
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

    private VBox createRendezVousCard(RendezVous rdv) {
        VBox card = new VBox(12);
        card.setStyle(CARD_BASE_STYLE);
        card.setPrefWidth(360);
        card.setMaxWidth(360);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = buildAvatar(resolveInitials(resolveMedecinName(rdv.getDisponibilite())), avatarBackgroundFor(rdv), "#4b4b4b");
        Label title = new Label(resolveMedecinName(rdv.getDisponibilite()));
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label subtitle = new Label(formatDateHeure(rdv.getDateHeure()));
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7b8aa0;");
        VBox titleBox = new VBox(2, title, subtitle);
        header.getChildren().addAll(avatar, titleBox);

        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        chips.getChildren().addAll(
                buildMetaChip("RDV", scenarioLabelFor(rdv.getStatut())),
                buildStatusChip(formatStatut(rdv.getStatut()).toUpperCase(Locale.ROOT))
        );

        VBox details = new VBox(6);
        details.getChildren().addAll(
                buildSimpleInfo("Motif", resolveMotif(rdv)),
                buildSimpleInfo("Date", formatDateHeure(rdv.getDateHeure()))
        );

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);

        if (!RendezVous.ANNULE.equalsIgnoreCase(rdv.getStatut())) {
            Button calendrierButton = new Button("Calendrier");
            calendrierButton.setStyle(ICON_BUTTON_SECONDARY_STYLE);
            calendrierButton.setOnAction(e ->
                    ViewAlertUtil.info("Rendez-vous",
                            "Medecin : " + resolveMedecinName(rdv.getDisponibilite()) + "\n"
                                    + "Date : " + formatDateHeure(rdv.getDateHeure()) + "\n"
                                    + "Motif : " + resolveMotif(rdv)));
            actions.getChildren().add(calendrierButton);
        }

        if (RendezVous.EN_ATTENTE.equalsIgnoreCase(rdv.getStatut()) || RendezVous.CONFIRME.equalsIgnoreCase(rdv.getStatut())) {
            Button detailsButton = new Button("Details");
            detailsButton.setStyle(ICON_BUTTON_PRIMARY_STYLE);
            detailsButton.setOnAction(e -> ViewAlertUtil.info(
                    "Rendez-vous",
                    "Medecin : " + resolveMedecinName(rdv.getDisponibilite()) + "\n"
                            + "Date : " + formatDateHeure(rdv.getDateHeure()) + "\n"
                            + "Motif : " + resolveMotif(rdv) + "\n"
                            + "Statut : " + formatStatut(rdv.getStatut())
            ));
            actions.getChildren().add(detailsButton);
        }

        if (!RendezVous.TERMINE.equalsIgnoreCase(rdv.getStatut())) {
            Button calendrierButton = new Button("Annuler");
            calendrierButton.setStyle(ICON_BUTTON_DANGER_STYLE);
            calendrierButton.setOnAction(e ->
                    ViewAlertUtil.info("Calendrier", "Placeholder calendrier pour le rendez-vous du " + formatDateHeure(rdv.getDateHeure()) + "."));
            actions.getChildren().add(calendrierButton);
        }

        if (actions.getChildren().isEmpty()) {
            Button calendrierButton = new Button("Voir");
            calendrierButton.setStyle(ICON_BUTTON_SECONDARY_STYLE);
            calendrierButton.setOnAction(e ->
                    ViewAlertUtil.info("Rendez-vous", "Aucune action disponible pour ce scenario."));
            actions.getChildren().add(calendrierButton);
        }

        card.getChildren().addAll(header, separator, chips, details, actions);
        return card;
    }

    private Label buildMetaChip(String label, String value) {
        Label chip = new Label(value == null || value.isBlank() ? label : value.toUpperCase(Locale.ROOT));
        chip.setStyle(META_CHIP_STYLE);
        return chip;
    }

    private Label buildStatusChip(String text) {
        return buildPill(text, "#eef7e8", "#4f6500");
    }

    private Label buildSelectionChip(String text) {
        return buildPill(text, "#eef4ff", "#0c447c");
    }

    private Label buildPill(String text, String background, String color) {
        Label pill = new Label(text);
        pill.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + color + "; "
                + "-fx-background-radius: 10; -fx-padding: 4 10; -fx-font-size: 11px; -fx-font-weight: 700;");
        return pill;
    }

    private Label buildAvatar(String initials, String background, String color) {
        Label avatar = new Label(initials);
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(54, 54);
        avatar.setPrefSize(54, 54);
        avatar.setMaxSize(54, 54);
        avatar.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + color + "; "
                + "-fx-background-radius: 999; -fx-font-size: 16px; -fx-font-weight: bold;");
        return avatar;
    }

    private Label buildSimpleInfo(String label, String value) {
        Label info = new Label((value == null || value.isBlank() ? label : value));
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #7b8aa0;");
        return info;
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

    private Disponibilite trouverCreneauParId(String id) {
        if (creneauxReservables == null || id == null) {
            return null;
        }
        try {
            int idInt = Integer.parseInt(id.trim());
            return creneauxReservables.stream()
                    .filter(d -> d.getId() == idInt)
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void afficherPopupRecommandation(Disponibilite creneau, AIRecommendation result) {
        String medecin = creneau.getMedecin() != null
                ? creneau.getMedecin().getFullName()
                : "Medecin inconnu";

        String details = "Medecin   : " + medecin + "\n"
                + "Date      : " + formatDate(creneau) + "\n"
                + "Heure     : " + formatTimeRange(creneau) + "\n"
                + "Confiance : " + (int) (result.getNiveauConfiance() * 100) + "%\n\n"
                + "Justification :\n" + result.getJustification();

        Label titre = new Label("Recommandation IA");
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label contenu = new Label(details);
        contenu.setWrapText(true);
        contenu.setStyle("-fx-font-size: 13px;");

        Button btnReserver = new Button("Reserver ce creneau");
        btnReserver.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; "
                + "-fx-font-size: 13px; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        btnReserver.setGraphic(new Label("✔"));

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-font-size: 13px; -fx-padding: 8 20 8 20; -fx-cursor: hand;");

        HBox boutons = new HBox(12, btnReserver, btnAnnuler);
        boutons.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(16, titre, contenu, boutons);
        layout.setPadding(new Insets(24));
        layout.setStyle("-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12;");
        layout.setPrefWidth(420);

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Recommandation IA");
        popup.setScene(new Scene(layout));
        popup.setResizable(false);

        btnAnnuler.setOnAction(e -> popup.close());
        btnReserver.setOnAction(e -> {
            popup.close();
            selectedDisponibilite = creneau;
            handleReserver();
        });

        popup.show();
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
        recommandationLabel.setText("Creneau suggere : " + resolveMedecinName(first) + " le " + formatDate(first) + " a " + formatTime(first.getHeureDebut()) + ".");
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

    private String resolvePatientDisplayName() {
        User patient = resolvePatientForIA();
        return resolvePatientDisplayName(patient);
    }

    private String resolvePatientDisplayName(User patient) {
        if (patient == null) {
            return "Patient";
        }
        if (patient.getFullName() == null || patient.getFullName().isBlank()) {
            return "Patient";
        }
        String[] parts = patient.getFullName().trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "Patient";
    }

    private String resolveShortRoleLabel(Disponibilite disponibilite) {
        return disponibilite != null && disponibilite.getMedecin() != null ? "MEDECIN" : "USER";
    }

    private String resolvePatientPreferenceHoraire() {
        User patient = resolvePatientForIA();
        if (patient == null || patient.getPreferredTime() == null) {
            return "matin";
        }
        return patient.getPreferredTime().getHour() < 12 ? "matin" : "apres-midi";
    }

    private String resolveMotif(RendezVous rdv) {
        if (rdv == null || rdv.getMotif() == null || rdv.getMotif().isBlank()) {
            return "Motif non renseigne";
        }
        return rdv.getMotif();
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

    private String scenarioLabelFor(String statut) {
        if (statut == null) {
            return "USER";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "EN ATTENTE";
            case RendezVous.CONFIRME -> "CONFIRME";
            case RendezVous.TERMINE -> "TERMINE";
            case RendezVous.ANNULE -> "ANNULE";
            default -> "USER";
        };
    }

    private String statusBackgroundFor(String statut) {
        if (statut == null) {
            return "#eef4ff";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "#fff4dc";
            case RendezVous.CONFIRME -> "#e7f7ee";
            case RendezVous.TERMINE -> "#ece9ff";
            case RendezVous.ANNULE -> "#fdeaea";
            default -> "#eef4ff";
        };
    }

    private String statusTextColorFor(String statut) {
        if (statut == null) {
            return "#46658f";
        }
        return switch (statut.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "#8b5a00";
            case RendezVous.CONFIRME -> "#10643c";
            case RendezVous.TERMINE -> "#5a48a8";
            case RendezVous.ANNULE -> "#9f2f2f";
            default -> "#46658f";
        };
    }

    private String resolveInitials(String text) {
        if (text == null || text.isBlank()) {
            return "?";
        }
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String avatarBackgroundFor(Disponibilite disponibilite) {
        String[] colors = {"#E6F1FB", "#E1F5EE", "#F1EFE8", "#FBEAF0", "#FAEEDA"};
        String name = resolveMedecinName(disponibilite);
        return colors[Math.abs(name.hashCode()) % colors.length];
    }

    private String avatarBackgroundFor(RendezVous rdv) {
        String[] colors = {"#E6F1FB", "#E1F5EE", "#F1EFE8", "#FBEAF0", "#FAEEDA"};
        String name = resolveMedecinName(rdv != null ? rdv.getDisponibilite() : null);
        return colors[Math.abs(name.hashCode()) % colors.length];
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
            return null;
        } catch (ServiceException e) {
            return null;
        }
    }

    private User resolvePatientForIA() {
        if (demoPatientId == null) {
            System.out.println("[SESSION][Reserver.resolvePatientForIA] demoPatientId=null | currentPatientId(SessionContext)="
                    + SessionContext.getCurrentPatientId());
            return null;
        }
        try {
            User patient = userService.findById(demoPatientId).orElse(null);
            if (patient != null) {
                System.out.println("Patient IA utilise = "
                        + patient.getEmail() + " / "
                        + patient.getId() + " / "
                        + patient.getFullName());
            } else {
                System.out.println("[SESSION][Reserver.resolvePatientForIA] Aucun user trouve pour id=" + demoPatientId);
            }
            return patient;
        } catch (ServiceException e) {
            System.out.println("[SESSION][Reserver.resolvePatientForIA] Erreur chargement patient id="
                    + demoPatientId + " : " + e.getMessage());
            return null;
        }
    }

    private String resolvePatientPreferenceHoraireForAI(User patient) {
        if (patient == null || patient.getPreferredTime() == null) {
            return "non precisee";
        }
        return patient.getPreferredTime().getHour() < 12 ? "matin" : "apres-midi";
    }

    private List<RendezVous> loadHistoriquePatientIA() {
        if (demoPatientId == null) {
            return List.of();
        }
        try {
            return rendezVousController.listerPourPatient(demoPatientId);
        } catch (ServiceException e) {
            return List.of();
        }
    }

    private boolean isPatientUser(User user) {
        return user != null && (user.hasRole(User.ROLE_PATIENT) || user.hasRole("USER"));
    }
}
