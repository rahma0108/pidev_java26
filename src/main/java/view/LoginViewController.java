package view;

import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import userfx.LoginController;

import java.io.IOException;

public class LoginViewController {

    @FXML
    private Button medecinButton;
    @FXML
    private Button patientButton;
    private UserService userService;

    @FXML
    private void initialize() {
        medecinButton.setOnAction(e -> ouvrirEspaceMedecin());
        patientButton.setOnAction(e -> ouvrirEspacePatient());
        try {
            userService = new UserService();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Initialisation", e.formatWithCauses());
        }
    }

    private void ouvrirEspaceMedecin() {
        Integer currentMedecin = SessionContext.getCurrentMedecinId();
        SessionContext.setCurrentMedecinId(currentMedecin);
        SessionContext.setCurrentPatientId(null);
        System.out.println("[SESSION][view.LoginView] Avant ouverture espace medecin: currentMedecinId="
                + SessionContext.getCurrentMedecinId());
        changerVue("/dashboard.fxml", "MediLink - Espace Medecin", 960, 640);
    }

    private void ouvrirEspacePatient() {
        Integer currentPatient = SessionContext.getCurrentPatientId();
        if (currentPatient == null) {
            userfx.User logged = LoginController.getLoggedInUser();
            if (logged != null) {
                currentPatient = logged.getId();
            }
        }
        SessionContext.setCurrentPatientId(currentPatient);
        SessionContext.setCurrentMedecinId(null);
        System.out.println("[SESSION][view.LoginView] Avant ouverture espace patient: currentPatientId="
                + SessionContext.getCurrentPatientId());
        changerVue("/home.fxml", "MediLink - Espace Patient", 1000, 680);
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
