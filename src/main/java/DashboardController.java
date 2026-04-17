import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

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

    @FXML private VBox recentUsersContainer;

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

            aiWelcomeLabel.setText("Loading your personalized message...");
            new Thread(() -> {
                String system = "You are a friendly assistant for MediLink medical platform. " +
                        "Write a short personalized welcome message (max 2 sentences) " +
                        "based on the user's role. Be professional and warm. " +
                        "For ADMIN: mention managing the platform. " +
                        "For MEDECIN: mention their patients. " +
                        "For USER/PATIENT: mention their health journey.";
                String prompt = "User name: " + loggedInUser.getFullName() +
                        "\nRole: " + role + "\nStatus: " + loggedInUser.getStatus();
                String aiReply = ClaudeAI.ask(system, prompt);
                Platform.runLater(() -> aiWelcomeLabel.setText(aiReply));
            }).start();
        }
        loadStats();
    }

    private void loadStats() {
        UserService us = new UserService();
        List<User> users = us.getAll();
        totalUsersLabel.setText(String.valueOf(users.size()));
        totalAppointmentsLabel.setText("—");
        totalMedsLabel.setText("—");
        totalEventsLabel.setText("3");
        populateUserCards(users);
    }

    private void populateUserCards(List<User> users) {
        recentUsersContainer.getChildren().clear();

        // Header row
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: #081018; -fx-background-radius: 16 16 0 0;");

        Region spacer = new Region();
        spacer.setPrefWidth(60);
        Label hName   = headerLabel("NAME",   180);
        Label hEmail  = headerLabel("EMAIL",  0);
        HBox.setHgrow(hEmail, Priority.ALWAYS);
        Label hRole   = headerLabel("ROLE",   120);
        Label hStatus = headerLabel("STATUS", 90);
        header.getChildren().addAll(spacer, hName, hEmail, hRole, hStatus);
        recentUsersContainer.getChildren().add(header);

        for (int i = 0; i < users.size(); i++) {
            recentUsersContainer.getChildren().add(buildCard(users.get(i), i == users.size() - 1));
        }
    }

    private Label headerLabel(String text, double width) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #2d6aad;");
        if (width > 0) l.setPrefWidth(width);
        return l;
    }

    private HBox buildCard(User user, boolean last) {
        String accent = accentColor(user.getRoles());

        Region bar = new Region();
        bar.setPrefSize(3, 40);
        bar.setMinSize(3, 40);
        bar.setStyle("-fx-background-color: " + accent + "; -fx-background-radius: 2;");

        Label initials = new Label(getInitials(user.getFullName()));
        initials.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";");
        StackPane avatar = new StackPane(initials);
        avatar.setPrefSize(38, 38);
        avatar.setMinSize(38, 38);
        avatar.setMaxSize(38, 38);
        avatar.setStyle("-fx-background-color: #112038; -fx-background-radius: 19;" +
                        "-fx-border-color: " + accent + "; -fx-border-radius: 19; -fx-border-width: 1.5;");

        HBox leftBlock = new HBox(8, bar, avatar);
        leftBlock.setAlignment(Pos.CENTER_LEFT);
        leftBlock.setPrefWidth(60);

        Label name = new Label(user.getFullName());
        name.setPrefWidth(180);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #dce8f5;");

        Label email = new Label(user.getEmail());
        HBox.setHgrow(email, Priority.ALWAYS);
        email.setStyle("-fx-font-size: 11px; -fx-text-fill: #3d6a96;");

        String roleText = user.getRoles() != null
                ? user.getRoles().replace("[", "").replace("]", "").replace("\"", "")
                : "USER";
        Label role = new Label(roleText);
        role.setPrefWidth(120);
        role.setAlignment(Pos.CENTER);
        role.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + accent + ";" +
                      "-fx-background-color: #112038;" +
                      "-fx-border-color: " + accent + "; -fx-border-radius: 6; -fx-border-width: 1;" +
                      "-fx-background-radius: 6; -fx-padding: 3 10;");

        String statusText = user.getStatus() != null ? user.getStatus() : "active";
        boolean active = "active".equalsIgnoreCase(statusText);
        String sc = active ? "#00e5a0" : "#ff6b6b";
        Label status = new Label((active ? "● " : "○ ") + statusText.toUpperCase());
        status.setPrefWidth(90);
        status.setAlignment(Pos.CENTER);
        status.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + sc + ";" +
                        "-fx-background-color: #0b1622;" +
                        "-fx-border-color: " + sc + "; -fx-border-radius: 6; -fx-border-width: 1;" +
                        "-fx-background-radius: 6; -fx-padding: 3 8;");

        HBox card = new HBox(10, leftBlock, name, email, role, status);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 20, 12, 14));
        String bottomBorder = last ? "transparent" : "#162030";
        card.setStyle("-fx-background-color: #0b1622;" +
                      "-fx-border-color: transparent transparent " + bottomBorder + " transparent;" +
                      "-fx-border-width: 0 0 1 0;");
        return card;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase() + String.valueOf(parts[1].charAt(0)).toUpperCase();
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    private String accentColor(String roles) {
        if (roles == null) return "#3a7bd5";
        String r = roles.toLowerCase();
        if (r.contains("admin"))   return "#a78bfa";
        if (r.contains("medecin")) return "#38bdf8";
        if (r.contains("patient")) return "#34d399";
        return "#3a7bd5";
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