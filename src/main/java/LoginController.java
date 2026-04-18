package userfx;

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
        System.out.println("[userfx] Sign In clicked for email=" + email);

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", "red");
            return;
        }

        try {
            UserService us = new UserService();
            User user = us.login(email, password);

            if (user != null) {
                System.out.println("[userfx] Login success, navigating by role.");
                navigateByRole(user.getRoles(), user);
                return;
            }
            System.out.println("[userfx] Login failed: invalid credentials.");
            showMessage("Invalid email or password.", "red");
        } catch (Exception e) {
            System.out.println("[userfx] Login error: " + e.getMessage());
            showMessage("Login unavailable (DB/config). Check local MySQL settings.", "red");
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

    private void navigateByRole(String roles, User user) {
        String safeRoles = roles != null ? roles : "";
        String normalizedRoles = safeRoles.toUpperCase();

        if (hasRole(normalizedRoles, "ADMIN")) {
            DashboardController.setLoggedInUser(user);
            navigateTo("/dashboard.fxml");
            return;
        }

        if (hasRole(normalizedRoles, "MEDECIN")) {
            navigateTo("/fxml/ListeDisponibilitesView.fxml");
            return;
        }

        HomeController.setUser(user);
        navigateTo("/home.fxml");
    }

    private boolean hasRole(String normalizedRoles, String role) {
        String withPrefix = "ROLE_" + role;
        return normalizedRoles.contains(withPrefix) || normalizedRoles.contains(role);
    }
}