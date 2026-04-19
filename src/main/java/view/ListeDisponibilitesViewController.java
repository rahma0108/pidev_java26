package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Disponibilite;
import models.RendezVous;
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
    private static final DateTimeFormatter RDV_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private Disponibilite selectionCourante;
    private Integer medecinIdContexte;
    private final List<RendezVous> rendezVousSource = new ArrayList<>();
    private SortDirection rendezVousSortDirection = SortDirection.DESC;

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

        rafraichirButton.setOnAction(e -> chargerDonnees());
        ajouterButton.setOnAction(e -> ouvrirFenetreAjout());
        supprimerButton.setOnAction(e -> supprimerSelection());
        retourButton.setOnAction(e -> retournerAccueil());

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

    private void chargerDonnees() {
        chargerDisponibilites();
        chargerRendezVous();
    }

    private void chargerDisponibilites() {
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
        confirmerButton.setOnAction(e -> executerActionRendezVous(
                "Confirmation",
                "Confirmer ce rendez-vous ?",
                () -> rendezVousController.confirmerRendezVous(rdv.getId(), medecinIdContexte),
                "Rendez-vous confirme."
        ));

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
        annulerButton.setOnAction(e -> executerActionRendezVous(
                "Annulation",
                "Annuler ce rendez-vous ? La disponibilite sera liberee si la logique metier le prevoit.",
                () -> rendezVousController.annulerRendezVous(rdv.getId()),
                "Rendez-vous annule."
        ));

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

    private void executerActionRendezVous(String titre, String confirmation, RendezVousCommand command, String succesMessage) {
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

    private void renderCards(List<Disponibilite> disponibilites) {
        cardsFlowPane.getChildren().clear();
        selectionCourante = null;
        selectionInfoLabel.setText("Aucune disponibilite selectionnee.");

        if (disponibilites == null || disponibilites.isEmpty()) {
            Label vide = new Label("Aucune disponibilite trouvee.");
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
        Label titre = new Label("Disponibilite #" + d.getId());
        titre.getStyleClass().add("card-title");

        Label date = new Label("Date: " + (d.getDate() != null ? d.getDate() : "-"));
        Label heure = new Label("Heure: "
                + (d.getHeureDebut() != null ? d.getHeureDebut() : "-")
                + " - "
                + (d.getHeureFin() != null ? d.getHeureFin() : "-"));
        Label statut = new Label("Statut: " + (d.getStatus() != null ? d.getStatus() : "-"));
        Label medecin = new Label("Medecin: "
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
            selectionInfoLabel.setText("Selection: disponibilite #" + d.getId());
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
                selected = l.getText().equals("Disponibilite #" + selectionCourante.getId());
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
            stage.setTitle("Ajouter une disponibilite");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Interface", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private void supprimerSelection() {
        if (selectionCourante == null) {
            ViewAlertUtil.erreur("Suppression", "Selectionnez une carte.");
            return;
        }
        if (!ViewAlertUtil.confirmer("Suppression", "Supprimer la disponibilite n " + selectionCourante.getId() + " ?")) {
            return;
        }
        try {
            disponibiliteController.supprimerDisponibilite(selectionCourante.getId());
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

    private String resolveStatutFilterValue(String statutLabel) {
        if (statutLabel == null || statutLabel.isBlank()) {
            return STATUT_TOUS;
        }
        return STATUT_FILTERS.getOrDefault(statutLabel, STATUT_TOUS);
    }

    private String resolveRendezVousEmptyMessage() {
        if (medecinIdContexte == null) {
            return "Aucun medecin connecte: impossible de charger les rendez-vous.";
        }
        return "Aucun rendez-vous trouve pour les filtres actuels.";
    }

    private static Map<String, String> createStatutFilters() {
        Map<String, String> filters = new LinkedHashMap<>();
        filters.put(LABEL_STATUT_TOUS, STATUT_TOUS);
        filters.put("En attente", RendezVous.EN_ATTENTE);
        filters.put("Confirme", RendezVous.CONFIRME);
        filters.put("Termine", RendezVous.TERMINE);
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
