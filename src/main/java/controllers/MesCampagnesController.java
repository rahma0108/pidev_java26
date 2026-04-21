package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.CampagneAide;
import models.UrgenceDemande;
import services.CampagneAideService;
import services.UrgenceDemandeService;
import ui.ParticleBackground;
import utils.MediLinkDialogs;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Liste des demandes d'urgence liées à la session locale (consultation et suivi des campagnes publiées).
 */
public class MesCampagnesController implements Initializable {

    private static final String CARD_STYLE = "-fx-background-color: rgba(15,23,42,0.78);"
            + " -fx-border-color: rgba(148,163,184,0.28); -fx-border-radius: 16; -fx-background-radius: 16;"
            + " -fx-padding: 14;"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0, 0, 3);";

    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.systemDefault());

    @FXML
    private StackPane rootStack;

    @FXML
    private Canvas particleCanvas;

    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox cardsContainer;

    @FXML
    private Button btnActualiser;

    @FXML
    private Button btnNouvelleDemande;

    @FXML
    private Button btnAccueil;

    @FXML
    private Button btnEspaceAdmin;

    private final UrgenceDemandeService urgenceDemandeService = new UrgenceDemandeService();
    private final CampagneAideService campagneAideService = new CampagneAideService();
    private ParticleBackground particules;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        URL css = getClass().getResource("/styles/medilink-care.css");
        if (css != null) {
            rootStack.getStylesheets().add(css.toExternalForm());
        }
        particules = new ParticleBackground(particleCanvas, rootStack, 96);
        particules.play();

        btnActualiser.setOnAction(e -> recharger());
        btnNouvelleDemande.setOnAction(e -> ouvrirNouvelleDemande());
        btnAccueil.setOnAction(e -> ouvrirAccueil());
        btnEspaceAdmin.setOnAction(e -> ouvrirEspaceAdmin());
        recharger();
    }

    private void recharger() {
        cardsContainer.getChildren().clear();
        try {
            var list = urgenceDemandeService.listerPourAuteurCourant();
            if (list.isEmpty()) {
                Label empty = new Label(
                        "Aucune demande liée à cette session. Envoyez une « Demande d'urgence » depuis l'accueil ; "
                                + "elle apparaîtra ici pour la consulter et suivre l'éventuelle campagne publiée.");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14; -fx-wrap-text: true;");
                empty.setMaxWidth(Double.MAX_VALUE);
                VBox wrap = new VBox(empty);
                wrap.setPadding(new Insets(20, 10, 20, 10));
                cardsContainer.getChildren().add(wrap);
                return;
            }
            for (UrgenceDemande d : list) {
                cardsContainer.getChildren().add(buildCard(d));
            }
        } catch (IOException ex) {
            afficherErreur("Chargement impossible", ex.getMessage());
        }
    }

    private VBox buildCard(UrgenceDemande d) {
        Label titre = new Label(libelleStatut(d.getStatut()));
        titre.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label date = new Label("Envoyée le : " + formaterDate(d.getCreatedAt()));
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
        String msg = d.getMessage() != null ? d.getMessage() : "";
        Label extrait = new Label(msg.length() > 220 ? msg.substring(0, 217) + "…" : msg);
        extrait.setWrapText(true);
        extrait.setStyle("-fx-text-fill: #cbd5e1;");
        extrait.setMaxWidth(Double.MAX_VALUE);

        VBox bloc = new VBox(8, titre, date, extrait);
        try {
            Optional<CampagneAide> camp = campagneAideService.trouverPourDemande(d.getId());
            if (camp.isPresent()) {
                CampagneAide c = camp.get();
                Label lc = new Label("Campagne sur l'accueil");
                lc.setStyle("-fx-font-weight: bold; -fx-text-fill: #4ade80;");
                Label lt = new Label(c.getTitre() != null ? c.getTitre() : "—");
                lt.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");
                lt.setWrapText(true);
                String corps = c.getCorps() != null ? c.getCorps() : "";
                Label lco = new Label(corps.length() > 300 ? corps.substring(0, 297) + "…" : corps);
                lco.setWrapText(true);
                lco.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
                lco.setMaxWidth(Double.MAX_VALUE);
                bloc.getChildren().addAll(lc, lt, lco);
            }
        } catch (IOException ignored) {
            // pas bloquant
        }

        if (d.getPieceImagePath() != null && !d.getPieceImagePath().isBlank()) {
            Label pj = new Label("Pièce jointe : enregistrée sur cet appareil.");
            pj.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
            bloc.getChildren().add(pj);
        }

        VBox card = new VBox(10, bloc);
        card.setPadding(new Insets(4, 0, 4, 0));
        card.setStyle(CARD_STYLE);
        return card;
    }

    private static String libelleStatut(String statut) {
        if (statut == null) {
            return "Statut : —";
        }
        return switch (statut) {
            case "en_attente" -> "En attente de traitement";
            case "sans_campagne" -> "Traitée — pas de campagne publique";
            case "campagne_publiee" -> "Campagne publiée sur l'accueil";
            default -> "Statut : " + statut;
        };
    }

    private static String formaterDate(Timestamp ts) {
        if (ts == null) {
            return "—";
        }
        return FMT_DATE.format(ts.toInstant());
    }

    private void ouvrirNouvelleDemande() {
        try {
            Stage owner = (Stage) rootStack.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/DemandeUrgence.fxml")));
            Parent root = loader.load();
            Stage w = new Stage();
            w.initOwner(owner);
            w.setTitle("Demande d'urgence — MediLink Care");
            w.setScene(new Scene(root, 560, 520));
            w.setOnHidden(ev -> recharger());
            w.show();
        } catch (IOException ex) {
            afficherErreur("Ouverture impossible", ex.getMessage());
        }
    }

    private void ouvrirAccueil() {
        try {
            Stage stage = (Stage) rootStack.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/ListeDons.fxml")));
            stage.setScene(new Scene(root, 960, 680));
        } catch (IOException ex) {
            afficherErreur("Retour accueil impossible", ex.getMessage());
        }
    }

    private void ouvrirEspaceAdmin() {
        try {
            Stage stage = (Stage) rootStack.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/AdminDons.fxml")));
            stage.setScene(new Scene(root, 1100, 720));
        } catch (IOException ex) {
            afficherErreur("Ouverture admin", ex.getMessage());
        }
    }

    private static void afficherErreur(String titre, String detail) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(detail != null ? detail : "Erreur inconnue.");
        MediLinkDialogs.style(a);
        a.showAndWait();
    }
}
