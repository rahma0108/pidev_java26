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
import java.util.Objects;
import java.util.ResourceBundle;

public class ListeDonsController implements Initializable {

    private static final String CARD_STYLE_BASE = "-fx-background-color: #ffffff; -fx-border-color: #d6d8db;"
            + " -fx-border-radius: 8; -fx-background-radius: 8;";
    private static final String CARD_STYLE_SELECTED = CARD_STYLE_BASE
            + " -fx-border-color: #2f80ed; -fx-border-width: 2;";

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
        btnModifier.setOnAction(e -> ouvrirModifier());
        btnSupprimer.setOnAction(e -> supprimerSelection());

        appliquerTri();
        appliquerFiltre();
        renderCards();
        majEtatBoutons();
    }

    private void majEtatBoutons() {
        boolean hasSelection = selectedDon != null;
        btnModifier.setDisable(!hasSelection);
        btnSupprimer.setDisable(!hasSelection);
    }

    private void chargerDepuisBase() {
        try {
            masterData.setAll(donService.getAll());
        } catch (SQLException ex) {
            afficherErreur("Impossible de charger les dons", ex.getMessage());
        }
        DonFiltreTriUtil.remplirComboStatuts(masterData, statutCombo);
        DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
    }

    private void appliquerFiltre() {
        filteredData.setPredicate(DonFiltreTriUtil.creerPredicate(
                searchField.getText(),
                statutCombo.getSelectionModel().getSelectedItem(),
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

    private void ouvrirModifier() {
        Don sel = selectedDon;
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez un don dans la liste pour le modifier.").showAndWait();
            return;
        }
        try {
            Stage stage = stageCourant();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/ModifierDon.fxml")));
            Parent root = loader.load();
            ModifierDonController ctrl = loader.getController();
            ctrl.setDon(sel);
            stage.setScene(new Scene(root, 560, 520));
        } catch (IOException ex) {
            afficherErreur("Ouverture du formulaire", ex.getMessage());
        }
    }

    private void supprimerSelection() {
        Don sel = selectedDon;
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Sélectionnez un don à supprimer.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer définitivement le don n°" + sel.getId() + " ?");
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
            Label empty = new Label("Aucun don à afficher (ajustez filtres / recherche).");
            empty.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14;");
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(20, 10, 20, 10));
            cardsContainer.getChildren().add(wrap);
        }
    }

    private VBox buildCard(Don don) {
        Label title = new Label("Don #" + don.getId() + " - " + safe(don.getArticleDescription()));
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        Label quantite = new Label("Quantité : " + don.getQuantite() + " " + safe(don.getUnite()));
        Label statut = new Label("Statut : " + safe(don.getStatut()));
        Label etat = new Label("État : " + safe(don.getEtat()));
        Label urgence = new Label("Urgence : " + safe(don.getNiveauUrgence()));
        Label cat = new Label("Catégorie (id) : " + don.getCategorieId());
        Label details = new Label("Détails : " + safe(don.getDetailsSupplementaires()));
        details.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row1 = new HBox(20, quantite, statut, spacer);
        HBox row2 = new HBox(20, etat, urgence, cat);
        VBox card = new VBox(8, title, row1, row2, details);
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

    private static void afficherErreur(String titre, String detail) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(detail != null ? detail : "Erreur inconnue.");
        a.showAndWait();
    }
}
