import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LoadingController {

    @FXML private Canvas animCanvas;
    @FXML private ProgressIndicator spinner;
    @FXML private ProgressBar progressBar;
    @FXML private Label loadingLabel;
    @FXML private Label subLabel;
    @FXML private Label percentLabel;
    @FXML private Circle dot1, dot2, dot3;
    @FXML private Circle outerRing, middleRing;

    private static String targetFxml;

    private final Random rand = new Random();
    private final List<LandingController.Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;

    private static final String[][] MESSAGES = {
        {"Initializing systems...",     "Preparing your workspace"},
        {"Connecting to database...",   "Fetching your data"},
        {"Loading AI modules...",       "Warming up intelligence"},
        {"Syncing health records...",   "Almost there"},
        {"Calibrating interface...",    "Fine-tuning the experience"},
        {"Securing connection...",      "Encrypting your session"},
        {"Booting MediLink core...",    "Starting services"},
        {"Analyzing user profile...",   "Personalizing your view"},
        {"Loading dashboard...",        "Setting up your workspace"},
        {"Preparing charts...",         "Crunching the numbers"},
    };

    public static void navigateTo(String fxml, javafx.scene.Scene scene) {
        targetFxml = fxml;
        try {
            Parent loadingRoot = FXMLLoader.load(
                LoadingController.class.getResource("/loading.fxml"));
            ThemeManager.applyWithFade(scene, loadingRoot, null);
        } catch (Exception e) {
            // Fallback: navigate directly
            try {
                Parent root = FXMLLoader.load(LoadingController.class.getResource(fxml));
                ThemeManager.applyWithFade(scene, root, null);
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @FXML
    public void initialize() {
        // Start particles
        double w = animCanvas.getWidth();
        double h = animCanvas.getHeight();
        for (int i = 0; i < 50; i++)
            particles.add(new LandingController.Particle(w, h, rand));

        particleTimer = new AnimationTimer() {
            public void handle(long now) { drawFrame(w, h); }
        };
        particleTimer.start();

        // Pick random message
        String[] msg = MESSAGES[rand.nextInt(MESSAGES.length)];
        loadingLabel.setText(msg[0]);
        subLabel.setText(msg[1]);

        // Random duration 1–5 seconds
        int durationMs = 1000 + rand.nextInt(4000);

        // Animate dots
        animateDots();

        // Animate rings
        animateRings();

        // Animate progress
        animateProgress(durationMs);

        // Navigate after duration
        new Thread(() -> {
            try { Thread.sleep(durationMs); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                stopAnimation();
                try {
                    Parent root = FXMLLoader.load(getClass().getResource(targetFxml));
                    ThemeManager.applyWithFade(progressBar.getScene(), root, null);
                } catch (Exception e) { e.printStackTrace(); }
            });
        }).start();
    }

    private void animateProgress(int durationMs) {
        progressBar.setProgress(0);
        final long[] startTime = {-1};
        final String[] midMessages = MESSAGES[rand.nextInt(MESSAGES.length)];

        AnimationTimer progressTimer = new AnimationTimer() {
            public void handle(long now) {
                if (startTime[0] < 0) startTime[0] = now;
                double elapsed  = (now - startTime[0]) / 1_000_000.0;
                double progress = Math.min(elapsed / durationMs, 1.0);

                progressBar.setProgress(progress);
                int pct = (int)(progress * 100);
                percentLabel.setText(pct + "%");

                if (pct == 50) {
                    loadingLabel.setText(midMessages[0]);
                    subLabel.setText(midMessages[1]);
                }
                if (progress >= 1.0) stop();
            }
        };
        progressTimer.start();
    }

    private void animateDots() {
        String[] colors = {"#185FA5","rgba(24,95,165,0.5)","rgba(24,95,165,0.2)"};
        Timeline tl = new Timeline();
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(300 * i), e -> {
                dot1.setStyle("-fx-fill: " + colors[(0+idx)%3] + ";");
                dot2.setStyle("-fx-fill: " + colors[(1+idx)%3] + ";");
                dot3.setStyle("-fx-fill: " + colors[(2+idx)%3] + ";");
            }));
        }
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    private void animateRings() {
        ScaleTransition st1 = new ScaleTransition(Duration.millis(1500), outerRing);
        st1.setFromX(1.0); st1.setToX(1.15);
        st1.setFromY(1.0); st1.setToY(1.15);
        st1.setAutoReverse(true);
        st1.setCycleCount(Timeline.INDEFINITE);
        st1.setInterpolator(Interpolator.EASE_BOTH);
        st1.play();

        ScaleTransition st2 = new ScaleTransition(Duration.millis(1500), middleRing);
        st2.setFromX(1.1); st2.setToX(0.95);
        st2.setFromY(1.1); st2.setToY(0.95);
        st2.setAutoReverse(true);
        st2.setCycleCount(Timeline.INDEFINITE);
        st2.setInterpolator(Interpolator.EASE_BOTH);
        st2.play();

        FadeTransition ft = new FadeTransition(Duration.millis(800), loadingLabel);
        ft.setFromValue(0.5); ft.setToValue(1.0);
        ft.setAutoReverse(true);
        ft.setCycleCount(Timeline.INDEFINITE);
        ft.play();
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
                if (dist < 120) {
                    gc.setStroke(Color.web("#185FA5", (1-dist/120)*0.15));
                    gc.setLineWidth(0.5);
                    gc.strokeLine(a.x, a.y, b.x, b.y);
                }
            }
        }
        for (LandingController.Particle p : particles) {
            p.update(w, h);
            gc.setFill(Color.web(p.color, p.opacity * 0.5));
            gc.fillOval(p.x-p.radius, p.y-p.radius, p.radius*2, p.radius*2);
        }
    }

    private void stopAnimation() {
        if (particleTimer != null) particleTimer.stop();
    }
}
