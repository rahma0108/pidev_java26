import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    public void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", "red");
            return;
        }

        UserService us = new UserService();
        User user = us.login(email, password);

        if (user != null) {
            DashboardController.setLoggedInUser(user);
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/dashboard.fxml"));
                emailField.getScene().setRoot(root);
            } catch (Exception e) {
                showMessage("Could not load dashboard.", "red");
                e.printStackTrace();
            }
        } else {
            showMessage("Invalid email or password.", "red");
        }
    }

    @FXML
    public void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Cannot open register: " + e.getMessage());
        }
    }

    private void showMessage(String msg, String color) {
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setText(msg);
    }
}
