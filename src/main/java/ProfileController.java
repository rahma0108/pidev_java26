import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProfileController {

    @FXML private javafx.scene.canvas.Canvas animCanvas;
    @FXML private Label bigAvatarLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileStatusLabel;
    @FXML private Label aiSummaryLabel;
    @FXML private Label msgLabel;
    @FXML private Label passStrengthLabel;
    @FXML private Label savingLabel;
    @FXML private Button themeToggleBtn;
    @FXML private ProgressIndicator saveSpinner;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;

    @FXML private Region sBar1, sBar2, sBar3, sBar4;

    private static User currentUser;
    private final Random rand = new Random();
    private final List<LandingController.Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private String previousFxml = "/home.fxml";

    public static void setUser(User user, String fromFxml) {
        currentUser = user;
    }

    public static void setUser(User user) {
        currentUser = user;
    }

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");

        // Particles background
        double w = animCanvas.getWidth();
        double h = animCanvas.getHeight();
        for (int i = 0; i < 60; i++)
            particles.add(new LandingController.Particle(w, h, rand));
        particleTimer = new AnimationTimer() {
            public void handle(long now) { drawFrame(w, h); }
        };
        particleTimer.start();

        // Load user data
        if (currentUser != null) {
            populateFields();
            loadAISummary();
        }

        // Password strength listener
        newPassField.textProperty().addListener((obs, old, val) -> updateStrength(val));
    }

    private void populateFields() {
        String name = currentUser.getFullName();
        nameField.setText(name);
        emailField.setText(currentUser.getEmail());
        phoneField.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");

        // Avatar initials
        String[] parts = name.trim().split(" ");
        String initials = parts[0].substring(0,1).toUpperCase() +
            (parts.length > 1 ? String.valueOf(parts[1].charAt(0)).toUpperCase() : "");
        bigAvatarLabel.setText(initials);
        profileNameLabel.setText(name);

        // Role badge
        String role = currentUser.getRoles() != null
            ? currentUser.getRoles().replace("[","").replace("]","")
                .replace("\"","").replace("ROLE_","")
            : "USER";
        profileRoleLabel.setText(role);
        profileStatusLabel.setText(currentUser.getStatus() != null ? currentUser.getStatus() : "ACTIVE");
    }

    private void loadAISummary() {
        new Thread(() -> {
            String summary = ClaudeAI.ask(
                "Write a 2-sentence professional profile summary for a medical platform user. Be warm and concise.",
                "Name: " + currentUser.getFullName() +
                "\nRole: " + currentUser.getRoles() +
                "\nStatus: " + currentUser.getStatus()
            );
            Platform.runLater(() -> {
                aiSummaryLabel.setText(summary);
                FadeTransition ft = new FadeTransition(Duration.millis(600), aiSummaryLabel);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            });
        }).start();
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

        passStrengthLabel.setText(password.isEmpty() ? "" : labels[Math.max(0, score-1)]);
        if (score > 0)
            passStrengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + colors[score-1] + ";");
    }

    @FXML
    public void handleSave() {
        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String newPass = newPassField.getText().trim();
        String confirm = confirmPassField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showMsg("Name and email are required.", "red"); return;
        }
        if (!newPass.isEmpty() && !newPass.equals(confirm)) {
            showMsg("Passwords do not match.", "red"); return;
        }

        // Show spinner
        saveSpinner.setVisible(true); saveSpinner.setManaged(true);
        savingLabel.setVisible(true); savingLabel.setManaged(true);

        new Thread(() -> {
            currentUser.setFullName(name);
            currentUser.setEmail(email);
            currentUser.setPhone(phone);
            if (!newPass.isEmpty()) currentUser.setPassword(newPass);

            new UserService().update(currentUser);

            Platform.runLater(() -> {
                saveSpinner.setVisible(false); saveSpinner.setManaged(false);
                savingLabel.setVisible(false); savingLabel.setManaged(false);
                populateFields();
                PopupHelper.showSuccess("Profile updated successfully!");
            });
        }).start();
    }

    @FXML
    public void goBack() {
        stopAnimation();
        try {
            // Go back to home or dashboard based on role
            String roles = currentUser.getRoles() != null ? currentUser.getRoles() : "";
            String target = (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_MEDECIN"))
                ? "/dashboard.fxml" : "/home.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(target));
            nameField.getScene().setRoot(root);
            ThemeManager.apply(nameField.getScene());
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

    private void showMsg(String msg, String color) {
        msgLabel.setStyle("-fx-text-fill: " + color + ";");
        msgLabel.setText(msg);
    }

    private void stopAnimation() {
        if (particleTimer != null) particleTimer.stop();
    }
}
