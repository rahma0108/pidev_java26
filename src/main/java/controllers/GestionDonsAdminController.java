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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Don;
import models.UrgenceDemande;
import services.CampagneAideService;
import services.CampagneIAService;
import services.DonService;
import services.UrgenceDemandeService;
import utils.MediLinkDialogs;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Fenêtre indépendante de gestion des dons (liste, filtres, actions admin).
 */
public class GestionDonsAdminController implements Initializable {

    private static final String CARD_STYLE = "-fx-background-color: #ffffff; -fx-border-color: #e2e8f0;"
            + " -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14;"
            + " -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 12, 0, 0, 2);";
    private static final int RECHERCHE_MAX = 255;
    private static final Pattern ENTIER_POSITIF_OU_VIDE = Pattern.compile("\\d*");

    @FXML
    private BorderPane rootPane;

    @FXML
    private Button btnActualiser;

    @FXML
    private Button btnTableauBord;

    @FXML
    private Button btnFermer;

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
    private TilePane cardsTile;

    @FXML
    private Label lblStatListeTotal;

    @FXML
    private Label lblStatListeAttente;

    @FXML
    private Label lblStatListeValide;

    @FXML
    private Label lblUrgencesTitre;

    @FXML
    private VBox urgencesListBox;

    private final DonService donService = new DonService();
    private final UrgenceDemandeService urgenceDemandeService = new UrgenceDemandeService();
    private final CampagneAideService campagneAideService = new CampagneAideService();
    private CampagneIAService campagneIa;
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

        btnActualiser.setOnAction(e -> {
            chargerDepuisBase();
            appliquerFiltre();
            rafraichirDemandesUrgence();
        });
        btnFermer.setOnAction(e -> fermerFenetre());
        btnTableauBord.setOnAction(e -> retourTableauBord());

