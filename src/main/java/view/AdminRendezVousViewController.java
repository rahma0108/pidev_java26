package view;

import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import models.Disponibilite;
import models.RendezVous;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminRendezVousViewController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private VBox rowsContainer;

    private RendezVousController rendezVousController;
    private final List<RendezVous> source = new ArrayList<>();

    @FXML
    private void initialize() {
        try {
            rendezVousController = new RendezVousController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Rendez-vous", e.formatWithCauses());
            return;
        }

        refreshList();
    }

    private void refreshList() {
        try {
            source.clear();
            source.addAll(rendezVousController.listerTousRendezVous());
            renderRows(source.stream()
                    .sorted(Comparator.comparing(RendezVous::getDateHeure, Comparator.nullsLast(java.time.LocalDateTime::compareTo)).reversed())
                    .toList());
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void renderRows(List<RendezVous> rendezVousList) {
        rowsContainer.getChildren().clear();

        if (rendezVousList == null || rendezVousList.isEmpty()) {
            Label empty = new Label("Aucun rendez-vous disponible.");
            empty.getStyleClass().add("admin-rdv-empty");
            rowsContainer.getChildren().add(empty);
            return;
        }

        for (RendezVous rdv : rendezVousList) {
            rowsContainer.getChildren().add(createRow(rdv));
        }
    }

    private HBox createRow(RendezVous rdv) {
        HBox row = new HBox(12);
        row.getStyleClass().add("admin-rdv-row");

        Label dispoCell = createCell(resolveDisponibilite(rdv), "col-dispo");
        Label medecinCell = createCell(resolveMedecin(rdv), "col-medecin");
        Label patientCell = createCell(resolvePatient(rdv), "col-patient");
        Label dateCell = createCell(formatDateTime(rdv != null ? rdv.getDateHeure() : null), "col-datetime");

        Label statusBadge = new Label(formatStatus(rdv != null ? rdv.getStatut() : null));
        statusBadge.getStyleClass().addAll("status-badge", statusClass(rdv != null ? rdv.getStatut() : null));
        HBox statusBox = new HBox(statusBadge);
        statusBox.getStyleClass().add("col-status");

        HBox actions = new HBox(6);
        actions.getStyleClass().addAll("admin-rdv-actions", "col-actions");

        Button calBtn = createActionButton("📅", "Calendrier");
        calBtn.setOnAction(e -> ViewAlertUtil.info("Calendrier", "Action calendrier à brancher."));

        Button viewBtn = createActionButton("👁", "Voir détail");
        viewBtn.setOnAction(e -> ViewAlertUtil.info("Détail", buildDetails(rdv)));

        Button editBtn = createActionButton("✎", "Modifier");
        editBtn.setOnAction(e -> ViewAlertUtil.info("Modifier", "Edition admin du rendez-vous à brancher."));

        Button deleteBtn = createActionButton("🗑", "Supprimer");
        deleteBtn.setDisable(rdv == null || RendezVous.TERMINE.equals(rdv.getStatut()));
        deleteBtn.setOnAction(e -> deleteRendezVous(rdv));

        actions.getChildren().addAll(calBtn, viewBtn, editBtn, deleteBtn);
        row.getChildren().addAll(dispoCell, medecinCell, patientCell, dateCell, statusBox, actions);
        return row;
    }

    private Label createCell(String text, String colClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("admin-rdv-cell", colClass);
        return label;
    }

    private Button createActionButton(String text, String tooltipText) {
        Button button = new Button(text);
        button.getStyleClass().add("admin-rdv-action-btn");
        button.setTooltip(new Tooltip(tooltipText));
        return button;
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

    private String resolveDisponibilite(RendezVous rdv) {
        if (rdv == null || rdv.getDisponibilite() == null) {
            return "-";
        }
        Disponibilite dispo = rdv.getDisponibilite();
        if (dispo.getDate() == null || dispo.getHeureDebut() == null || dispo.getHeureFin() == null) {
            return "Disponibilité #" + dispo.getId();
        }
        return dispo.getDate().format(DATE_FORMATTER)
                + " - " + dispo.getHeureDebut().format(TIME_FORMATTER)
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

    private String statusClass(String status) {
        if (status == null) {
            return "status-default";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case RendezVous.EN_ATTENTE -> "status-waiting";
            case RendezVous.CONFIRME -> "status-confirmed";
            case RendezVous.ANNULE -> "status-cancelled";
            case RendezVous.TERMINE -> "status-done";
            default -> "status-default";
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

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "-" : value.trim();
    }

}
