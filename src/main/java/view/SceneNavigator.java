package view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

final class SceneNavigator {

    private SceneNavigator() {
    }

    static void switchScene(Node sourceNode, String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) sourceNode.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.centerOnScreen();
        } catch (IOException e) {
            ViewAlertUtil.erreur("Navigation", "Impossible d'ouvrir " + fxmlPath + " : " + e.getMessage());
        }
    }
}
