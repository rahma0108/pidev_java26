import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
                    ? loggedInUser.getRoles().replace("[", "").replace("]", "").replace("\"", "")
                    : "USER";
            roleLabel.setText(role);
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
        totalUsersLabel.setText(String.valueOf(users.size()));
        recentUsersTable.setItems(FXCollections.observableArrayList(users));
        totalAppointmentsLabel.setText("—");
        totalMedsLabel.setText("—");
        totalEventsLabel.setText("3");
    }

    @FXML public void showDashboard() { pageTitle.setText("Dashboard"); loadStats(); }
    @FXML public void showUsers() { navigateTo("/userlist.fxml"); }
    @FXML public void showAppointments() { pageTitle.setText("Appointments — coming soon"); }
    @FXML public void showMedications() { pageTitle.setText("Medications — coming soon"); }
    @FXML public void showEvents() { pageTitle.setText("Events — coming soon"); }
    @FXML public void handleLogout() { navigateTo("/main.fxml"); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            pageTitle.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }
}