package userfx;

import controllers.RendezVousController;
import exceptions.ServiceException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.RendezVous;

import java.util.List;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label pageTitle;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label terminatedRendezVousLabel;
    @FXML private Label aiWelcomeLabel;
    @FXML private Button themeToggleBtn;
    @FXML private Button dashboardNavButton;
    @FXML private Button usersNavButton;
    @FXML private Button rendezVousNavButton;
    @FXML private Button medicationsNavButton;
    @FXML private Button eventsNavButton;
    @FXML private PieChart rolesPieChart;
    @FXML private PieChart statusPieChart;
    @FXML private StackPane centerContentHost;
    @FXML private ScrollPane dashboardScrollPane;

    @FXML private ImageView logoImage;
    @FXML private VBox statCard1, statCard2, statCard3, statCard4;
    @FXML private VBox chartPanel1, chartPanel2;
    @FXML private Label chartTitle1, chartTitle2, qaTitle;
    @FXML private VBox qaCard1, qaCard2, qaCard3, qaCard4;

    private static final String CARD_LIGHT =
        "-fx-background-color: white; -fx-background-radius: 14; " +
        "-fx-border-color: #e0e0e0; -fx-border-radius: 14; -fx-border-width: 0.5;";
    private static final String CARD_DARK =
        "-fx-background-color: #1a2540; -fx-background-radius: 14; " +
        "-fx-border-color: #2a3a5a; -fx-border-radius: 14; -fx-border-width: 0.5;";
    private static final String PANEL_LIGHT =
        CARD_LIGHT + " -fx-padding: 16;";
    private static final String PANEL_DARK =
        CARD_DARK + " -fx-padding: 16;";
    private static final String NAV_ACTIVE_STYLE =
            "-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; "
                    + "-fx-font-size: 13px; -fx-alignment: CENTER-LEFT; "
                    + "-fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: transparent;";
    private static final String NAV_DEFAULT_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: white; "
                    + "-fx-font-size: 13px; -fx-alignment: CENTER-LEFT; "
                    + "-fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: transparent;";

    private static User loggedInUser;
    private Parent adminRendezVousView;
    private Parent adminDonsView;

    public static void setLoggedInUser(User user) {
        loggedInUser = user;
    }

    @FXML
    public void initialize() {
        if (loggedInUser != null) {
            welcomeLabel.setText(loggedInUser.getFullName());
            String role = loggedInUser.getRoles() != null
                    ? loggedInUser.getRoles().replace("[","").replace("]","").replace("\"","")
                    : "USER";
            roleLabel.setText(role);

            aiWelcomeLabel.setText("Loading...");
            new Thread(() -> {
                String system = "You are a friendly assistant for MediLink. " +
                        "Write a short personalized welcome (max 2 sentences) based on role. " +
                        "Always reply in English.";
                String reply = ClaudeAI.ask(system, "Name: " + loggedInUser.getFullName() + "\nRole: " + role);
                Platform.runLater(() -> aiWelcomeLabel.setText(reply));
            }).start();
        }

        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        applyTheme();
        setActiveMenu(dashboardNavButton);
        loadStats();
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        applyTheme();
    }

    private void applyTheme() {
        boolean dark = ThemeManager.isDark();

        // Logo
        String logoRes = dark ? "/logo2.png" : "/logo.png";
        var url = getClass().getResource(logoRes);
        if (url != null && logoImage != null)
            logoImage.setImage(new Image(url.toExternalForm()));

        // Page title
        pageTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: "
                + (dark ? "#e0e0e0" : "#1a1a2e") + ";");

        // Stat cards
        String card = dark ? CARD_DARK : CARD_LIGHT;
        for (VBox c : new VBox[]{statCard1, statCard2, statCard3, statCard4})
            if (c != null) c.setStyle(card + " -fx-alignment: CENTER; -fx-spacing: 4;");

        // Chart panels
        String panel = dark ? PANEL_DARK : PANEL_LIGHT;
        if (chartPanel1 != null) chartPanel1.setStyle(panel);
        if (chartPanel2 != null) chartPanel2.setStyle(panel);

        // Chart + QA titles
        String titleStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 0 8 0; -fx-text-fill: "
                + (dark ? "#e0e0e0" : "#333") + ";";
        if (chartTitle1 != null) chartTitle1.setStyle(titleStyle);
        if (chartTitle2 != null) chartTitle2.setStyle(titleStyle);
        if (qaTitle != null) qaTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (dark ? "#e0e0e0" : "#333") + ";");

        // Quick action cards
        applyQaCard(qaCard1, dark,
                dark ? "rgba(24,95,165,0.25)"  : "#E6F1FB",
                dark ? "rgba(24,95,165,0.5)"   : "#B5D4F4",
                dark ? "#7fc4fd" : "#0C447C",
                dark ? "#aad4f5" : "#185FA5");
        applyQaCard(qaCard2, dark,
                dark ? "rgba(15,110,86,0.25)"  : "#E1F5EE",
                dark ? "rgba(15,110,86,0.5)"   : "#9FE1CB",
                dark ? "#5DCAA5" : "#085041",
                dark ? "#3aaa85" : "#0F6E56");
        applyQaCard(qaCard3, dark,
                dark ? "rgba(153,60,29,0.25)"  : "#FAECE7",
                dark ? "rgba(153,60,29,0.5)"   : "#F5C4B3",
                dark ? "#f4a07a" : "#712B13",
                dark ? "#cc8060" : "#993C1D");
        applyQaCard(qaCard4, dark,
                dark ? "rgba(83,74,183,0.25)"  : "#EEEDFE",
                dark ? "rgba(83,74,183,0.5)"   : "#CECBF6",
                dark ? "#AFA9EC" : "#26215C",
                dark ? "#8a7fe0" : "#534AB7");
    }

    private void applyQaCard(VBox card, boolean dark,
                             String bg, String border,
                             String titleColor, String subColor) {
        if (card == null) return;
        card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 12; " +
                "-fx-border-color: " + border + "; -fx-border-radius: 12; " +
                "-fx-border-width: 0.5; -fx-cursor: hand; -fx-padding: 10;");
        int labelIdx = 0;
        for (Node child : card.getChildren()) {
            if (child instanceof Label lbl) {
                if (labelIdx == 0)
                    lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");
                else
                    lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + subColor + ";");
                labelIdx++;
            }
        }
    }

    private void loadStats() {
        UserService us = new UserService();
        List<User> users = us.getAll();

        long admins   = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_ADMIN")).count();
        long medecins = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_MEDECIN")).count();
        long patients = users.stream().filter(u -> u.getRoles() != null
                && !u.getRoles().contains("ROLE_ADMIN")
                && !u.getRoles().contains("ROLE_MEDECIN")).count();

        totalUsersLabel.setText(String.valueOf(users.size()));
        loadRendezVousStats();

        rolesPieChart.getData().clear();
        if (admins > 0)   rolesPieChart.getData().add(new PieChart.Data("Admins ("   + admins   + ")", admins));
        if (medecins > 0) rolesPieChart.getData().add(new PieChart.Data("Doctors ("  + medecins + ")", medecins));
        if (patients > 0) rolesPieChart.getData().add(new PieChart.Data("Patients (" + patients + ")", patients));

        Platform.runLater(() -> {
            String[] roleColors   = {"#185FA5", "#0F6E56", "#534AB7"};
            String[] statusColors = {"#F0B429", "#1D9E75", "#4E73DF", "#E24B4A"};

            for (int i = 0; i < rolesPieChart.getData().size(); i++) {
                PieChart.Data s = rolesPieChart.getData().get(i);
                if (s.getNode() != null)
                    s.getNode().setStyle("-fx-pie-color: " + roleColors[i % roleColors.length] + ";");
            }
            for (int i = 0; i < statusPieChart.getData().size(); i++) {
                PieChart.Data s = statusPieChart.getData().get(i);
                if (s.getNode() != null)
                    s.getNode().setStyle("-fx-pie-color: " + statusColors[i % statusColors.length] + ";");
            }
        });
    }

    private void loadRendezVousStats() {
        try {
            RendezVousController rendezVousController = new RendezVousController();
            List<RendezVous> rendezVousList = rendezVousController.listerTousRendezVous();

            long total = rendezVousList.size();
            long enAttente = countByStatus(rendezVousList, RendezVous.EN_ATTENTE);
            long confirmes = countByStatus(rendezVousList, RendezVous.CONFIRME);
            long termines = countByStatus(rendezVousList, RendezVous.TERMINE);
            long annules = countByStatus(rendezVousList, RendezVous.ANNULE);

            totalAppointmentsLabel.setText(Long.toString(total));
            if (terminatedRendezVousLabel != null) {
                terminatedRendezVousLabel.setText("Terminés: " + termines);
            }

            statusPieChart.getData().clear();
            if (enAttente > 0) {
                statusPieChart.getData().add(new PieChart.Data("En attente (" + enAttente + ")", enAttente));
            }
            if (confirmes > 0) {
                statusPieChart.getData().add(new PieChart.Data("Confirmés (" + confirmes + ")", confirmes));
            }
            if (termines > 0) {
                statusPieChart.getData().add(new PieChart.Data("Terminés (" + termines + ")", termines));
            }
            if (annules > 0) {
                statusPieChart.getData().add(new PieChart.Data("Annulés (" + annules + ")", annules));
            }
        } catch (ServiceException e) {
            totalAppointmentsLabel.setText("—");
            if (terminatedRendezVousLabel != null) {
                terminatedRendezVousLabel.setText("Terminés: —");
            }
            statusPieChart.getData().clear();
        }
    }

    private long countByStatus(List<RendezVous> rendezVousList, String status) {
        return rendezVousList.stream()
                .filter(rdv -> rdv.getStatut() != null && rdv.getStatut().equalsIgnoreCase(status))
                .count();
    }

    @FXML
    public void showDashboard() {
        pageTitle.setText("Dashboard");
        setActiveMenu(dashboardNavButton);
        if (centerContentHost != null && dashboardScrollPane != null) {
            centerContentHost.getChildren().setAll(dashboardScrollPane);
        }
        loadStats();
    }
    @FXML public void showUsers()        { setActiveMenu(usersNavButton); navigateTo("/userlist.fxml"); }
    @FXML
    public void showAppointments() {
        setActiveMenu(rendezVousNavButton);
        Parent view = getAdminRendezVousView();
        if (view == null || centerContentHost == null) {
            return;
        }
        centerContentHost.getChildren().setAll(view);
    }
    @FXML public void showMedications()  { setActiveMenu(medicationsNavButton); pageTitle.setText("Medications — coming soon"); }
    @FXML
    public void showEvents() {
        setActiveMenu(eventsNavButton);
        if (!isAdminUser()) {
            showAccessDenied("Acces reserve aux administrateurs.");
            return;
        }
        Parent view = getAdminDonsView();
        if (view == null || centerContentHost == null) {
            showAccessDenied("Impossible de charger la vue admin des dons.");
            return;
        }
        centerContentHost.getChildren().setAll(view);
    }
    @FXML public void handleLogout() {
        if (PopupHelper.confirmLogout()) {
            navigateTo("/main.fxml");
        }
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            pageTitle.getScene().setRoot(root);
            ThemeManager.apply(pageTitle.getScene());
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private Parent getAdminRendezVousView() {
        if (adminRendezVousView != null) {
            return adminRendezVousView;
        }
        try {
            adminRendezVousView = FXMLLoader.load(getClass().getResource("/fxml/AdminRendezVousView.fxml"));
            return adminRendezVousView;
        } catch (Exception e) {
            System.err.println("Rendez-vous admin view error: " + e.getMessage());
            return null;
        }
    }

    private Parent getAdminDonsView() {
        if (adminDonsView != null) {
            return adminDonsView;
        }
        try {
            adminDonsView = FXMLLoader.load(getClass().getResource("/AdminDons.fxml"));
            return adminDonsView;
        } catch (Exception e) {
            System.err.println("Admin dons view error: " + e.getMessage());
            return null;
        }
    }

    private boolean isAdminUser() {
        if (loggedInUser == null || loggedInUser.getRoles() == null) {
            return false;
        }
        String roles = loggedInUser.getRoles().toUpperCase();
        return roles.contains("ROLE_ADMIN") || roles.contains("ADMIN");
    }

    private void showAccessDenied(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Acces refuse");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setActiveMenu(Button activeButton) {
        for (Button button : new Button[]{dashboardNavButton, usersNavButton, rendezVousNavButton, medicationsNavButton, eventsNavButton}) {
            if (button == null) {
                continue;
            }
            button.setStyle(button == activeButton ? NAV_ACTIVE_STYLE : NAV_DEFAULT_STYLE);
        }
    }
}