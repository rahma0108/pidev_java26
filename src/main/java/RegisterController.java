package userfx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;
    @FXML private Label aiSuggestionLabel;
    @FXML private Button registerBtn;

    @FXML
    public void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String phone    = phoneField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmPasswordField.getText().trim();

        // Basic validation first
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all required fields.", "red");
            return;
        }
        if (!password.equals(confirm)) {
            showMessage("Passwords do not match.", "red");
            return;
        }
        if (!email.contains("@")) {
            showMessage("Please enter a valid email.", "red");
            return;
        }

        // Disable button and call AI to validate
        registerBtn.setDisable(true);
        aiSuggestionLabel.setText("AI is checking your details...");
        aiSuggestionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #185FA5;" +
                "-fx-background-color: #E6F1FB; -fx-background-radius: 8; -fx-padding: 8;");
        showMessage("", "black");

        new Thread(() -> {
            String system = "You are a registration validator for MediLink medical platform. " +
                    "Analyze the registration data and respond with ONLY a JSON object like this: " +
                    "{\"valid\": true, \"message\": \"All good! Your account looks great.\"} " +
                    "or {\"valid\": false, \"message\": \"Your password is too weak. Use at least 8 characters with numbers.\"} " +
                    "Rules to check: " +
                    "- Password must be at least 8 characters long. " +
                    "- Password should contain at least one number. " +
                    "- Name should be at least 3 characters. " +
                    "- Email must look valid. " +
                    "If anything fails set valid to false and explain what to fix. " +
                    "Always reply in English. Reply ONLY with the JSON, no extra text.";

            String prompt = "Name: " + name +
                    "\nEmail: " + email +
                    "\nPassword: " + password +
                    "\nPassword length: " + password.length();

            String aiReply = ClaudeAI.ask(system, prompt);

            Platform.runLater(() -> {
                try {
                    // Parse AI JSON response
                    com.google.gson.JsonObject result =
                            com.google.gson.JsonParser.parseString(aiReply).getAsJsonObject();

                    boolean isValid = result.get("valid").getAsBoolean();
                    String aiMessage = result.get("message").getAsString();

                    if (isValid) {
                        // ✅ AI approved — create the account
                        aiSuggestionLabel.setStyle(
                                "-fx-font-size: 12px; -fx-text-fill: #085041;" +
                                        "-fx-background-color: #E1F5EE; -fx-background-radius: 8; -fx-padding: 8;");
                        aiSuggestionLabel.setText("✓ " + aiMessage);

                        User user = new User(email, password, name, "[\"ROLE_USER\"]", "ACTIVE");
                        user.setPhone(phone);
                        new UserService().insert(user);
                        showMessage("Account created! Redirecting to login...", "green");

                        // Redirect after 2 seconds
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(this::goToLogin);
                            } catch (InterruptedException ignored) {}
                        }).start();

                    } else {
                        // ❌ AI rejected — show error, keep form open
                        aiSuggestionLabel.setStyle(
                                "-fx-font-size: 12px; -fx-text-fill: #791F1F;" +
                                        "-fx-background-color: #FCEBEB; -fx-background-radius: 8; -fx-padding: 8;");
                        aiSuggestionLabel.setText("✗ " + aiMessage);
                        showMessage("Please fix the issues above.", "red");
                        registerBtn.setDisable(false); // re-enable so user can try again
                    }

                } catch (Exception e) {
                    // If JSON parsing fails, fall back to basic check
                    System.err.println("AI parse error: " + e.getMessage());
                    aiSuggestionLabel.setText("AI check failed, using basic validation.");
                    if (password.length() >= 6) {
                        User user = new User(email, password, name, "[\"ROLE_USER\"]", "ACTIVE");
                        user.setPhone(phone);
                        new UserService().insert(user);
                        showMessage("Account created!", "green");
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(this::goToLogin);
                            } catch (InterruptedException ignored) {}
                        }).start();
                    }
                    registerBtn.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    public void goToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            nameField.getScene().setRoot(root);
            ThemeManager.apply(nameField.getScene());
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
        messageLabel.setText(msg);
    }
    @FXML
    public void goToLanding() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
            nameField.getScene().setRoot(root);
            ThemeManager.apply(nameField.getScene());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}