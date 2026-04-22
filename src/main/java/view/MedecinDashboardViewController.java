package view;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import models.RendezVous;
import models.User;
import services.UserService;

import java.util.List;

public class MedecinDashboardViewController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label roleLabel;
    @FXML
    private Label totalUsersLabel;
    @FXML
    private Label totalAppointmentsLabel;
    @FXML
    private Label totalMedsLabel;
    @FXML
    private Label totalEventsLabel;
    @FXML
    private Label aiWelcomeLabel;
    @FXML
    private PieChart rolesPieChart;
    @FXML
    private PieChart statusPieChart;
    @FXML
    private Label pageTitle;

    private Integer medecinId;
    private UserService userService;
    private DisponibiliteController disponibiliteController;
    private RendezVousController rendezVousController;
    private boolean darkTheme;

    @FXML
    private void initialize() {
        try {
            userService = new UserService();
            disponibiliteController = new DisponibiliteController();
            rendezVousController = new RendezVousController();
            medecinId = resolveCurrentMedecinId();
            initHeader();
            initKpis();
            initCharts();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Initialisation médecin", e.formatWithCauses());
        }
    }

    @FXML
    private void showDashboard() {
        pageTitle.setText("Dashboard Médecin");
    }

    @FXML
    private void showUsers() {
        ViewAlertUtil.info("Utilisateurs", "Gestion utilisateurs non branchée dans ce scénario.");
    }

    @FXML
    private void showAppointments() {
        SceneNavigator.switchScene(getAnyNode(), "/fxml/ListeDisponibilitesView.fxml", "MediLink - Espace Médecin", 900, 560);
    }

    @FXML
    private void showMedications() {
        ViewAlertUtil.info("Médicaments", "Module médicaments à brancher.");
    }

    @FXML
    private void showEvents() {
        SceneNavigator.switchScene(getAnyNode(), "/com/medilink/main.fxml", "MediLink - Evenements", 1100, 750);
    }

    @FXML
    private void handleLogout() {
        SessionContext.clear();
        SceneNavigator.switchScene(getAnyNode(), "/fxml/LoginView.fxml", "MediLink - Connexion", 600, 380);
    }

    @FXML
    private void toggleTheme() {
        Node root = getAnyNode().getScene().getRoot();
        darkTheme = !darkTheme;
        root.setStyle(darkTheme ? "-fx-background-color: #1d1f27;" : "-fx-background-color: #f0f4f8;");
    }

    private void initHeader() throws ServiceException {
        if (medecinId == null) {
            welcomeLabel.setText("Bienvenue");
            roleLabel.setText("Médecin");
            aiWelcomeLabel.setText("Utilisez le menu Appointments pour gérer vos disponibilités.");
            return;
        }
        User medecin = userService.findById(medecinId).orElse(null);
        String name = medecin != null ? medecin.getFullName() : "Médecin";
        welcomeLabel.setText("Bienvenue " + name);
        roleLabel.setText("Espace médecin");
        aiWelcomeLabel.setText("Conseil: mettez à jour vos disponibilités et vérifiez les rendez-vous à venir.");
    }

    private void initKpis() {
        try {
            int rdvCount = medecinId != null ? rendezVousController.listerPourMedecin(medecinId).size() : 0;
            int disposCount = medecinId != null ? disponibiliteController.afficherDisponibilitesMedecin(medecinId).size() : 0;
            totalUsersLabel.setText(medecinId != null ? "1" : "0");
            totalAppointmentsLabel.setText(Integer.toString(rdvCount));
            totalMedsLabel.setText(Integer.toString(disposCount));
            totalEventsLabel.setText("0");
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("KPI", e.formatWithCauses());
        }
    }

    private void initCharts() {
        rolesPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Médecin", 1),
                new PieChart.Data("Patient", 1)
        ));
        try {
            List<RendezVous> rdvList = medecinId != null ? rendezVousController.listerPourMedecin(medecinId) : List.of();
            long enAttente = rdvList.stream().filter(r -> RendezVous.EN_ATTENTE.equals(r.getStatut())).count();
            long confirmes = rdvList.stream().filter(r -> RendezVous.CONFIRME.equals(r.getStatut())).count();
            long termines = rdvList.stream().filter(r -> RendezVous.TERMINE.equals(r.getStatut())).count();
            statusPieChart.setData(FXCollections.observableArrayList(
                    new PieChart.Data("En attente", enAttente),
                    new PieChart.Data("Confirmés", confirmes),
                    new PieChart.Data("Terminés", termines)
            ));
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Graphiques", e.formatWithCauses());
        }
    }

    private Node getAnyNode() {
        return pageTitle;
    }

    private Integer resolveCurrentMedecinId() throws ServiceException {
        Integer fromSession = SessionContext.getCurrentMedecinId();
        if (fromSession != null) {
            return fromSession;
        }
        Integer fallback = userService.findFirstByRole(User.ROLE_MEDECIN).map(User::getId).orElse(null);
        SessionContext.setCurrentMedecinId(fallback);
        return fallback;
    }
}
