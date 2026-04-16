import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    @FXML
    public void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();

        // Validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all required fields.", "red");
            return;
        }
        if (!password.equals(confirm)) {
            showMessage("Passwords do not match.", "red");
            return;
        }
        if (!email.contains("@")) {
            showMessage("Please enter a valid email.", "red");
            return;
        }
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters.", "red");
            return;
        }

        // Create and insert user
        User user = new User(email, password, name, "[\"ROLE_USER\"]", "ACTIVE");
        user.setPhone(phone);

        UserService us = new UserService();
        us.insert(user);

        showMessage("Account created! Redirecting to login...", "green");

        // Navigate back to login after short delay
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(this::goToLogin);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            nameField.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
}
