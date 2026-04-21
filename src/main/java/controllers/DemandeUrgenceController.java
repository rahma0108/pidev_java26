package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.UrgenceDemandeService;
import utils.MediLinkDialogs;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

/**
 * Fenêtre dédiée : soumettre une demande d'urgence (message transmis à l'admin uniquement).
 */
public class DemandeUrgenceController implements Initializable {

    private static final String BG_PAGE = "#060d18";
    private static final String BG_FIELD = "#0f172a";

    @FXML
    private BorderPane rootPane;

    @FXML
    private ScrollPane scrollContenu;

    @FXML
    private Button btnRetour;

    @FXML
    private Button btnEnvoyer;

    @FXML
    private Button btnAjouterImage;

    @FXML
    private Hyperlink linkRetirerImage;

    @FXML
    private Label lblImageSelectionnee;

    @FXML
    private TextArea txtMessage;

    private final UrgenceDemandeService urgenceDemandeService = new UrgenceDemandeService();
    private Path imageJustificativeSelectionnee;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnRetour.setOnAction(e -> fermer());
        btnEnvoyer.setOnAction(e -> envoyer());
        btnAjouterImage.setOnAction(e -> choisirImageJustificative());
        linkRetirerImage.setOnAction(e -> retirerImageJustificative());
        rootPane.sceneProperty().addListener((obs, oldSc, newSc) -> {
            if (newSc != null) {
                newSc.setFill(Color.web(BG_PAGE));
                Platform.runLater(this::forcerFondsSombres);
            }
        });
        Platform.runLater(this::forcerFondsSombres);
    }

    /**
     * Le thème Modena laisse parfois des surfaces blanches (TextArea interne, viewport ScrollPane).
     */
    private void forcerFondsSombres() {
        Scene sc = rootPane.getScene();
        if (sc != null) {
            sc.setFill(Color.web(BG_PAGE));
        }
        if (scrollContenu != null) {
            scrollContenu.setStyle("-fx-background-color: " + BG_PAGE + "; -fx-background: " + BG_PAGE + ";");
            scrollContenu.applyCss();
            scrollContenu.layout();
            appliquerFond(scrollContenu.lookup(".viewport"), BG_PAGE);
            appliquerFond(scrollContenu.lookup(".corner"), BG_PAGE);
        }
        if (txtMessage != null) {
            txtMessage.setStyle(
                    "-fx-control-inner-background: " + BG_FIELD + ";"
                            + "-fx-background-color: " + BG_FIELD + ";"
                            + "-fx-text-fill: #e2e8f0;"
                            + "-fx-prompt-text-fill: #64748b;"
                            + "-fx-highlight-fill: #334155;"
                            + "-fx-highlight-text-fill: #f8fafc;");
            txtMessage.applyCss();
            txtMessage.layout();
            Node innerScroll = txtMessage.lookup(".scroll-pane");
            if (innerScroll != null) {
                innerScroll.setStyle("-fx-background-color: " + BG_FIELD + ";");
                appliquerFond(innerScroll.lookup(".viewport"), BG_FIELD);
                appliquerFond(innerScroll.lookup(".corner"), BG_FIELD);
            }
            appliquerFond(txtMessage.lookup(".content"), BG_FIELD);
        }
    }

    private static void appliquerFond(Node node, String couleurHex) {
        if (node != null) {
            node.setStyle("-fx-background-color: " + couleurHex + ";");
        }
    }

    private void fermer() {
        Stage s = (Stage) rootPane.getScene().getWindow();
        s.close();
    }

    private void choisirImageJustificative() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image justificative");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        Stage owner = (Stage) rootPane.getScene().getWindow();
        var file = chooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }
        imageJustificativeSelectionnee = file.toPath();
        lblImageSelectionnee.setText("Image : " + file.getName());
        linkRetirerImage.setVisible(true);
        linkRetirerImage.setManaged(true);
    }

    private void retirerImageJustificative() {
        imageJustificativeSelectionnee = null;
        lblImageSelectionnee.setText("Aucune image sélectionnée");
        linkRetirerImage.setVisible(false);
        linkRetirerImage.setManaged(false);
    }

    private String copierPieceJointeSiPresente() throws IOException {
        if (imageJustificativeSelectionnee == null) {
            return null;
        }
        if (!Files.isRegularFile(imageJustificativeSelectionnee)) {
            throw new IOException("Image justificative introuvable.");
        }
        long taille = Files.size(imageJustificativeSelectionnee);
        long limite = 5L * 1024L * 1024L;
        if (taille > limite) {
            throw new IOException("Image trop volumineuse (max 5 Mo).");
        }
        String nom = imageJustificativeSelectionnee.getFileName().toString();
        String ext = "";
        int idx = nom.lastIndexOf('.');
        if (idx >= 0) {
            ext = nom.substring(idx).toLowerCase();
        }
        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp")) {
            throw new IOException("Format d'image non supporté. Utilisez PNG, JPG, JPEG ou WEBP.");
        }
        Path dossier = Path.of(System.getProperty("user.home"), ".medilink", "urgence-justificatifs");
        Files.createDirectories(dossier);
        String fichier = "demande_" + System.currentTimeMillis() + ext;
        Path destination = dossier.resolve(fichier);
        Files.copy(imageJustificativeSelectionnee, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toAbsolutePath().toString();
    }

    private void envoyer() {
        String msg = txtMessage.getText() != null ? txtMessage.getText().trim() : "";
        if (msg.length() < 8) {
            Alert w = new Alert(Alert.AlertType.WARNING,
                    "Veuillez décrire la situation en au moins quelques mots (8 caractères minimum).");
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
            String pieceImagePath = copierPieceJointeSiPresente();
            urgenceDemandeService.enregistrer(msg, pieceImagePath);
            txtMessage.clear();
            retirerImageJustificative();
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Votre message a été transmis aux équipes. Il n'apparaît pas sur l'accueil ; un administrateur pourra "
                            + "lancer une campagne d'aide publique si nécessaire. Vous pouvez suivre ou modifier votre "
                            + "demande (tant qu'elle est en attente) depuis « Mes campagnes » sur l'accueil.");
            MediLinkDialogs.style(ok);
            ok.showAndWait();
            fermer();
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Envoi impossible");
            a.setHeaderText(null);
            a.setContentText(ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue.");
            MediLinkDialogs.style(a);
            a.showAndWait();
        }
    }
}
