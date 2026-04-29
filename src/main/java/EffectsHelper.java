import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class EffectsHelper {

    // ── 1. TYPING EFFECT on any label ──
    public static void typeText(Label label, String fullText, int msPerChar) {
        label.setText("");
        final int[] idx = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(msPerChar), e -> {
            if (idx[0] < fullText.length()) {
                label.setText(fullText.substring(0, ++idx[0]));
            }
        }));
        tl.setCycleCount(fullText.length());
        tl.play();
    }

    // ── 2. GLOW PULSE on a region ──
    public static void glowPulse(Region node, String color) {
        String glow1 = String.format(
            "-fx-effect: dropshadow(gaussian, %s, 20, 0.6, 0, 0);", color);
        String glow2 = String.format(
            "-fx-effect: dropshadow(gaussian, %s, 4, 0.1, 0, 0);", color);

        String baseStyle = node.getStyle();

        Timeline tl = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(node.styleProperty(), baseStyle + glow2)),
            new KeyFrame(Duration.millis(600),
                new KeyValue(node.styleProperty(), baseStyle + glow1)),
            new KeyFrame(Duration.millis(1200),
                new KeyValue(node.styleProperty(), baseStyle + glow2))
        );
        tl.setCycleCount(3);
        tl.play();
    }

    // ── 3. COUNT UP animation on a label ──
    public static void countUp(Label label, int target, String suffix, int delayMs) {
        final int[] current = {0};
        int steps = 40;
        int increment = Math.max(1, target / steps);

        Timeline tl = new Timeline(new KeyFrame(Duration.millis(30), e -> {
            current[0] = Math.min(current[0] + increment, target);
            label.setText(String.valueOf(current[0]) + suffix);
            if (current[0] >= target)
                label.setText(target + suffix);
        }));
        tl.setCycleCount(steps + 5);
        tl.setDelay(Duration.millis(delayMs));
        tl.play();
    }

    // ── 4. SLIDE IN from bottom ──
    public static void slideInUp(javafx.scene.Node node, int delayMs) {
        node.setOpacity(0);
        node.setTranslateY(40);

        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), node);
        tt.setFromY(40); tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(Interpolator.EASE_OUT);

        ft.play(); tt.play();
    }

    // ── 5. SHAKE effect (for errors) ──
    public static void shake(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setByX(10);
        tt.setAutoReverse(true);
        tt.setCycleCount(6);
        tt.play();
    }

    // ── 6. BOUNCE effect ──
    public static void bounce(javafx.scene.Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.15); st.setToY(1.15);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    // ── 7. FADE IN ──
    public static void fadeIn(javafx.scene.Node node, int delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.play();
    }

    // ── 8. NEON GLOW border on hover ──
    public static void addHoverGlow(Region node, String glowColor) {
        String base = node.getStyle();
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
            st.setToX(1.03); st.setToY(1.03); st.play();
            node.setStyle(base + "-fx-effect: dropshadow(gaussian, " +
                glowColor + ", 15, 0.4, 0, 0);");
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
            st.setToX(1.0); st.setToY(1.0); st.play();
            node.setStyle(base);
        });
    }
}
