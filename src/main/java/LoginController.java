import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;

    @FXML
    public void goToLanding() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
            emailField.getScene().setRoot(root);
            ThemeManager.apply(emailField.getScene());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        ThemeManager.apply(themeToggleBtn.getScene());
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
    }

    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", "red");
            return;
        }

        UserService us = new UserService();
        User user = us.login(email, password);

        if (user != null) {
            String roles = user.getRoles() != null ? user.getRoles() : "";

            if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_MEDECIN")) {
                // ── ADMIN / DOCTOR → Dashboard ──
                DashboardController.setLoggedInUser(user);
                navigateTo("/dashboard.fxml");
            } else {
                // ── SIMPLE USER → Patient Home ──
                HomeController.setUser(user);
                navigateTo("/home.fxml");
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
            ThemeManager.apply(emailField.getScene());
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            emailField.getScene().setRoot(root);
            ThemeManager.apply(emailField.getScene());
        } catch (Exception e) {
            showMessage("Could not load page.", "red");
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, String color) {
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setText(msg);
    }
}