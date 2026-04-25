package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Disponibilite;
import models.RendezVous;
import services.AppointmentMailerService;
import userfx.LoginController;
import userfx.User;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ListeDisponibilitesViewController {

    private static final String STATUT_TOUS = "Tous les statuts";
    private static final String LABEL_STATUT_TOUS = "Tous";
    private static final Map<String, String> STATUT_FILTERS = createStatutFilters();
    private static final String STATUT_DISPO_TOUS = "Tous les statuts";
    private static final String LABEL_STATUT_DISPO_TOUS = "Tous";
    private static final Map<String, String> DISPONIBILITE_FILTERS = createDisponibiliteFilters();
    private static final DateTimeFormatter RDV_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private DatePicker filtreDatePicker;
    @FXML
    private Button rafraichirButton;
    @FXML
    private Button ajouterButton;
    @FXML
    private Button retourButton;
    @FXML
    private TextField rendezVousSearchField;
    @FXML
    private ComboBox<String> rendezVousStatutComboBox;
    @FXML
    private Button triDateCroissantButton;
    @FXML
    private Button triDateDecroissantButton;
    @FXML
    private Button resetRendezVousButton;
    @FXML
    private VBox rendezVousListContainer;
    @FXML
    private Label rendezVousCountLabel;
    @FXML
    private ComboBox<String> disponibiliteStatutComboBox;
    @FXML
    private Button triDisponibiliteCroissantButton;
    @FXML
    private Button triDisponibiliteDecroissantButton;
    @FXML
    private Button resetDisponibiliteButton;
    @FXML
    private VBox disponibiliteListContainer;
    @FXML
    private Label disponibiliteCountLabel;

    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private Integer medecinIdContexte;
    private final List<RendezVous> rendezVousSource = new ArrayList<>();
    private final List<Disponibilite> disponibiliteSource = new ArrayList<>();
    private SortDirection rendezVousSortDirection = SortDirection.DESC;
    private SortDirection disponibiliteSortDirection = SortDirection.ASC;

    private enum SortDirection {
        ASC,
        DESC
    }

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
            rendezVousController = new RendezVousController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de donnees", e.formatWithCauses());
            return;
        }
        medecinIdContexte = resolveCurrentMedecinId();

        configureRendezVousToolbar();
        configureDisponibiliteToolbar();

        if (rafraichirButton != null) {
            rafraichirButton.setOnAction(e -> chargerRendezVous());
        }
        if (ajouterButton != null) {
            ajouterButton.setOnAction(e -> ouvrirFenetreDisponibilite(null));
        }
        if (retourButton != null) {
            retourButton.setOnAction(e -> retournerAccueil());
        }

        chargerDonnees();
    }

    private void configureRendezVousToolbar() {
        if (rendezVousStatutComboBox != null) {
            rendezVousStatutComboBox.getItems().setAll(STATUT_FILTERS.keySet());
            rendezVousStatutComboBox.setValue(LABEL_STATUT_TOUS);
            rendezVousStatutComboBox.setOnAction(e -> appliquerFiltresRendezVous());
        }
        if (rendezVousSearchField != null) {
            rendezVousSearchField.textProperty().addListener((obs, oldV, newV) -> appliquerFiltresRendezVous());
        }
        if (triDateCroissantButton != null) {
            triDateCroissantButton.setOnAction(e -> {
                rendezVousSortDirection = SortDirection.ASC;
                appliquerFiltresRendezVous();
            });
        }
        if (triDateDecroissantButton != null) {
            triDateDecroissantButton.setOnAction(e -> {
                rendezVousSortDirection = SortDirection.DESC;
                appliquerFiltresRendezVous();
            });
        }
        if (resetRendezVousButton != null) {
            resetRendezVousButton.setOnAction(e -> resetRendezVousToolbar());
        }
    }

    private void configureDisponibiliteToolbar() {
        if (disponibiliteStatutComboBox != null) {
            disponibiliteStatutComboBox.getItems().setAll(DISPONIBILITE_FILTERS.keySet());
            disponibiliteStatutComboBox.setValue(LABEL_STATUT_DISPO_TOUS);
            disponibiliteStatutComboBox.setOnAction(e -> appliquerFiltresDisponibilites());
        }
        if (triDisponibiliteCroissantButton != null) {
            triDisponibiliteCroissantButton.setOnAction(e -> {
                disponibiliteSortDirection = SortDirection.ASC;
                appliquerFiltresDisponibilites();
            });
        }
        if (triDisponibiliteDecroissantButton != null) {
            triDisponibiliteDecroissantButton.setOnAction(e -> {
                disponibiliteSortDirection = SortDirection.DESC;
                appliquerFiltresDisponibilites();
            });
        }
        if (resetDisponibiliteButton != null) {
            resetDisponibiliteButton.setOnAction(e -> resetDisponibiliteToolbar());
        }
    }

    private void chargerDonnees() {
        chargerDisponibilites();
        chargerRendezVous();
    }

    private void chargerDisponibilites() {
        if (disponibiliteController == null || disponibiliteListContainer == null) {
            return;
        }
        try {
            disponibiliteSource.clear();
            if (medecinIdContexte != null) {
                disponibiliteSource.addAll(disponibiliteController.afficherDisponibilitesMedecin(medecinIdContexte));
            }
            appliquerFiltresDisponibilites();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void chargerRendezVous() {
        if (rendezVousController == null || rendezVousListContainer == null) {
            return;
        }
        try {
            rendezVousSource.clear();
            if (medecinIdContexte != null) {
                rendezVousSource.addAll(rendezVousController.listerPourMedecin(medecinIdContexte));
            }
            appliquerFiltresRendezVous();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Rendez-vous", e.formatWithCauses());
        }
    }

    private void appliquerFiltresRendezVous() {
        if (rendezVousListContainer == null) {
            return;
        }

        String recherche = rendezVousSearchField != null && rendezVousSearchField.getText() != null
                ? rendezVousSearchField.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        String statutLabel = rendezVousStatutComboBox != null ? rendezVousStatutComboBox.getValue() : LABEL_STATUT_TOUS;
        String statutChoisi = resolveStatutFilterValue(statutLabel);

        Comparator<RendezVous> comparator = Comparator.comparing(
                RendezVous::getDateHeure,
                Comparator.nullsLast(LocalDateTime::compareTo)
        );
        if (rendezVousSortDirection == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        List<RendezVous> filtered = rendezVousSource.stream()
                .filter(rdv -> matchesRecherche(rdv, recherche))
                .filter(rdv -> matchesStatut(rdv, statutChoisi))
                .sorted(comparator)
                .collect(Collectors.toList());

        renderRendezVous(filtered);
    }

    private void appliquerFiltresDisponibilites() {
        if (disponibiliteListContainer == null) {
            return;
        }

        String statutLabel = disponibiliteStatutComboBox != null ? disponibiliteStatutComboBox.getValue() : LABEL_STATUT_DISPO_TOUS;
        String statutChoisi = resolveDisponibiliteFilterValue(statutLabel);

        Comparator<Disponibilite> comparator = Comparator
                .comparing(Disponibilite::getDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(Disponibilite::getHeureDebut, Comparator.nullsLast(java.time.LocalTime::compareTo));
        if (disponibiliteSortDirection == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        List<Disponibilite> filtered = disponibiliteSource.stream()
                .filter(dispo -> matchesDisponibiliteStatut(dispo, statutChoisi))
                .sorted(comparator)
                .collect(Collectors.toList());

        renderDisponibilites(filtered);
    }

    private boolean matchesRecherche(RendezVous rdv, String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return true;
        }
        String patient = rdv != null && rdv.getPatient() != null && rdv.getPatient().getFullName() != null
                ? rdv.getPatient().getFullName().toLowerCase(Locale.ROOT)
                : "";
        String motif = rdv != null && rdv.getMotif() != null
                ? rdv.getMotif().toLowerCase(Locale.ROOT)
                : "";
        return patient.contains(recherche) || motif.contains(recherche);
    }

    private boolean matchesStatut(RendezVous rdv, String statutChoisi) {
        if (statutChoisi == null || STATUT_TOUS.equals(statutChoisi)) {
            return true;
        }
        return rdv != null && statutChoisi.equalsIgnoreCase(rdv.getStatut());
    }

    private boolean matchesDisponibiliteStatut(Disponibilite disponibilite, String statutChoisi) {
        if (statutChoisi == null || STATUT_DISPO_TOUS.equals(statutChoisi)) {
            return true;
        }
        return disponibilite != null && statutChoisi.equalsIgnoreCase(resolveDisponibiliteEffectiveStatus(disponibilite));
    }

    private void renderRendezVous(List<RendezVous> rendezVousList) {
        rendezVousListContainer.getChildren().clear();

        int count = rendezVousList != null ? rendezVousList.size() : 0;
        if (rendezVousCountLabel != null) {
            rendezVousCountLabel.setText(count + (count > 1 ? " rendez-vous" : " rendez-vous"));
        }

        if (rendezVousList == null || rendezVousList.isEmpty()) {
            Label empty = new Label(resolveRendezVousEmptyMessage());
            empty.getStyleClass().add("rdv-empty-state");
            rendezVousListContainer.getChildren().add(empty);
            return;
        }

        for (RendezVous rdv : rendezVousList) {
            rendezVousListContainer.getChildren().add(createRendezVousRow(rdv));
        }
    }

    private HBox createRendezVousRow(RendezVous rdv) {
        HBox row = new HBox(14);
        row.getStyleClass().add("rdv-row");

        Label dateLabel = createRowCell(formatDateHeure(rdv.getDateHeure()), "rdv-cell-label", "rdv-col-date");
        Label patientLabel = createRowCell(resolvePatientName(rdv), "rdv-cell-label", "rdv-col-patient");
        Label motifLabel = createRowCell(resolveMotif(rdv), "rdv-cell-label", "rdv-col-motif");

        Label statutBadge = new Label(formatStatut(rdv.getStatut()));
        statutBadge.getStyleClass().addAll("status-badge", statusClassFor(rdv.getStatut()));
        HBox statutBox = new HBox(statutBadge);
        statutBox.getStyleClass().add("rdv-col-status");

        HBox actionsBox = new HBox(8);
        actionsBox.getStyleClass().addAll("rdv-actions-box", "rdv-col-actions");

        Button calendrierButton = createActionButton("📅", "Ajouter au calendrier",
                "secondary-button", "rdv-action-button", "icon-action-button", "calendar-action");
        calendrierButton.setOnAction(e ->
                ViewAlertUtil.info("Calendrier", "Placeholder calendrier pour le rendez-vous du " + formatDateHeure(rdv.getDateHeure()) + "."));

        Button confirmerButton = createActionButton("✓", "Confirmer",
                "secondary-button", "rdv-action-button", "icon-action-button", "confirm-action");
        confirmerButton.setDisable(!RendezVous.EN_ATTENTE.equals(rdv.getStatut()));
        confirmerButton.setOnAction(e -> {
            executerActionRendezVous(
                    "Confirmation",
                    "Confirmer ce rendez-vous ?",
                    () -> rendezVousController.confirmerRendezVous(rdv.getId(), medecinIdContexte),
                    "Rendez-vous confirme."
            );
            try {
                new AppointmentMailerService().sendConfirmationEmail(rdv);
            } catch (Exception ignored) {}
        });

        Button terminerButton = createActionButton("🏁", "Marquer termine",
                "secondary-button", "rdv-action-button", "icon-action-button", "done-action");
        terminerButton.setDisable(
                !RendezVous.EN_ATTENTE.equals(rdv.getStatut())
                        && !RendezVous.CONFIRME.equals(rdv.getStatut())
        );
        terminerButton.setOnAction(e -> executerActionRendezVous(
                "Cloture",
                "Marquer ce rendez-vous comme termine ?",
                () -> rendezVousController.terminerRendezVous(rdv.getId(), medecinIdContexte),
                "Rendez-vous marque comme termine."
        ));

        Button annulerButton = createActionButton("✕", "Annuler",
                "danger-button", "rdv-action-button", "icon-action-button", "cancel-action");
        annulerButton.setDisable(RendezVous.TERMINE.equals(rdv.getStatut()));
        annulerButton.setOnAction(e -> {
            executerActionRendezVous(
                    "Annulation",
                    "Annuler ce rendez-vous ? La disponibilite sera liberee si la logique metier le prevoit.",
                    () -> rendezVousController.annulerRendezVous(rdv.getId()),
                    "Rendez-vous annule."
            );
            try {
                new AppointmentMailerService().sendCancellationEmail(rdv);
            } catch (Exception ignored) {}
        });

        actionsBox.getChildren().addAll(calendrierButton, confirmerButton, terminerButton, annulerButton);
        row.getChildren().addAll(dateLabel, patientLabel, motifLabel, statutBox, actionsBox);
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

    private void executerActionRendezVous(
            String titre,
            String confirmation,
            RendezVousCommand command,
            String succesMessage
    ) {
        if (medecinIdContexte == null) {
            ViewAlertUtil.erreur(titre, "Aucun medecin connecte dans le contexte de session.");
            return;
        }
        if (!ViewAlertUtil.confirmer(titre, confirmation)) {
            return;
        }
        try {
            command.run();
            chargerDonnees();
            ViewAlertUtil.info(titre, succesMessage);

        } catch (ServiceException e) {
            ViewAlertUtil.erreur(titre, e.formatWithCauses());
        }
    }

    private void resetRendezVousToolbar() {
        if (rendezVousSearchField != null) {
            rendezVousSearchField.clear();
        }
        if (rendezVousStatutComboBox != null) {
            rendezVousStatutComboBox.setValue(LABEL_STATUT_TOUS);
        }
        rendezVousSortDirection = SortDirection.DESC;
        appliquerFiltresRendezVous();
    }

    private void resetDisponibiliteToolbar() {
        if (disponibiliteStatutComboBox != null) {
            disponibiliteStatutComboBox.setValue(LABEL_STATUT_DISPO_TOUS);
        }
        disponibiliteSortDirection = SortDirection.ASC;
        appliquerFiltresDisponibilites();
    }

    private void renderDisponibilites(List<Disponibilite> disponibilites) {
        disponibiliteListContainer.getChildren().clear();

        int count = disponibilites != null ? disponibilites.size() : 0;
        if (disponibiliteCountLabel != null) {
            disponibiliteCountLabel.setText(count + (count > 1 ? " disponibilites" : " disponibilite"));
        }

        if (disponibilites == null || disponibilites.isEmpty()) {
            Label empty = new Label(resolveDisponibiliteEmptyMessage());
            empty.getStyleClass().add("rdv-empty-state");
            disponibiliteListContainer.getChildren().add(empty);
            return;
        }

        for (Disponibilite disponibilite : disponibilites) {
            disponibiliteListContainer.getChildren().add(createDisponibiliteRow(disponibilite));
        }
    }

    private HBox createDisponibiliteRow(Disponibilite disponibilite) {
        HBox row = new HBox(14);
        row.getStyleClass().add("rdv-row");

        Label dateLabel = createRowCell(formatDate(disponibilite.getDate()), "rdv-cell-label", "dispo-col-date");
        Label debutLabel = createRowCell(formatTime(disponibilite.getHeureDebut()), "rdv-cell-label", "dispo-col-start");
        Label finLabel = createRowCell(formatTime(disponibilite.getHeureFin()), "rdv-cell-label", "dispo-col-end");

        Label statutBadge = new Label(formatDisponibiliteStatut(disponibilite));
        statutBadge.getStyleClass().addAll("status-badge", disponibiliteStatusClassFor(disponibilite));
        HBox statutBox = new HBox(statutBadge);
        statutBox.getStyleClass().add("dispo-col-status");

        HBox actionsBox = new HBox(8);
        actionsBox.getStyleClass().addAll("rdv-actions-box", "dispo-col-actions");

        if (isDisponibiliteEditable(disponibilite)) {
            Button modifierButton = createActionButton("\u270E", "Modifier",
                    "secondary-button", "rdv-action-button", "icon-action-button", "edit-action");
            modifierButton.setOnAction(e -> ouvrirFenetreDisponibilite(disponibilite));

            Button supprimerButton = createActionButton("\uD83D\uDDD1", "Supprimer",
                    "danger-button", "rdv-action-button", "icon-action-button", "cancel-action");
            supprimerButton.setOnAction(e -> supprimerDisponibilite(disponibilite));

            actionsBox.getChildren().addAll(modifierButton, supprimerButton);
        } else {
            Label reserveeLabel = new Label("Reserve");
            reserveeLabel.getStyleClass().addAll("rdv-cell-label", "dispo-reserved-text");
            actionsBox.getChildren().add(reserveeLabel);
        }

        row.getChildren().addAll(dateLabel, debutLabel, finLabel, statutBox, actionsBox);
        return row;
    }

    private void ouvrirFenetreDisponibilite(Disponibilite disponibilite) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AjouterDisponibiliteView.fxml"));
            Parent root = loader.load();
            AjouterDisponibiliteViewController ctrl = loader.getController();
            ctrl.setMedecinIdContexte(medecinIdContexte);
            if (disponibilite != null) {
                ctrl.setDisponibiliteAEditer(disponibilite);
            }
            ctrl.setAfterSaveCallback(this::chargerDonnees);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(ajouterButton.getScene().getWindow());
            stage.setTitle(disponibilite == null ? "Ajouter une disponibilite" : "Modifier une disponibilite");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void supprimerDisponibilite(Disponibilite disponibilite) {
        if (disponibilite == null) {
            return;
        }
        if (!isDisponibiliteEditable(disponibilite)) {
            ViewAlertUtil.erreur("Suppression", "Cette disponibilite est reservee et ne peut pas etre supprimee.");
            return;
        }
        if (!ViewAlertUtil.confirmer("Suppression", "Supprimer la disponibilite n " + disponibilite.getId() + " ?")) {
            return;
        }
        try {
            disponibiliteController.supprimerDisponibilite(disponibilite.getId());
            chargerDonnees();
            ViewAlertUtil.info("Suppression", "Disponibilite supprimee.");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Suppression", e.formatWithCauses());
        }
    }

    private void retournerAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) retourButton.getScene().getWindow();
            stage.setTitle("MediLink - Espace Medecin");
            stage.setScene(new Scene(root, 960, 640));
            stage.centerOnScreen();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible de revenir a l'accueil : " + e.getMessage());
        }
    }

    private String resolvePatientName(RendezVous rdv) {
        if (rdv == null || rdv.getPatient() == null || rdv.getPatient().getFullName() == null || rdv.getPatient().getFullName().isBlank()) {
            return "-";
        }
        return rdv.getPatient().getFullName();
    }

    private String resolveMotif(RendezVous rdv) {
        if (rdv == null || rdv.getMotif() == null || rdv.getMotif().isBlank()) {
            return "-";
        }
        return rdv.getMotif();
    }

    private String formatDateHeure(LocalDateTime dateHeure) {
        return dateHeure != null ? dateHeure.format(RDV_FORMATTER) : "-";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "-";
    }

    private String formatTime(java.time.LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : "-";
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

    private String formatDisponibiliteStatut(Disponibilite disponibilite) {
        String statut = resolveDisponibiliteEffectiveStatus(disponibilite);
        if (Disponibilite.STATUS_RESERVEE.equals(statut)) {
            return "Reservee";
        }
        return "Libre";
    }

    private String disponibiliteStatusClassFor(Disponibilite disponibilite) {
        return isDisponibiliteEditable(disponibilite) ? "status-available" : "status-reserved";
    }

    private boolean isDisponibiliteEditable(Disponibilite disponibilite) {
        return disponibilite != null && disponibilite.getRendezVous() == null;
    }

    private String resolveDisponibiliteEffectiveStatus(Disponibilite disponibilite) {
        if (disponibilite == null) {
            return "";
        }
        if (disponibilite.getRendezVous() != null) {
            return Disponibilite.STATUS_RESERVEE;
        }
        String statut = disponibilite.getStatus();
        if (statut == null || statut.isBlank()) {
            return Disponibilite.STATUS_LIBRE;
        }
        String normalized = statut.toUpperCase(Locale.ROOT);
        if ("RESERVEE".equals(normalized)) {
            return Disponibilite.STATUS_RESERVEE;
        }
        if ("LIBRE".equals(normalized)) {
            return Disponibilite.STATUS_LIBRE;
        }
        return normalized;
    }

    private String resolveStatutFilterValue(String statutLabel) {
        if (statutLabel == null || statutLabel.isBlank()) {
            return STATUT_TOUS;
        }
        return STATUT_FILTERS.getOrDefault(statutLabel, STATUT_TOUS);
    }

    private String resolveDisponibiliteFilterValue(String statutLabel) {
        if (statutLabel == null || statutLabel.isBlank()) {
            return STATUT_DISPO_TOUS;
        }
        return DISPONIBILITE_FILTERS.getOrDefault(statutLabel, STATUT_DISPO_TOUS);
    }

    private String resolveRendezVousEmptyMessage() {
        if (medecinIdContexte == null) {
            return "Aucun medecin connecte: impossible de charger les rendez-vous.";
        }
        return "Aucun rendez-vous trouve pour les filtres actuels.";
    }

    private String resolveDisponibiliteEmptyMessage() {
        if (medecinIdContexte == null) {
            return "Aucun medecin connecte: impossible de charger les disponibilites.";
        }
        return "Aucune disponibilite trouvee pour les filtres actuels.";
    }

    private static Map<String, String> createStatutFilters() {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put(LABEL_STATUT_TOUS, STATUT_TOUS);
        filters.put("En attente", RendezVous.EN_ATTENTE);
        filters.put("Confirme", RendezVous.CONFIRME);
        filters.put("Termine", RendezVous.TERMINE);
        return filters;
    }

    private static Map<String, String> createDisponibiliteFilters() {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put(LABEL_STATUT_DISPO_TOUS, STATUT_DISPO_TOUS);
        filters.put("Libre", Disponibilite.STATUS_LIBRE);
        filters.put("Reservee", Disponibilite.STATUS_RESERVEE);
        return filters;
    }

    private Integer resolveCurrentMedecinId() {
        Integer fromSession = SessionContext.getCurrentMedecinId();
        if (fromSession != null) {
            return fromSession;
        }

        User loggedUser = LoginController.getLoggedInUser();
        if (loggedUser == null) {
            return null;
        }

        String roles = loggedUser.getRoles() != null ? loggedUser.getRoles().toUpperCase(Locale.ROOT) : "";
        if (roles.contains("ROLE_MEDECIN") || roles.contains("MEDECIN")) {
            SessionContext.setCurrentMedecinId(loggedUser.getId());
            SessionContext.setCurrentPatientId(null);
            return loggedUser.getId();
        }
        return null;
    }

    @FunctionalInterface
    private interface RendezVousCommand {
        void run() throws ServiceException;
    }
}
