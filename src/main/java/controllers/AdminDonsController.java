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
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class AdminDonsController implements Initializable {

    private static final String CARD_STYLE = "-fx-background-color: #ffffff; -fx-border-color: #d7deea;"
            + " -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 2;";
    private static final int RECHERCHE_MAX = 255;
    private static final Pattern ENTIER_POSITIF_OU_VIDE = Pattern.compile("\\d*");

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
    private Button btnActualiser;

    @FXML
    private Button btnRetourListe;

    @FXML
    private VBox cardsContainer;

    private final DonService donService = new DonService();
    private final ObservableList<Don> masterData = FXCollections.observableArrayList();
    private FilteredList<Don> filteredData;
    private SortedList<Don> sortedData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DonFiltreTriUtil.preparerCombosTri(tri1ChampCombo, tri2ChampCombo, tri3ChampCombo);
        DonFiltreTriUtil.preparerCombosOrdre(tri1OrdreCombo, tri2OrdreCombo, tri3OrdreCombo);

        searchField.setTextFormatter(new TextFormatter<>(limiteLongueur(RECHERCHE_MAX)));
        tfQuantiteMin.setTextFormatter(new TextFormatter<>(entierPositifOuVide()));
        tfQuantiteMax.setTextFormatter(new TextFormatter<>(entierPositifOuVide()));

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

        tri1ChampCombo.getSelectionModel().select("ID");
        tri1OrdreCombo.getSelectionModel().select(DonFiltreTriUtil.ORDRE_DECROISSANT);

        btnActualiser.setOnAction(e -> {
            chargerDepuisBase();
            appliquerFiltre();
        });
        btnRetourListe.setOnAction(e -> retourListe());

        appliquerTri();
        appliquerFiltre();
        renderCards();
    }

    private static UnaryOperator<TextFormatter.Change> limiteLongueur(int max) {
        return c -> c.getControlNewText().length() <= max ? c : null;
    }

    private static UnaryOperator<TextFormatter.Change> entierPositifOuVide() {
        return c -> ENTIER_POSITIF_OU_VIDE.matcher(c.getControlNewText()).matches() ? c : null;
    }

    private void chargerDepuisBase() {
        try {
            masterData.setAll(donService.getAll());
        } catch (SQLException ex) {
            afficherErreur("Chargement impossible", ex.getMessage());
        }
        DonFiltreTriUtil.remplirComboStatuts(masterData, statutCombo);
        DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
    }

    private void appliquerFiltre() {
        if (!bornesQuantiteCoherentes()) {
            return;
        }
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

    /**
     * Vérifie qté min / max : entiers déjà contrôlés à la saisie ; ici cohérence min ≤ max.
     */
    private boolean bornesQuantiteCoherentes() {
        Integer qMin = DonFiltreTriUtil.parseIntOptional(tfQuantiteMin.getText());
        Integer qMax = DonFiltreTriUtil.parseIntOptional(tfQuantiteMax.getText());
        if (qMin != null && qMax != null && qMin > qMax) {
            new Alert(Alert.AlertType.WARNING,
                    "« Qté min » ne peut pas être supérieure à « Qté max ».").showAndWait();
            return false;
        }
        return true;
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

    private void renderCards() {
        cardsContainer.getChildren().clear();
        if (sortedData.isEmpty()) {
            Label empty = new Label(masterData.isEmpty()
                    ? "Aucun don en base."
                    : "Aucun don à afficher (ajustez filtres / recherche).");
            empty.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14;");
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(20, 10, 20, 10));
            cardsContainer.getChildren().add(wrap);
            return;
        }

        for (Don don : sortedData) {
            cardsContainer.getChildren().add(buildCard(don));
        }
    }

    private VBox buildCard(Don don) {
        Label title = new Label(safe(don.getArticleDescription()));
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        Label statut = new Label("Statut : " + safe(don.getStatut()));
        statut.setStyle("-fx-font-weight: bold;");
        Label quantite = new Label("Quantité : " + don.getQuantite() + " " + safe(don.getUnite()));
        Label etat = new Label("État : " + safe(don.getEtat()));
        Label urgence = new Label("Urgence : " + safe(don.getNiveauUrgence()));
        Label cat = new Label("Catégorie (id) : " + don.getCategorieId());
        Label details = new Label("Détails : " + safe(don.getDetailsSupplementaires()));
        details.setWrapText(true);

        Button btnModifier = new Button("Modifier");
        Button btnSupprimer = new Button("Supprimer");
        btnModifier.setOnAction(e -> ouvrirModifier(don));
        btnSupprimer.setOnAction(e -> supprimerAvecControle(don));

        Button btnAccepter = new Button("Accepter");
        Button btnRejeter = new Button("Rejeter");
        btnAccepter.setOnAction(e -> traiterDon(don, true));
        btnRejeter.setOnAction(e -> traiterDon(don, false));
        boolean enAttente = "en_attente".equals(don.getStatut());
        btnAccepter.setDisable(!enAttente);
        btnRejeter.setDisable(!enAttente);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, spacer, btnModifier, btnSupprimer, btnAccepter, btnRejeter);

        VBox card = new VBox(8, title, statut, quantite, etat, urgence, cat, details, actions);
        card.setPadding(new Insets(10));
        card.setStyle(CARD_STYLE);
        return card;
    }

    private void ouvrirModifier(Don don) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/ModifierDon.fxml")));
            Parent root = loader.load();
            ModifierDonController ctrl = loader.getController();
            ctrl.setRetourFxml("/AdminDons.fxml");
            ctrl.setDon(don);
            stage.setScene(new Scene(root, 560, 520));
        } catch (IOException ex) {
            afficherErreur("Ouverture du formulaire", ex.getMessage());
        }
    }

    private void supprimerAvecControle(Don don) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer le don n°" + don.getId() + " ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        TextInputDialog saisie = new TextInputDialog();
        saisie.setTitle("Confirmation");
        saisie.setHeaderText("Pour confirmer la suppression, saisissez l'identifiant du don.");
        saisie.setContentText("ID du don :");

        Optional<String> rep = saisie.showAndWait();
        if (rep.isEmpty()) {
            return;
        }
        String texte = rep.get().trim();
        if (texte.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "L'ID ne peut pas être vide.").showAndWait();
            return;
        }
        try {
            int saisi = Integer.parseInt(texte);
            if (saisi != don.getId()) {
                new Alert(Alert.AlertType.WARNING,
                        "L'ID saisi ne correspond pas au don. Suppression annulée.").showAndWait();
                return;
            }
        } catch (NumberFormatException ex) {
            new Alert(Alert.AlertType.ERROR, "L'ID doit être un nombre entier valide.").showAndWait();
            return;
        }

        try {
            donService.delete(don.getId());
            masterData.removeIf(d -> d.getId() == don.getId());
            DonFiltreTriUtil.remplirComboStatuts(masterData, statutCombo);
            DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                    filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
            appliquerFiltre();
            new Alert(Alert.AlertType.INFORMATION, "Don supprimé.").showAndWait();
        } catch (SQLException ex) {
            afficherErreur("Suppression impossible", ex.getMessage());
        }
    }

    private void traiterDon(Don don, boolean accepter) {
        if (!"en_attente".equals(don.getStatut())) {
            new Alert(Alert.AlertType.WARNING,
                    "Seuls les dons « en attente » peuvent être acceptés ou rejetés.").showAndWait();
            return;
        }
        String action = accepter ? "accepter" : "rejeter";
        String prochainStatut = accepter ? "valide" : "rejete";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Décision");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirmer : " + action + " le don n°" + don.getId() + " ?\nStatut → " + prochainStatut + ".");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            boolean ok = accepter ? donService.validerDon(don.getId()) : donService.rejeterDon(don.getId());
            if (ok) {
                don.setStatut(prochainStatut);
                DonFiltreTriUtil.remplirComboStatuts(masterData, statutCombo);
                DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                        filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
                appliquerFiltre();
                new Alert(Alert.AlertType.INFORMATION,
                        accepter ? "Le don a été accepté (statut : valide)." : "Le don a été rejeté.").showAndWait();
            } else {
                new Alert(Alert.AlertType.WARNING,
                        "Impossible : le statut n'est plus « en attente » (rafraîchissez la liste).").showAndWait();
                chargerDepuisBase();
                appliquerFiltre();
            }
        } catch (SQLException ex) {
            afficherErreur("Action impossible", ex.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void retourListe() {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/ListeDons.fxml")));
            stage.setScene(new Scene(root, 960, 680));
        } catch (IOException ex) {
            afficherErreur("Retour impossible", ex.getMessage());
        }
    }

    private static void afficherErreur(String titre, String detail) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(detail != null ? detail : "Erreur inconnue.");
        a.showAndWait();
    }
}
