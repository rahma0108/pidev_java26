package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Disponibilite;
import models.RendezVous;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReserverRendezVousViewController {

    @FXML
    private TextField patientIdTextField;
    @FXML
    private TextField disponibiliteIdTextField;
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
    private Button ajouterButton;
    @FXML
    private Button supprimerButton;
    @FXML
    private Button reserverButton;

    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private Runnable afterReserveCallback;

    public void prefillDisponibiliteId(int id) {
        disponibiliteIdTextField.setText(Integer.toString(id));
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

        creneauxTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        creneauxTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, row) -> {
            if (row != null) {
                disponibiliteIdTextField.setText(Integer.toString(row.getId()));
            }
        });

        try {
            disponibiliteController = new DisponibiliteController();
            rendezVousController = new RendezVousController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de données", e.formatWithCauses());
            return;
        }

        ajouterButton.setOnAction(e -> chargerCreneauxLibres());
        supprimerButton.setOnAction(e -> {
            creneauxTableView.getSelectionModel().clearSelection();
            disponibiliteIdTextField.clear();
        });
        reserverButton.setOnAction(e -> handleReserver());
    }

    private void chargerCreneauxLibres() {
        try {
            List<Disponibilite> all = disponibiliteController.afficherDisponibilites();
            List<Disponibilite> libres = all.stream()
                    .filter(d -> Disponibilite.STATUS_LIBRE.equals(d.getStatus()))
                    .collect(Collectors.toList());
            LocalDate filtre = dateFiltrePicker.getValue();
            if (filtre != null) {
                libres = libres.stream()
                        .filter(d -> d.getDate() != null && d.getDate().equals(filtre))
                        .collect(Collectors.toList());
            }
            creneauxTableView.getItems().setAll(libres);
            ViewAlertUtil.info("Créneaux", libres.size() + " créneau(x) libre(s) affiché(s).");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement", e.formatWithCauses());
        }
    }

    private void handleReserver() {
        try {
            int patientId = Integer.parseInt(patientIdTextField.getText().trim());
            int dispoId = Integer.parseInt(disponibiliteIdTextField.getText().trim());
            String motif = motifTextField.getText();
            RendezVous rdv = rendezVousController.reserverRendezVous(dispoId, patientId, motif);
            ViewAlertUtil.info("Réservation", "Rendez-vous créé (n° " + rdv.getId() + ").");
            if (afterReserveCallback != null) {
                afterReserveCallback.run();
            }
            chargerCreneauxLibres();
        } catch (NumberFormatException e) {
            ViewAlertUtil.erreur("Saisie", "Patient et disponibilité doivent être des nombres entiers.");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Réservation", e.formatWithCauses());
        }
    }
}
