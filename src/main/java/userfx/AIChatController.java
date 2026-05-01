package userfx;


import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class AIChatController {

    @FXML private VBox messagesBox;
    @FXML private TextField inputField;
    @FXML private Button sendBtn;
    @FXML private ScrollPane scrollPane;
    @FXML private Label typingLabel;

    private final List<String> history = new ArrayList<>();
    private int userCount = 0;

    @FXML
    public void initialize() {
        // Load context
        try {
            userCount = new UserService().getAll().size();
        } catch (Exception ignored) {}

        // Welcome message
        addBotMessage("Hello! I'm your MediLink AI assistant 🏥\nI can answer questions about your platform. Try asking:\n• How many users do we have?\n• What roles exist?\n• Give me a platform summary");

        // Send on Enter
        inputField.setOnAction(e -> handleSend());
    }

    @FXML
    public void handleSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        addUserMessage(text);
        history.add("User: " + text);

        sendBtn.setDisable(true);
        typingLabel.setVisible(true);
        typingLabel.setManaged(true);

        new Thread(() -> {
            String system = "You are an AI assistant for MediLink, a medical platform. " +
                            "You have access to this platform data: " +
                            "Total users: " + userCount + ". " +
                            "Roles available: ROLE_ADMIN, ROLE_MEDECIN, ROLE_USER. " +
                            "Tables: user, rendez_vous, medicaments, ordonnances, evenements. " +
                            "Answer questions about the platform concisely. " +
                            "Be friendly and professional. Max 3 sentences per answer.";

            String context = String.join("\n", history);
            String reply = ClaudeAI.ask(system, context + "\nUser: " + text);

            history.add("Assistant: " + reply);
            Platform.runLater(() -> {
                typingLabel.setVisible(false);
                typingLabel.setManaged(false);
                sendBtn.setDisable(false);
                addBotMessage(reply);
            });
        }).start();
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(4, 0, 4, 40));

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(260);
        msg.setStyle(
            "-fx-background-color: #185FA5;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 14 14 4 14;" +
            "-fx-padding: 10 14;");

        row.getChildren().add(msg);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 40, 4, 0));

        // Bot avatar
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size: 16px;");

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(260);
        msg.setStyle(
            "-fx-background-color: rgba(24,95,165,0.12);" +
            "-fx-text-fill: #1a1a2e;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 14 14 14 4;" +
            "-fx-padding: 10 14;" +
            "-fx-border-color: rgba(24,95,165,0.2);" +
            "-fx-border-radius: 14 14 14 4;" +
            "-fx-border-width: 0.5;");

        row.getChildren().addAll(avatar, msg);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    @FXML
    public void handleClearChat() {
        messagesBox.getChildren().clear();
        history.clear();
        addBotMessage("Chat cleared! How can I help you?");
    }
}
