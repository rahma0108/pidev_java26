import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

public class ThemeManager {

    private static boolean isDark = false;

    public static boolean isDark() { return isDark; }

    public static void toggle(Scene scene) {
        isDark = !isDark;
        apply(scene);
    }

    public static void apply(Scene scene) {
        if (scene == null) return;

        // Remove dark stylesheet first
        scene.getStylesheets().removeIf(s -> s.contains("dark.css"));

        if (isDark) {
            // Only add dark.css — do NOT set inline styles that override card colors
            var url = ThemeManager.class.getResource("/dark.css");
            if (url != null) scene.getStylesheets().add(url.toExternalForm());
        }

        // Always load captcha style
        var captchaUrl = ThemeManager.class.getResource("/captcha_style.css");
        if (captchaUrl != null &&
            !scene.getStylesheets().contains(captchaUrl.toExternalForm())) {
            scene.getStylesheets().add(captchaUrl.toExternalForm());
        }
    }

    // ── Fade transition ──
    public static void applyWithFade(Scene scene, Parent newRoot, Runnable onDone) {
        javafx.scene.Node oldRoot = scene.getRoot();
        javafx.animation.FadeTransition fadeOut =
            new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(200), oldRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            scene.setRoot(newRoot);
            newRoot.setOpacity(0);
            apply(scene);
            javafx.animation.FadeTransition fadeIn =
                new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), newRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(ev -> { if (onDone != null) onDone.run(); });
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ── Navigate with loading screen ──
    public static void navigateWithLoading(Scene scene, String targetFxml) {
        LoadingController.navigateTo(targetFxml, scene);
    }
}
