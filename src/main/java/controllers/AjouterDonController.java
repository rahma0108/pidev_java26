package controllers;



import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;

import javafx.fxml.Initializable;

import javafx.scene.Parent;

import javafx.scene.Scene;

import javafx.scene.control.Alert;

import javafx.scene.control.Button;

import javafx.scene.control.ComboBox;

import javafx.scene.control.DatePicker;

import javafx.scene.control.Label;

import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;

import javafx.scene.control.TextArea;

import javafx.scene.control.TextField;
import javafx.concurrent.Task;

import javafx.application.Platform;

import javafx.scene.layout.VBox;

import javafx.scene.paint.Color;

import javafx.scene.canvas.Canvas;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.scene.web.WebView;

import models.Don;

import ui.ParticleBackground;

import services.DonIAService;
import services.DonService;
import services.StripePaymentService;

import utils.MediLinkDialogs;



import java.io.IOException;

import java.net.URL;

import java.sql.Date;

import java.sql.SQLException;

import java.util.Objects;

import java.util.ResourceBundle;



public class AjouterDonController implements Initializable {



    @FXML

    private ComboBox<DonFormChoices.CategorieOption> cbCategorie;



    @FXML

    private TextField tfDescription;



    @FXML

    private TextField tfQuantite;



    @FXML

    private ComboBox<String> cbUnite;



    @FXML

    private TextArea taDetails;



    @FXML

    private ComboBox<String> cbEtat;



    @FXML

    private ComboBox<String> cbNiveauUrgence;



    @FXML

    private DatePicker dpExpiration;



    @FXML

    private Button btnEnregistrer;



    @FXML

    private Button btnAnnuler;



    @FXML

    private StackPane rootStack;



    @FXML

    private Canvas particleCanvas;



    @FXML

    private VBox formRoot;



    @FXML

    private Label lblTitre;



    @FXML

    private Label lblCat;



    @FXML

    private Label lblDesc;



    @FXML

    private Label lblQty;



    @FXML

    private Label lblUnite;



    @FXML

    private Label lblDetails;



    @FXML

    private Label lblEtat;



    @FXML

    private Label lblUrg;



    @FXML

    private Label lblExp;



    @FXML
    private Label lblMontant;

    @FXML
    private TextField tfMontant;

    @FXML
    private ProgressIndicator piPaiement;


    private ParticleBackground particules;



    private final DonService donService = new DonService();

    /** Initialisé à la demande (nécessite anthropic.api.key ou ANTHROPIC_API_KEY). */
    private DonIAService donIAService;
    private StripePaymentService stripePaymentService;



    @Override

    public void initialize(URL location, ResourceBundle resources) {

        particules = new ParticleBackground(particleCanvas, rootStack, 96);

        particules.play();

        DonFormChoices.preparerComboCategorieAjout(cbCategorie);

        DonFormChoices.preparerComboString(cbUnite, DonFormChoices.UNITES, "Unités");

        DonFormChoices.preparerComboString(cbEtat, DonFormChoices.ETATS_DON, "Neuf / Non ouvert");

        DonFormChoices.preparerComboString(cbNiveauUrgence, DonFormChoices.NIVEAUX_URGENCE, "Moyen");

        appliquerStylesFormulaire();
        cbCategorie.valueProperty().addListener((obs, oldV, newV) -> appliquerModeCategorie());
        appliquerModeCategorie();

        btnEnregistrer.setOnAction(e -> enregistrer());

        btnAnnuler.setOnAction(e -> retourListe());

    }



    private static final Color TEXTE_COMBO = Color.web("#e2e8f0");

    private static final Color TEXTE_INVITE_COMBO = Color.web("#94a3b8");

