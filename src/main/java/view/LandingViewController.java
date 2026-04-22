package view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class LandingViewController {

    @FXML
    private Button themeToggleBtn;
    @FXML
    private Label counter1;
    @FXML
    private Label counter2;
    @FXML
    private Label counter3;
    @FXML
    private Label counter4;

    @FXML
    private void initialize() {
        animateCounter(counter1, 520);
        animateCounter(counter2, 84);
        animateCounter(counter3, 1460);
        animateCounter(counter4, 320);
    }

    @FXML
    private void goToLogin() {
        SceneNavigator.switchScene(themeToggleBtn, "/fxml/LoginView.fxml", "MediLink - Connexion", 600, 380);
    }

    @FXML
    private void goToRegister() {
        // Temporary behavior: no account flow yet, forward to role selection.
        goToLogin();
    }

    @FXML
    private void toggleTheme() {
        Node root = themeToggleBtn != null ? themeToggleBtn.getScene().getRoot() : null;
        if (root == null) {
            return;
        }
        String lightStyle = "-fx-background-color: #050d1a;";
        String darkStyle = "-fx-background-color: #eef4fb;";
        String current = root.getStyle();
        boolean dark = current != null && current.contains("#050d1a");
        root.setStyle(dark ? darkStyle : lightStyle);
        themeToggleBtn.setText(dark ? "☀️" : "🌙");
    }

    private static void animateCounter(Label label, int target) {
        if (label == null) {
            return;
        }
        Timeline timeline = new Timeline();
        int steps = 24;
        for (int i = 1; i <= steps; i++) {
            final int value = (target * i) / steps;
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * 35L), e -> label.setText(Integer.toString(value))));
        }
        timeline.play();
    }
}
