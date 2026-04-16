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
    @FXML private Label messageLabel;
    @FXML private Label formTitle;

    private UserService userService = new UserService();
    private User selectedUser = null;

    @FXML
    public void initialize() {
        loadCards();
    }

    private void loadCards() {
        cardsContainer.getChildren().clear();
        List<User> users = userService.getAll();
        for (User user : users) {
            cardsContainer.getChildren().add(createCard(user));
        }
    }

    private VBox createCard(User user) {
        VBox card = new VBox(10);
        card.setPrefWidth(240);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-width: 0.5; -fx-border-radius: 12; -fx-background-radius: 12;");

        // Avatar + name row
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar circle with initials
        StackPane avatar = new StackPane();
        Circle circle = new Circle(22);
        String[] colors = {"#E6F1FB", "#E1F5EE", "#F1EFE8", "#FBEAF0", "#FAEEDA"};
        String[] textColors = {"#0C447C", "#085041", "#444441", "#72243E", "#633806"};
        int colorIdx = Math.abs(user.getFullName().hashCode()) % colors.length;
        circle.setFill(Color.web(colors[colorIdx]));
        Text initials = new Text(getInitials(user.getFullName()));
        initials.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        initials.setFill(Color.web(textColors[colorIdx]));
        avatar.getChildren().addAll(circle, initials);

        // Name + email
        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        emailLabel.setMaxWidth(160);
        nameBox.getChildren().addAll(nameLabel, emailLabel);

        header.getChildren().addAll(avatar, nameBox);

        // Separator
        Separator sep = new Separator();

        // Badges row
        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.getChildren().add(makeRoleBadge(user.getRoles()));
        badges.getChildren().add(makeStatusBadge(user.getStatus()));

        // Buttons row
        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER);

        Button editBtn = new Button("Edit");
        editBtn.setPrefWidth(100);
        editBtn.setStyle("-fx-background-color: #185FA5; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent;");
        editBtn.setOnAction(e -> fillFormForEdit(user));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setPrefWidth(100);
        deleteBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-color: transparent;");
        deleteBtn.setOnAction(e -> handleDelete(user));

        buttons.getChildren().addAll(editBtn, deleteBtn);
        card.getChildren().addAll(header, sep, badges, buttons);
        return card;
    }

    private Label makeRoleBadge(String roles) {
        String role = "USER";
        String bg = "#F1EFE8"; String fg = "#444441";
        if (roles != null) {
            if (roles.contains("ROLE_ADMIN"))   { role = "ADMIN";   bg = "#E6F1FB"; fg = "#0C447C"; }
            else if (roles.contains("ROLE_MEDECIN")) { role = "MEDECIN"; bg = "#E1F5EE"; fg = "#085041"; }
        }
        Label badge = new Label(role);
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; " +
                "-fx-background-radius: 10;");
        return badge;
    }

    private Label makeStatusBadge(String status) {
        String bg = "#EAF3DE"; String fg = "#27500A";
        if ("INACTIVE".equalsIgnoreCase(status)) { bg = "#FCEBEB"; fg = "#791F1F"; }
        Label badge = new Label(status != null ? status : "ACTIVE");
        badge.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; " +
                "-fx-background-radius: 10;");
        return badge;
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "?";
        String[] parts = fullName.trim().split(" ");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
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

    @FXML
    public void handleAdd() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Name, email and password are required.", "red");
            return;
        }
        User u = new User(email, password, name, "[\"ROLE_USER\"]", "ACTIVE");
        u.setPhone(phoneField.getText().trim());
        userService.insert(u);
        showMessage("User added!", "green");
        handleClear();
        loadCards();
    }

    @FXML
    public void handleUpdate() {
        if (selectedUser == null) {
            showMessage("Click Edit on a card first.", "red");
            return;
        }
        selectedUser.setFullName(nameField.getText().trim());
        selectedUser.setEmail(emailField.getText().trim());
        selectedUser.setPhone(phoneField.getText().trim());
        userService.update(selectedUser);
        showMessage("User updated!", "green");
        handleClear();
        loadCards();
    }

    private void handleDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + user.getFullName() + "?");
        confirm.setContentText("This cannot be undone.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.delete(user.getId());
                showMessage("User deleted.", "green");
                loadCards();
            }
        });
    }

    @FXML
    public void handleClear() {
        selectedUser = null;
        formTitle.setText("Add New User");
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        phoneField.clear();
        messageLabel.setText("");
    }

    @FXML
    public void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/dashboard.fxml"));
            cardsContainer.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
}