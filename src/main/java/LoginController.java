import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;
    @FXML private Button googleBtn;
    @FXML private CheckBox rememberMeBox;

    // CAPTCHA
    @FXML private ImageView captchaImage;
    @FXML private TextField captchaField;
    @FXML private Label captchaErrorLabel;
    @FXML private Label captchaFallbackLabel;
    @FXML private Button refreshCaptchaBtn;
    @FXML private VBox captchaImageBox;

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        ThemeManager.apply(themeToggleBtn.getScene());

        String savedEmail = RememberMeHelper.load();
        if (!savedEmail.isEmpty()) {
            emailField.setText(savedEmail);
            rememberMeBox.setSelected(true);
        }

        loadCaptcha();
    }

    private void loadCaptcha() {
        captchaField.clear();
        captchaErrorLabel.setText("");
        captchaFallbackLabel.setText("Loading...");
        captchaImage.setImage(null);
        refreshCaptchaBtn.setDisable(true);

        new Thread(() -> {
            byte[] imageBytes = CaptchaService.fetchCaptchaImage();
            Platform.runLater(() -> {
                refreshCaptchaBtn.setDisable(false);
                if (imageBytes != null) {
                    // Show image CAPTCHA
                    Image img = new Image(new ByteArrayInputStream(imageBytes));
                    captchaImage.setImage(img);
                    captchaImageBox.setVisible(true);
                    captchaImageBox.setManaged(true);
                    captchaFallbackLabel.setText("");
                    captchaFallbackLabel.setVisible(false);
                    captchaFallbackLabel.setManaged(false);
                } else {
                    // Fallback math CAPTCHA
                    String question = CaptchaService.generateMathFallback();
                    captchaImageBox.setVisible(false);
                    captchaImageBox.setManaged(false);
                    captchaFallbackLabel.setText(question);
                    captchaFallbackLabel.setVisible(true);
                    captchaFallbackLabel.setManaged(true);
                }
            });
        }).start();
    }

    @FXML
    public void refreshCaptcha() {
        javafx.animation.RotateTransition rt = new javafx.animation.RotateTransition(
                javafx.util.Duration.millis(500), refreshCaptchaBtn);
        rt.setByAngle(360);
        rt.play();
        loadCaptcha();
    }

    private boolean validateCaptcha() {
        String input = captchaField.getText().trim();
        if (input.isEmpty()) {
            captchaErrorLabel.setText("Please answer the security check.");
            return false;
        }
        boolean valid = CaptchaService.validate(input);
        if (!valid) {
            captchaErrorLabel.setText("Wrong answer! Try again.");
            loadCaptcha();
        }
        return valid;
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
            showMessage("Please fill in all fields.", "red"); return;
        }
        if (!email.contains("@")) {
            showMessage("Please enter a valid email.", "red"); return;
        }
        if (!validateCaptcha()) return;

        UserService us = new UserService();
        User user = us.login(email, password);

        if (user != null) {
            if (rememberMeBox.isSelected()) RememberMeHelper.save(email);
            else RememberMeHelper.clear();
            routeUser(user);
        } else {
            showMessage("Invalid email or password.", "red");
            loadCaptcha();
        }
    }

    @FXML
    public void handleGoogleSignIn() {
        if (!validateCaptcha()) return;

        showMessage("Opening Google Sign In...", "#185FA5");
        googleBtn.setDisable(true);
        googleBtn.setText("Signing in...");

        new Thread(() -> {
            com.google.api.services.oauth2.model.Userinfo googleUser =
                    GoogleAuthService.signIn();

            Platform.runLater(() -> {
                googleBtn.setDisable(false);
                googleBtn.setText("G  Continue with Google");
                loadCaptcha();

                if (googleUser == null) {
                    showMessage("Google Sign In cancelled.", "red"); return;
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