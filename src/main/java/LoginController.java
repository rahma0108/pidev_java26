import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;
    @FXML private Button googleBtn;
    @FXML private CheckBox rememberMeBox;
    @FXML private VBox loginCard;
    @FXML private HBox rootPane;
    @FXML private VBox captchaContainer;

    private CaptchaController captchaCtrl;

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        applyCurrentTheme();

        if (loginCard != null) {
            EffectsHelper.slideInUp(loginCard, 150);
        }

        // Auto-login if remembered
        if (RememberMeHelper.isRemembered()) {
            String savedEmail    = RememberMeHelper.loadEmail();
            String savedPassword = RememberMeHelper.loadPassword();

            if (!savedEmail.isEmpty() && !savedPassword.isEmpty()
                    && !savedPassword.equals("GOOGLE_AUTH")) {
                emailField.setText(savedEmail);
                passwordField.setText(savedPassword);
                rememberMeBox.setSelected(true);

                new Thread(() -> {
                    try { Thread.sleep(800); } catch (Exception ignored) {}
                    Platform.runLater(() -> {
                        UserService us = new UserService();
                        User user = us.login(savedEmail, savedPassword);
                        if (user != null) {
                            System.out.println("Auto-login: " + user.getFullName());
                            routeUser(user);
                        }
                    });
                }).start();
            }
        }

        loadCaptchaComponent();
    }

    private void applyCurrentTheme() {
        if (loginCard == null || rootPane == null) return;
        if (ThemeManager.isDark()) {
            loginCard.setStyle(
                    "-fx-background-color: #16213e;" +
                            "-fx-padding: 28 32;" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-radius: 16;" +
                            "-fx-border-color: #1a3a5a;" +
                            "-fx-border-width: 1;");
            rootPane.setStyle("-fx-background-color: #0d1b2a;");
            emailField.setStyle(
                    "-fx-background-color: #0d1b2a;" +
                            "-fx-text-fill: #e0e0e0;" +
                            "-fx-prompt-text-fill: #445566;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-color: #1a3a5a;" +
                            "-fx-font-size: 13px;");
            passwordField.setStyle(
                    "-fx-background-color: #0d1b2a;" +
                            "-fx-text-fill: #e0e0e0;" +
                            "-fx-prompt-text-fill: #445566;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-color: #1a3a5a;" +
                            "-fx-font-size: 13px;");
        } else {
            loginCard.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-padding: 28 32;" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-radius: 16;" +
                            "-fx-border-color: #e0e0e0;" +
                            "-fx-border-width: 0.5;");
            rootPane.setStyle("-fx-background-color: #f0f4f8;");
            emailField.setStyle(
                    "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-color: #ddd;" +
                            "-fx-font-size: 13px;");
            passwordField.setStyle(
                    "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-color: #ddd;" +
                            "-fx-font-size: 13px;");
        }
    }

    private void loadCaptchaComponent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/captcha.fxml"));
            javafx.scene.Node captchaNode = loader.load();
            captchaCtrl = loader.getController();
            captchaContainer.getChildren().clear();
            captchaContainer.getChildren().add(captchaNode);
        } catch (Exception e) {
            System.err.println("Could not load captcha: " + e.getMessage());
        }
    }

    private boolean isCaptchaVerified() {
        if (captchaCtrl == null) return true;
        if (!captchaCtrl.isVerified()) {
            showMessage("Please complete the security check first.", "red");
            return false;
        }
        return true;
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(emailField.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        applyCurrentTheme();
    }

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
        if (!isCaptchaVerified()) return;

        UserService us = new UserService();
        User user = us.login(email, password);

        if (user != null) {
            if (rememberMeBox.isSelected()) RememberMeHelper.save(email, password);
            else RememberMeHelper.clear();
            routeUser(user);
        } else {
            showMessage("Invalid email or password.", "red");
            EffectsHelper.shake(emailField);
            EffectsHelper.shake(passwordField);
            loadCaptchaComponent();
        }
    }

    @FXML
    public void handleGoogleSignIn() {
        if (!isCaptchaVerified()) return;

        showMessage("Opening Google Sign In...", "#185FA5");
        googleBtn.setDisable(true);
        googleBtn.setText("Signing in...");

        new Thread(() -> {
            try {
                com.google.api.services.oauth2.model.Userinfo googleUser =
                        GoogleAuthService.signIn();

                Platform.runLater(() -> {
                    googleBtn.setDisable(false);
                    googleBtn.setText("G  Continue with Google");
                    loadCaptchaComponent();

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
                        // Save with GOOGLE_AUTH marker — no password
                        if (rememberMeBox.isSelected())
                            RememberMeHelper.save(email, "GOOGLE_AUTH");
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
            } catch (Exception e) {
                Platform.runLater(() -> {
                    googleBtn.setDisable(false);
                    googleBtn.setText("G  Continue with Google");
                    showMessage("Google Sign In error: " + e.getMessage(), "red");
                    e.printStackTrace();
                });
            }
        }).start();
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
    @FXML public void goToLanding()        { navigateTo("/landing.fxml"); }

    private void navigateTo(String fxml) {
        try {
            var url = getClass().getResource(fxml);
            if (url == null) {
                showMessage("Page not found: " + fxml, "red"); return;
            }
            Parent root = FXMLLoader.load(url);
            ThemeManager.applyWithFade(emailField.getScene(), root, null);
        } catch (Exception e) {
            showMessage("Navigation error: " + e.getMessage(), "red");
            e.printStackTrace();
        }
    }

    private void showMessage(String msg, String color) {
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setText(msg);
    }
}