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

    @FXML
    public void initialize() {
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");

        // Setup user info
        if (loggedInUser != null) {
            greetingLabel.setText("Hello, " + loggedInUser.getFullName().split(" ")[0] + "!");
            String role = loggedInUser.getRoles() != null
                ? loggedInUser.getRoles().replace("[","").replace("]","").replace("\"","").replace("ROLE_","")
                : "USER";
            roleChipLabel.setText(role);

            String initials = loggedInUser.getFullName().length() >= 2
                ? loggedInUser.getFullName().substring(0,1).toUpperCase() +
                  (loggedInUser.getFullName().contains(" ")
                    ? String.valueOf(loggedInUser.getFullName().split(" ")[1].charAt(0)).toUpperCase()
                    : "")
                : "?";
            avatarLabel.setText(initials);
        }

        // Start particles
        double w = animCanvas.getWidth();
        double h = animCanvas.getHeight();
        for (int i = 0; i < 70; i++)
            particles.add(new LandingController.Particle(w, h, rand));

        particleTimer = new AnimationTimer() {
            public void handle(long now) { drawFrame(w, h); }
        };
        particleTimer.start();

        // Animate hero fade in
        heroLabel.setOpacity(0);
        heroSub.setOpacity(0);
        FadeTransition f1 = new FadeTransition(Duration.millis(900), heroLabel);
        f1.setFromValue(0); f1.setToValue(1); f1.setDelay(Duration.millis(200)); f1.play();
        FadeTransition f2 = new FadeTransition(Duration.millis(900), heroSub);
        f2.setFromValue(0); f2.setToValue(1); f2.setDelay(Duration.millis(500)); f2.play();

        // Animate cards sliding in
        animateCardIn(cardRdv,   0);
        animateCardIn(cardMed,   150);
        animateCardIn(cardOrd,   300);
        animateCardIn(cardEvent, 450);

        // Load counts from DB
        loadCounts();

        // Load AI tip
        loadAITip();
    }

    private void drawFrame(double w, double h) {
        GraphicsContext gc = animCanvas.getGraphicsContext2D();
        gc.setFill(ThemeManager.isDark() ? Color.web("#050d1a") : Color.web("#0a1628"));
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
        card.setOpacity(0);
        card.setTranslateY(30);

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
                // Count from DB
                java.sql.Connection conn = MyConnection.getInstance().getConnection();

                // Appointments
                java.sql.ResultSet rs1 = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM rendez_vous");
                int rdv = rs1.next() ? rs1.getInt(1) : 0;

                // Medications
                java.sql.ResultSet rs2 = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM medicaments");
                int meds = rs2.next() ? rs2.getInt(1) : 0;

                // Ordonnances
                java.sql.ResultSet rs3 = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM ordonnances");
                int ords = rs3.next() ? rs3.getInt(1) : 0;

                // Events
                java.sql.ResultSet rs4 = conn.createStatement()
                    .executeQuery("SELECT COUNT(*) FROM evenements");
                int events = rs4.next() ? rs4.getInt(1) : 0;

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

    @FXML
    public void refreshAITip() {
        loadAITip();
    }

    private void loadAITip() {
        aiTipLabel.setText("Loading your personalized health tip...");
        new Thread(() -> {
            String name = loggedInUser != null ? loggedInUser.getFullName().split(" ")[0] : "User";
            String system = "You are a friendly medical assistant for MediLink. " +
                            "Give ONE short, practical, positive health tip (max 2 sentences). " +
                            "Make it feel personal and encouraging. Always reply in English.";
            String prompt = "Give a health tip for a patient named " + name;
            String tip = ClaudeAI.ask(system, prompt);
            Platform.runLater(() -> {
                aiTipLabel.setText(tip);
                FadeTransition ft = new FadeTransition(Duration.millis(500), aiTipLabel);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            });
        }).start();
    }

    // ── Hover effects ──
    @FXML
    public void onCardHover(javafx.scene.input.MouseEvent e) {
        VBox card = (VBox) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
        st.setToX(1.05); st.setToY(1.05); st.play();
    }

    @FXML
    public void onCardExit(javafx.scene.input.MouseEvent e) {
        VBox card = (VBox) e.getSource();
        ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
        st.setToX(1.0); st.setToY(1.0); st.play();
    }

    // ── Navigation ──
    @FXML public void goToRdv()         { navigateTo("/rendez_vous.fxml"); }
    @FXML public void goToMedicaments() { navigateTo("/medicaments.fxml"); }
    @FXML public void goToOrdonnances() { navigateTo("/ordonnances.fxml"); }
    @FXML public void goToEvents()      { navigateTo("/evenements.fxml"); }

    @FXML
    public void handleLogout() {
        stopAnimation();
        navigateTo("/main.fxml");
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        themeToggleBtn.setText(ThemeManager.isDark() ? "☀️" : "🌙");
    }

    private void navigateTo(String fxml) {
        stopAnimation();
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            heroLabel.getScene().setRoot(root);
            ThemeManager.apply(heroLabel.getScene());
        } catch (Exception e) {
            System.err.println("Page not built yet: " + fxml);
            showComingSoon(fxml);
        }
    }

    private void showComingSoon(String page) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(null);
        alert.setContentText("This page (" + page.replace("/","").replace(".fxml","") +
                             ") will be built by your teammate. Stay tuned!");
        alert.show();
    }

    private void stopAnimation() {
        if (particleTimer != null) particleTimer.stop();
    }
}
