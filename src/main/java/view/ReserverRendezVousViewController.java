package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.Disponibilite;
import models.RendezVous;
import models.User;
import services.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ReserverRendezVousViewController {
    private static final DateTimeFormatter DATE_HEURE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private TextField motifTextField;
    @FXML
    private DatePicker dateFiltrePicker;
    @FXML
    private FlowPane creneauxCardsContainer;
    @FXML
    private VBox disponibilitesEmptyBox;
    @FXML
    private Button retourButton;
    @FXML
    private Button mesRendezVousButton;
    @FXML
    private Button reserverButton;
    @FXML
    private TableView<RendezVous> mesRendezVousTable;
    @FXML
    private TableColumn<RendezVous, String> medecinColumn;
    @FXML
    private TableColumn<RendezVous, String> dateHeureColumn;
    @FXML
    private TableColumn<RendezVous, String> statutColumn;
    @FXML
    private TableColumn<RendezVous, Void> calendrierColumn;

    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private UserService userService;
    private Runnable afterReserveCallback;
    private List<Disponibilite> creneauxReservables = new ArrayList<>();
    private List<Disponibilite> creneauxVisibles = new ArrayList<>();
    private Integer demoPatientId;
    private Integer preselectedDisponibiliteId;
    private Disponibilite selectedDisponibilite;
    private final ObservableList<RendezVous> mesRendezVousItems = FXCollections.observableArrayList();

    public void prefillDisponibiliteId(int id) {
        this.preselectedDisponibiliteId = id;
        selectionnerDisponibiliteSiVisible(id);
    }

    public void setAfterReserveCallback(Runnable afterReserveCallback) {
        this.afterReserveCallback = afterReserveCallback;
    }

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
            rendezVousController = new RendezVousController();
            userService = new UserService();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }
        demoPatientId = resoudreDemoPatientId();
        configurerTableMesRendezVous();

        retourButton.setOnAction(e -> retournerAccueil());
        mesRendezVousButton.setOnAction(e -> ouvrirPreferences());
        reserverButton.setOnAction(e -> handleReserver());
        dateFiltrePicker.valueProperty().addListener((obs, oldV, newV) -> appliquerFiltreVisuel());

        chargerCreneauxLibres();
        chargerMesRendezVous();
    }

    private void chargerCreneauxLibres() {
        try {
            List<Disponibilite> libres = disponibiliteController.afficherCreneauxReservables().stream()
                    .filter(d -> d.getMedecin() != null)
                    .collect(Collectors.toList());
            creneauxReservables = libres;
            appliquerFiltreVisuel();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void appliquerFiltreVisuel() {
        LocalDate filtre = dateFiltrePicker.getValue();
        List<Disponibilite> visibles = creneauxReservables;
        if (filtre != null) {
            visibles = creneauxReservables.stream()
                    .filter(d -> d.getDate() != null && d.getDate().equals(filtre))
                    .collect(Collectors.toList());
        }
        creneauxVisibles = visibles;
        if (selectedDisponibilite != null && visibles.stream().noneMatch(d -> d.getId() == selectedDisponibilite.getId())) {
            selectedDisponibilite = null;
        }
        renderCreneauxCards(visibles);
        if (preselectedDisponibiliteId != null) {
            selectionnerDisponibiliteSiVisible(preselectedDisponibiliteId);
        }
    }

    private void handleReserver() {
        if (demoPatientId == null) {
            ViewAlertUtil.erreur("Configuration", "Aucun patient de démo valide trouvé (config ou base). Vérifiez app.demo.patientId ou les rôles utilisateurs.");
            return;
        }
        try {
            Disponibilite selected = selectedDisponibilite;
            if (selected == null) {
                ViewAlertUtil.erreur("Saisie", "Sélectionnez une disponibilité dans les cartes.");
                return;
            }
            int dispoId = selected.getId();
            String motif = motifTextField.getText();
            RendezVous rdv = rendezVousController.reserverRendezVous(dispoId, demoPatientId, motif);
            ViewAlertUtil.info("Réservation", "Rendez-vous créé (n° " + rdv.getId() + ").");
            if (afterReserveCallback != null) {
                afterReserveCallback.run();
            }
            chargerCreneauxLibres();
            chargerMesRendezVous();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Réservation", e.formatWithCauses());
        }
    }

    private void selectionnerDisponibiliteSiVisible(int disponibiliteId) {
        for (Disponibilite d : creneauxVisibles) {
            if (d.getId() == disponibiliteId) {
                selectedDisponibilite = d;
                renderCreneauxCards(creneauxVisibles);
                break;
            }
        }
    }

    private void retournerAccueil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) reserverButton.getScene().getWindow();
            stage.setTitle("MediLink - Accueil");
            stage.setScene(new Scene(root, 600, 380));
            stage.centerOnScreen();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Navigation", "Impossible de revenir à l'accueil : " + e.getMessage());
        }
    }

    private void ouvrirPreferences() {
        ViewAlertUtil.info("Mes préférences", "Personnalisation des préférences patient disponible bientôt.");
    }

    private void chargerMesRendezVous() {
        if (demoPatientId == null) {
            mesRendezVousItems.clear();
            return;
        }
        try {
            List<RendezVous> rdvs = rendezVousController.listerPourPatient(demoPatientId);
            mesRendezVousItems.setAll(rdvs);
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Rendez-vous", e.formatWithCauses());
        }
    }

    private void renderCreneauxCards(List<Disponibilite> disponibilites) {
        creneauxCardsContainer.getChildren().clear();
        boolean empty = disponibilites == null || disponibilites.isEmpty();
        disponibilitesEmptyBox.setVisible(empty);
        disponibilitesEmptyBox.setManaged(empty);
        if (empty) {
            return;
        }
        for (Disponibilite d : disponibilites) {
            creneauxCardsContainer.getChildren().add(createCreneauCard(d));
        }
    }

    private VBox createCreneauCard(Disponibilite d) {
        Label title = new Label("Dispo #" + d.getId());
        title.getStyleClass().add("card-title");
        Label date = new Label("Date: " + (d.getDate() != null ? d.getDate() : "-"));
        Label debut = new Label("Début: " + (d.getHeureDebut() != null ? d.getHeureDebut() : "-"));
        Label fin = new Label("Fin: " + (d.getHeureFin() != null ? d.getHeureFin() : "-"));
        Label statut = new Label("Statut: " + (d.getStatus() != null ? d.getStatus() : "-"));
        Label medecin = new Label("Médecin: "
                + (d.getMedecin() != null && d.getMedecin().getFullName() != null ? d.getMedecin().getFullName() : "-"));
        date.getStyleClass().add("card-text");
        debut.getStyleClass().add("card-text");
        fin.getStyleClass().add("card-text");
        statut.getStyleClass().add("card-text");
        medecin.getStyleClass().add("card-text");
        VBox card = new VBox(6, title, date, debut, fin, statut, medecin);
        card.setPrefWidth(220);
        applyCardState(card, d.equals(selectedDisponibilite));
        card.setOnMouseClicked(e -> {
            selectedDisponibilite = d;
            renderCreneauxCards(creneauxVisibles);
        });
        return card;
    }

    private static void applyCardState(VBox card, boolean selected) {
        card.getStyleClass().setAll("availability-card");
        if (selected) {
            card.getStyleClass().add("selected");
        }
    }

    private void configurerTableMesRendezVous() {
        mesRendezVousTable.setItems(mesRendezVousItems);
        mesRendezVousTable.setPlaceholder(new Label("Aucun rendez-vous trouvé."));
        mesRendezVousTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);

        medecinColumn.setCellValueFactory(data -> {
            RendezVous rdv = data.getValue();
            String medecinNom = "-";
            if (rdv != null && rdv.getDisponibilite() != null && rdv.getDisponibilite().getMedecin() != null
                    && rdv.getDisponibilite().getMedecin().getFullName() != null
                    && !rdv.getDisponibilite().getMedecin().getFullName().isBlank()) {
                medecinNom = rdv.getDisponibilite().getMedecin().getFullName();
            }
            return new SimpleStringProperty(medecinNom);
        });

        dateHeureColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatDateHeure(data.getValue() != null ? data.getValue().getDateHeure() : null)));

        statutColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue() != null && data.getValue().getStatut() != null
                        ? data.getValue().getStatut()
                        : "-"));

        calendrierColumn.setCellFactory(col -> new TableCell<>() {
            private final Button calendrierBtn = new Button("📅 Calendrier");

            {
                calendrierBtn.getStyleClass().add("btn-outline");
                calendrierBtn.setOnAction(e -> {
                    RendezVous rdv = getTableRow().getItem();
                    if (rdv == null) {
                        return;
                    }
                    ViewAlertUtil.info("Calendrier", "RDV du " + formatDateHeure(rdv.getDateHeure()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : calendrierBtn);
            }
        });
    }

    private static String formatDateHeure(LocalDateTime dt) {
        return dt == null ? "-" : DATE_HEURE_FMT.format(dt);
    }

    private Integer chargerDemoPatientId() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(in);
            String value = properties.getProperty("app.demo.patientId");
            if (value == null || value.isBlank()) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer resoudreDemoPatientId() {
        Integer configuredId = chargerDemoPatientId();
        try {
            if (configuredId != null) {
                User configuredUser = userService.findById(configuredId).orElse(null);
                if (configuredUser != null && configuredUser.hasRole(User.ROLE_PATIENT)) {
                    return configuredId;
                }
            }
            return userService.findFirstByRole(User.ROLE_PATIENT)
                    .map(User::getId)
                    .orElse(null);
        } catch (ServiceException e) {
            return null;
        }
    }
}
