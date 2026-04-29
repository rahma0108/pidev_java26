import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ForgotPasswordController {

    // ── Step 1
    @FXML private VBox step1Box;
    @FXML private TextField emailField;
    @FXML private Label emailErrorLabel;
    @FXML private ProgressIndicator spinner1;
    @FXML private Label sendingLabel;

    // ── Step 2
    @FXML private VBox step2Box;
    @FXML private TextField codeField;
    @FXML private PasswordField newPassField;
    @FXML private Label resetErrorLabel;
    @FXML private Label strengthLabel;

    // ── Strength bars
    @FXML private Region sBar1, sBar2, sBar3, sBar4;

    private final UserService userService = new UserService();
    private String generatedCode;
    private String targetEmail;

    // ─────────────────────────────────────────
    // INIT — wire up password strength listener
    // ─────────────────────────────────────────
    @FXML
    public void initialize() {
        newPassField.textProperty().addListener((obs, oldVal, newVal) ->
                updateStrengthBars(newVal)
        );
    }

    // ─────────────────────────────────────────
    // STEP 1 — Send code
    // ─────────────────────────────────────────
    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();
        emailErrorLabel.setText("");

        if (email.isEmpty()) {
            showEmailError("Please enter your email address.", false);
            return;
        }

        if (!email.matches("^[\\w.+\\-]+@[\\w\\-]+\\.[a-zA-Z]{2,}$")) {
            showEmailError("Please enter a valid email address.", false);
            return;
        }

        User user = userService.getByEmail(email);
        if (user == null) {
            showEmailError("No account found with this email.", false);
            return;
        }

        targetEmail = email;
        generatedCode = EmailService.generateCode();

        // Show spinner
        spinner1.setVisible(true);
        spinner1.setManaged(true);
        sendingLabel.setText("Sending code...");
        sendingLabel.setVisible(true);
        sendingLabel.setManaged(true);

        new Thread(() -> {
            try {
                EmailService.sendResetCode(targetEmail, generatedCode);
                Platform.runLater(() -> {
                    spinner1.setVisible(false);
                    spinner1.setManaged(false);
                    sendingLabel.setVisible(false);
                    sendingLabel.setManaged(false);

                    // Switch to step 2
                    step1Box.setVisible(false);
                    step1Box.setManaged(false);
                    step2Box.setVisible(true);
                    step2Box.setManaged(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    spinner1.setVisible(false);
                    spinner1.setManaged(false);
                    sendingLabel.setVisible(false);
                    sendingLabel.setManaged(false);
                    showEmailError("Failed to send email: " + e.getMessage(), false);
                });
            }
        }).start();
    }

    // ─────────────────────────────────────────
    // STEP 2 — Verify code and reset password
    // ─────────────────────────────────────────
    @FXML
    private void handleResetPassword() {
        resetErrorLabel.setText("");

        String enteredCode = codeField.getText().trim();
        String newPassword = newPassField.getText();

        if (enteredCode.isEmpty()) {
            showResetError("Please enter the code sent to your email.", false);
            return;
        }

        if (!enteredCode.equals(generatedCode)) {
            showResetError("Invalid code. Please check your email and try again.", false);
            return;
        }

        if (newPassword.length() < 8) {
            showResetError("Password must be at least 8 characters.", false);
            return;
        }

        User user = userService.getByEmail(targetEmail);
        if (user == null) {
            showResetError("Something went wrong. Please restart the process.", false);
            return;
        }

        user.setPassword(newPassword);
        userService.update(user);

        showResetError("✓ Password reset successfully! Redirecting...", true);

        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            Platform.runLater(this::goToLogin);
        }).start();
    }

    // ─────────────────────────────────────────
    // PASSWORD STRENGTH BARS
    // ─────────────────────────────────────────
    private void updateStrengthBars(String password) {
        int score = 0;
        if (password.length() >= 8)                         score++;
        if (password.matches(".*[A-Z].*"))                  score++;
        if (password.matches(".*[0-9].*"))                  score++;
        if (password.matches(".*[^a-zA-Z0-9].*"))           score++;

        String[] colors = {"#E24B4A", "#EF9F27", "#1D9E75", "#1D9E75"};
        String[] labels = {"", "Weak", "Fair", "Strong", "Very strong"};
        Region[] bars   = {sBar1, sBar2, sBar3, sBar4};
        String activeColor = score > 0 ? colors[score - 1] : "#E24B4A";

        for (int i = 0; i < bars.length; i++) {
            String fill = i < score ? activeColor : "rgba(255,255,255,0.1)";
            bars[i].setStyle("-fx-background-color: " + fill + "; -fx-background-radius: 4;");
        }

        strengthLabel.setText(score > 0 ? labels[score] : "");
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                (score > 0 ? activeColor : "#667788") + ";");
    }

    // ─────────────────────────────────────────
    // NAVIGATION
    // ─────────────────────────────────────────
    @FXML
    private void goToLogin() {
        navigateTo("/forgot_password.fxml".replace("forgot_password", "landing"));
    }

    @FXML
    private void goToLanding() {
        navigateTo("/landing.fxml");
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            emailField.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────
    // THEME TOGGLE
    // ─────────────────────────────────────────
    @FXML
    private void toggleTheme() {
        ThemeManager.toggle(emailField.getScene());
    }

    // ─────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────
    private void showEmailError(String msg, boolean isSuccess) {
        emailErrorLabel.setText(msg);
        emailErrorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                (isSuccess ? "#5DCAA5" : "#E24B4A") + ";");
    }

    private void showResetError(String msg, boolean isSuccess) {
        resetErrorLabel.setText(msg);
        resetErrorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                (isSuccess ? "#5DCAA5" : "#E24B4A") + ";");
    }
}