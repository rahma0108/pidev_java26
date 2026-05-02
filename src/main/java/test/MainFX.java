package test;



import javafx.application.Application;
import javafx.geometry.Insets;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import javafx.stage.Stage;


import java.net.URL;

import java.util.Objects;



public class MainFX extends Application {
    @Override

    public void start(Stage stage) {
        stage.setTitle("Gestion des dons — Medilink");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        try {
            URL fxmlUrl = MainFX.class.getResource("/ListeDons.fxml");
            if (fxmlUrl == null) {
                throw new IllegalStateException("FXML introuvable sur le classpath : /ListeDons.fxml");
            }
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(fxmlUrl));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 960, 680));
        } catch (Throwable t) {
            System.err.println("[MainFX] Erreur au démarrage : " + t.getClass().getName() + " — " + t.getMessage());
            t.printStackTrace(System.err);
            stage.setScene(construireSceneSecours(t));
        }
        stage.show();
    }

    private static Scene construireSceneSecours(Throwable t) {
        String detail = t == null ? "Erreur inconnue."
                : t.getClass().getSimpleName() + " : " + (t.getMessage() == null ? "(sans message)" : t.getMessage());
        Label titre = new Label("L'interface principale n'a pas pu se lancer.");
        titre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        titre.setTextFill(Color.web("#f8fafc"));
        Label sousTitre = new Label("Un écran de secours est affiché. Consultez la console pour la trace complète.");
        sousTitre.setWrapText(true);
        sousTitre.setTextFill(Color.web("#cbd5e1"));
        Label details = new Label(detail);
        details.setWrapText(true);
        details.setTextFill(Color.web("#fca5a5"));
        details.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        VBox root = new VBox(12, titre, sousTitre, details);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #020617);");
        return new Scene(root, 960, 680);
    }



    public static void main(String[] args) {

        try {

            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("[MainFX] Erreur non capturée dans " + thread.getName() + " : "
                        + throwable.getClass().getName() + " — " + throwable.getMessage());
                throwable.printStackTrace(System.err);
            });
            launch(args);

        } catch (Throwable t) {

            System.err.println("[MainFX] Erreur dans launch() : " + t.getClass().getName() + " — " + t.getMessage());

            t.printStackTrace(System.err);

        }

    }

}

