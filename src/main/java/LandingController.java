import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LandingController {

    @FXML private Canvas animCanvas;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label counter1;
    @FXML private Label counter2;
    @FXML private Label counter3;
    @FXML private Label counter4;
    @FXML private Button themeToggleBtn;

    private final Random rand = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;
    private boolean isDarkBg = true;

    // ── Particle class — public so HomeController can reuse ──
    public static class Particle {
        public double x, y, vx, vy, radius, opacity;
        public String color;

        public Particle(double canvasW, double canvasH, Random r) {
            x = r.nextDouble() * canvasW;
            y = r.nextDouble() * canvasH;
            vx = (r.nextDouble() - 0.5) * 0.6;
            vy = (r.nextDouble() - 0.5) * 0.6;
            radius = 1.5 + r.nextDouble() * 2.5;
            opacity = 0.2 + r.nextDouble() * 0.5;
            String[] colors = {"#185FA5","#7fc4fd","#0F6E56","#534AB7","#1D9E75"};
            color = colors[r.nextInt(colors.length)];
        }

        public void update(double w, double h) {
            x += vx; y += vy;
            if (x < 0 || x > w) vx *= -1;
            if (y < 0 || y > h) vy *= -1;
        }
    }

    @FXML
    public void initialize() {
        themeToggleBtn.setText("🌙");

        double w = animCanvas.getWidth();
        double h = animCanvas.getHeight();

        for (int i = 0; i < 80; i++)
            particles.add(new Particle(w, h, rand));

        particleTimer = new AnimationTimer() {
            public void handle(long now) { drawFrame(w, h); }
        };
        particleTimer.start();

        // Fade in title
        titleLabel.setOpacity(0);
        subtitleLabel.setOpacity(0);
        FadeTransition ft1 = new FadeTransition(Duration.millis(1200), titleLabel);
        ft1.setFromValue(0); ft1.setToValue(1);
        ft1.setDelay(Duration.millis(300));
        FadeTransition ft2 = new FadeTransition(Duration.millis(1200), subtitleLabel);
        ft2.setFromValue(0); ft2.setToValue(1);
        ft2.setDelay(Duration.millis(700));
        ft1.play(); ft2.play();

        // Animate subtitle cycling
        animateSubtitle();

        // Animate counters
        animateCounter(counter1, 500, "500+");
        animateCounter(counter2, 50,  "50+");
        animateCounter(counter3, 1200, "1.2K+");
        animateCounter(counter4, 300, "300+");
    }

    private void drawFrame(double w, double h) {
        GraphicsContext gc = animCanvas.getGraphicsContext2D();
        gc.setFill(isDarkBg ? Color.web("#050d1a") : Color.web("#e8f0f8"));
        gc.fillRect(0, 0, w, h);

        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                Particle b = particles.get(j);
                double dist = Math.hypot(a.x - b.x, a.y - b.y);
                if (dist < 100) {
                    gc.setStroke(Color.web("#185FA5", (1 - dist/100) * 0.15));
                    gc.setLineWidth(0.5);
                    gc.strokeLine(a.x, a.y, b.x, b.y);
                }
            }
        }
        for (Particle p : particles) {
            p.update(w, h);
            gc.setFill(Color.web(p.color, p.opacity));
            gc.fillOval(p.x - p.radius, p.y - p.radius, p.radius*2, p.radius*2);
        }
    }

    private void animateSubtitle() {
        String[] texts = {
            "Connecting patients with doctors",
            "AI-powered health management",
            "Smart appointments, anytime",
            "Your health, our priority"
        };
        final int[] idx = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            idx[0] = (idx[0] + 1) % texts.length;
            FadeTransition ft = new FadeTransition(Duration.millis(600), subtitleLabel);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(ev -> {
                subtitleLabel.setText(texts[idx[0]]);
                FadeTransition ft2 = new FadeTransition(Duration.millis(600), subtitleLabel);
                ft2.setFromValue(0); ft2.setToValue(1);
                ft2.play();
            });
            ft.play();
        }));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    private void animateCounter(Label label, int target, String finalText) {
        final int[] current = {0};
        int steps = 60;
        int increment = Math.max(1, target / steps);
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(30), e -> {
            current[0] = Math.min(current[0] + increment, target);
            label.setText(String.valueOf(current[0]));
            if (current[0] >= target) label.setText(finalText);
        }));
        tl.setCycleCount(steps + 5);
        tl.setDelay(Duration.millis(800));
        tl.play();
    }

    @FXML
    public void toggleTheme() {
        isDarkBg = !isDarkBg;
        // Landing has its own dark bg — don't apply ThemeManager
        themeToggleBtn.setText(isDarkBg ? "☀️" : "🌙");
    }

    @FXML
    public void goToLogin() {
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            ThemeManager.applyWithFade(titleLabel.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void goToRegister() {
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/register.fxml"));
            ThemeManager.applyWithFade(titleLabel.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopAnimation() {
        if (particleTimer != null) particleTimer.stop();
    }
}
