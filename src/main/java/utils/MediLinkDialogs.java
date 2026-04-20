package utils;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;

import java.net.URL;

/**
 * Applique le thème {@code medilink-care.css} aux popups (Alert / Dialog) — pas d’héritage depuis la scène principale.
 */
public final class MediLinkDialogs {

    private MediLinkDialogs() {
    }

    public static void style(Alert alert) {
        if (alert == null) {
            return;
        }
        apply(alert.getDialogPane());
    }

    public static void style(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }
        apply(dialog.getDialogPane());
    }

    private static void apply(DialogPane pane) {
        if (pane == null) {
            return;
        }
        URL css = MediLinkDialogs.class.getResource("/styles/medilink-care.css");
        if (css != null) {
            String url = css.toExternalForm();
            if (!pane.getStylesheets().contains(url)) {
                pane.getStylesheets().add(url);
            }
        }
        if (!pane.getStyleClass().contains("dialog-care")) {
            pane.getStyleClass().add("dialog-care");
        }
    }
}
