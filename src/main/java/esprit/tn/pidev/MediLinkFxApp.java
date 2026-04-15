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
                MediLinkFxApp.class.getResource("/fxml/ListeDisponibilitesView.fxml")));
        Scene scene = new Scene(loader.load(), 920, 580);
        stage.setTitle("MediLink — Disponibilités");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
