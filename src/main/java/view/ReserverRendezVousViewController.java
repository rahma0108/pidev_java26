package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Disponibilite;
import models.RendezVous;
import models.User;
import services.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ReserverRendezVousViewController {

    @FXML
    private TextField motifTextField;
    @FXML
    private DatePicker dateFiltrePicker;
    @FXML
    private TableView<Disponibilite> creneauxTableView;
    @FXML
    private TableColumn<Disponibilite, Integer> colonneDispoId;
    @FXML
    private TableColumn<Disponibilite, LocalDate> colonneDate;
    @FXML
    private TableColumn<Disponibilite, String> colonneHeureDebut;
    @FXML
    private TableColumn<Disponibilite, String> colonneHeureFin;
    @FXML
    private TableColumn<Disponibilite, String> colonneStatut;
    @FXML
    private TableColumn<Disponibilite, String> colonneMedecin;
    @FXML
    private Button retourButton;
    @FXML
    private Button mesRendezVousButton;
    @FXML
    private Button reserverButton;

    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private UserService userService;
    private Runnable afterReserveCallback;
    private List<Disponibilite> creneauxReservables = new ArrayList<>();
    private Integer demoPatientId;
    private Integer preselectedDisponibiliteId;

    public void prefillDisponibiliteId(int id) {
        this.preselectedDisponibiliteId = id;
        selectionnerDisponibiliteSiVisible(id);
    }

    public void setAfterReserveCallback(Runnable afterReserveCallback) {
        this.afterReserveCallback = afterReserveCallback;
    }

    @FXML
    private void initialize() {
        colonneDispoId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colonneDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colonneHeureDebut.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getHeureDebut() != null ? c.getValue().getHeureDebut().toString() : ""));
        colonneHeureFin.setCellValueFactory(c ->
                new ReadOnlyObjectWrapper<>(c.getValue().getHeureFin() != null ? c.getValue().getHeureFin().toString() : ""));
        colonneStatut.setCellValueFactory(new PropertyValueFactory<>("status"));
        colonneMedecin.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().getMedecin() != null ? c.getValue().getMedecin().getFullName() : ""));
        creneauxTableView.setPlaceholder(new Label("Aucun créneau réellement réservable."));

        creneauxTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        try {
            disponibiliteController = new DisponibiliteController();
            rendezVousController = new RendezVousController();
            userService = new UserService();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }
        demoPatientId = resoudreDemoPatientId();

        retourButton.setOnAction(e -> retournerAccueil());
        mesRendezVousButton.setOnAction(e -> ouvrirMesRendezVous());
        reserverButton.setOnAction(e -> handleReserver());
        dateFiltrePicker.valueProperty().addListener((obs, oldV, newV) -> appliquerFiltreVisuel());

        chargerCreneauxLibres();
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
        creneauxTableView.getItems().setAll(visibles);
        Disponibilite selected = creneauxTableView.getSelectionModel().getSelectedItem();
        if (selected != null && visibles.stream().noneMatch(d -> d.getId() == selected.getId())) {
            creneauxTableView.getSelectionModel().clearSelection();
        }
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
            Disponibilite selected = creneauxTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ViewAlertUtil.erreur("Saisie", "Sélectionnez une disponibilité dans le tableau.");
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
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Réservation", e.formatWithCauses());
        }
    }

    private void selectionnerDisponibiliteSiVisible(int disponibiliteId) {
        for (Disponibilite d : creneauxTableView.getItems()) {
            if (d.getId() == disponibiliteId) {
                creneauxTableView.getSelectionModel().select(d);
                creneauxTableView.scrollTo(d);
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

    private void ouvrirMesRendezVous() {
        if (demoPatientId == null) {
            ViewAlertUtil.erreur("Configuration", "Aucun patient de démo valide trouvé.");
            return;
        }
        try {
            List<RendezVous> rdvs = rendezVousController.listerPourPatient(demoPatientId);

            TableView<RendezVous> table = new TableView<>();
            TableColumn<RendezVous, Integer> cId = new TableColumn<>("RDV #");
            cId.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getId()));
            TableColumn<RendezVous, String> cDateHeure = new TableColumn<>("Date/Heure");
            cDateHeure.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                    c.getValue().getDateHeure() != null ? c.getValue().getDateHeure().toString() : ""));
            TableColumn<RendezVous, String> cStatut = new TableColumn<>("Statut");
            cStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().getStatut()));
            TableColumn<RendezVous, String> cMotif = new TableColumn<>("Motif");
            cMotif.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                    c.getValue().getMotif() != null ? c.getValue().getMotif() : ""));

            table.getColumns().addAll(cId, cDateHeure, cStatut, cMotif);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.getItems().setAll(rdvs);
            table.setPlaceholder(new Label("Aucun rendez-vous pour ce patient."));

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(reserverButton.getScene().getWindow());
            stage.setTitle("Mes rendez-vous");
            stage.setScene(new Scene(new VBox(table), 700, 420));
            stage.show();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Rendez-vous", e.formatWithCauses());
        }
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
