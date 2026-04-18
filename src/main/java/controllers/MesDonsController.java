package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Don;
import services.DonService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class MesDonsController implements Initializable {

    private static final String CARD_STYLE_BASE = "-fx-background-color: #ffffff; -fx-border-color: #d7deea;"
            + " -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 2;";
    private static final String CARD_STYLE_SELECTED = CARD_STYLE_BASE
            + " -fx-border-color: #2563eb; -fx-border-width: 2;";
    private static final Set<String> STATUTS_MES_DONS = Set.of("en_attente", "valide");

    private Map<Integer, String> libellesCategorieCourants = Map.of();

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statutCombo;

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
    private Button btnModifier;

    @FXML
    private Button btnSupprimer;

    @FXML
    private Button btnAccueil;

    @FXML
    private Button btnEspaceAdmin;

    @FXML
    private VBox cardsContainer;

    private final DonService donService = new DonService();
    private final ObservableList<Don> masterData = FXCollections.observableArrayList();
    private FilteredList<Don> filteredData;
    private SortedList<Don> sortedData;
    private Don selectedDon;
    private VBox selectedCard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DonFiltreTriUtil.preparerCombosTri(tri1ChampCombo, tri2ChampCombo, tri3ChampCombo);
        DonFiltreTriUtil.preparerCombosOrdre(tri1OrdreCombo, tri2OrdreCombo, tri3OrdreCombo);

        chargerDepuisBase();

        filteredData = new FilteredList<>(masterData, don -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.addListener((ListChangeListener<Don>) change -> renderCards());

        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltre());
        statutCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltre());
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
        btnModifier.setOnAction(e -> ouvrirModifierDon(selectedDon));
        btnSupprimer.setOnAction(e -> supprimerDon(selectedDon));
        btnAccueil.setOnAction(e -> ouvrirAccueil());
        btnEspaceAdmin.setOnAction(e -> ouvrirEspaceAdmin());

        appliquerTri();
        appliquerFiltre();
        renderCards();
        majEtatBoutons();
    }

    /** Modification / suppression réservées aux dons en attente ou validés sur cet écran. */
    private static boolean peutModifierOuSupprimer(Don d) {
        if (d == null || d.getStatut() == null) {
            return false;
        }
        String s = d.getStatut().trim().toLowerCase();
        return "en_attente".equals(s) || "valide".equals(s);
    }

    private void majEtatBoutons() {
        boolean ok = peutModifierOuSupprimer(selectedDon);
        btnModifier.setDisable(!ok);
        btnSupprimer.setDisable(!ok);
    }

    private void chargerDepuisBase() {
        try {
            masterData.setAll(donService.getAll());
        } catch (SQLException ex) {
            afficherErreur("Impossible de charger les dons", ex.getMessage());
        }
        DonFiltreTriUtil.remplirComboStatuts(masterData, statutCombo);
        statutCombo.getItems().removeIf(s -> !"Tous".equals(s) && !"en_attente".equals(s) && !"valide".equals(s));
        if (statutCombo.getSelectionModel().getSelectedItem() == null) {
            statutCombo.getSelectionModel().selectFirst();
        }
        DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
    }

    private void appliquerFiltre() {
        var basePredicate = DonFiltreTriUtil.creerPredicate(
                searchField.getText(),
                statutCombo.getSelectionModel().getSelectedItem(),
                filtreUrgenceCombo.getSelectionModel().getSelectedItem(),
                filtreEtatCombo.getSelectionModel().getSelectedItem(),
                filtreUniteCombo.getSelectionModel().getSelectedItem(),
                filtreCategorieCombo.getValue(),
                DonFiltreTriUtil.parseIntOptional(tfQuantiteMin.getText()),
                DonFiltreTriUtil.parseIntOptional(tfQuantiteMax.getText()));

        filteredData.setPredicate(don -> STATUTS_MES_DONS.contains(safe(don.getStatut())) && basePredicate.test(don));
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

    private void ouvrirAccueil() {
        try {
            Stage stage = stageCourant();
            Parent root = FXMLLoader.load(Objects.requireNonNull(
                    getClass().getResource("/ListeDons.fxml")));
            stage.setScene(new Scene(root, 960, 680));
        } catch (IOException ex) {
            afficherErreur("Ouverture accueil", ex.getMessage());
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

    private void ouvrirModifierDon(Don sel) {
        if (!peutModifierOuSupprimer(sel)) {
            new Alert(Alert.AlertType.WARNING,
                    "Sélectionnez un don « en attente » ou « valide » pour le modifier.").showAndWait();
            return;
        }
        try {
            Stage stage = stageCourant();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/ModifierDon.fxml")));
            Parent root = loader.load();
            ModifierDonController ctrl = loader.getController();
            ctrl.setRetourFxml("/MesDons.fxml");
            ctrl.setDon(sel);
            stage.setScene(new Scene(root, 560, 520));
        } catch (IOException ex) {
            afficherErreur("Ouverture du formulaire", ex.getMessage());
        }
    }

    private void supprimerDon(Don sel) {
        if (!peutModifierOuSupprimer(sel)) {
            new Alert(Alert.AlertType.WARNING,
                    "Sélectionnez un don « en attente » ou « valide » pour le supprimer.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer définitivement ce don ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            donService.delete(sel.getId());
            selectedDon = null;
            selectedCard = null;
            chargerDepuisBase();
            appliquerFiltre();
            majEtatBoutons();
            new Alert(Alert.AlertType.INFORMATION, "Don supprimé.").showAndWait();
        } catch (SQLException ex) {
            afficherErreur("Suppression impossible", ex.getMessage());
        }
    }

    private Stage stageCourant() {
        return (Stage) rootPane.getScene().getWindow();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        libellesCategorieCourants = DonFormChoices.categoriesSansPlaceholder().stream()
                .collect(Collectors.toMap(DonFormChoices.CategorieOption::id, DonFormChoices.CategorieOption::libelle, (a, b) -> a));
        boolean selectedStillVisible = false;
        for (Don don : sortedData) {
            VBox card = buildCard(don);
            if (selectedDon != null && don.getId() == selectedDon.getId()) {
                selectedStillVisible = true;
                appliquerSelectionCard(card, don);
            }
            cardsContainer.getChildren().add(card);
        }
        if (!selectedStillVisible) {
            selectedDon = null;
            selectedCard = null;
            majEtatBoutons();
        }
        if (sortedData.isEmpty()) {
            Label empty = new Label("Aucun don à afficher (mes dons : en attente + valides).");
            empty.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14;");
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(20, 10, 20, 10));
            cardsContainer.getChildren().add(wrap);
        }
    }

    private VBox buildCard(Don don) {
        Label title = new Label(safe(don.getArticleDescription()));
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        Label quantite = new Label("Quantité : " + don.getQuantite() + " " + safe(don.getUnite()));
        Label statut = new Label("Statut : " + safe(don.getStatut()));
        statut.setStyle(styleStatut(don.getStatut()));
        Label etat = new Label("État : " + safe(don.getEtat()));
        Label urgence = new Label("Urgence : " + safe(don.getNiveauUrgence()));
        Label cat = new Label("Catégorie : " + libellesCategorieCourants.getOrDefault(don.getCategorieId(), "—"));
        Label details = new Label("Détails : " + safe(don.getDetailsSupplementaires()));
        details.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row1 = new HBox(20, quantite, statut, spacer);
        HBox row2 = new HBox(20, etat, urgence, cat);

        Button btnMod = new Button("Modifier");
        Button btnSup = new Button("Supprimer");
        boolean actionsOk = peutModifierOuSupprimer(don);
        btnMod.setDisable(!actionsOk);
        btnSup.setDisable(!actionsOk);
        btnMod.setOnAction(e -> ouvrirModifierDon(don));
        btnSup.setOnAction(e -> supprimerDon(don));

        Region spacerActions = new Region();
        HBox.setHgrow(spacerActions, Priority.ALWAYS);
        HBox actions = new HBox(10, spacerActions, btnMod, btnSup);

        VBox card = new VBox(8, title, row1, row2, details, actions);
        card.setPadding(new Insets(10));
        card.setStyle(CARD_STYLE_BASE);
        card.setOnMouseClicked(e -> appliquerSelectionCard(card, don));
        return card;
    }

    private void appliquerSelectionCard(VBox card, Don don) {
        if (selectedCard != null) {
            selectedCard.setStyle(CARD_STYLE_BASE);
        }
        selectedDon = don;
        selectedCard = card;
        selectedCard.setStyle(CARD_STYLE_SELECTED);
        majEtatBoutons();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String styleStatut(String statutRaw) {
        String statut = statutRaw == null ? "" : statutRaw.trim().toLowerCase();
        return switch (statut) {
            case "en_attente", "en attente" -> "-fx-font-weight: bold; -fx-text-fill: #f59e0b;";
            case "rejete", "rejeté" -> "-fx-font-weight: bold; -fx-text-fill: #dc2626;";
            case "valide", "accepte", "accepté" -> "-fx-font-weight: bold; -fx-text-fill: #16a34a;";
            default -> "-fx-font-weight: bold;";
        };
    }

    private static void afficherErreur(String titre, String detail) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(detail != null ? detail : "Erreur inconnue.");
        a.showAndWait();
    }
}
