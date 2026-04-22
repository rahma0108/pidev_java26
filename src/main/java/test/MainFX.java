package test;



import javafx.application.Application;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;

import javafx.scene.Scene;

import javafx.stage.Stage;



import java.net.URL;

import java.util.Objects;



<<<<<<< HEAD
public class MainFX extends Application {



    @Override

    public void start(Stage stage) {

        try {

            URL fxmlUrl = MainFX.class.getResource("/ListeDons.fxml");

            if (fxmlUrl == null) {

                System.err.println("[MainFX] FXML introuvable sur le classpath : /ListeDons.fxml");

                return;

            }

            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(fxmlUrl));

            Parent root = loader.load();

            stage.setTitle("Gestion des dons — Medilink");

            stage.setScene(new Scene(root, 960, 680));

            stage.show();

        } catch (Throwable t) {

            System.err.println("[MainFX] Erreur au démarrage : " + t.getClass().getName() + " — " + t.getMessage());

            t.printStackTrace(System.err);

        }

    }
=======
public class MainFX {
>>>>>>> gestion-dons-java



    public static void main(String[] args) {

        try {

<<<<<<< HEAD
            launch(args);
=======
            Application.launch(FxBootstrap.class, args);
>>>>>>> gestion-dons-java

        } catch (Throwable t) {

            System.err.println("[MainFX] Erreur dans launch() : " + t.getClass().getName() + " — " + t.getMessage());

            t.printStackTrace(System.err);

        }

    }

<<<<<<< HEAD
=======
    public static class FxBootstrap extends Application {
        @Override
        public void start(Stage stage) {

            try {

                URL fxmlUrl = MainFX.class.getResource("/landing.fxml");

                if (fxmlUrl == null) {

                    System.err.println("[MainFX] FXML introuvable sur le classpath : /landing.fxml");

                    return;

                }

                FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(fxmlUrl));

                Parent root = loader.load();

                stage.setTitle("MediLink Care");

                stage.setScene(new Scene(root, 1366, 860));
                stage.setMinWidth(1200);
                stage.setMinHeight(760);

                stage.show();

            } catch (Throwable t) {

                System.err.println("[MainFX] Erreur au démarrage : " + t.getClass().getName() + " — " + t.getMessage());

                t.printStackTrace(System.err);

            }

        }
    }

>>>>>>> gestion-dons-java
}

