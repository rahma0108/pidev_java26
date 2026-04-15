package view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginViewController {

    @FXML
    private Button medecinButton;
    @FXML
    private Button patientButton;

    @FXML
    private void initialize() {
        medecinButton.setOnAction(e -> ouvrirEspaceMedecin());
        patientButton.setOnAction(e -> ouvrirEspacePatient());
    }

    private void ouvrirEspaceMedecin() {
        changerVue("/fxml/ListeDisponibilitesView.fxml", "MediLink - Espace Medecin", 920, 580);
    }

    private void ouvrirEspacePatient() {
        changerVue("/fxml/ReserverRendezVousView.fxml", "MediLink - Espace Patient", 820, 580);
    }

    private void changerVue(String fxmlPath, String titre, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) medecinButton.getScene().getWindow();
            stage.setTitle(titre);
            stage.setScene(new Scene(root, width, height));
            stage.centerOnScreen();
        } catch (IOException e) {
            String details = e.getCause() != null && e.getCause().getMessage() != null
                    ? e.getCause().getMessage()
                    : e.getMessage();
            ViewAlertUtil.erreur("Navigation", "Impossible d'ouvrir la vue : " + details);
        }
    }
}
