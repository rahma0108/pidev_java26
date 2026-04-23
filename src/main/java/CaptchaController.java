import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Random;

public class CaptchaController {

    @FXML private TextArea questionLabel;
    @FXML private TextField answerField;
    @FXML private VBox sliderBox;
    @FXML private VBox questionBox;
    @FXML private StackPane sliderBtn;
    @FXML private Pane targetZone;
    @FXML private Label targetIcon;
    @FXML private Label sliderIcon;
    @FXML private Label instructionLabel;
    @FXML private Label statusLabel;

    private boolean verified = false;
    private double dragStartX;
    private double sliderCurrentX = 10;
    private static final double SLIDER_MIN = 10;
    private static final double SLIDER_MAX = 222;
    private final Random rand = new Random();

    // For text-based answer matching
    private String expectedTextAnswer = null;
    private int expectedNumberAnswer = Integer.MIN_VALUE;

    public boolean isVerified() { return verified; }

    @FXML
    public void initialize() {
        loadRandomCaptcha();
    }

    @FXML
    public void refreshCaptcha() {
        loadRandomCaptcha();
    }

    public void loadRandomCaptcha() {
        verified = false;
        statusLabel.setText("");
        expectedTextAnswer = null;
        expectedNumberAnswer = Integer.MIN_VALUE;

        // Remove old listener by replacing field
        answerField.setText("");

        if (rand.nextInt(2) == 0) {
            showSlider();
        } else {
            showQuestion();
        }
    }

    // ── SLIDER ──
    private void showSlider() {
        sliderBox.setVisible(true);  sliderBox.setManaged(true);
        questionBox.setVisible(false); questionBox.setManaged(false);

        sliderCurrentX = SLIDER_MIN;
        sliderBtn.setLayoutX(sliderCurrentX);
        sliderBtn.setStyle("-fx-background-color: #185FA5; -fx-background-radius: 10; -fx-cursor: hand;");
        sliderIcon.setText("→");
        targetIcon.setText("🔒");
        targetZone.setStyle("-fx-border-color: rgba(24,95,165,0.4); -fx-border-style: dashed; -fx-border-width: 2; -fx-border-radius: 8;");
        instructionLabel.setText("Drag the arrow all the way to the lock to verify");

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

        double progress = (sliderCurrentX - SLIDER_MIN) / (SLIDER_MAX - SLIDER_MIN);
        sliderBtn.setStyle("-fx-background-color: " +
                (progress > 0.5 ? "#0F6E56" : "#185FA5") +
                "; -fx-background-radius: 10; -fx-cursor: hand;");

        if (sliderCurrentX >= SLIDER_MAX - 15) verifySlider();
        e.consume();
    }

    private void onSliderReleased(MouseEvent e) {
        if (!verified) {
            TranslateTransition snap = new TranslateTransition(Duration.millis(300), sliderBtn);
            snap.setToX(SLIDER_MIN - sliderBtn.getLayoutX());
            snap.setOnFinished(ev -> {
                sliderCurrentX = SLIDER_MIN;
                sliderBtn.setLayoutX(SLIDER_MIN);
                sliderBtn.setTranslateX(0);
                sliderBtn.setStyle("-fx-background-color: #185FA5; -fx-background-radius: 10; -fx-cursor: hand;");
            });
            snap.play();
        }
        e.consume();
    }

    private void verifySlider() {
        verified = true;
        sliderBtn.setLayoutX(SLIDER_MAX);
        sliderBtn.setStyle("-fx-background-color: #0F6E56; -fx-background-radius: 10;");
        sliderIcon.setText("✓");
        targetIcon.setText("🔓");
        targetZone.setStyle("-fx-border-color: #5DCAA5; -fx-border-style: solid; -fx-border-width: 2; -fx-border-radius: 8;");
        statusLabel.setStyle("-fx-text-fill: #5DCAA5;");
        statusLabel.setText("Human verified!");

        ScaleTransition st = new ScaleTransition(Duration.millis(200), sliderBtn);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.2); st.setToY(1.2);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    // ── QUESTION ──
    private void showQuestion() {
        sliderBox.setVisible(false); sliderBox.setManaged(false);
        questionBox.setVisible(true); questionBox.setManaged(true);
        answerField.clear();

        int qType = rand.nextInt(4);

        switch (qType) {
            case 0: {
                // Word count
                String[][] options = {
                        {"The sky is blue today", "5"},
                        {"I love coding in Java", "5"},
                        {"MediLink is a health platform", "5"},
                        {"Doctors help patients recover quickly", "5"},
                        {"Java is a programming language", "5"}
                };
                String[] pick = options[rand.nextInt(options.length)];
                String sentence = pick[0];
                int count = sentence.split(" ").length;
                expectedNumberAnswer = count;
                questionLabel.setText("Count the words:\n\"" + sentence + "\"");
                instructionLabel.setText("Type the number of words");
                break;
            }
            case 1: {
                // Biggest number
                int a = 10 + rand.nextInt(80);
                int b = 10 + rand.nextInt(80);
                int c = 10 + rand.nextInt(80);
                // Make sure all different
                while (b == a) b = 10 + rand.nextInt(80);
                while (c == a || c == b) c = 10 + rand.nextInt(80);
                expectedNumberAnswer = Math.max(a, Math.max(b, c));
                questionLabel.setText("Largest number?\n\n" + a + "     " + b + "     " + c);
                instructionLabel.setText("Type the largest number");
                break;
            }
            case 2: {
                // Missing letter
                String[][] pairs = {
                        {"H_alth", "e"},
                        {"D_ctor", "o"},
                        {"Hosp_tal", "i"},
                        {"Medic_ne", "i"},
                        {"Pati_nt", "e"},
                        {"Nur_e", "s"},
                        {"Cli_ic", "n"},
                        {"_ospital", "h"}
                };
                String[] pair = pairs[rand.nextInt(pairs.length)];
                expectedTextAnswer = pair[1].toLowerCase();
                questionLabel.setText("Missing letter?\n\n" + pair[0]);
                instructionLabel.setText("Type the missing letter");
                break;
            }
            case 3: {
                // Even or odd
                int num = 2 + rand.nextInt(97);
                expectedTextAnswer = (num % 2 == 0) ? "even" : "odd";
                questionLabel.setText("Even or odd?\n\n" + num);
                instructionLabel.setText("Type:  even  or  odd");
                break;
            }
        }
    }

    @FXML
    public void checkAnswer() {
        String input = answerField.getText().trim().toLowerCase();
        if (input.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #E24B4A;");
            statusLabel.setText("Please type your answer.");
            return;
        }

        boolean correct = false;

        if (expectedTextAnswer != null) {
            correct = input.equals(expectedTextAnswer);
        } else if (expectedNumberAnswer != Integer.MIN_VALUE) {
            try {
                correct = Integer.parseInt(input) == expectedNumberAnswer;
            } catch (NumberFormatException e) {
                statusLabel.setStyle("-fx-text-fill: #E24B4A;");
                statusLabel.setText("Please type a valid answer.");
                return;
            }
        }

        if (correct) {
            verified = true;
            statusLabel.setStyle("-fx-text-fill: #5DCAA5;");
            statusLabel.setText("Correct! Verified!");
            answerField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #5DCAA5; -fx-font-size: 13px;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #E24B4A;");
            statusLabel.setText("Wrong! Try again.");
            answerField.clear();
            // Shake
            TranslateTransition shake = new TranslateTransition(Duration.millis(70), answerField);
            shake.setByX(8); shake.setAutoReverse(true); shake.setCycleCount(4);
            shake.play();
            // Load new question
            showQuestion();
        }
    }
}