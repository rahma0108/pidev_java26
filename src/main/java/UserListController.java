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
    @FXML private TextField searchField;
    @FXML private Label messageLabel;
    @FXML private Label aiProfileLabel;
    @FXML private Label formTitle;

    // Add form fields (kept for Add User)
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField phoneField;

    private UserService userService = new UserService();
    private User selectedUser = null;

    @FXML
    public void initialize() {
        loadCards(null);
    }

    private void loadCards(String filter) {
        cardsContainer.getChildren().clear();
        List<User> users = userService.getAll();

        for (User user : users) {
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
            Label empty = new Label("No users found" + (filter != null ? " for \"" + filter + "\"" : ""));
            empty.setStyle("-fx-font-size: 14px; -fx-text-fill: #888; -fx-padding: 20;");
            cardsContainer.getChildren().add(empty);
        }
    }

    // ── EDIT POPUP DIALOG ──
    private void showEditPopup(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText(null);

        // Custom dialog pane styling
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 16;");
        pane.setPrefWidth(400);

        // Content
        VBox content = new VBox(12);
        content.setPadding(new Insets(24));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = makeAvatar(user, 40);
        VBox titleBox = new VBox(2);
        Label title = new Label("Edit User");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label subtitle = new Label(user.getEmail());
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(avatar, titleBox);

        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 4 0;");

        // Form fields
        TextField nameEdit  = styledField(user.getFullName());
        TextField emailEdit = styledField(user.getEmail());
        TextField phoneEdit = styledField(user.getPhone() != null ? user.getPhone() : "");
        PasswordField passEdit = new PasswordField();
        passEdit.setPromptText("Leave empty to keep current password");
        passEdit.setPrefHeight(38);
        passEdit.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #ddd; -fx-font-size: 13px;");

        // Role selector
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ROLE_USER", "ROLE_MEDECIN", "ROLE_ADMIN");
        roleBox.setPrefHeight(38);
        roleBox.setMaxWidth(Double.MAX_VALUE);
        String currentRole = "ROLE_USER";
        if (user.getRoles() != null) {
            if (user.getRoles().contains("ROLE_ADMIN"))   currentRole = "ROLE_ADMIN";
            else if (user.getRoles().contains("ROLE_MEDECIN")) currentRole = "ROLE_MEDECIN";
        }
        roleBox.setValue(currentRole);
        roleBox.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-font-size: 13px;");

        // Status selector
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("ACTIVE", "INACTIVE");
        statusBox.setValue(user.getStatus() != null ? user.getStatus() : "ACTIVE");
        statusBox.setPrefHeight(38);
        statusBox.setMaxWidth(Double.MAX_VALUE);
        statusBox.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-font-size: 13px;");

        content.getChildren().addAll(
                header, sep,
                fieldLabel("Full Name"), nameEdit,
                fieldLabel("Email"), emailEdit,
                fieldLabel("Phone"), phoneEdit,
                fieldLabel("New Password (optional)"), passEdit,
                fieldLabel("Role"), roleBox,
                fieldLabel("Status"), statusBox
        );

        pane.setContent(content);

        // Buttons
        ButtonType saveBtn   = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(saveBtn, cancelBtn);

        // Style the Save button
        Button saveButton = (Button) pane.lookupButton(saveBtn);
        saveButton.setStyle("-fx-background-color: #185FA5; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 13px; " +
                "-fx-border-color: transparent;");

        // Handle result
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveBtn) {
                user.setFullName(nameEdit.getText().trim());
                user.setEmail(emailEdit.getText().trim());
                user.setPhone(phoneEdit.getText().trim());
                user.setRoles("[\"" + roleBox.getValue() + "\"]");
                user.setStatus(statusBox.getValue());
                if (!passEdit.getText().trim().isEmpty()) {
                    user.setPassword(passEdit.getText().trim());
                }
                userService.update(user);
                showMessage("User updated successfully!", "green");
                loadCards(null);
            }
        });
    }

    // ── AI SEARCH ──
    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) { loadCards(null); return; }

        showMessage("AI is searching...", "#185FA5");
        new Thread(() -> {
            String system = "You are a search assistant. Extract ONE search keyword from the query. " +
                    "Examples: 'show doctors' -> 'MEDECIN', 'find admins' -> 'ADMIN', " +
                    "'active users' -> 'ACTIVE'. Reply ONLY with the keyword.";
            String keyword = ClaudeAI.ask(system, query);
            Platform.runLater(() -> {
                showMessage("Results for: " + keyword.trim(), "#185FA5");
                loadCards(keyword.trim());
            });
        }).start();
    }

    // ── AI PROFILE SUMMARY ──
    private void showAIProfile(User user) {
        aiProfileLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #534AB7;" +
                "-fx-background-color: #EEEDFE; -fx-background-radius: 8; -fx-padding: 10;");
        aiProfileLabel.setText("Generating summary...");
        new Thread(() -> {
            String system = "Write a 2-sentence professional profile for a medical platform user. Be concise.";
            String prompt = "Name: " + user.getFullName() + "\nRole: " + user.getRoles() +
                    "\nStatus: " + user.getStatus() + "\nEmail: " + user.getEmail();
            String summary = ClaudeAI.ask(system, prompt);
            Platform.runLater(() -> aiProfileLabel.setText(summary));
        }).start();
    }

    // ── CREATE CARD ──
    private VBox createCard(User user) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-width: 0.5; -fx-border-radius: 12; -fx-background-radius: 12;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(makeAvatar(user, 22), makeNameBox(user));

        Separator sep = new Separator();

        // Badges
        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().addAll(makeRoleBadge(user.getRoles()), makeStatusBadge(user.getStatus()));

        // Buttons
        HBox buttons = new HBox(6);
        buttons.setAlignment(Pos.CENTER);

        Button aiBtn = makeBtn("AI Summary", "#534AB7");
        aiBtn.setOnAction(e -> showAIProfile(user));

        Button editBtn = makeBtn("Edit", "#185FA5");
        editBtn.setOnAction(e -> showEditPopup(user)); // ← opens popup

        Button deleteBtn = makeBtn("Delete", "#c0392b");
        deleteBtn.setOnAction(e -> handleDelete(user));

        buttons.getChildren().addAll(aiBtn, editBtn, deleteBtn);
        card.getChildren().addAll(header, sep, badges, buttons);
        return card;
    }

    // ── ADD USER ──
    @FXML
    public void handleAdd() {
        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();
        String pass  = passwordField.getText().trim();
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showMessage("Name, email and password are required.", "red"); return;
        }
        User u = new User(email, pass, name, "[\"ROLE_USER\"]", "ACTIVE");
        u.setPhone(phoneField.getText().trim());
        userService.insert(u);
        showMessage("User added!", "green");
        handleClear();
        loadCards(null);
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
                loadCards(null);
            }
        });
    }

    @FXML
    public void handleClear() {
        if (nameField != null) nameField.clear();
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (phoneField != null) phoneField.clear();
        if (searchField != null) searchField.clear();
        messageLabel.setText("");
        if (aiProfileLabel != null) aiProfileLabel.setText("Click 'AI Summary' on any card");
        loadCards(null);
    }

    @FXML
    public void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/dashboard.fxml"));
            cardsContainer.getScene().setRoot(root);
        } catch (Exception e) { System.err.println("Navigation error: " + e.getMessage()); }
    }

    // ── HELPERS ──
    private StackPane makeAvatar(User user, double radius) {
        StackPane avatar = new StackPane();
        Circle circle = new Circle(radius);
        String[] bgs = {"#E6F1FB","#E1F5EE","#F1EFE8","#FBEAF0","#FAEEDA"};
        String[] fgs = {"#0C447C","#085041","#444441","#72243E","#633806"};
        int idx = Math.abs(user.getFullName().hashCode()) % bgs.length;
        circle.setFill(Color.web(bgs[idx]));
        Text initials = new Text(getInitials(user.getFullName()));
        initials.setStyle("-fx-font-size: " + (radius * 0.6) + "px; -fx-font-weight: bold;");
        initials.setFill(Color.web(fgs[idx]));
        avatar.getChildren().addAll(circle, initials);
        return avatar;
    }

    private VBox makeNameBox(User user) {
        VBox box = new VBox(2);
        Label name = new Label(user.getFullName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label email = new Label(user.getEmail());
        email.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        email.setMaxWidth(160);
        box.getChildren().addAll(name, email);
        return box;
    }

    private Button makeBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(30);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 11px; " +
                "-fx-cursor: hand; -fx-border-color: transparent;");
        return btn;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        return l;
    }

    private TextField styledField(String value) {
        TextField tf = new TextField(value);
        tf.setPrefHeight(38);
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #ddd; -fx-font-size: 13px;");
        return tf;
    }

    private Label makeRoleBadge(String roles) {
        String role = "USER"; String bg = "#F1EFE8"; String fg = "#444441";
        if (roles != null) {
            if (roles.contains("ROLE_ADMIN"))        { role = "ADMIN";   bg = "#E6F1FB"; fg = "#0C447C"; }
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

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
}