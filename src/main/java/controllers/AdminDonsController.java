package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminDonsController implements Initializable {

    private static final String CARD_STYLE = "-fx-background-color: #ffffff; -fx-border-color: #d6d8db;"
            + " -fx-border-radius: 8; -fx-background-radius: 8;";

    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox cardsContainer;

    @FXML
    private Button btnActualiser;

    @FXML
    private Button btnRetourListe;

    private final DonService donService = new DonService();
    private final ObservableList<Don> dons = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnActualiser.setOnAction(e -> chargerTous());
        btnRetourListe.setOnAction(e -> retourListe());
        chargerTous();
    }

    private void chargerTous() {
        try {
            List<Don> data = donService.getAll();
            data.sort(Comparator.comparingInt(Don::getId).reversed());
            dons.setAll(data);
            renderCards();
        } catch (SQLException ex) {
            afficherErreur("Chargement impossible", ex.getMessage());
        }
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        if (dons.isEmpty()) {
            Label empty = new Label("Aucun don en base.");
            empty.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14;");
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(20, 10, 20, 10));
            cardsContainer.getChildren().add(wrap);
            return;
        }

        for (Don don : dons) {
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
            dons.removeIf(d -> d.getId() == don.getId());
            renderCards();
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
                dons.sort(Comparator.comparingInt(Don::getId).reversed());
                renderCards();
                new Alert(Alert.AlertType.INFORMATION,
                        accepter ? "Le don a été accepté (statut : valide)." : "Le don a été rejeté.").showAndWait();
            } else {
                new Alert(Alert.AlertType.WARNING,
                        "Impossible : le statut n'est plus « en attente » (rafraîchissez la liste).").showAndWait();
                chargerTous();
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
