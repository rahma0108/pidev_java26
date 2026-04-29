import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeController {

    @FXML private Canvas animCanvas;
    @FXML private Label greetingLabel;
    @FXML private Label roleChipLabel;
    @FXML private Label avatarLabel;
    @FXML private Label heroLabel;
    @FXML private Label heroSub;
    @FXML private Label aiTipLabel;
    @FXML private Label rdvCount;
    @FXML private Label medCount;
    @FXML private Label ordCount;
    @FXML private Label eventCount;
    @FXML private Button themeToggleBtn;

    @FXML private VBox cardRdv;
    @FXML private VBox cardMed;
    @FXML private VBox cardOrd;
    @FXML private VBox cardEvent;

    private static User loggedInUser;
    private final Random rand = new Random();
    private final List<LandingController.Particle> particles = new ArrayList<>();
    private AnimationTimer particleTimer;

    public static void setUser(User user) {
        loggedInUser = user;
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");

        if (loggedInUser != null) {
            greetingLabel.setText("Hello, " + loggedInUser.getFullName().split(" ")[0] + "!");
            String role = loggedInUser.getRoles() != null
                ? loggedInUser.getRoles().replace("[","").replace("]","")
                    .replace("\"","").replace("ROLE_","")
                : "USER";
            roleChipLabel.setText(role);

            String[] parts = loggedInUser.getFullName().trim().split(" ");
            String initials = parts[0].substring(0,1).toUpperCase() +
                (parts.length > 1 ? String.valueOf(parts[1].charAt(0)).toUpperCase() : "");
            avatarLabel.setText(initials);
        }

        // Bind canvas to window size
        animCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                animCanvas.widthProperty().bind(newScene.widthProperty());
                animCanvas.heightProperty().bind(newScene.heightProperty());
            }
        });

        double w = animCanvas.getWidth();
        double h = animCanvas.getHeight();
        for (int i = 0; i < 70; i++)
            particles.add(new LandingController.Particle(w, h, rand));

        particleTimer = new AnimationTimer() {
            public void handle(long now) {
                drawFrame(animCanvas.getWidth(), animCanvas.getHeight());
            }
        };
        particleTimer.start();

        // Fade in hero
        heroLabel.setOpacity(0); heroSub.setOpacity(0);
        fade(heroLabel, 900, 200); fade(heroSub, 900, 500);

        // Animate cards
        animateCardIn(cardRdv,   0);
        animateCardIn(cardMed,   150);
        animateCardIn(cardOrd,   300);
        animateCardIn(cardEvent, 450);

        // Add hover glow to cards
        javafx.application.Platform.runLater(() -> {
            EffectsHelper.addHoverGlow(cardRdv,   "#185FA5");
            EffectsHelper.addHoverGlow(cardMed,   "#0F6E56");
            EffectsHelper.addHoverGlow(cardOrd,   "#534AB7");
            EffectsHelper.addHoverGlow(cardEvent, "#1D9E75");
        });

        // Typing effect on hero
        javafx.application.Platform.runLater(() ->
            EffectsHelper.typeText(heroSub, "Everything you need, in one place", 40));

        loadCounts();
        loadAITip();
    }

    private void fade(javafx.scene.Node node, int ms, int delay) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delay));
        ft.play();
    }

    private void drawFrame(double w, double h) {
        GraphicsContext gc = animCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#050d1a"));
        gc.fillRect(0, 0, w, h);

        for (int i = 0; i < particles.size(); i++) {
            LandingController.Particle a = particles.get(i);
            for (int j = i + 1; j < particles.size(); j++) {
                LandingController.Particle b = particles.get(j);
                double dist = Math.hypot(a.x - b.x, a.y - b.y);
                if (dist < 110) {
                    gc.setStroke(Color.web("#185FA5", (1 - dist/110) * 0.12));
                    gc.setLineWidth(0.5);
                    gc.strokeLine(a.x, a.y, b.x, b.y);
                }
            }
        }
        for (LandingController.Particle p : particles) {
            p.update(w, h);
            gc.setFill(Color.web(p.color, p.opacity * 0.7));
            gc.fillOval(p.x - p.radius, p.y - p.radius, p.radius*2, p.radius*2);
        }
    }

    private void animateCardIn(VBox card, int delayMs) {
        card.setOpacity(0); card.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(600), card);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs + 400));
        TranslateTransition tt = new TranslateTransition(Duration.millis(600), card);
        tt.setFromY(30); tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs + 400));
        tt.setInterpolator(Interpolator.EASE_OUT);
        ft.play(); tt.play();
    }

    private void loadCounts() {
        new Thread(() -> {
            try {
                java.sql.Connection conn = MyConnection.getInstance().getConnection();
                int rdv    = getCount(conn, "SELECT COUNT(*) FROM rendez_vous");
                int meds   = getCount(conn, "SELECT COUNT(*) FROM medicaments");
                int ords   = getCount(conn, "SELECT COUNT(*) FROM ordonnances");
                int events = getCount(conn, "SELECT COUNT(*) FROM evenements");
                Platform.runLater(() -> {
                    rdvCount.setText(rdv + " upcoming");
                    medCount.setText(meds + " items");
                    ordCount.setText(ords + " prescriptions");
                    eventCount.setText(events + " upcoming");
                });
            } catch (Exception e) {
                System.err.println("Count error: " + e.getMessage());
            }
        }).start();
    }

    private int getCount(java.sql.Connection conn, String sql) throws Exception {
        java.sql.ResultSet rs = conn.createStatement().executeQuery(sql);
        return rs.next() ? rs.getInt(1) : 0;
    }

    @FXML public void refreshAITip() { loadAITip(); }
    @FXML
    public void goToProfile() {
        stopAnimation();
        try {
            ProfileController.setUser(loggedInUser);
            Parent root = FXMLLoader.load(getClass().getResource("/profile.fxml"));
            ThemeManager.applyWithFade(animCanvas.getScene(), root, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void loadAITip() {
        aiTipLabel.setText("Loading your personalized health tip...");
        new Thread(() -> {
            String name = loggedInUser != null ? loggedInUser.getFullName().split(" ")[0] : "User";
            String tip  = ClaudeAI.ask(
                "You are a friendly medical assistant. Give ONE short practical health tip (max 2 sentences). Be encouraging. Always reply in English.",
                "Health tip for patient named " + name
            );
            Platform.runLater(() -> {
                aiTipLabel.setText(tip);
                FadeTransition ft = new FadeTransition(Duration.millis(500), aiTipLabel);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            });
        }).start();
    }

    // ── Hover effects ──
    @FXML public void onCardHover(javafx.scene.input.MouseEvent e) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), (VBox) e.getSource());
        st.setToX(1.05); st.setToY(1.05); st.play();
    }

    @FXML public void onCardExit(javafx.scene.input.MouseEvent e) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), (VBox) e.getSource());
        st.setToX(1.0); st.setToY(1.0); st.play();
    }

    // ── Navigation — Coming Soon for all ──
    @FXML public void goToRdv()         { PopupHelper.showComingSoon("Appointments"); }
    @FXML public void goToMedicaments() { PopupHelper.showComingSoon("Medications"); }
    @FXML public void goToOrdonnances() { PopupHelper.showComingSoon("Prescriptions"); }
    @FXML public void goToEvents()      { 
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/event_front.fxml"));
            ThemeManager.applyWithFade(animCanvas.getScene(), root, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        RememberMeHelper.clear(); // ← ADD THIS LINE
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
            ThemeManager.applyWithFade(animCanvas.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
    }

    private void stopAnimation() {
        if (particleTimer != null) particleTimer.stop();
    }
}
