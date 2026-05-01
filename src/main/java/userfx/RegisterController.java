package userfx;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label aiSuggestionLabel;
    @FXML private Label strengthLabel;
    @FXML private Button registerBtn;
    @FXML private Button themeToggleBtn;
    @FXML private HBox loadingBox;

    // Strength bars
    @FXML private Region strengthBar1;
    @FXML private Region strengthBar2;
    @FXML private Region strengthBar3;
    @FXML private Region strengthBar4;

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");

        // Live password strength on each keystroke
        passwordField.textProperty().addListener((obs, old, newVal) -> {
            updateStrength(newVal);
        });
    }

    // ── PASSWORD STRENGTH INDICATOR ──
    private void updateStrength(String password) {
        int score = 0;
        if (password.length() >= 6)  score++;
        if (password.length() >= 10) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[A-Z].*") || password.matches(".*[!@#$%^&*].*")) score++;

        String[] colors = {"#E24B4A", "#EF9F27", "#1D9E75", "#0F6E56"};
        String[] labels = {"Weak", "Fair", "Good", "Strong"};
        String[] barBg  = {"#e0e0e0", "#e0e0e0", "#e0e0e0", "#e0e0e0"};

        Region[] bars = {strengthBar1, strengthBar2, strengthBar3, strengthBar4};

        for (int i = 0; i < 4; i++) {
            if (i < score) {
                bars[i].setStyle("-fx-background-color: " + colors[score-1] +
                        "; -fx-background-radius: 4;");
            } else {
                bars[i].setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 4;");
            }
        }

        if (password.isEmpty()) {
            strengthLabel.setText("");
        } else {
            strengthLabel.setText(labels[score > 0 ? score-1 : 0]);
            strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                    colors[score > 0 ? score-1 : 0] + ";");
        }
    }

    @FXML
    public void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmPasswordField.getText().trim();

        // Basic validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all required fields.", "red"); return;
        }
        if (!password.equals(confirm)) {
            showMessage("Passwords do not match.", "red"); return;
        }
        if (!email.contains("@")) {
            showMessage("Please enter a valid email.", "red"); return;
        }

        // Show spinner, disable button
        registerBtn.setDisable(true);
        registerBtn.setText("Checking...");
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        showMessage("", "black");
        aiSuggestionLabel.setText("");

        new Thread(() -> {
            String system = "You are a registration validator for MediLink medical platform. " +
                    "Analyze registration data and respond ONLY with a JSON object: " +
                    "{\"valid\": true, \"message\": \"Great! Your account looks good.\"} " +
                    "or {\"valid\": false, \"message\": \"explain what to fix\"} " +
                    "Rules: password >= 8 chars, has a number, name >= 3 chars, valid email. " +
                    "Always reply in English. Reply ONLY with the JSON.";

            String prompt = "Name: " + name + "\nEmail: " + email +
                    "\nPassword length: " + password.length() +
                    "\nHas number: " + password.matches(".*[0-9].*");

            String aiReply = ClaudeAI.ask(system, prompt);

            Platform.runLater(() -> {
                // Hide spinner
                loadingBox.setVisible(false);
                loadingBox.setManaged(false);
                registerBtn.setText("Create Account");
                registerBtn.setDisable(false);

                try {
                    com.google.gson.JsonObject result =
                            com.google.gson.JsonParser.parseString(aiReply).getAsJsonObject();
                    boolean isValid  = result.get("valid").getAsBoolean();
                    String aiMessage = result.get("message").getAsString();

                    if (isValid) {
                        aiSuggestionLabel.setStyle(
                                "-fx-font-size: 12px; -fx-text-fill: #085041;" +
                                        "-fx-background-color: #E1F5EE; -fx-background-radius: 8; -fx-padding: 8;");
                        aiSuggestionLabel.setText("✓ " + aiMessage);

                        User user = new User(email, password, name, "[\"ROLE_USER\"]", "ACTIVE");
                        user.setPhone(phone);
                        new UserService().insert(user);
                        showMessage("Account created! Redirecting...", "green");

                        new Thread(() -> {
                            try { Thread.sleep(1800); Platform.runLater(this::goToLogin); }
                            catch (InterruptedException ignored) {}
                        }).start();

                    } else {
                        aiSuggestionLabel.setStyle(
                                "-fx-font-size: 12px; -fx-text-fill: #791F1F;" +
                                        "-fx-background-color: #FCEBEB; -fx-background-radius: 8; -fx-padding: 8;");
                        aiSuggestionLabel.setText("✗ " + aiMessage);
                        showMessage("Please fix the issues above.", "red");
                    }

                } catch (Exception e) {
                    // Fallback
                    if (password.length() >= 6) {
                        User user = new User(email, password, name, "[\"ROLE_USER\"]", "ACTIVE");
                        user.setPhone(phone);
                        new UserService().insert(user);
                        showMessage("Account created!", "green");
                        new Thread(() -> {
                            try { Thread.sleep(1800); Platform.runLater(this::goToLogin); }
                            catch (InterruptedException ignored) {}
                        }).start();
                    } else {
                        showMessage("Password too short.", "red");
                    }
                }
            });
        }).start();
    }

    @FXML public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
    }

    @FXML public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            nameField.getScene().setRoot(root);
            ThemeManager.apply(nameField.getScene());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void goToLanding() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
            nameField.getScene().setRoot(root);
            ThemeManager.apply(nameField.getScene());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
}