package esprit.tn.pidev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Point d'entrée JavaFX : charge le FXML et affiche une fenêtre.
 * <p>Lancer cette classe (Run), pas {@link MediLinkApp}.</p>
 */
public class MediLinkFxApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                MediLinkFxApp.class.getResource("/fxml/LoginView.fxml")));
        Scene scene = new Scene(loader.load(), 980, 620);
        URL globalCss = MediLinkFxApp.class.getResource("/fxml/style.css");
        if (globalCss != null) {
            scene.getStylesheets().add(globalCss.toExternalForm());
        }
        stage.setTitle("MediLink - Accueil");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
