import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.util.List;

public class UserListController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField searchField;
    @FXML private Label messageLabel;
    @FXML private Label formTitle;
    @FXML private Label aiProfileLabel;

    private UserService userService = new UserService();
    private User selectedUser = null;
    private List<User> allUsers;

    @FXML
    public void initialize() {
        loadCards(null);
    }

    private void loadCards(String filter) {
        cardsContainer.getChildren().clear();
        allUsers = userService.getAll();

        for (User user : allUsers) {
            // ── AI FEATURE 3: Smart Search filtering ──
            if (filter != null && !filter.isEmpty()) {
                String f = filter.toLowerCase();
                boolean matches =
                        user.getFullName().toLowerCase().contains(f) ||
                                user.getEmail().toLowerCase().contains(f) ||
                                (user.getRoles() != null && user.getRoles().toLowerCase().contains(f)) ||
                                (user.getStatus() != null && user.getStatus().toLowerCase().contains(f));
                if (!matches) continue;
            }
            cardsContainer.getChildren().add(createCard(user));
        }

        if (cardsContainer.getChildren().isEmpty()) {
            Label empty = new Label("No users found for \"" + filter + "\"");
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            cardsContainer.getChildren().add(empty);
        }
    }

    // ── AI FEATURE 3: Natural language search ──
    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { loadCards(null); return; }

        messageLabel.setStyle("-fx-text-fill: #185FA5;");
        messageLabel.setText("AI is searching...");

        new Thread(() -> {
            String system = "You are a search assistant for a user management system. " +
                    "The user typed a natural language search query. " +
                    "Extract the key search term to filter users by. " +
                    "Reply with ONLY the search keyword, nothing else. " +
                    "Examples: 'show doctors' -> 'MEDECIN', " +
                    "'find admins' -> 'ADMIN', " +
                    "'active users' -> 'ACTIVE', " +
                    "'find Mohamed' -> 'Mohamed'";

            String keyword = ClaudeAI.ask(system, query);
            Platform.runLater(() -> {
                messageLabel.setText("Showing results for: " + keyword);
                loadCards(keyword.trim());
            });
        }).start();
    }

    private VBox createCard(User user) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-width: 0.5; -fx-border-radius: 12; -fx-background-radius: 12;");

        // Avatar
        StackPane avatar = new StackPane();
        Circle circle = new Circle(22);
        String[] bgs   = {"#E6F1FB","#E1F5EE","#F1EFE8","#FBEAF0","#FAEEDA"};
        String[] fgs   = {"#0C447C","#085041","#444441","#72243E","#633806"};
        int idx = Math.abs(user.getFullName().hashCode()) % bgs.length;
        circle.setFill(Color.web(bgs[idx]));
        Text initials = new Text(getInitials(user.getFullName()));
        initials.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        initials.setFill(Color.web(fgs[idx]));
        avatar.getChildren().addAll(circle, initials);

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        emailLabel.setMaxWidth(160);
        nameBox.getChildren().addAll(nameLabel, emailLabel);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(avatar, nameBox);

        Separator sep = new Separator();

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(makeRoleBadge(user.getRoles()), makeStatusBadge(user.getStatus()));

        // Buttons
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER);

        Button profileBtn = new Button("AI Summary");
        profileBtn.setPrefWidth(100);
        profileBtn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-color: transparent;");
        profileBtn.setOnAction(e -> showAIProfile(user));

        Button editBtn = new Button("Edit");
        editBtn.setPrefWidth(66);
        editBtn.setStyle("-fx-background-color: #185FA5; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-color: transparent;");
        editBtn.setOnAction(e -> fillFormForEdit(user));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setPrefWidth(66);
        deleteBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-color: transparent;");
        deleteBtn.setOnAction(e -> handleDelete(user));

        buttons.getChildren().addAll(profileBtn, editBtn, deleteBtn);
        card.getChildren().addAll(header, sep, badges, buttons);
        return card;
    }

    // ── AI FEATURE 3: AI Profile Summary ──
    private void showAIProfile(User user) {
        selectedUser = user;
        aiProfileLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #534AB7;");
        aiProfileLabel.setText("Generating AI summary...");

        new Thread(() -> {
            String system = "You are a medical platform assistant. " +
                    "Write a brief 2-sentence professional profile summary for this user. " +
                    "Be factual and professional. Mention their role and status.";

            String prompt = "Name: " + user.getFullName() +
                    "\nEmail: " + user.getEmail() +
                    "\nRole: " + user.getRoles() +
                    "\nStatus: " + user.getStatus() +
                    "\nPhone: " + (user.getPhone() != null ? user.getPhone() : "not provided");

            String summary = ClaudeAI.ask(system, prompt);
            Platform.runLater(() -> aiProfileLabel.setText(summary));
        }).start();
    }

    private Label makeRoleBadge(String roles) {
        String role = "USER"; String bg = "#F1EFE8"; String fg = "#444441";
        if (roles != null) {
            if (roles.contains("ROLE_ADMIN"))    { role = "ADMIN";   bg = "#E6F1FB"; fg = "#0C447C"; }
            else if (roles.contains("ROLE_MEDECIN")) { role = "MEDECIN"; bg = "#E1F5EE"; fg = "#085041"; }
        }
        Label b = new Label(role);
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8;-fx-background-radius:10;");
        return b;
    }

    private Label makeStatusBadge(String status) {
        String bg = "#EAF3DE"; String fg = "#27500A";
        if ("INACTIVE".equalsIgnoreCase(status)) { bg = "#FCEBEB"; fg = "#791F1F"; }
        Label b = new Label(status != null ? status : "ACTIVE");
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:3 8;-fx-background-radius:10;");
        return b;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] p = name.trim().split(" ");
        return p.length == 1 ? p[0].substring(0,1).toUpperCase()
                : (p[0].substring(0,1) + p[1].substring(0,1)).toUpperCase();
    }

    private void fillFormForEdit(User user) {
        selectedUser = user;
        formTitle.setText("Edit User");
        nameField.setText(user.getFullName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone() != null ? user.getPhone() : "");
        passwordField.clear();
        showMessage("Editing: " + user.getFullName(), "#185FA5");
    }

    @FXML public void handleAdd() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String pass  = passwordField.getText().trim();
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showMessage("Name, email and password are required.", "red"); return;
        }
        User u = new User(email, pass, name, "[\"ROLE_USER\"]", "ACTIVE");
        u.setPhone(phoneField.getText().trim());
        userService.insert(u);
        showMessage("User added!", "green");
        handleClear(); loadCards(null);
    }

    @FXML public void handleUpdate() {
        if (selectedUser == null) { showMessage("Click Edit on a card first.", "red"); return; }
        selectedUser.setFullName(nameField.getText().trim());
        selectedUser.setEmail(emailField.getText().trim());
        selectedUser.setPhone(phoneField.getText().trim());
        userService.update(selectedUser);
        showMessage("User updated!", "green");
        handleClear(); loadCards(null);
    }

    private void handleDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + user.getFullName() + "?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                userService.delete(user.getId());
                showMessage("User deleted.", "green");
                handleClear(); loadCards(null);
            }
        });
    }

    @FXML public void handleClear() {
        selectedUser = null;
        formTitle.setText("Add New User");
        nameField.clear(); emailField.clear();
        passwordField.clear(); phoneField.clear();
        messageLabel.setText(""); aiProfileLabel.setText("");
        if (searchField != null) searchField.clear();
        loadCards(null);
    }

    @FXML public void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/dashboard.fxml"));
            cardsContainer.getScene().setRoot(root);
        } catch (Exception e) { System.err.println("Navigation error: " + e.getMessage()); }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
}