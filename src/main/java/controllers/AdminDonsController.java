package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import models.Don;
import services.DonService;
import utils.MediLinkDialogs;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Tableau de bord admin (KPI + graphiques). La gestion détaillée des dons est dans {@link GestionDonsAdminController}.
 */
public class AdminDonsController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Button btnActualiser;

    @FXML
    private Button btnRetourListe;

    @FXML
    private Button btnOuvrirGestion;

    @FXML
    private Label lblKpiTotal;

    @FXML
    private Label lblKpiAttente;

    @FXML
    private Label lblKpiValide;

    @FXML
    private Label lblKpiRejete;

    @FXML
    private PieChart pieByStatut;

    @FXML
    private PieChart pieByUrgence;

    private final DonService donService = new DonService();
    private final ObservableList<Don> masterData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        URL css = getClass().getResource("/styles/admin-dons.css");
        if (css != null) {
            rootPane.getStylesheets().add(css.toExternalForm());
        }

        chargerDepuisBase();
        majTableauDeBord();

        btnActualiser.setOnAction(e -> {
            chargerDepuisBase();
            majTableauDeBord();
        });
        btnRetourListe.setOnAction(e -> retourListe());
        btnOuvrirGestion.setOnAction(e -> ouvrirFenetreGestionDons());
    }

    private void ouvrirFenetreGestionDons() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/GestionDonsAdmin.fxml")));
            Parent root = loader.load();
            Stage gestion = new Stage();
            gestion.setTitle("Gestion des dons — MediLink");
            Stage owner = (Stage) rootPane.getScene().getWindow();
            gestion.initOwner(owner);
            gestion.setScene(new Scene(root, 1180, 780));
            gestion.show();
        } catch (IOException ex) {
            afficherErreur("Ouverture impossible", ex.getMessage());
        }
    }

    private void chargerDepuisBase() {
        try {
            masterData.setAll(donService.getAll());
        } catch (SQLException ex) {
            afficherErreur("Chargement impossible", ex.getMessage());
        }
    }

    private void majTableauDeBord() {
        int total = masterData.size();
        int att = (int) masterData.stream().filter(d -> "en_attente".equals(normStatut(d.getStatut()))).count();
        int val = (int) masterData.stream().filter(d -> "valide".equals(normStatut(d.getStatut()))).count();
        int rej = (int) masterData.stream().filter(d -> "rejete".equals(normStatut(d.getStatut()))).count();
        lblKpiTotal.setText(Integer.toString(total));
        lblKpiAttente.setText(Integer.toString(att));
        lblKpiValide.setText(Integer.toString(val));
        lblKpiRejete.setText(Integer.toString(rej));

        ObservableList<PieChart.Data> statSlices = FXCollections.observableArrayList();
        if (total == 0) {
            statSlices.add(new PieChart.Data("Aucun don", 1));
        } else {
            if (att > 0) {
                statSlices.add(new PieChart.Data("En attente", att));
            }
            if (val > 0) {
                statSlices.add(new PieChart.Data("Validés", val));
            }
            if (rej > 0) {
                statSlices.add(new PieChart.Data("Rejetés", rej));
            }
            int autres = total - att - val - rej;
            if (autres > 0) {
                statSlices.add(new PieChart.Data("Autres", autres));
            }
            if (statSlices.isEmpty()) {
                statSlices.add(new PieChart.Data("Aucun don", 1));
            }
        }
        pieByStatut.setData(statSlices);

        Map<String, Long> parUrgence = masterData.stream()
                .collect(Collectors.groupingBy(
                        d -> {
                            String u = d.getNiveauUrgence();
                            return (u == null || u.isBlank()) ? "Non renseigné" : u.trim();
                        },
                        Collectors.counting()));
        ObservableList<PieChart.Data> urgSlices = FXCollections.observableArrayList();
        if (total == 0) {
            urgSlices.add(new PieChart.Data("Aucun don", 1));
        } else {
            parUrgence.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByKey(String.CASE_INSENSITIVE_ORDER))
                    .forEach(e -> urgSlices.add(new PieChart.Data(e.getKey(), e.getValue())));
        }
        pieByUrgence.setData(urgSlices);
    }

    private static String normStatut(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase();
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
        MediLinkDialogs.style(a);
        a.showAndWait();
    }
}
