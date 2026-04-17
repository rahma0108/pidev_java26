import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

import java.util.List;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label pageTitle;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalAppointmentsLabel;
    @FXML private Label totalMedsLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label aiWelcomeLabel;

    @FXML private PieChart rolesPieChart;
    @FXML private PieChart statusPieChart;

    private static User loggedInUser;

    public static void setLoggedInUser(User user) {
        loggedInUser = user;
    }

    @FXML
    public void initialize() {
        if (loggedInUser != null) {
            welcomeLabel.setText(loggedInUser.getFullName());
            String role = loggedInUser.getRoles() != null
                    ? loggedInUser.getRoles()
                    .replace("[", "").replace("]", "").replace("\"", "")
                    : "USER";
            roleLabel.setText(role);

            // AI welcome message
            aiWelcomeLabel.setText("Loading...");
            new Thread(() -> {
                String system = "You are a friendly assistant for MediLink medical platform. " +
                        "Write a short personalized welcome (max 2 sentences) based on role. " +
                        "Always reply in English.";
                String prompt = "Name: " + loggedInUser.getFullName() + "\nRole: " + role;
                String reply  = ClaudeAI.ask(system, prompt);
                Platform.runLater(() -> aiWelcomeLabel.setText(reply));
            }).start();
        }

        loadStats();
    }

    private void loadStats() {
        UserService us = new UserService();
        List<User> users = us.getAll();

        // Role counts
        long admins   = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_ADMIN")).count();
        long medecins = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_MEDECIN")).count();
        long patients = users.stream().filter(u -> u.getRoles() != null
                && !u.getRoles().contains("ROLE_ADMIN")
                && !u.getRoles().contains("ROLE_MEDECIN")).count();

        // Status counts
        long active   = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long inactive = users.stream().filter(u -> !"ACTIVE".equalsIgnoreCase(u.getStatus())).count();

        // Stat cards
        totalUsersLabel.setText(String.valueOf(users.size()));
        totalAppointmentsLabel.setText("—");
        totalMedsLabel.setText("—");
        totalEventsLabel.setText("3");

        // ── CHART 1: Roles ──
        rolesPieChart.getData().clear();
        if (admins > 0)   rolesPieChart.getData().add(new PieChart.Data("Admins ("   + admins   + ")", admins));
        if (medecins > 0) rolesPieChart.getData().add(new PieChart.Data("Doctors ("  + medecins + ")", medecins));
        if (patients > 0) rolesPieChart.getData().add(new PieChart.Data("Patients (" + patients + ")", patients));

        // ── CHART 2: Status ──
        statusPieChart.getData().clear();
        if (active > 0)   statusPieChart.getData().add(new PieChart.Data("Active ("   + active   + ")", active));
        if (inactive > 0) statusPieChart.getData().add(new PieChart.Data("Inactive (" + inactive + ")", inactive));

        // Apply colors after rendering
        Platform.runLater(() -> {
            // Roles chart colors
            String[] roleColors = {"#185FA5", "#0F6E56", "#534AB7"};
            for (int i = 0; i < rolesPieChart.getData().size(); i++) {
                PieChart.Data slice = rolesPieChart.getData().get(i);
                if (slice.getNode() != null)
                    slice.getNode().setStyle("-fx-pie-color: " + roleColors[i % roleColors.length] + ";");
            }

            // Status chart colors
            String[] statusColors = {"#1D9E75", "#E24B4A"};
            for (int i = 0; i < statusPieChart.getData().size(); i++) {
                PieChart.Data slice = statusPieChart.getData().get(i);
                if (slice.getNode() != null)
                    slice.getNode().setStyle("-fx-pie-color: " + statusColors[i % statusColors.length] + ";");
            }
        });
    }

    @FXML public void showDashboard()    { pageTitle.setText("Dashboard"); loadStats(); }
    @FXML public void showUsers()        { navigateTo("/userlist.fxml"); }
    @FXML public void showAppointments() { pageTitle.setText("Appointments — coming soon"); }
    @FXML public void showMedications()  { pageTitle.setText("Medications — coming soon"); }
    @FXML public void showEvents()       { pageTitle.setText("Events — coming soon"); }
    @FXML public void handleLogout()     { navigateTo("/main.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            pageTitle.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }
}