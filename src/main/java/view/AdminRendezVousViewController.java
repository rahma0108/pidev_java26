package view;

import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Disponibilite;
import models.RendezVous;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AdminRendezVousViewController {

    private static final String STATUS_ALL = "Tous les statuts";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Button sortDateCroissantButton;
    @FXML private Button sortDateDecroissantButton;
    @FXML private Button resetButton;
    @FXML private Label countLabel;
    @FXML private FlowPane rowsContainer;

    private RendezVousController rendezVousController;
    private final List<RendezVous> source = new ArrayList<>();
    private final Map<String, String> statusMapping = buildStatusMapping();
    private boolean dateAscending = false;

    @FXML
    private void initialize() {
        try {
            rendezVousController = new RendezVousController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Rendez-vous", e.formatWithCauses());
            return;
        }

        configureToolbar();
        refreshList();
    }

    private void configureToolbar() {
        if (statusFilterComboBox != null) {
            statusFilterComboBox.getItems().setAll(statusMapping.keySet());
            statusFilterComboBox.setValue(STATUS_ALL);
            statusFilterComboBox.setOnAction(e -> applyFilters());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }
        if (sortDateCroissantButton != null) {
            sortDateCroissantButton.setOnAction(e -> {
                dateAscending = true;
                applyFilters();
            });
        }
        if (sortDateDecroissantButton != null) {
            sortDateDecroissantButton.setOnAction(e -> {
                dateAscending = false;
                applyFilters();
            });
        }
        if (resetButton != null) {
            resetButton.setOnAction(e -> resetFilters());
        }
    }

    private void refreshList() {
        try {
            source.clear();
            source.addAll(rendezVousController.listerTousRendezVous());
            applyFilters();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void applyFilters() {
        if (rowsContainer == null) {
            return;
        }

        String query = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        String statusFilter = statusMapping.getOrDefault(
                statusFilterComboBox != null ? statusFilterComboBox.getValue() : STATUS_ALL,
                STATUS_ALL
        );

        Comparator<RendezVous> comparator = Comparator.comparing(
                RendezVous::getDateHeure,
                Comparator.nullsLast(LocalDateTime::compareTo)
        );
        if (!dateAscending) {
            comparator = comparator.reversed();
        }

        List<RendezVous> filtered = source.stream()
                .filter(rdv -> matchesSearch(rdv, query))
                .filter(rdv -> matchesStatus(rdv, statusFilter))
                .sorted(comparator)
                .toList();

        renderCards(filtered);
    }

    private boolean matchesSearch(RendezVous rdv, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        return safe(resolvePatient(rdv)).toLowerCase(Locale.ROOT).contains(query)
                || safe(resolveMedecin(rdv)).toLowerCase(Locale.ROOT).contains(query)
                || safe(rdv != null ? rdv.getMotif() : null).toLowerCase(Locale.ROOT).contains(query)
                || safe(resolveDisponibilite(rdv)).toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesStatus(RendezVous rdv, String statusFilter) {
        if (STATUS_ALL.equals(statusFilter)) {
            return true;
        }
        return rdv != null && statusFilter.equalsIgnoreCase(safe(rdv.getStatut()));
    }

    private void resetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (statusFilterComboBox != null) {
            statusFilterComboBox.setValue(STATUS_ALL);
        }
        dateAscending = false;
        applyFilters();
    }

    private void renderCards(List<RendezVous> rendezVousList) {
        rowsContainer.getChildren().clear();
        if (countLabel != null) {
            int count = rendezVousList == null ? 0 : rendezVousList.size();
            countLabel.setText(count + " rendez-vous");
        }

        if (rendezVousList == null || rendezVousList.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous trouvé pour les filtres actuels.");
            empty.getStyleClass().add("admin-rdv-empty");
            rowsContainer.getChildren().add(empty);
            return;
        }

        for (RendezVous rdv : rendezVousList) {
            rowsContainer.getChildren().add(createCard(rdv));
        }
    }

    private VBox createCard(RendezVous rdv) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setMaxWidth(320);
        card.getStyleClass().add("admin-rdv-card");
        card.setPadding(new Insets(16));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label avatar = buildAvatar(resolveInitials(resolvePatient(rdv)), avatarBackgroundFor(rdv), "#27466f");

        VBox nameBox = new VBox(2);
        Label patient = new Label(resolvePatient(rdv));
        patient.getStyleClass().add("admin-rdv-card-title");
        Label medecin = new Label(resolveMedecin(rdv));
        medecin.getStyleClass().add("admin-rdv-card-subtitle");
        nameBox.getChildren().addAll(patient, medecin);
        header.getChildren().addAll(avatar, nameBox);

        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                buildBadge(resolveRoleBadge(rdv), "badge-role"),
                buildBadge(formatStatus(rdv != null ? rdv.getStatut() : null).toUpperCase(Locale.ROOT), badgeClassForStatus(rdv != null ? rdv.getStatut() : null))
        );

        VBox details = new VBox(6);
        details.getChildren().addAll(
                buildInfo(resolveDisponibilite(rdv)),
                buildInfo(formatDateTime(rdv != null ? rdv.getDateHeure() : null)),
                buildInfo("Motif : " + safe(rdv != null ? rdv.getMotif() : null))
        );

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Button infoBtn = buildActionButton("Voir", "admin-rdv-btn-purple");
        infoBtn.setOnAction(e -> showDetailsPopup(rdv));

        Button editBtn = buildActionButton("Modifier", "admin-rdv-btn-blue");
        editBtn.setOnAction(e -> editRendezVous(rdv));

        Button deleteBtn = buildActionButton("Annuler", "admin-rdv-btn-red");
        deleteBtn.setDisable(rdv == null || RendezVous.TERMINE.equals(rdv.getStatut()));
        deleteBtn.setOnAction(e -> deleteRendezVous(rdv));

        buttons.getChildren().addAll(infoBtn, editBtn, deleteBtn);
        card.getChildren().addAll(header, separator, badges, details, buttons);
        return card;
    }

    private Button buildActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().addAll("admin-rdv-card-btn", styleClass);
        return button;
    }

    private Label buildAvatar(String initials, String background, String textColor) {
        Label avatar = new Label(initials);
        avatar.getStyleClass().add("admin-rdv-avatar");
        avatar.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + textColor + ";");
        return avatar;
    }

    private Label buildBadge(String text, String styleClass) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("admin-rdv-badge", styleClass);
        return badge;
    }

    private Label buildInfo(String text) {
        Label info = new Label(text);
        info.getStyleClass().add("admin-rdv-card-info");
        info.setWrapText(true);
        return info;
    }

    private void deleteRendezVous(RendezVous rdv) {
        if (rdv == null) {
            return;
        }
        if (!ViewAlertUtil.confirmer("Suppression", "Supprimer ce rendez-vous ?")) {
            return;
        }
        try {
            rendezVousController.annulerRendezVous(rdv.getId());
            refreshList();
            ViewAlertUtil.info("Suppression", "Rendez-vous supprimé.");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Suppression", e.formatWithCauses());
        }
    }

    private void editRendezVous(RendezVous rdv) {
        if (rdv == null) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog(rdv.getMotif() == null ? "" : rdv.getMotif());
        dialog.setTitle("Modifier rendez-vous");
        dialog.setHeaderText("Modifier le motif du rendez-vous");
        dialog.setContentText("Motif :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            rendezVousController.modifierMotifRendezVous(rdv.getId(), result.get());
            refreshList();
            ViewAlertUtil.info("Modification", "Motif du rendez-vous mis à jour.");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Modification", e.formatWithCauses());
        }
    }

    private String resolveDisponibilite(RendezVous rdv) {
        if (rdv == null || rdv.getDisponibilite() == null) {
            return "-";
        }
        Disponibilite dispo = rdv.getDisponibilite();
        if (dispo.getDate() == null || dispo.getHeureDebut() == null || dispo.getHeureFin() == null) {
            return "Disponibilité #" + dispo.getId();
        }
        return dispo.getDate().format(DATE_FORMATTER)
                + " • " + dispo.getHeureDebut().format(TIME_FORMATTER)
                + " à " + dispo.getHeureFin().format(TIME_FORMATTER);
    }

    private String resolveMedecin(RendezVous rdv) {
        if (rdv == null || rdv.getDisponibilite() == null || rdv.getDisponibilite().getMedecin() == null) {
            return "-";
        }
        return safe(rdv.getDisponibilite().getMedecin().getFullName());
    }

    private String resolvePatient(RendezVous rdv) {
        if (rdv == null || rdv.getPatient() == null) {
            return "-";
        }
        return safe(rdv.getPatient().getFullName());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Inconnu";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "En attente";
            case RendezVous.CONFIRME -> "Confirmé";
            case RendezVous.ANNULE -> "Annulé";
            case RendezVous.TERMINE -> "Terminé";
            default -> status;
        };
    }

    private String buildDetails(RendezVous rdv) {
        if (rdv == null) {
            return "Rendez-vous introuvable.";
        }
        return "Patient: " + resolvePatient(rdv) + "\n"
                + "Médecin: " + resolveMedecin(rdv) + "\n"
                + "Disponibilité: " + resolveDisponibilite(rdv) + "\n"
                + "Date et heure: " + formatDateTime(rdv.getDateHeure()) + "\n"
                + "Statut: " + formatStatus(rdv.getStatut()) + "\n"
                + "Motif: " + safe(rdv.getMotif());
    }

    private void showDetailsPopup(RendezVous rdv) {
        if (rdv == null) {
            return;
        }

        VBox root = new VBox(16);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 16; "
                + "-fx-border-color: #dde5f0; -fx-border-radius: 16;");

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        Label avatar = buildAvatar(resolveInitials(resolvePatient(rdv)), avatarBackgroundFor(rdv), "#27466f");

        VBox headerText = new VBox(3);
        Label title = new Label(resolvePatient(rdv));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label subtitle = new Label(resolveMedecin(rdv));
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #7b8aa0;");
        headerText.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(avatar, headerText);

        javafx.scene.control.Separator separator = new javafx.scene.control.Separator();

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(
                buildBadge(resolveRoleBadge(rdv), "badge-role"),
                buildBadge(formatStatus(rdv.getStatut()).toUpperCase(Locale.ROOT), badgeClassForStatus(rdv.getStatut()))
        );

        VBox details = new VBox(8);
        details.getChildren().addAll(
                buildPopupLine("Disponibilité", resolveDisponibilite(rdv)),
                buildPopupLine("Date et heure", formatDateTime(rdv.getDateHeure())),
                buildPopupLine("Statut", formatStatus(rdv.getStatut())),
                buildPopupLine("Motif", safe(rdv.getMotif()))
        );

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Fermer");
        closeBtn.getStyleClass().addAll("admin-rdv-card-btn", "admin-rdv-btn-blue");
        footer.getChildren().add(closeBtn);

        root.getChildren().addAll(header, separator, badges, details, footer);

        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Détail du rendez-vous");
        popup.setScene(new Scene(root, 430, 320));
        popup.setResizable(false);

        closeBtn.setOnAction(e -> popup.close());
        popup.show();
    }

    private HBox buildPopupLine(String labelText, String valueText) {
        Label label = new Label(labelText + " :");
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #42526b;");
        Label value = new Label(valueText == null || valueText.isBlank() ? "-" : valueText);
        value.setStyle("-fx-font-size: 13px; -fx-text-fill: #1f2735;");
        value.setWrapText(true);
        HBox line = new HBox(8, label, value);
        line.setAlignment(Pos.TOP_LEFT);
        return line;
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }

    private Map<String, String> buildStatusMapping() {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put(STATUS_ALL, STATUS_ALL);
        mapping.put("En attente", RendezVous.EN_ATTENTE);
        mapping.put("Confirmé", RendezVous.CONFIRME);
        mapping.put("Terminé", RendezVous.TERMINE);
        mapping.put("Annulé", RendezVous.ANNULE);
        return mapping;
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

    private String avatarBackgroundFor(RendezVous rdv) {
        String[] colors = {"#E6F1FB", "#E1F5EE", "#F1EFE8", "#FBEAF0", "#FAEEDA"};
        String seed = resolvePatient(rdv);
        return colors[Math.abs(seed.hashCode()) % colors.length];
    }

    private String resolveRoleBadge(RendezVous rdv) {
        String medecin = resolveMedecin(rdv).toUpperCase(Locale.ROOT);
        if (!"-".equals(medecin)) {
            return "MEDECIN";
        }
        return "USER";
    }

    private String badgeClassForStatus(String status) {
        if (status == null) {
            return "badge-default";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "badge-waiting";
            case RendezVous.CONFIRME -> "badge-confirmed";
            case RendezVous.ANNULE -> "badge-cancelled";
            case RendezVous.TERMINE -> "badge-done";
            default -> "badge-default";
        };
    }
}
