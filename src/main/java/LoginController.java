package userfx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import view.NavigationService;
import view.SessionContext;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;
    @FXML private ImageView logoImage;
    private static User loggedInUser;

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    @FXML
    public void goToLanding() {
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            NavigationService.naviguerVers(stage, "/landing.fxml", "MediLink");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        ThemeManager.apply(themeToggleBtn.getScene());
        applyLogo();
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        applyLogo();
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
                loggedInUser = user;
                System.out.println("[AUTH] User authentifie: email=" + user.getEmail()
                        + " | id=" + user.getId()
                        + " | roles=" + user.getRoles());
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
            Stage stage = (Stage) emailField.getScene().getWindow();
            NavigationService.naviguerVers(stage, "/register.fxml", "MediLink - Inscription");
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private void navigateTo(String fxml) {
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            NavigationService.naviguerVers(stage, fxml, "MediLink");
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
            SessionContext.clear();
            DashboardController.setLoggedInUser(user);
            navigateTo("/dashboard.fxml");
            return;
        }

        if (hasRole(normalizedRoles, "MEDECIN")) {
            SessionContext.setCurrentMedecinId(user.getId());
            SessionContext.setCurrentPatientId(null);
            System.out.println("[SESSION] Avant ouverture espace medecin: currentMedecinId="
                    + SessionContext.getCurrentMedecinId());
            navigateTo("/fxml/ListeDisponibilitesView.fxml");
            return;
        }

        SessionContext.setCurrentPatientId(user.getId());
        SessionContext.setCurrentMedecinId(null);
        System.out.println("[SESSION] Avant ouverture espace patient: currentPatientId="
                + SessionContext.getCurrentPatientId());
        HomeController.setUser(user);
        navigateTo("/home.fxml");
    }

    private boolean hasRole(String normalizedRoles, String role) {
        String withPrefix = "ROLE_" + role;
        return normalizedRoles.contains(withPrefix) || normalizedRoles.contains(role);
    }

    private void applyLogo() {
        if (logoImage == null) {
            return;
        }
        String res = ThemeManager.isDark() ? "/logo2.png" : "/logo.png";
        var url = getClass().getResource(res);
        if (url != null) {
            logoImage.setImage(new Image(url.toExternalForm()));
        }
    }
}
