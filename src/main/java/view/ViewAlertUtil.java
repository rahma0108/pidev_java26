package view;

import javafx.scene.control.Alert;

final class ViewAlertUtil {

    private ViewAlertUtil() {
    }

    static void erreur(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(message != null ? message : "");
        a.getDialogPane().setMinWidth(480);
        a.getDialogPane().setMaxWidth(720);
        a.setResizable(true);
        a.showAndWait();
    }

    static void info(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(message != null ? message : "");
        a.showAndWait();
    }

    static boolean confirmer(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(message != null ? message : "");
        return a.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK;
    }
}