        appliquerTri();
        appliquerFiltre();
        renderCards();
        rafraichirDemandesUrgence();
    }

    private void rafraichirDemandesUrgence() {
        urgencesListBox.getChildren().clear();
        try {
            var list = urgenceDemandeService.listerPourAdmin();
            long nAttente = list.stream().filter(u -> "en_attente".equals(u.getStatut())).count();
            lblUrgencesTitre.setText("Demandes d'urgence (" + nAttente + " en attente, " + list.size() + " au total)");
            for (UrgenceDemande d : list) {
                VBox row = new VBox(6);
                Label badge = new Label(libelleStatutUrgence(d.getStatut()));
                badge.setWrapText(true);
                Label excerpt = new Label(tronquer(d.getMessage(), 140));
                excerpt.setWrapText(true);
                excerpt.setStyle("-fx-text-fill: #334155; -fx-font-size: 11;");
                boolean attente = "en_attente".equals(d.getStatut());
                if (attente) {
                    badge.setStyle("-fx-text-fill: #c2410c; -fx-font-size: 10; -fx-font-weight: bold;");
                    row.setStyle("-fx-background-color: #fff7ed; -fx-border-color: #fed7aa; -fx-border-radius: 8; -fx-padding: 8;");
                    Button tr = new Button("Traiter");
                    tr.setStyle("-fx-background-color: #ea580c; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 12;");
                    tr.setOnAction(ev -> ouvrirTraitementUrgence(d));
                    row.getChildren().addAll(badge, excerpt, tr);
                } else if ("campagne_publiee".equals(d.getStatut())) {
                    badge.setStyle("-fx-text-fill: #15803d; -fx-font-size: 10; -fx-font-weight: bold;");
                    row.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 8; -fx-padding: 8;");
                    Button voir = new Button("Voir");
                    voir.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 12;");
                    voir.setOnAction(ev -> ouvrirLectureDemandeUrgence(d));
                    row.getChildren().addAll(badge, excerpt, voir);
                } else {
                    badge.setStyle("-fx-text-fill: #475569; -fx-font-size: 10; -fx-font-weight: bold;");
                    row.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 8;");
                    Button voir = new Button("Voir");
                    voir.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 12;");
                    voir.setOnAction(ev -> ouvrirLectureDemandeUrgence(d));
                    row.getChildren().addAll(badge, excerpt, voir);
                }
                urgencesListBox.getChildren().add(row);
            }
        } catch (IOException ex) {
            lblUrgencesTitre.setText("Demandes d'urgence (erreur)");
            Label err = new Label(
                    "Lecture du fichier local impossible (~/.medilink/urgence_campagnes.store). "
                            + (ex.getMessage() != null ? ex.getMessage() : ""));
            err.setWrapText(true);
            err.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 11;");
            urgencesListBox.getChildren().add(err);
        }
    }

    private void ouvrirLectureDemandeUrgence(UrgenceDemande d) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Demande d'urgence");
        dialog.setHeaderText(libelleStatutUrgence(d.getStatut()));
        VBox content = new VBox(10);
        TextArea full = new TextArea(d.getMessage());
        full.setEditable(false);
        full.setWrapText(true);
        full.setPrefRowCount(10);
        Label note = new Label("Demande déjà traitée — consultation seule.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        Button btnVoirPiece = creerBoutonPieceJointe(d);
        if (btnVoirPiece != null) {
            content.getChildren().add(btnVoirPiece);
        }
        content.getChildren().addAll(full, note);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK);
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }
        MediLinkDialogs.style(dialog);
        dialog.showAndWait();
    }

    private static String libelleStatutUrgence(String statut) {
        if (statut == null) {
            return "—";
        }
        return switch (statut) {
            case "en_attente" -> "En attente de traitement";
            case "sans_campagne" -> "Traitée — pas de campagne publique";
            case "campagne_publiee" -> "Traitée — campagne publiée sur l'accueil";
            default -> statut;
        };
    }

    private void ouvrirTraitementUrgence(UrgenceDemande d) {
        if (!"en_attente".equals(d.getStatut())) {
            ouvrirLectureDemandeUrgence(d);
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Demande d'urgence");
        dialog.setHeaderText("Message reçu depuis l'accueil public");
        VBox content = new VBox(10);
        TextArea full = new TextArea(d.getMessage());
        full.setEditable(false);
        full.setWrapText(true);
        full.setPrefRowCount(10);
        Label help = new Label(
                "« Pas besoin de campagne » : la demande est archivée sans affichage public. "
                        + "« Lancer une campagne (IA) » : l'IA rédige titre + texte ; la campagne apparaît sur l'accueil.");
        help.setWrapText(true);
        help.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11;");
        Button btnVoirPiece = creerBoutonPieceJointe(d);
        if (btnVoirPiece != null) {
            content.getChildren().add(btnVoirPiece);
        }
        content.getChildren().addAll(full, help);
        dialog.getDialogPane().setContent(content);
        ButtonType pas = new ButtonType("Pas besoin de campagne", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType lancer = new ButtonType("Lancer une campagne (IA)", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(pas, lancer);
        if (rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }
        MediLinkDialogs.style(dialog);
        var result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        if (result.get() == pas) {
            try {
                urgenceDemandeService.marquerSansCampagne(d.getId());
                rafraichirDemandesUrgence();
                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Demande classée sans campagne publique.");
                MediLinkDialogs.style(ok);
                ok.showAndWait();
            } catch (IOException ex) {
                afficherErreur("Mise à jour impossible", ex.getMessage());
            }
            return;
        }
        try {
            if (campagneIa == null) {
                campagneIa = new CampagneIAService();
            }
            var gen = campagneIa.genererDepuisMessage(d.getMessage());
            campagneAideService.publier(d.getId(), gen.titre(), gen.corps());
            rafraichirDemandesUrgence();
            Alert ok = new Alert(Alert.AlertType.INFORMATION,
                    "Campagne générée par l'IA et publiée sur l'accueil.\n\nTitre : " + gen.titre());
            MediLinkDialogs.style(ok);
            ok.showAndWait();
        } catch (IOException | IllegalStateException ex) {
            afficherErreur("IA ou publication", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private static String tronquer(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private Button creerBoutonPieceJointe(UrgenceDemande d) {
        if (d.getPieceImagePath() == null || d.getPieceImagePath().isBlank()) {
            return null;
        }
        Path piece = Path.of(d.getPieceImagePath());
        if (!Files.isRegularFile(piece)) {
            Button faux = new Button("Pièce jointe introuvable");
            faux.setDisable(true);
            faux.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 8;");
            return faux;
        }
        Button voir = new Button("Ouvrir justificatif");
        voir.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 6 12;");
        voir.setOnAction(ev -> ouvrirFichierLocal(piece));
        return voir;
    }

    private void ouvrirFichierLocal(Path fichier) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String chemin = fichier.toAbsolutePath().toString();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "", chemin);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", chemin);
            } else {
                pb = new ProcessBuilder("xdg-open", chemin);
            }
            pb.start();
        } catch (IOException ex) {
            afficherErreur("Ouverture du justificatif", ex.getMessage());
        }
    }

    private void fermerFenetre() {
        Stage s = (Stage) rootPane.getScene().getWindow();
        s.close();
    }

    private void retourTableauBord() {
        Stage me = (Stage) rootPane.getScene().getWindow();
        Stage owner = (Stage) me.getOwner();
        me.close();
        if (owner != null) {
            owner.toFront();
        }
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

    private boolean bornesQuantiteCoherentes() {
        Integer qMin = DonFiltreTriUtil.parseIntOptional(tfQuantiteMin.getText());
        Integer qMax = DonFiltreTriUtil.parseIntOptional(tfQuantiteMax.getText());
        if (qMin != null && qMax != null && qMin > qMax) {
            Alert w = new Alert(Alert.AlertType.WARNING,
                    "« Qté min » ne peut pas être supérieure à « Qté max ».");
            MediLinkDialogs.style(w);
            w.showAndWait();
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

    private void majStatsListe() {
        int n = sortedData.size();
        int att = (int) sortedData.stream().filter(d -> "en_attente".equals(safeStatut(d.getStatut()))).count();
        int val = (int) sortedData.stream().filter(d -> "valide".equals(safeStatut(d.getStatut()))).count();
        lblStatListeTotal.setText(Integer.toString(n));
        lblStatListeAttente.setText(Integer.toString(att));
        lblStatListeValide.setText(Integer.toString(val));
    }

    private static String safeStatut(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase();
    }

    private void renderCards() {
        majStatsListe();
        cardsTile.getChildren().clear();
        if (sortedData.isEmpty()) {
            Label empty = new Label(masterData.isEmpty()
                    ? "Aucun don en base."
                    : "Aucun don à afficher (ajustez filtres / recherche).");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14;");
            VBox wrap = new VBox(empty);
            wrap.setPadding(new Insets(20, 10, 20, 10));
            cardsTile.getChildren().add(wrap);
            return;
        }

        for (Don don : sortedData) {
            cardsTile.getChildren().add(buildCard(don));
        }
    }

    private VBox buildCard(Don don) {
        Label title = new Label(safe(don.getArticleDescription()));
        title.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        title.setWrapText(true);
        Label statut = new Label("Statut : " + safe(don.getStatut()));
        statut.setStyle("-fx-font-weight: bold;");
        Label quantite = new Label("Quantité : " + don.getQuantite() + " " + safe(don.getUnite()));
        Label etat = new Label("État : " + safe(don.getEtat()));
        Label urgence = new Label("Urgence : " + safe(don.getNiveauUrgence()));
        Label details = new Label("Détails : " + safe(don.getDetailsSupplementaires()));
        details.setWrapText(true);

        VBox iaBloc = creerBlocAvisIa(don);

        Button btnModifier = new Button("Modifier");
        Button btnSupprimer = new Button("Supprimer");
        btnModifier.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 8;");
        btnSupprimer.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-background-radius: 8;");
        btnModifier.setOnAction(e -> ouvrirModifier(don));
        btnSupprimer.setOnAction(e -> supprimerAvecControle(don));

        Button btnAccepter = new Button("Accepter");
        Button btnRejeter = new Button("Rejeter");
        btnAccepter.setStyle("-fx-background-color: #16a34a; -fx-text-fill: white; -fx-background-radius: 8;");
        btnRejeter.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-background-radius: 8;");
        btnAccepter.setOnAction(e -> traiterDon(don, true));
        btnRejeter.setOnAction(e -> traiterDon(don, false));
        boolean enAttente = "en_attente".equals(don.getStatut());
        btnAccepter.setDisable(!enAttente);
        btnRejeter.setDisable(!enAttente);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, spacer, btnModifier, btnSupprimer, btnAccepter, btnRejeter);

        VBox card = new VBox(10, title, statut, quantite, etat, urgence, details, iaBloc, actions);
        card.setPadding(new Insets(12));
        card.setStyle(CARD_STYLE);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void ouvrirModifier(Don don) {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/ModifierDon.fxml")));
            Parent root = loader.load();
            ModifierDonController ctrl = loader.getController();
            ctrl.setRetourFxml("/GestionDonsAdmin.fxml");
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
        confirm.setContentText("Voulez-vous vraiment supprimer ce don ?");
        MediLinkDialogs.style(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            donService.delete(don.getId());
            masterData.removeIf(d -> d.getId() == don.getId());
            DonFiltreTriUtil.remplirComboStatuts(masterData, statutCombo);
            DonFiltreTriUtil.remplirFiltresSecondaires(masterData,
                    filtreUrgenceCombo, filtreEtatCombo, filtreUniteCombo, filtreCategorieCombo);
            appliquerFiltre();
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Don supprimé.");
            MediLinkDialogs.style(ok);
            ok.showAndWait();
        } catch (SQLException ex) {
            afficherErreur("Suppression impossible", ex.getMessage());
        }
    }

    private void traiterDon(Don don, boolean accepter) {
        if (!"en_attente".equals(don.getStatut())) {
            Alert w = new Alert(Alert.AlertType.WARNING,
                    "Seuls les dons « en attente » peuvent être acceptés ou rejetés.");
            MediLinkDialogs.style(w);
            w.showAndWait();
            return;
        }
        String action = accepter ? "accepter" : "rejeter";
        String prochainStatut = accepter ? "valide" : "rejete";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Décision");
        confirm.setHeaderText(null);
        StringBuilder msg = new StringBuilder();
        msg.append("Confirmer : ").append(action).append(" ce don ?\nStatut → ").append(prochainStatut).append(".");
        if (avisIaPresent(don)) {
            msg.append("\n\n— Avis IA —");
            if (texteUtile(don.getDecisionIA())) {
                msg.append("\nDécision : ").append(don.getDecisionIA().trim());
            }
            if (texteUtile(don.getRaisonIA())) {
                msg.append("\n").append(don.getRaisonIA().trim());
            }
            if (texteUtile(don.getTraductionIA())) {
                msg.append("\nTraduction : ").append(don.getTraductionIA().trim());
            }
        }
        if (msg.length() > 550) {
            TextArea ta = new TextArea(msg.toString());
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(14);
            ta.setPrefColumnCount(56);
            confirm.getDialogPane().setContent(ta);
            confirm.getDialogPane().setMinWidth(520);
        } else {
            confirm.setContentText(msg.toString());
        }
        MediLinkDialogs.style(confirm);
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
                Alert info = new Alert(Alert.AlertType.INFORMATION,
                        accepter ? "Le don a été accepté (statut : valide)." : "Le don a été rejeté.");
                MediLinkDialogs.style(info);
                info.showAndWait();
            } else {
                Alert warn = new Alert(Alert.AlertType.WARNING,
                        "Impossible : le statut n'est plus « en attente » (rafraîchissez la liste).");
                MediLinkDialogs.style(warn);
                warn.showAndWait();
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

    private static boolean texteUtile(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean avisIaPresent(Don don) {
        return texteUtile(don.getDecisionIA()) || texteUtile(don.getRaisonIA()) || texteUtile(don.getTraductionIA());
    }

    /**
     * Les {@link Label} tronquent souvent avec « … » dans une tuile de largeur fixe ; une {@link TextArea}
     * en lecture seule permet tout le texte avec défilement.
     */
    private static TextArea zoneTexteIaLectureSeule(String texte, boolean italique) {
        TextArea ta = new TextArea(texte != null ? texte : "");
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setFocusTraversable(false);
        int lignes = Math.max(2, Math.min(20, (texte != null ? texte.length() : 0) / 65 + 2));
        ta.setPrefRowCount(lignes);
        ta.setMinHeight(Region.USE_PREF_SIZE);
        ta.setMaxHeight(280);
        ta.setMaxWidth(Double.MAX_VALUE);
        String style = italique
                ? "-fx-font-style: italic; -fx-text-fill: #475569; -fx-control-inner-background: rgba(255,255,255,0.92);"
                : "-fx-text-fill: #334155; -fx-control-inner-background: rgba(255,255,255,0.92);";
        ta.setStyle(style + " -fx-background-insets: 0; -fx-padding: 6;");
        return ta;
    }

    /** Bloc affiché sur chaque carte : aide admin pour les dons en attente (nécessite persistance IA en base). */
    private static VBox creerBlocAvisIa(Don don) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(8, 0, 0, 0));
        if (!avisIaPresent(don)) {
            if ("en_attente".equals(don.getStatut())) {
                Label vide = new Label("Avis IA : absent pour ce don (ancien dépôt sans fichier, ou dossier différent). "
                        + "Les avis sont dans le dossier gdons_avis_ia du projet, ou dans don.ia.storage.dir. "
                        + "Créez un nouveau don après recompilation pour tester.");
                vide.setWrapText(true);
                vide.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");
                box.getChildren().add(vide);
            }
            return box;
        }
        Label titreIa = new Label("Avis IA (aide à la décision)");
        titreIa.setStyle("-fx-font-weight: bold; -fx-text-fill: #1d4ed8;");
        box.getChildren().add(titreIa);
        if (texteUtile(don.getDecisionIA())) {
            Label dec = new Label("Décision : " + don.getDecisionIA().trim());
            dec.setWrapText(true);
            dec.setMaxWidth(Double.MAX_VALUE);
            dec.setStyle("-fx-text-fill: #0f172a;");
            box.getChildren().add(dec);
        }
        if (texteUtile(don.getRaisonIA())) {
            box.getChildren().add(zoneTexteIaLectureSeule(don.getRaisonIA().trim(), false));
        }
        if (texteUtile(don.getTraductionIA())) {
            box.getChildren().add(zoneTexteIaLectureSeule("Traduction : " + don.getTraductionIA().trim(), true));
        }
        box.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 10; -fx-padding: 10; -fx-border-color: #bfdbfe; -fx-border-radius: 10;");
        return box;
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
