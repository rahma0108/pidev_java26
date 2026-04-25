package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import models.User;
import services.NotificationAIService;
import services.NotificationEmailService;
import services.UserService;
import models.AINotification;
import models.RendezVous;

import java.util.List;

public class PatientHomeViewController {

    @FXML
    private Label greetingLabel;
    @FXML
    private Label roleChipLabel;
    @FXML
    private Label avatarLabel;
    @FXML
    private Label rdvCount;
    @FXML
    private Label medCount;
    @FXML
    private Label ordCount;
    @FXML
    private Label eventCount;
    @FXML
    private Label aiTipLabel;

    private Integer patientId;
    private UserService userService;
    private RendezVousController rendezVousController;
    private DisponibiliteController disponibiliteController;
    private boolean darkTheme;

    @FXML
    private void initialize() {
        try {
            userService = new UserService();
            rendezVousController = new RendezVousController();
            disponibiliteController = new DisponibiliteController();
            patientId = resolveCurrentPatientId();
            loadProfile();
            loadCounters();
            refreshAITip();

            // TEST TEMPORAIRE - a supprimer apres validation
            new Thread(() -> {
                try {
                    if (patientId == null) {
                        return;
                    }
                    User patient = userService.findById(patientId).orElse(null);
                    List<RendezVous> rdvs = rendezVousController.listerPourPatient(patientId);
                    if (patient != null && !rdvs.isEmpty()) {
                        NotificationAIService aiService = new NotificationAIService();
                        AINotification notif = aiService.genererNotification(
                                patient, rdvs.get(0), rdvs
                        );
                        System.out.println("NOTIF : " + notif);

                        if (!notif.isEchec()) {
                            NotificationEmailService emailService = new NotificationEmailService();
                            emailService.envoyerNotification(patient, notif);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Test : " + e.getMessage());
                }
            }).start();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Initialisation patient", e.formatWithCauses());
        }
    }

    @FXML
    private void goToRdv() {
        SceneNavigator.switchScene(getAnyNode(), "/fxml/ReserverRendezVousView.fxml", "MediLink - Espace Patient", 1120, 740);
    }

    @FXML
    private void goToMedicaments() {
        ViewAlertUtil.info("Médicaments", "Espace médicaments à brancher.");
    }

    @FXML
    private void goToOrdonnances() {
        ViewAlertUtil.info("Ordonnances", "Espace ordonnances à brancher.");
    }

    @FXML
    private void goToEvents() {
        ViewAlertUtil.info("Événements", "Espace événements à brancher.");
    }

    @FXML
    private void refreshAITip() {
        aiTipLabel.setText("Hydratez-vous bien et privilégiez des horaires réguliers pour vos consultations.");
    }

    @FXML
    private void toggleTheme() {
        Node root = getAnyNode().getScene().getRoot();
        darkTheme = !darkTheme;
        root.setStyle(darkTheme ? "-fx-background-color: #050d1a;" : "-fx-background-color: #eef4fb;");
    }

    @FXML
    private void handleLogout() {
        SessionContext.clear();
        SceneNavigator.switchScene(getAnyNode(), "/fxml/LoginView.fxml", "MediLink - Connexion", 600, 380);
    }

    @FXML
    private void onCardHover(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof VBox box) {
            box.setOpacity(0.92);
        }
    }

    @FXML
    private void onCardExit(javafx.scene.input.MouseEvent event) {
        if (event.getSource() instanceof VBox box) {
            box.setOpacity(1.0);
        }
    }

    private void loadCounters() {
        if (patientId == null) {
            rdvCount.setText("0 upcoming");
            return;
        }
        try {
            int patientRdv = rendezVousController.listerPourPatient(patientId).size();
            int reservables = disponibiliteController.afficherCreneauxReservables().size();
            rdvCount.setText(patientRdv + " rdv");
            medCount.setText(reservables + " créneaux");
            ordCount.setText("bientôt");
            eventCount.setText("bientôt");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Chargement patient", e.formatWithCauses());
        }
    }

    private void loadProfile() {
        if (patientId == null) {
            greetingLabel.setText("Bonjour !");
            roleChipLabel.setText("Patient");
            avatarLabel.setText("P");
            return;
        }
        try {
            User patient = userService.findById(patientId).orElse(null);
            if (patient == null) {
                return;
            }
            greetingLabel.setText("Bonjour, " + safeName(patient.getFullName()));
            roleChipLabel.setText("Patient");
            avatarLabel.setText(initialOf(patient.getFullName()));
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Profil patient", e.formatWithCauses());
        }
    }

    private Integer resolveCurrentPatientId() throws ServiceException {
        Integer fromSession = SessionContext.getCurrentPatientId();
        if (fromSession != null) {
            return fromSession;
        }
        Integer fallback = userService.findFirstByRole(User.ROLE_PATIENT).map(User::getId).orElse(null);
        SessionContext.setCurrentPatientId(fallback);
        return fallback;
    }

    private Node getAnyNode() {
        return greetingLabel;
    }

    private static String initialOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "P";
        }
        return fullName.trim().substring(0, 1).toUpperCase();
    }

    private static String safeName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Patient";
        }
        return fullName;
    }
}
