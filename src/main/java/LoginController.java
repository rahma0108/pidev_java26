import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;
    @FXML private Button googleBtn;
    @FXML private CheckBox rememberMeBox;

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        ThemeManager.apply(themeToggleBtn.getScene());

        // Auto-fill saved email
        String savedEmail = RememberMeHelper.load();
        if (!savedEmail.isEmpty()) {
            emailField.setText(savedEmail);
            rememberMeBox.setSelected(true);
        }
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
    }

    // ── Normal login ──
    @FXML
    public void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", "red"); return;
        }
        if (!email.contains("@")) {
            showMessage("Please enter a valid email.", "red"); return;
        }

        UserService us = new UserService();
        User user = us.login(email, password);

        if (user != null) {
            if (rememberMeBox.isSelected()) RememberMeHelper.save(email);
            else RememberMeHelper.clear();
            routeUser(user);
        } else {
            showMessage("Invalid email or password.", "red");
        }
    }

    // ── Google Sign In ──
    @FXML
    public void handleGoogleSignIn() {
        showMessage("Opening Google Sign In...", "#185FA5");
        googleBtn.setDisable(true);
        googleBtn.setText("⏳ Signing in...");

        new Thread(() -> {
            com.google.api.services.oauth2.model.Userinfo googleUser = GoogleAuthService.signIn();

            Platform.runLater(() -> {
                googleBtn.setDisable(false);
                googleBtn.setText("G  Continue with Google");

                if (googleUser == null) {
                    showMessage("Google Sign In cancelled or failed.", "red");
                    return;
                }

                String email    = googleUser.getEmail();
                String name     = googleUser.getName();

                // Check if user exists in DB
                UserService us = new UserService();
                User existing = us.getAll().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst().orElse(null);

                if (existing != null) {
                    // ── Existing user → login directly ──
                    showMessage("Welcome back, " + existing.getFullName() + "!", "green");
                    if (rememberMeBox.isSelected()) RememberMeHelper.save(email);
                    routeUser(existing);
                } else {
                    // ── New user → auto-register with ROLE_USER ──
                    User newUser = new User(
                        email,
                        "GOOGLE_AUTH_" + System.currentTimeMillis(), // no password needed
                        name,
                        "[\"ROLE_USER\"]",
                        "ACTIVE"
                    );
                    us.insert(newUser);

                    // Fetch back with ID
                    User created = us.getAll().stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(email))
                        .findFirst().orElse(newUser);

                    showMessage("Account created! Welcome, " + name + "!", "green");
                    routeUser(created);
                }
            });
        }).start();
    }

    // ── Route by role ──
    private void routeUser(User user) {
        String roles = user.getRoles() != null ? user.getRoles() : "";
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_MEDECIN")) {
            DashboardController.setLoggedInUser(user);
            navigateTo("/dashboard.fxml");
        } else {
            HomeController.setUser(user);
            navigateTo("/home.fxml");
        }
    }

    @FXML public void goToRegister()        { navigateTo("/register.fxml"); }
    @FXML public void goToForgotPassword()  { navigateTo("/forgot_password.fxml"); }
    @FXML public void goToLanding() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
            ThemeManager.applyWithFade(emailField.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxml) {
        boolean skipLoading = fxml.contains("landing") || fxml.contains("register") || fxml.contains("forgot");
        if (skipLoading) {
            try {
                Parent root = FXMLLoader.load(getClass().getResource(fxml));
                ThemeManager.applyWithFade(emailField.getScene(), root, null);
            } catch (Exception e) { e.printStackTrace(); }
        } else {
            ThemeManager.navigateWithLoading(emailField.getScene(), fxml);
        }
    }

    private void showMessage(String msg, String color) {
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setText(msg);
    }
}
