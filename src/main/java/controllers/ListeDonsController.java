package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ui.ParticleBackground;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.CampagneAide;
import models.Don;
import services.CampagneAideService;
import services.DonService;
import utils.MediLinkDialogs;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.util.Duration;

public class ListeDonsController implements Initializable {

    private static final double LARGEUR_CAMPAGNE = 350;
    private static final double LARGEUR_MIN_CAMPAGNE = 320;
    private static final String CARD_STYLE_BASE = "-fx-background-color: rgba(15,23,42,0.78);"
            + " -fx-border-color: rgba(148,163,184,0.28); -fx-border-radius: 16; -fx-background-radius: 16;"
            + " -fx-padding: 14;"
            + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0, 0, 3);";

    @FXML
    private StackPane rootStack;

    @FXML
    private Canvas particleCanvas;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filtreUrgenceCombo;

    @FXML
    private ComboBox<String> filtreEtatCombo;

    @FXML
    private ComboBox<String> filtreUniteCombo;

    @FXML
    private ComboBox<DonFormChoices.CategorieOption> filtreCategorieCombo;

    @FXML
    private TextField tfQuantiteMin;

    @FXML
    private TextField tfQuantiteMax;

    @FXML
    private ComboBox<String> tri1ChampCombo;

    @FXML
    private ComboBox<String> tri1OrdreCombo;

    @FXML
    private ComboBox<String> tri2ChampCombo;

    @FXML
    private ComboBox<String> tri2OrdreCombo;

    @FXML
    private ComboBox<String> tri3ChampCombo;

    @FXML
    private ComboBox<String> tri3OrdreCombo;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnMesDons;

    @FXML
    private Button btnMesCampagnes;

    @FXML
    private Button btnEspaceAdmin;

    @FXML
    private Button btnDemandeUrgence;

    @FXML
    private VBox campagnesBox;

    @FXML
    private Label lblDonsTitre;

    @FXML
    private VBox cardsContainer;

    private final DonService donService = new DonService();
    private final CampagneAideService campagneAideService = new CampagneAideService();
    private final ObservableList<Don> masterData = FXCollections.observableArrayList();
    private FilteredList<Don> filteredData;
    private SortedList<Don> sortedData;
    private List<CampagneAide> campagnesActives = List.of();
    private int campagneIndex = 0;
    private Timeline rotationCampagnes;
    private Label campagneTitreLabel;
    private Label campagneCorpsLabel;
    private Button btnPauseCampagnes;
    private boolean campagneEnPause = false;
    private boolean pauseManuelleCampagnes = false;
    private boolean pauseSurvolCampagnes = false;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean positionCampagneInitialisee = false;

    private ParticleBackground particules;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        URL css = getClass().getResource("/styles/medilink-care.css");
        if (css != null) {
            rootStack.getStylesheets().add(css.toExternalForm());
        }

        particules = new ParticleBackground(particleCanvas, rootStack, 96);
        particules.play();

        DonFiltreTriUtil.preparerCombosTri(tri1ChampCombo, tri2ChampCombo, tri3ChampCombo);
        DonFiltreTriUtil.preparerCombosOrdre(tri1OrdreCombo, tri2OrdreCombo, tri3OrdreCombo);

        chargerDepuisBaseAsync();

        filteredData = new FilteredList<>(masterData, don -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.addListener((ListChangeListener<Don>) change -> renderCards());

        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltre());
        filtreUrgenceCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltre());
        filtreEtatCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltre());
        filtreUniteCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltre());
        filtreCategorieCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltre());
        tfQuantiteMin.textProperty().addListener((obs, o, n) -> appliquerFiltre());
        tfQuantiteMax.textProperty().addListener((obs, o, n) -> appliquerFiltre());

        tri1ChampCombo.valueProperty().addListener((obs, o, n) -> appliquerTri());
        tri1OrdreCombo.valueProperty().addListener((obs, o, n) -> appliquerTri());
        tri2ChampCombo.valueProperty().addListener((obs, o, n) -> appliquerTri());
        tri2OrdreCombo.valueProperty().addListener((obs, o, n) -> appliquerTri());
        tri3ChampCombo.valueProperty().addListener((obs, o, n) -> appliquerTri());
        tri3OrdreCombo.valueProperty().addListener((obs, o, n) -> appliquerTri());

        btnAjouter.setOnAction(e -> ouvrirAjouter());
        btnMesDons.setOnAction(e -> ouvrirMesDons());
        btnMesCampagnes.setOnAction(e -> ouvrirMesCampagnes());
        btnEspaceAdmin.setOnAction(e -> ouvrirEspaceAdmin());
        btnDemandeUrgence.setOnAction(e -> ouvrirDemandeUrgence());

        initialiserCampagnesAccueil();
        activerDeplacementCampagnes();
        appliquerTri();
        appliquerFiltre();
        renderCards();
    }

    private void ouvrirDemandeUrgence() {
        try {
            Stage owner = stageCourant();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/DemandeUrgence.fxml")));
            Parent root = loader.load();
            Stage w = new Stage();
            w.initOwner(owner);
            w.initModality(Modality.WINDOW_MODAL);
            w.setTitle("Demande d'urgence — MediLink Care");
            Scene sc = new Scene(root, 920, 640);
            sc.setFill(Color.web("#060d18"));
            URL css = getClass().getResource("/styles/medilink-care.css");
            if (css != null) {
                sc.getStylesheets().add(css.toExternalForm());
            }
            w.setScene(sc);
            w.setMinWidth(720);
            w.setMinHeight(520);
            w.show();
        } catch (IOException ex) {
            afficherErreur("Ouverture de la demande d'urgence", ex.getMessage());
        }
    }

    private void chargerDepuisBaseAsync() {
        Task<List<Don>> task = new Task<>() {
            @Override
            protected List<Don> call() throws Exception {
                return donService.getAll();
            }
        };
        task.setOnSucceeded(evt -> {
            masterData.setAll(task.getValue());
            DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                    filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
            appliquerFiltre();
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                    filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
            String message = ex == null ? "Erreur inconnue." : ex.getMessage();
            afficherErreur("Impossible de charger les dons", message);
            appliquerFiltre();
        });
        Thread t = new Thread(task, "chargement-dons");
        t.setDaemon(true);
        t.start();
    }

    private void appliquerFiltre() {
        filteredData.setPredicate(DonFiltreTriUtil.creerPredicate(
                searchField.getText(),
                "valide",
                filtreUrgenceCombo.getSelectionModel().getSelectedItem(),
                filtreEtatCombo.getSelectionModel().getSelectedItem(),
                filtreUniteCombo.getSelectionModel().getSelectedItem(),
                filtreCategorieCombo.getValue(),
                DonFiltreTriUtil.parseIntOptional(tfQuantiteMin.getText()),
                DonFiltreTriUtil.parseIntOptional(tfQuantiteMax.getText())));
        renderCards();
    }

    private void appliquerTri() {
        Comparator<Don> chain = DonFiltreTriUtil.comparateurMultiNiveaux(
                tri1ChampCombo.getValue(),
                tri1OrdreCombo.getValue(),
                tri2ChampCombo.getValue(),
                tri2OrdreCombo.getValue(),
                tri3ChampCombo.getValue(),
                tri3OrdreCombo.getValue());
        sortedData.setComparator(chain);
        renderCards();
    }

    private void ouvrirAjouter() {
        try {
            Stage stage = stageCourant();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/AjouterDon.fxml")));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 520, 480));
        } catch (IOException ex) {
            afficherErreur("Ouverture du formulaire", ex.getMessage());
        }
    }

    private void ouvrirEspaceAdmin() {
        try {
            Stage stage = stageCourant();
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/AdminDons.fxml")));
            stage.setScene(new Scene(root, 960, 680));
        } catch (IOException ex) {
            afficherErreur("Ouverture de l'espace admin", ex.getMessage());
        }
    }

    private void ouvrirMesDons() {
        try {
            Stage stage = stageCourant();
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/MesDons.fxml")));
            stage.setScene(new Scene(root, 960, 680));
        } catch (IOException ex) {
            afficherErreur("Ouverture de la page Mes dons", ex.getMessage());
        }
    }

    private void ouvrirMesCampagnes() {
        try {
            Stage stage = stageCourant();
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/MesCampagnes.fxml")));
            stage.setScene(new Scene(root, 960, 680));
        } catch (IOException ex) {
            afficherErreur("Ouverture de la page Mes campagnes", ex.getMessage());
        }
    }

    private Stage stageCourant() {
        return (Stage) rootStack.getScene().getWindow();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        for (Don don : sortedData) {
            cardsContainer.getChildren().add(buildCard(don));
        }
        if (sortedData.isEmpty()) {
            Label empty = new Label("Aucun don à afficher (ajustez filtres / recherche).");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14;");
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(20, 10, 20, 10));
            cardsContainer.getChildren().add(wrap);
        }
    }

    private static final String CAMPAGNE_CARD_STYLE = "-fx-background-color: rgba(15,23,42,0.96);"
            + " -fx-border-color: rgba(251,191,36,0.6); -fx-border-radius: 16; -fx-background-radius: 16;"
            + " -fx-padding: 14;"
            + " -fx-effect: dropshadow(gaussian, rgba(2,6,23,0.9), 34, 0.28, 0, 10);";

    private void initialiserCampagnesAccueil() {
        if (rotationCampagnes != null) {
            rotationCampagnes.stop();
        }
        campagnesBox.getChildren().clear();
        try {
            campagnesActives = campagneAideService.listerPourAccueil(12);
            if (campagnesActives.isEmpty()) {
                campagnesBox.setVisible(false);
                campagnesBox.setManaged(false);
                return;
            }
            campagnesBox.setVisible(true);
            campagnesBox.setManaged(true);
            VBox alerte = buildCampagneCard();
            campagnesBox.getChildren().add(alerte);
            campagneIndex = 0;
            campagneEnPause = false;
            pauseManuelleCampagnes = false;
            afficherCampagne(campagneIndex);
            demarrerRotationCampagnes();
        } catch (IOException ex) {
            campagnesBox.setVisible(false);
            campagnesBox.setManaged(false);
        }
    }

    private VBox buildCampagneCard() {
        campagneTitreLabel = new Label();
        campagneTitreLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        campagneTitreLabel.setWrapText(true);
        campagneTitreLabel.setMaxWidth(Double.MAX_VALUE);
        campagneCorpsLabel = new Label();
        campagneCorpsLabel.setWrapText(true);
        campagneCorpsLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14;");
        campagneCorpsLabel.setMaxWidth(Double.MAX_VALUE);

        btnPauseCampagnes = new Button("Pause");
        btnPauseCampagnes.setStyle(
                "-fx-background-color: rgba(15,23,42,0.9); -fx-text-fill: #f8fafc; -fx-font-weight: bold;"
                        + " -fx-background-radius: 999; -fx-border-radius: 999;"
                        + " -fx-border-color: rgba(251,191,36,0.5); -fx-cursor: hand;");
        btnPauseCampagnes.setOnAction(e -> basculerPauseCampagnes());

        HBox actions = new HBox(btnPauseCampagnes);
        actions.setPadding(new Insets(2, 0, 0, 0));

        VBox card = new VBox(8, campagneTitreLabel, campagneCorpsLabel, actions);
        card.setPadding(new Insets(10));
        card.setStyle(CAMPAGNE_CARD_STYLE);
        card.setTranslateY(6);
        card.setPrefWidth(LARGEUR_CAMPAGNE);
        card.setMinWidth(LARGEUR_MIN_CAMPAGNE);
        card.setMaxWidth(LARGEUR_CAMPAGNE);
        card.setOnMouseEntered(e -> mettreCampagnesEnPauseSurvol());
        card.setOnMouseExited(e -> reprendreCampagnesApresSurvol());
        return card;
    }

    private void activerDeplacementCampagnes() {
        campagnesBox.setManaged(false);
        campagnesBox.setPickOnBounds(true);
        campagnesBox.setPrefWidth(LARGEUR_CAMPAGNE);
        campagnesBox.setMinWidth(LARGEUR_MIN_CAMPAGNE);
        campagnesBox.setMaxWidth(LARGEUR_CAMPAGNE);
        rootStack.layoutBoundsProperty().addListener((obs, oldV, newV) -> {
            if (!positionCampagneInitialisee && newV.getWidth() > 0 && newV.getHeight() > 0) {
                positionnerCampagnesParDefaut();
            }
        });
        campagnesBox.setOnMousePressed(e -> {
            if (e.getButton() != MouseButton.PRIMARY || e.getTarget() instanceof Button) {
                return;
            }
            dragOffsetX = e.getX();
            dragOffsetY = e.getY();
        });
        campagnesBox.setOnMouseDragged(e -> {
            if (!e.isPrimaryButtonDown()) {
                return;
            }
            Point2D p = rootStack.sceneToLocal(e.getSceneX(), e.getSceneY());
            double nextX = p.getX() - dragOffsetX;
            double nextY = p.getY() - dragOffsetY;
            deplacerCampagnesAvecLimites(nextX, nextY);
        });
    }

    private void deplacerCampagnesAvecLimites(double nextX, double nextY) {
        double largeur = campagnesBox.getWidth() > 0 ? campagnesBox.getWidth() : LARGEUR_CAMPAGNE;
        double hauteur = campagnesBox.getHeight() > 0 ? campagnesBox.getHeight() : campagnesBox.prefHeight(-1);
        double maxX = Math.max(0, rootStack.getWidth() - largeur);
        double maxY = Math.max(0, rootStack.getHeight() - hauteur);
        double x = Math.max(0, Math.min(nextX, maxX));
        double y = Math.max(0, Math.min(nextY, maxY));
        campagnesBox.setLayoutX(x);
        campagnesBox.setLayoutY(y);
    }

    private void positionnerCampagnesParDefaut() {
        campagnesBox.applyCss();
        campagnesBox.layout();
        Bounds b = campagnesBox.getLayoutBounds();
        double marge = 22;
        double x = Math.max(0, rootStack.getWidth() - b.getWidth() - marge);
        double y = marge;
        campagnesBox.setLayoutX(x);
        campagnesBox.setLayoutY(y);
        positionCampagneInitialisee = true;
    }

    private void demarrerRotationCampagnes() {
        if (campagnesActives.size() <= 1) {
            if (btnPauseCampagnes != null) {
                btnPauseCampagnes.setDisable(true);
                btnPauseCampagnes.setText("Unique");
            }
            return;
        }
        rotationCampagnes = new Timeline(new KeyFrame(Duration.seconds(2), e -> afficherCampagneSuivante()));
        rotationCampagnes.setCycleCount(Timeline.INDEFINITE);
        rotationCampagnes.play();
    }

    private void afficherCampagneSuivante() {
        if (campagnesActives.isEmpty()) {
            return;
        }
        campagneIndex = (campagneIndex + 1) % campagnesActives.size();
        afficherCampagne(campagneIndex);
    }

    private void afficherCampagne(int index) {
        if (campagnesActives.isEmpty() || campagneTitreLabel == null || campagneCorpsLabel == null) {
            return;
        }
        CampagneAide campagne = campagnesActives.get(index);
        campagneTitreLabel.setText(safe(campagne.getTitre()));
        campagneCorpsLabel.setText(safe(campagne.getCorps()));
        // La box est flottante (managed=false), donc on force le recalcul de taille
        // pour éviter le texte tronqué lors des changements de campagne.
        campagnesBox.applyCss();
        campagnesBox.autosize();
        deplacerCampagnesAvecLimites(campagnesBox.getLayoutX(), campagnesBox.getLayoutY());
    }

    private void basculerPauseCampagnes() {
        if (campagneEnPause) {
            pauseManuelleCampagnes = false;
            reprendreCampagnes();
        } else {
            pauseManuelleCampagnes = true;
            mettreCampagnesEnPauseManuelle();
        }
    }

    private void mettreCampagnesEnPauseManuelle() {
        campagneEnPause = true;
        if (btnPauseCampagnes != null) {
            btnPauseCampagnes.setText("Reprendre");
        }
        if (rotationCampagnes != null) {
            rotationCampagnes.pause();
        }
    }

    private void mettreCampagnesEnPauseSurvol() {
        if (pauseManuelleCampagnes || campagneEnPause) {
            return;
        }
        pauseSurvolCampagnes = true;
        campagneEnPause = true;
        if (rotationCampagnes != null) {
            rotationCampagnes.pause();
        }
    }

    private void reprendreCampagnesApresSurvol() {
        if (!pauseSurvolCampagnes || pauseManuelleCampagnes) {
            return;
        }
        pauseSurvolCampagnes = false;
        reprendreCampagnes();
    }

    private void reprendreCampagnes() {
        if (pauseManuelleCampagnes) {
            return;
        }
        campagneEnPause = false;
        if (btnPauseCampagnes != null) {
            btnPauseCampagnes.setText("Pause");
        }
        if (rotationCampagnes != null && campagnesActives.size() > 1) {
            rotationCampagnes.play();
        }
    }

    private VBox buildCard(Don don) {
        Label title = new Label(safe(don.getArticleDescription()));
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        Label quantite = new Label("Quantité : " + don.getQuantite() + " " + safe(don.getUnite()));
        quantite.setStyle("-fx-text-fill: #cbd5e1;");
        Label statut = new Label("Statut : " + safe(don.getStatut()));
        statut.setStyle(styleStatut(don.getStatut()));
        Label etat = new Label("État : " + safe(don.getEtat()));
        etat.setStyle("-fx-text-fill: #cbd5e1;");
        Label urgence = new Label("Urgence : " + safe(don.getNiveauUrgence()));
        urgence.setStyle("-fx-text-fill: #cbd5e1;");
        Label details = new Label("Détails : " + safe(nettoyerDetailsPaiement(don.getDetailsSupplementaires())));
        details.setStyle("-fx-text-fill: #94a3b8;");
        details.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row1 = new HBox(20, quantite, statut, spacer);
        HBox row2 = new HBox(20, etat, urgence);
        VBox card = new VBox(8, title, row1, row2, details);
        card.setPadding(new Insets(10));
        card.setStyle(CARD_STYLE_BASE);
        return card;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String nettoyerDetailsPaiement(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value
                .replaceAll("(?im)^\\s*Stripe\\s*$", "")
                .replaceAll("(?im)^\\s*checkout_session\\s*=.*$", "")
                .trim();
    }

    private static String styleStatut(String statutRaw) {
        String statut = statutRaw == null ? "" : statutRaw.trim().toLowerCase();
        return switch (statut) {
            case "en_attente", "en attente" -> "-fx-font-weight: bold; -fx-text-fill: #fbbf24;";
            case "rejete", "rejeté" -> "-fx-font-weight: bold; -fx-text-fill: #f87171;";
            case "valide", "accepte", "accepté" -> "-fx-font-weight: bold; -fx-text-fill: #4ade80;";
            default -> "-fx-font-weight: bold; -fx-text-fill: #e2e8f0;";
        };
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
