package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.CampagneAide;
import models.UrgenceDemande;
import services.CampagneAideService;
import services.UrgenceDemandeService;
import ui.ParticleBackground;
import utils.MediLinkDialogs;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Liste des demandes d'urgence liées à la session locale ; modification possible tant qu'elles sont en attente.
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
        recharger();
    }

    private void recharger() {
        cardsContainer.getChildren().clear();
        try {
            var list = urgenceDemandeService.listerPourAuteurCourant();
            if (list.isEmpty()) {
                Label empty = new Label(
                        "Aucune demande liée à cette session. Envoyez une « Demande d'urgence » depuis l'accueil ; "
                                + "elle apparaîtra ici pour la consulter ou la modifier tant qu'elle est en attente.");
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

        Button btnMod = new Button("Modifier");
        btnMod.getStyleClass().add("btn-care-secondary");
        boolean peutMod = "en_attente".equals(d.getStatut());
        btnMod.setDisable(!peutMod);
        if (!peutMod) {
            btnMod.setTooltip(new Tooltip(
                    "Modification possible uniquement tant que la demande est en attente de traitement."));
        }
        btnMod.setOnAction(e -> ouvrirModification(d));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, spacer, btnMod);

        VBox card = new VBox(10, bloc, actions);
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

    private void ouvrirModification(UrgenceDemande d) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la demande d'urgence");
        dialog.setHeaderText(null);

        TextArea ta = new TextArea(d.getMessage() != null ? d.getMessage() : "");
        ta.setWrapText(true);
        ta.setPrefRowCount(10);

        Label lblImg = new Label("Image : aucun changement");
        lblImg.setStyle("-fx-text-fill: #94a3b8;");
        Hyperlink linkRetirer = new Hyperlink("Retirer la pièce jointe");
        linkRetirer.setVisible(d.getPieceImagePath() != null && !d.getPieceImagePath().isBlank());
        linkRetirer.setManaged(linkRetirer.isVisible());

        final Path[] nouvelleImage = {null};
        final boolean[] retirerImage = {false};

        Button btnChoisir = new Button("Changer l'image…");
        btnChoisir.getStyleClass().add("btn-care-secondary");
        btnChoisir.setOnAction(ev -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir une image");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            Stage owner = (Stage) rootStack.getScene().getWindow();
            var f = chooser.showOpenDialog(owner);
            if (f != null) {
                nouvelleImage[0] = f.toPath();
                retirerImage[0] = false;
                lblImg.setText("Nouvelle image : " + f.getName());
                linkRetirer.setVisible(true);
                linkRetirer.setManaged(true);
            }
        });

        linkRetirer.setOnAction(ev -> {
            nouvelleImage[0] = null;
            retirerImage[0] = true;
            lblImg.setText("Pièce jointe : sera retirée à l'enregistrement");
        });

        HBox ligneImg = new HBox(10, btnChoisir, linkRetirer);
        VBox content = new VBox(12,
                new Label("Message (8 à 4000 caractères) :"),
                ta,
                lblImg,
                ligneImg);
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(
                new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL);
        if (rootStack.getScene() != null && rootStack.getScene().getWindow() != null) {
            dialog.initOwner(rootStack.getScene().getWindow());
        }
        MediLinkDialogs.style(dialog);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }
        String msg = ta.getText() != null ? ta.getText().trim() : "";
        if (msg.length() < 8) {
            Alert w = new Alert(Alert.AlertType.WARNING, "Le message doit contenir au moins 8 caractères.");
            MediLinkDialogs.style(w);
            w.showAndWait();
            return;
        }
        if (msg.length() > 4000) {
            Alert w = new Alert(Alert.AlertType.WARNING, "Message trop long (4000 caractères maximum).");
            MediLinkDialogs.style(w);
            w.showAndWait();
            return;
        }

        try {
            boolean modPiece = retirerImage[0] || nouvelleImage[0] != null;
            String chemin = null;
            if (retirerImage[0]) {
                chemin = null;
            } else if (nouvelleImage[0] != null) {
                chemin = copierPieceJointeVersMedilink(nouvelleImage[0]);
            }
            urgenceDemandeService.mettreAJourParAuteurCourant(d.getId(), msg, modPiece, chemin);
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Demande mise à jour.");
            MediLinkDialogs.style(ok);
            ok.showAndWait();
            recharger();
        } catch (IOException ex) {
            afficherErreur("Enregistrement impossible", ex.getMessage());
        }
    }

    private static String copierPieceJointeVersMedilink(Path source) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Fichier image introuvable.");
        }
        long taille = Files.size(source);
        long limite = 5L * 1024L * 1024L;
        if (taille > limite) {
            throw new IOException("Image trop volumineuse (max 5 Mo).");
        }
        String nom = source.getFileName().toString();
        String ext = "";
        int idx = nom.lastIndexOf('.');
        if (idx >= 0) {
            ext = nom.substring(idx).toLowerCase();
        }
        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp")) {
            throw new IOException("Format d'image non supporté (PNG, JPG, WEBP).");
        }
        Path dossier = Path.of(System.getProperty("user.home"), ".medilink", "urgence-justificatifs");
        Files.createDirectories(dossier);
        String fichier = "demande_" + System.currentTimeMillis() + ext;
        Path destination = dossier.resolve(fichier);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toAbsolutePath().toString();
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

    private static void afficherErreur(String titre, String detail) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(detail != null ? detail : "Erreur inconnue.");
        MediLinkDialogs.style(a);
        a.showAndWait();
    }
}
