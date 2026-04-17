import javafx.scene.Scene;

public class ThemeManager {

    private static boolean isDark = false;

    private static final String LIGHT_STYLE =
        "-fx-base: #f0f4f8;" +
        "-fx-background: #f0f4f8;" +
        "-fx-control-inner-background: white;";

    private static final String DARK_STYLE =
        "-fx-base: #0f1626;" +
        "-fx-background: #0f1626;" +
        "-fx-control-inner-background: #16213e;" +
        "-fx-accent: #185FA5;";

    public static boolean isDark() {
        return isDark;
    }

    public static void toggle(Scene scene) {
        isDark = !isDark;
        apply(scene);
    }

    public static void apply(Scene scene) {
        if (scene == null) return;
        scene.getRoot().setStyle(isDark ? DARK_STYLE : LIGHT_STYLE);

        // Add or remove dark stylesheet
        if (isDark) {
            if (!scene.getStylesheets().contains("dark.css")) {
                var url = ThemeManager.class.getResource("/dark.css");
                if (url != null) scene.getStylesheets().add(url.toExternalForm());
            }
        } else {
            scene.getStylesheets().removeIf(s -> s.contains("dark.css"));
        }
    }
}
