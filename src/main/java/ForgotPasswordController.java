import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ForgotPasswordController {

    @FXML private Canvas animCanvas;
    @FXML private Button themeToggleBtn;
    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPassField;
    @FXML private Label emailErrorLabel;
    @FXML private Label resetErrorLabel;
    @FXML private Label strengthLabel;
    @FXML private Label sendingLabel;
    @FXML private ProgressIndicator spinner1;
    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private Region sBar1, sBar2, sBar3, sBar4;

    private final Random rand = new Random();
    private final List<LandingController.Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private String generatedCode = "";

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");

        // Particles
        double w = animCanvas.getWidth();
        double h = animCanvas.getHeight();
        for (int i = 0; i < 60; i++)
            particles.add(new LandingController.Particle(w, h, rand));
        particleTimer = new AnimationTimer() {
            public void handle(long now) { drawFrame(w, h); }
        };
        particleTimer.start();

        // Password strength listener
        newPassField.textProperty().addListener((obs, old, val) -> updateStrength(val));
    }

    // ── STEP 1: Send code ──
    @FXML
    public void handleSendCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty() || !email.contains("@")) {
            showEmailError("Please enter a valid email address.", "red");
            return;
        }

        // Check if email exists in DB
        UserService us = new UserService();
        List<User> users = us.getAll();
        boolean exists = users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));

        if (!exists) {
            showEmailError("No account found with this email.", "red");
            return;
        }

        // Show spinner
        spinner1.setVisible(true); spinner1.setManaged(true);
        sendingLabel.setText("Sending code..."); sendingLabel.setVisible(true); sendingLabel.setManaged(true);

        // Generate 6-digit code
        generatedCode = String.valueOf(100000 + rand.nextInt(900000));

        // Simulate sending (in real app you'd send email)
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                spinner1.setVisible(false); spinner1.setManaged(false);
                sendingLabel.setVisible(false); sendingLabel.setManaged(false);

                // Show step 2
                step1Box.setVisible(false); step1Box.setManaged(false);
                step2Box.setVisible(true); step2Box.setManaged(true);

                // Fade in step 2
                FadeTransition ft = new FadeTransition(Duration.millis(500), step2Box);
                ft.setFromValue(0); ft.setToValue(1); ft.play();

                // For demo — show the code in a popup
                PopupHelper.showInfo("Reset Code v1.0",
                    "el API mailling mazel mouch hadher ama...\n\n code mta3ek: " + generatedCode);
            });
        }).start();
    }

    // ── STEP 2: Reset password ──
    @FXML
    public void handleResetPassword() {
        String code    = codeField.getText().trim();
        String newPass = newPassField.getText().trim();

        if (code.isEmpty()) {
            showResetError("Please enter the reset code.", "red"); return;
        }
        if (!code.equals(generatedCode)) {
            showResetError("Invalid code. Please try again.", "red"); return;
        }
        if (newPass.length() < 6) {
            showResetError("Password must be at least 6 characters.", "red"); return;
        }

        // Update password in DB
        String email = emailField.getText().trim();
        UserService us = new UserService();
        List<User> users = us.getAll();
        users.stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(email))
            .findFirst()
            .ifPresent(user -> {
                user.setPassword(newPass);
                us.update(user);
            });

        PopupHelper.showSuccess("Password reset successfully!\nYou can now login with your new password.");
        goToLogin();
    }

    private void updateStrength(String password) {
        int score = 0;
        if (password.length() >= 6)  score++;
        if (password.length() >= 10) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[A-Z].*") || password.matches(".*[!@#$%^&*].*")) score++;

        String[] colors = {"#E24B4A","#EF9F27","#1D9E75","#0F6E56"};
        String[] labels = {"Weak","Fair","Good","Strong"};
        Region[] bars   = {sBar1, sBar2, sBar3, sBar4};

        for (int i = 0; i < 4; i++) {
            bars[i].setStyle(i < score
                ? "-fx-background-color: " + colors[score-1] + "; -fx-background-radius: 4;"
                : "-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 4;");
        }
        strengthLabel.setText(password.isEmpty() ? "" : labels[Math.max(0, score-1)]);
        if (score > 0)
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + colors[score-1] + ";");
    }

    @FXML
    public void goToLogin() {
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            ThemeManager.applyWithFade(emailField.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void goToLanding() {
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
            ThemeManager.applyWithFade(emailField.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
    }

    private void drawFrame(double w, double h) {
        GraphicsContext gc = animCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#050d1a"));
        gc.fillRect(0, 0, w, h);
        for (int i = 0; i < particles.size(); i++) {
            LandingController.Particle a = particles.get(i);
            for (int j = i+1; j < particles.size(); j++) {
                LandingController.Particle b = particles.get(j);
                double dist = Math.hypot(a.x-b.x, a.y-b.y);
                if (dist < 100) {
                    gc.setStroke(Color.web("#185FA5", (1-dist/100)*0.1));
                    gc.setLineWidth(0.5);
                    gc.strokeLine(a.x, a.y, b.x, b.y);
                }
            }
        }
        for (LandingController.Particle p : particles) {
            p.update(w, h);
            gc.setFill(Color.web(p.color, p.opacity * 0.6));
            gc.fillOval(p.x-p.radius, p.y-p.radius, p.radius*2, p.radius*2);
        }
    }

    private void showEmailError(String msg, String color) {
        emailErrorLabel.setStyle("-fx-text-fill: " + color + ";");
        emailErrorLabel.setText(msg);
    }

    private void showResetError(String msg, String color) {
        resetErrorLabel.setStyle("-fx-text-fill: " + color + ";");
        resetErrorLabel.setText(msg);
    }

    private void stopAnimation() {
        if (particleTimer != null) particleTimer.stop();
    }
}
