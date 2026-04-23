import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.util.Random;
import java.util.function.Consumer;

public class CaptchaController {

    @FXML private Pane puzzlePane;
    @FXML private Label instructionLabel;
    @FXML private Label statusLabel;
    @FXML private Label questionLabel;
    @FXML private TextField answerField;
    @FXML private VBox sliderBox;
    @FXML private VBox questionBox;
    @FXML private StackPane sliderBtn;
    @FXML private Pane targetZone;
    @FXML private Label targetIcon;
    @FXML private Label sliderIcon;

    private boolean verified = false;
    private double dragStartX;
    private double sliderCurrentX = 10;
    private static final double SLIDER_MIN = 10;
    private static final double SLIDER_MAX = 222;
    private Consumer<Boolean> onVerified;
    private final Random rand = new Random();

    // Captcha types
    private enum CaptchaType { SLIDER, QUESTION }
    private CaptchaType currentType;
    private int correctAnswer;

    public void setOnVerified(Consumer<Boolean> callback) {
        this.onVerified = callback;
    }

    public boolean isVerified() { return verified; }

    @FXML
    public void initialize() {
        loadRandomCaptcha();
    }

    public void loadRandomCaptcha() {
        verified = false;
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: #667788;");

        // Pick random type
        int type = rand.nextInt(2);
        if (type == 0) {
            showSlider();
        } else {
            showQuestion();
        }
    }

    // ── SLIDER CAPTCHA ──
    private void showSlider() {
        currentType = CaptchaType.SLIDER;
        sliderBox.setVisible(true);  sliderBox.setManaged(true);
        questionBox.setVisible(false); questionBox.setManaged(false);

        // Reset slider position
        sliderCurrentX = SLIDER_MIN;
        sliderBtn.setLayoutX(sliderCurrentX);
        sliderBtn.setStyle(
            "-fx-background-color: #185FA5; -fx-background-radius: 10; " +
            "-fx-cursor: hand;");
        sliderIcon.setText("→");
        targetIcon.setText("🔒");
        targetZone.setStyle(
            "-fx-border-color: rgba(24,95,165,0.4); -fx-border-style: dashed;" +
            "-fx-border-width: 2; -fx-border-radius: 8;");

        instructionLabel.setText("Drag the arrow to the lock icon to verify");

        // Mouse events
        sliderBtn.setOnMousePressed(this::onSliderPressed);
        sliderBtn.setOnMouseDragged(this::onSliderDragged);
        sliderBtn.setOnMouseReleased(this::onSliderReleased);
    }

    private void onSliderPressed(MouseEvent e) {
        dragStartX = e.getSceneX() - sliderCurrentX;
        e.consume();
    }

    private void onSliderDragged(MouseEvent e) {
        double newX = e.getSceneX() - dragStartX;
        newX = Math.max(SLIDER_MIN, Math.min(newX, SLIDER_MAX));
        sliderCurrentX = newX;
        sliderBtn.setLayoutX(sliderCurrentX);

        // Change color as user drags
        double progress = (sliderCurrentX - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN);
        if (progress > 0.5) {
            sliderBtn.setStyle(
                "-fx-background-color: #0F6E56; -fx-background-radius: 10; -fx-cursor: hand;");
        } else {
            sliderBtn.setStyle(
                "-fx-background-color: #185FA5; -fx-background-radius: 10; -fx-cursor: hand;");
        }

        // Check if reached target
        if (sliderCurrentX >= SLIDER_MAX - 15) {
            verifySlider();
        }
        e.consume();
    }

    private void onSliderReleased(MouseEvent e) {
        if (!verified) {
            // Snap back with animation
            TranslateTransition snap = new TranslateTransition(Duration.millis(300), sliderBtn);
            snap.setToX(SLIDER_MIN - sliderBtn.getLayoutX());
            snap.setOnFinished(ev -> {
                sliderCurrentX = SLIDER_MIN;
                sliderBtn.setLayoutX(SLIDER_MIN);
                sliderBtn.setTranslateX(0);
                sliderBtn.setStyle(
                    "-fx-background-color: #185FA5; -fx-background-radius: 10; -fx-cursor: hand;");
            });
            snap.play();
        }
        e.consume();
    }

