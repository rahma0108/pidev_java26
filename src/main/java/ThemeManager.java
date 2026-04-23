import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

public class ThemeManager {

    private static boolean isDark = false;

    private static final String LIGHT_STYLE =
            "-fx-base: #f0f4f8;" +
                    "-fx-background: #f0f4f8;" +
                    "-fx-control-inner-background: white;";

    private static final String DARK_STYLE =
            "-fx-base: #1a1a2e;" +
                    "-fx-background: #1a1a2e;" +
                    "-fx-control-inner-background: #16213e;" +
                    "-fx-text-fill: white;";

    public static boolean isDark() { return isDark; }

    public static void toggle(Scene scene) {
        isDark = !isDark;
        apply(scene);
    }

    public static void apply(Scene scene) {
        if (scene == null) return;
        scene.getRoot().setStyle(isDark ? DARK_STYLE : LIGHT_STYLE);
        if (isDark) {
            var url = ThemeManager.class.getResource("/dark.css");
            if (url != null && !scene.getStylesheets().contains(url.toExternalForm()))
                scene.getStylesheets().add(url.toExternalForm());
        } else {
            scene.getStylesheets().removeIf(s -> s.contains("dark.css"));
        }
    }

    // ── Fade transition ──
    public static void applyWithFade(Scene scene, Parent newRoot, Runnable onDone) {
        javafx.scene.Node oldRoot = scene.getRoot();
        javafx.animation.FadeTransition fadeOut =
                new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), oldRoot);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            scene.setRoot(newRoot);
            newRoot.setOpacity(0);
            apply(scene);
            javafx.animation.FadeTransition fadeIn =
                    new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), newRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setOnFinished(ev -> { if (onDone != null) onDone.run(); });
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ── Navigate WITH loading screen ──
    public static void navigateWithLoading(Scene scene, String targetFxml) {
        LoadingController.navigateTo(targetFxml, scene);
    }
}