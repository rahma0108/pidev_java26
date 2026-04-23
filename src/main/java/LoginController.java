import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;
    @FXML private Button googleBtn;
    @FXML private CheckBox rememberMeBox;

    private boolean turnstileVerified = false;

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        ThemeManager.apply(themeToggleBtn.getScene());

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

    // ── Show Turnstile popup then proceed ──
    private void showTurnstile(Runnable onVerified) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/turnstile_dialog.fxml"));
            Parent root = loader.load();
            TurnstileController ctrl = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("Security Verification");

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
            stage.initStyle(StageStyle.TRANSPARENT);

            ctrl.setCallbacks(
                () -> { // onSuccess
                    turnstileVerified = true;
                    onVerified.run();
                },
                () -> { // onCancel
                    turnstileVerified = false;
                    showMessage("Verification cancelled.", "red");
                }
            );

            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Turnstile error: " + e.getMessage());
            // If WebView fails, skip captcha and proceed
            onVerified.run();
        }
    }

    // ── Normal Login ──
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

        // Show Turnstile first, then login
        showTurnstile(() -> {
            UserService us = new UserService();
            User user = us.login(email, password);
            if (user != null) {
                if (rememberMeBox.isSelected()) RememberMeHelper.save(email);
                else RememberMeHelper.clear();
                routeUser(user);
            } else {
                showMessage("Invalid email or password.", "red");
                turnstileVerified = false;
            }
        });
    }

    // ── Google Sign In ──
    @FXML
    public void handleGoogleSignIn() {
        // Show Turnstile first, then Google
        showTurnstile(() -> {
            showMessage("Opening Google Sign In...", "#185FA5");
            googleBtn.setDisable(true);
            googleBtn.setText("⏳ Signing in...");

            new Thread(() -> {
                com.google.api.services.oauth2.model.Userinfo googleUser =
                    GoogleAuthService.signIn();

                Platform.runLater(() -> {
                    googleBtn.setDisable(false);
                    googleBtn.setText("G  Continue with Google");

                    if (googleUser == null) {
                        showMessage("Google Sign In cancelled.", "red");
                        return;
                    }

                    String email = googleUser.getEmail();
                    String name  = googleUser.getName();
                    UserService us = new UserService();

                    User existing = us.getAll().stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(email))
                        .findFirst().orElse(null);

                    if (existing != null) {
                        if (rememberMeBox.isSelected()) RememberMeHelper.save(email);
                        routeUser(existing);
                    } else {
                        User newUser = new User(email,
                            "GOOGLE_AUTH_" + System.currentTimeMillis(),
                            name, "[\"ROLE_USER\"]", "ACTIVE");
                        us.insert(newUser);
                        User created = us.getAll().stream()
                            .filter(u -> u.getEmail().equalsIgnoreCase(email))
                            .findFirst().orElse(newUser);
                        routeUser(created);
                    }
                });
            }).start();
        });
    }

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

    @FXML public void goToRegister()       { navigateTo("/register.fxml"); }
    @FXML public void goToForgotPassword() { navigateTo("/forgot_password.fxml"); }
    @FXML public void goToLanding() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
            ThemeManager.applyWithFade(emailField.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateTo(String fxml) {
        boolean skipLoading = fxml.contains("landing") ||
                              fxml.contains("register") ||
                              fxml.contains("forgot");
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
