import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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

    @FXML private PieChart usersPieChart;

    @FXML private TableView<User> recentUsersTable;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> rolesCol;
    @FXML private TableColumn<User, String> statusCol;

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
            aiWelcomeLabel.setText("Loading your personalized message...");
            new Thread(() -> {
                String system = "You are a friendly assistant for MediLink medical platform. " +
                        "Write a short personalized welcome message (max 2 sentences) " +
                        "based on the user role. Be professional and warm. " +
                        "For ADMIN: mention managing the platform. " +
                        "For ROLE_MEDECIN: mention their patients. " +
                        "For ROLE_USER: mention their health journey. " +
                        "Always reply in English.";
                String prompt = "Name: " + loggedInUser.getFullName() + "\nRole: " + role;
                String reply  = ClaudeAI.ask(system, prompt);
                Platform.runLater(() -> aiWelcomeLabel.setText(reply));
            }).start();
        }

        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        emailCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        rolesCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRoles()));
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        loadStats();
    }

    private void loadStats() {
        UserService us = new UserService();
        List<User> users = us.getAll();

        // Count by role
        long admins   = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_ADMIN")).count();
        long medecins = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_MEDECIN")).count();
        long patients = users.stream().filter(u -> u.getRoles() != null && u.getRoles().contains("ROLE_USER") && !u.getRoles().contains("ROLE_MEDECIN") && !u.getRoles().contains("ROLE_ADMIN")).count();

        // Count by status
        long active   = users.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long inactive = users.stream().filter(u -> "INACTIVE".equalsIgnoreCase(u.getStatus())).count();

        // Update stat cards
        totalUsersLabel.setText(String.valueOf(users.size()));
        totalAppointmentsLabel.setText("—");
        totalMedsLabel.setText("—");
        totalEventsLabel.setText("3");

        // Update pie chart
        usersPieChart.getData().clear();

        PieChart.Data adminSlice   = new PieChart.Data("Admins (" + admins + ")", admins > 0 ? admins : 0.01);
        PieChart.Data medecinSlice = new PieChart.Data("Doctors (" + medecins + ")", medecins > 0 ? medecins : 0.01);
        PieChart.Data patientSlice = new PieChart.Data("Patients (" + patients + ")", patients > 0 ? patients : 0.01);
        PieChart.Data activeSlice  = new PieChart.Data("Active (" + active + ")", active > 0 ? active : 0.01);
        PieChart.Data inactiveSlice= new PieChart.Data("Inactive (" + inactive + ")", inactive > 0 ? inactive : 0.01);

        usersPieChart.getData().addAll(adminSlice, medecinSlice, patientSlice, activeSlice, inactiveSlice);

        // Apply colors after adding to chart
        Platform.runLater(() -> {
            String[] colors = {"#185FA5", "#0F6E56", "#534AB7", "#1D9E75", "#c0392b"};
            for (int i = 0; i < usersPieChart.getData().size(); i++) {
                PieChart.Data slice = usersPieChart.getData().get(i);
                if (slice.getNode() != null) {
                    slice.getNode().setStyle("-fx-pie-color: " + colors[i] + ";");
                }
            }
        });

        recentUsersTable.setItems(FXCollections.observableArrayList(users));
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