    private void verifySlider() {
        verified = true;
        sliderBtn.setLayoutX(SLIDER_MAX);
        sliderBtn.setStyle(
            "-fx-background-color: #0F6E56; -fx-background-radius: 10;");
        sliderIcon.setText("✓");
        targetIcon.setText("🔓");
        targetZone.setStyle(
            "-fx-border-color: #5DCAA5; -fx-border-style: solid;" +
            "-fx-border-width: 2; -fx-border-radius: 8;");
        statusLabel.setStyle("-fx-text-fill: #5DCAA5;");
        statusLabel.setText("Human verified!");

        // Bounce animation
        ScaleTransition st = new ScaleTransition(Duration.millis(200), sliderBtn);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.2); st.setToY(1.2);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();

        if (onVerified != null) onVerified.accept(true);
    }

    // ── QUESTION CAPTCHA ──
    private void showQuestion() {
        currentType = CaptchaType.QUESTION;
        sliderBox.setVisible(false); sliderBox.setManaged(false);
        questionBox.setVisible(true); questionBox.setManaged(true);
        answerField.clear();

        // Random question types
        int qType = rand.nextInt(4);
        switch (qType) {
            case 0: // Word count
                String[] sentences = {
                    "The sky is blue today",
                    "I love coding in Java",
                    "MediLink is a health platform",
                    "Doctors help patients recover"
                };
                String sentence = sentences[rand.nextInt(sentences.length)];
                correctAnswer = sentence.split(" ").length;
                questionLabel.setText("How many words are in:\n\"" + sentence + "\"?");
                break;

            case 1: // Which is biggest
                int a = 10 + rand.nextInt(50);
                int b = 10 + rand.nextInt(50);
                int c = 10 + rand.nextInt(50);
                correctAnswer = Math.max(a, Math.max(b, c));
                questionLabel.setText("Which is the largest number?\n" + a + ", " + b + ", " + c);
                break;

            case 2: // Missing letter
                String[] words = {"H_alth", "D_ctor", "Hosp_tal", "Medic_ne", "Pati_nt"};
                String[] answers = {"e", "o", "i", "i", "e"};
                int idx = rand.nextInt(words.length);
                questionLabel.setText("What letter is missing?\n" + words[idx]);
                // Store answer as string comparison
                correctAnswer = -1;
                answerField.setOnAction(null);
                final String letterAnswer = answers[idx];
                instructionLabel.setText("Type the missing letter");
                answerField.textProperty().addListener((obs, old, val) -> {
                    if (val.trim().equalsIgnoreCase(letterAnswer)) {
                        verifyQuestion(true);
                    }
                });
                return;

            case 3: // Even or odd
                int num = 1 + rand.nextInt(99);
                correctAnswer = num % 2 == 0 ? 1 : 0;
                questionLabel.setText("Is " + num + " even or odd?\nType: even or odd");
                correctAnswer = -2; // special case
                final boolean isEven = num % 2 == 0;
                answerField.textProperty().addListener((obs, old, val) -> {
                    String v = val.trim().toLowerCase();
                    if ((isEven && v.equals("even")) || (!isEven && v.equals("odd"))) {
                        verifyQuestion(true);
                    }
                });
                instructionLabel.setText("Type your answer");
                return;
        }

        instructionLabel.setText("Type the correct answer");
        answerField.setOnAction(e -> checkAnswer());
    }

    @FXML
    public void checkAnswer() {
        String input = answerField.getText().trim();
        if (input.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #E24B4A;");
            statusLabel.setText("Please type your answer.");
            return;
        }
        try {
            int ans = Integer.parseInt(input);
            verifyQuestion(ans == correctAnswer);
        } catch (NumberFormatException e) {
            statusLabel.setStyle("-fx-text-fill: #E24B4A;");
            statusLabel.setText("Please type a number.");
        }
    }

    private void verifyQuestion(boolean correct) {
        if (correct) {
            verified = true;
            statusLabel.setStyle("-fx-text-fill: #5DCAA5;");
            statusLabel.setText("Correct! Human verified!");
            answerField.setStyle(
                "-fx-background-radius: 8; -fx-border-radius: 8;" +
                "-fx-border-color: #5DCAA5; -fx-font-size: 13px;");
            if (onVerified != null) onVerified.accept(true);
        } else {
            statusLabel.setStyle("-fx-text-fill: #E24B4A;");
            statusLabel.setText("Wrong! Try again.");
            answerField.clear();
            // Shake animation
            TranslateTransition shake = new TranslateTransition(Duration.millis(80), answerField);
            shake.setByX(8); shake.setAutoReverse(true); shake.setCycleCount(4);
            shake.play();
        }
    }

    @FXML
    public void refreshCaptcha() {
        loadRandomCaptcha();
    }
}
