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

    // CAPTCHA fields
    @FXML private Label captchaLabel;
    @FXML private TextField captchaField;
    @FXML private Label captchaErrorLabel;
    @FXML private Button refreshCaptchaBtn;

    // Fallback math captcha
    private int mathAnswer = -1;
    private boolean usingFallback = false;
    private final java.util.Random rand = new java.util.Random();

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
        ThemeManager.apply(themeToggleBtn.getScene());

        String savedEmail = RememberMeHelper.load();
        if (!savedEmail.isEmpty()) {
            emailField.setText(savedEmail);
            rememberMeBox.setSelected(true);
        }

        // Load CAPTCHA from API on background thread
        loadCaptcha();
    }

    private void loadCaptcha() {
        captchaLabel.setText("Loading...");
        captchaField.setDisable(true);
        refreshCaptchaBtn.setDisable(true);

        new Thread(() -> {
            String question = CaptchaService.fetchQuestion();
            Platform.runLater(() -> {
                captchaField.setDisable(false);
                refreshCaptchaBtn.setDisable(false);
                captchaField.clear();
                captchaErrorLabel.setText("");

                if (question != null) {
                    // ✅ API question loaded
                    usingFallback = false;
                    captchaLabel.setText(question);
                } else {
                    // ❌ API down — use math fallback
                    usingFallback = true;
                    loadMathFallback();
                }
            });
        }).start();
    }

    private void loadMathFallback() {
        int a = 1 + rand.nextInt(9);
        int b = 1 + rand.nextInt(9);
        int op = rand.nextInt(2);
        if (op == 0) {
            mathAnswer = a + b;
            captchaLabel.setText(a + "  +  " + b + "  =  ?");
        } else {
            if (a < b) { int t = a; a = b; b = t; }
            mathAnswer = a - b;
            captchaLabel.setText(a + "  -  " + b + "  =  ?");
        }
    }

    @FXML
    public void refreshCaptcha() {
        // Rotate animation on button
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

        boolean valid;
        if (usingFallback) {
            try {
                valid = Integer.parseInt(input) == mathAnswer;
            } catch (NumberFormatException e) {
                captchaErrorLabel.setText("Please enter a number.");
                return false;
            }
        } else {
            valid = CaptchaService.validateAnswer(input);
        }

        if (valid) {
            captchaErrorLabel.setText("");
            return true;
        } else {
            captchaErrorLabel.setText("Wrong answer! Try again.");
            loadCaptcha();
            return false;
        }
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
        googleBtn.setText("⏳ Signing in...");

        new Thread(() -> {
            com.google.api.services.oauth2.model.Userinfo googleUser = GoogleAuthService.signIn();
            Platform.runLater(() -> {
                googleBtn.setDisable(false);
                googleBtn.setText("G  Continue with Google");
                loadCaptcha();

                if (googleUser == null) {
                    showMessage("Google Sign In cancelled or failed.", "red");
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