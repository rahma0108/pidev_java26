package esprit.tn.pidev;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Point d'entrée JavaFX : charge le FXML et affiche une fenêtre.
 * <p>Lancer cette classe (Run), pas {@link MediLinkApp}.</p>
 */
public class MediLinkFxApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                MediLinkFxApp.class.getResource("/landing.fxml")));
        Scene scene = new Scene(loader.load(), 1000, 650);
        stage.setTitle("MediLink - Landing");
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