    /** Apparence sombre lisible sur le fond particules — sans feuille CSS externe. */
    private void appliquerStylesFormulaire() {
        rootStack.setStyle("-fx-background-color: transparent;");
        formRoot.setStyle(
                "-fx-background-color: rgba(15,23,42,0.72);"
                        + "-fx-background-radius: 16;"
                        + "-fx-border-color: rgba(148,163,184,0.22);"
                        + "-fx-border-radius: 16;"
                        + "-fx-border-width: 1;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 24, 0, 0, 4);");
        lblTitre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        String lblMuted = "-fx-text-fill: #cbd5e1;";
        lblCat.setStyle(lblMuted);
        lblDesc.setStyle(lblMuted);
        lblQty.setStyle(lblMuted);
        lblUnite.setStyle(lblMuted);
        lblMontant.setStyle(lblMuted);
        lblDetails.setStyle(lblMuted);
        lblEtat.setStyle(lblMuted);
        lblUrg.setStyle(lblMuted);
        lblExp.setStyle(lblMuted);
        String champ = "-fx-background-color: rgba(30,41,59,0.85);"
                + "-fx-control-inner-background: rgba(30,41,59,0.92);"
                + "-fx-border-color: rgba(148,163,184,0.35);"
                + "-fx-border-radius: 10; -fx-background-radius: 10;"
                + "-fx-text-fill: #f1f5f9; -fx-prompt-text-fill: #94a3b8; -fx-padding: 8 12;";
        tfDescription.setStyle(champ);
        tfQuantite.setStyle(champ);
        tfMontant.setStyle(champ);
        taDetails.setStyle(champ);
        String combo = "-fx-background-color: rgba(30,41,59,0.92);"
                + "-fx-border-color: rgba(148,163,184,0.35);"
                + "-fx-border-radius: 10; -fx-background-radius: 10;"
                + "-fx-text-fill: #e2e8f0;";
        cbCategorie.setStyle(combo);
        cbUnite.setStyle(combo);
        cbEtat.setStyle(combo);
        cbNiveauUrgence.setStyle(combo);
        stylerComboCategorie(cbCategorie);
        stylerComboString(cbUnite);
        stylerComboString(cbEtat);
        stylerComboString(cbNiveauUrgence);
        btnEnregistrer.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 999;"
                        + "-fx-padding: 10 20; -fx-font-weight: bold; -fx-cursor: hand;");
        btnAnnuler.setStyle(
                "-fx-background-color: rgba(51,65,85,0.9); -fx-text-fill: #e2e8f0; -fx-background-radius: 999;"
                        + "-fx-padding: 10 18; -fx-font-weight: bold;"
                        + "-fx-border-color: rgba(148,163,184,0.35); -fx-border-radius: 999; -fx-cursor: hand;");
        piPaiement.setStyle("-fx-progress-color: #f59e0b;");
        Platform.runLater(() -> {
            javafx.scene.Node content = taDetails.lookup(".content");
            if (content != null) {
                content.setStyle("-fx-background-color: rgba(30,41,59,0.92);");
            }
            dpExpiration.setStyle("-fx-background-color: transparent;");
            dpExpiration.getEditor().setStyle(champ);
        });
    }

    private static void stylerComboString(ComboBox<String> cb) {
        cb.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Assure la lisibilité dans la popup (fond sombre + texte clair).
                    setStyle("-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0;");
                }
                setTextFill(TEXTE_COMBO);
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
                setTextFill(TEXTE_COMBO);
            }
        });
    }

    private static void stylerComboCategorie(ComboBox<DonFormChoices.CategorieOption> cb) {
        cb.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DonFormChoices.CategorieOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.libelle());
                    // Assure la lisibilité dans la popup (fond sombre + texte clair).
                    setStyle("-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0;");
                }
                if (item != null && item.id() <= 0) {
                    setTextFill(TEXTE_INVITE_COMBO);
                } else {
                    setTextFill(TEXTE_COMBO);
                }
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DonFormChoices.CategorieOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.libelle());
                }
                if (item != null && item.id() <= 0) {
                    setTextFill(TEXTE_INVITE_COMBO);
                } else {
                    setTextFill(TEXTE_COMBO);
                }
            }
        });
    }



    private boolean estCategorieArgent(DonFormChoices.CategorieOption cat) {
        return cat != null && cat.libelle() != null && cat.libelle().trim().equalsIgnoreCase("argent");
    }

    private void appliquerModeCategorie() {
        boolean argent = estCategorieArgent(cbCategorie.getValue());
        setVisibleManaged(lblQty, !argent);
        setVisibleManaged(tfQuantite, !argent);
        setVisibleManaged(lblUnite, !argent);
        setVisibleManaged(cbUnite, !argent);
        setVisibleManaged(lblMontant, argent);
        setVisibleManaged(tfMontant, argent);
        setVisibleManaged(piPaiement, false);
    }

    private static void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void enregistrer() {

        try {

            DonFormChoices.CategorieOption cat = cbCategorie.getValue();

            if (cat == null || cat.id() <= 0) {

                Alert aw = new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une catégorie de don.");
                MediLinkDialogs.style(aw);
                aw.showAndWait();

                return;

            }

            int categorieId = cat.id();

            if (estCategorieArgent(cat)) {
                enregistrerDonArgent(cat);
                return;
            }

            int quantite = Integer.parseInt(tfQuantite.getText().trim());

            String desc = tfDescription.getText() != null ? tfDescription.getText().trim() : "";

            String unite = valeurCombo(cbUnite);

            String details = taDetails.getText() != null ? taDetails.getText().trim() : "";

            String etat = valeurCombo(cbEtat);

            String urgence = valeurCombo(cbNiveauUrgence);

            var dateExp = dpExpiration.getValue();



            String erreur = DonSaisieValidator.validerFormulaireDon(

                    categorieId, desc, quantite, unite, etat, urgence, details, dateExp);

            if (erreur != null) {

                Alert aw = new Alert(Alert.AlertType.WARNING, erreur);
                MediLinkDialogs.style(aw);
                aw.showAndWait();

                return;

            }



            Date expiration = dateExp == null ? null : Date.valueOf(dateExp);



            Don d = new Don(categorieId, desc, quantite, unite, details, etat, urgence, "en_attente", expiration);
            d.setTitre(desc);
            d.setCategorie(cat.libelle());
            d.setDescription(details);

            DonIAService ia = donIAService();
            ia.enregistrerDonAvecAnalyse(d);

            Alert ai = new Alert(Alert.AlertType.INFORMATION,
                    "Don enregistré avec succès.\n\n"
                            + "Il sera examiné par un administrateur. L’avis éventuel de l’IA sera visible "
                            + "dans la gestion des dons (dons en attente).");
            MediLinkDialogs.style(ai);
            ai.showAndWait();

            retourListe();

        } catch (NumberFormatException ex) {

            Alert ae = new Alert(Alert.AlertType.ERROR, "La quantité doit être un nombre entier valide.");
            MediLinkDialogs.style(ae);
            ae.showAndWait();

        } catch (IllegalArgumentException ex) {
            Alert aw = new Alert(Alert.AlertType.WARNING, ex.getMessage());
            MediLinkDialogs.style(aw);
            aw.showAndWait();

        } catch (SQLException ex) {

            Alert a = new Alert(Alert.AlertType.ERROR);

            a.setTitle("Erreur");

            a.setContentText(ex.getMessage());

            MediLinkDialogs.style(a);

            a.showAndWait();

        } catch (IllegalStateException ex) {

            afficherErreurIaLisible("Configuration IA", "Clé API ou fournisseur IA", ex.getMessage());

        } catch (IOException ex) {

            afficherErreurIaLisible("Analyse IA", "Erreur d’appel au modèle", ex.getMessage());

        }

    }

    private void enregistrerDonArgent(DonFormChoices.CategorieOption cat) {
        int montant = Integer.parseInt(tfMontant.getText().trim());
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit etre strictement positif.");
        }

        String desc = tfDescription.getText() != null ? tfDescription.getText().trim() : "";
        String details = taDetails.getText() != null ? taDetails.getText().trim() : "";
        String etat = valeurCombo(cbEtat);
        String urgence = valeurCombo(cbNiveauUrgence);
        var dateExp = dpExpiration.getValue();

        String erreur = DonSaisieValidator.validerFormulaireDon(cat.id(), desc, montant, "TND", etat, urgence, details, dateExp);
        if (erreur != null) {
            Alert aw = new Alert(Alert.AlertType.WARNING, erreur);
            MediLinkDialogs.style(aw);
            aw.showAndWait();
            return;
        }

        setPaiementEnCours(true);
        Task<StripePaymentService.CheckoutSessionResult> createTask = new Task<>() {
            @Override
            protected StripePaymentService.CheckoutSessionResult call() {
                if (stripePaymentService == null) {
                    stripePaymentService = new StripePaymentService();
                }
                return stripePaymentService.createCheckoutSession(montant * 100, "Don monetaire Medilink");
            }
        };

        createTask.setOnSucceeded(e -> {
            StripePaymentService.CheckoutSessionResult session = createTask.getValue();
            lancerPollingStripe(session.sessionId(), cat, montant, desc, details, etat, urgence, dateExp);
            ouvrirCheckoutDansWebView(session.checkoutUrl());
        });
        createTask.setOnFailed(e -> {
            setPaiementEnCours(false);
            Throwable ex = createTask.getException();
            Alert err = new Alert(Alert.AlertType.ERROR, ex == null ? "Erreur Stripe." : ex.getMessage());
            MediLinkDialogs.style(err);
            err.showAndWait();
        });

        Thread t = new Thread(createTask, "stripe-create-intent");
        t.setDaemon(true);
        t.start();
    }

    private void lancerPollingStripe(
            String checkoutSessionId,
            DonFormChoices.CategorieOption cat,
            int montant,
            String desc,
            String details,
            String etat,
            String urgence,
            java.time.LocalDate dateExp) {

        Task<StripePaymentService.CheckoutSessionState> pollTask = new Task<>() {
            @Override
            protected StripePaymentService.CheckoutSessionState call() {
                int tries = 0;
                while (tries < 60) {
                    tries++;
                    StripePaymentService.CheckoutSessionState state =
                            stripePaymentService.getCheckoutSessionState(checkoutSessionId);
                    String sessionStatus = state.status();
                    String paymentStatus = state.paymentStatus();

                    if ("complete".equalsIgnoreCase(sessionStatus) && "paid".equalsIgnoreCase(paymentStatus)) {
                        return state;
                    }
                    if ("expired".equalsIgnoreCase(sessionStatus)) {
                        return state;
                    }
                    if ("complete".equalsIgnoreCase(sessionStatus) && "unpaid".equalsIgnoreCase(paymentStatus)) {
                        return state;
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return new StripePaymentService.CheckoutSessionState("interrupted", "failed");
                    }
                }
                return new StripePaymentService.CheckoutSessionState("timeout", "unpaid");
            }
        };

        pollTask.setOnSucceeded(e -> {
            setPaiementEnCours(false);
            StripePaymentService.CheckoutSessionState state = pollTask.getValue();
            String sessionStatus = state == null ? "" : state.status();
            String paymentStatus = state == null ? "" : state.paymentStatus();
            if (!("complete".equalsIgnoreCase(sessionStatus) && "paid".equalsIgnoreCase(paymentStatus))) {
                Alert ko = new Alert(Alert.AlertType.ERROR, "Paiement non reussi. Don non enregistre.");
                MediLinkDialogs.style(ko);
                ko.showAndWait();
                return;
            }
            try {
                Date expiration = dateExp == null ? null : Date.valueOf(dateExp);
                String detailsFinal = details;
                Don d = new Don(cat.id(), desc, montant, "TND", detailsFinal, etat, urgence, "en_attente", expiration);
                d.setTitre(desc);
                d.setCategorie(cat.libelle());
                d.setDescription(detailsFinal);
                // Pas d'analyse IA immediate pour Argent.
                donService.add(d);
                donService.addPaiementDonStripe(
                        d.getId(),
                        montant,
                        checkoutSessionId,
                        sessionStatus,
                        paymentStatus
                );
                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Paiement reussi. Don enregistre.");
                MediLinkDialogs.style(ok);
                ok.showAndWait();
                retourListe();
            } catch (SQLException ex) {
                Alert err = new Alert(Alert.AlertType.ERROR, ex.getMessage());
                MediLinkDialogs.style(err);
                err.showAndWait();
            }
        });

        pollTask.setOnFailed(e -> {
            setPaiementEnCours(false);
            Throwable ex = pollTask.getException();
            Alert err = new Alert(Alert.AlertType.ERROR, ex == null ? "Erreur verification Stripe." : ex.getMessage());
            MediLinkDialogs.style(err);
            err.showAndWait();
        });

        Thread t = new Thread(pollTask, "stripe-polling");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Affiche Stripe Checkout dans une fenêtre intégrée (WebView) au lieu du navigateur externe.
     * La fenêtre se ferme lorsque Stripe redirige vers les URLs success / cancel configurées.
     */
    private void ouvrirCheckoutDansWebView(String checkoutUrl) {
        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new IllegalArgumentException("URL Checkout Stripe invalide.");
        }
        if (stripePaymentService == null) {
            stripePaymentService = new StripePaymentService();
        }
        String successUrl = stripePaymentService.getCheckoutSuccessUrl();
        String cancelUrl = stripePaymentService.getCheckoutCancelUrl();

        Stage owner = (Stage) rootStack.getScene().getWindow();
        Stage payStage = new Stage();
        payStage.initOwner(owner);
        payStage.initModality(Modality.WINDOW_MODAL);
        payStage.setTitle("Paiement securise — MediLink Care");

        WebView webView = new WebView();
        webView.setPrefSize(880, 620);
        webView.getEngine().locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc == null || newLoc.isBlank()) {
                return;
            }
            if (urlCorrespondSansQueryNiFragment(newLoc, successUrl)
                    || urlCorrespondSansQueryNiFragment(newLoc, cancelUrl)) {
                payStage.close();
            }
        });
        webView.getEngine().load(checkoutUrl);

        Button fermer = new Button("Fermer");
        fermer.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 16;");
        fermer.setOnAction(ev -> payStage.close());
        HBox barre = new HBox(fermer);
        barre.setAlignment(Pos.CENTER_RIGHT);
        barre.setPadding(new Insets(8, 12, 8, 12));
        barre.setStyle("-fx-background-color: #f1f5f9;");

        BorderPane root = new BorderPane();
        root.setCenter(webView);
        root.setBottom(barre);

        Scene sc = new Scene(root, 900, 680);
        payStage.setScene(sc);
        payStage.show();
    }

    private static boolean urlCorrespondSansQueryNiFragment(String actuelle, String attendue) {
        if (actuelle == null || attendue == null || attendue.isBlank()) {
            return false;
        }
        String a = retirerQueryEtFragment(actuelle.trim());
        String b = retirerQueryEtFragment(attendue.trim());
        return a.equalsIgnoreCase(b);
    }

    private static String retirerQueryEtFragment(String url) {
        int cut = url.length();
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '?' || c == '#') {
                cut = i;
                break;
            }
        }
        return url.substring(0, cut);
    }

    private void setPaiementEnCours(boolean enCours) {
        btnEnregistrer.setDisable(enCours);
        setVisibleManaged(piPaiement, enCours);
    }

    /** Alerte large avec texte défilant : le constructeur Alert(type, msg) tronque souvent les longs messages. */
    private static void afficherErreurIaLisible(String titreFenetre, String enTete, String messageComplet) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titreFenetre);
        a.setHeaderText(enTete);
        TextArea ta = new TextArea(messageComplet != null ? messageComplet : "");
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(12);
        ta.setPrefColumnCount(60);
        a.getDialogPane().setContent(ta);
        a.getDialogPane().setMinWidth(560);
        MediLinkDialogs.style(a);
        a.showAndWait();
    }

    private DonIAService donIAService() throws IOException {
        if (donIAService == null) {
            donIAService = new DonIAService();
        }
        return donIAService;
    }



    private static String valeurCombo(ComboBox<String> cb) {

        String v = cb.getValue();

        return v != null ? v.trim() : "";

    }



    private void retourListe() {

        try {

            Stage stage = (Stage) btnAnnuler.getScene().getWindow();

            Parent root = FXMLLoader.load(Objects.requireNonNull(

                    getClass().getResource("/ListeDons.fxml")));

            stage.setScene(new Scene(root, 900, 550));

        } catch (IOException ex) {

            Alert a = new Alert(Alert.AlertType.ERROR);

            a.setContentText("Impossible de revenir à la liste : " + ex.getMessage());

            MediLinkDialogs.style(a);

            a.showAndWait();

        }

    }

}

