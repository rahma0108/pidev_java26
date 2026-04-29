package view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;

public class NavigationService {

    public static void naviguerVers(Stage stage, String fxmlPath, String titre) {
        try {
            Node contenuActuel = stage.getScene().getRoot();

            FadeTransition fadeOut = new FadeTransition(Duration.millis(180), contenuActuel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                try {
                    URL url = NavigationService.class.getResource(fxmlPath);
                    Parent nouvelleVue = FXMLLoader.load(url);

                    nouvelleVue.setOpacity(0);

                    TranslateTransition slide = new TranslateTransition(Duration.millis(220), nouvelleVue);
                    slide.setFromY(18);
                    slide.setToY(0);

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(220), nouvelleVue);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);

                    ParallelTransition entree = new ParallelTransition(fadeIn, slide);

                    Scene ancienneScene = stage.getScene();
                    Scene nouvelleScene = new Scene(nouvelleVue, stage.getWidth(), stage.getHeight());
                    if (ancienneScene != null) {
                        nouvelleScene.getStylesheets().addAll(ancienneScene.getStylesheets());
                    }

                    stage.setScene(nouvelleScene);
                    if (titre != null) {
                        stage.setTitle(titre);
                    }

                    entree.play();
                } catch (Exception ex) {
                    System.err.println("[Nav] Erreur chargement : " + ex.getMessage());
                }
            });

            fadeOut.play();
        } catch (Exception e) {
            System.err.println("[Nav] Erreur navigation : " + e.getMessage());
        }
    }

    public static void animerApparition(Node node) {
        if (node == null) {
            return;
        }

        node.setOpacity(0);

        FadeTransition fade = new FadeTransition(Duration.millis(350), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(350), node);
        slide.setFromY(14);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    public static void animerBoutonHover(Node bouton) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(120), bouton);
        scale.setToX(1.04);
        scale.setToY(1.04);
        scale.play();
    }

    public static void animerBoutonSortie(Node bouton) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(120), bouton);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    public static void animerListe(javafx.scene.layout.Pane container) {
        if (container == null) {
            return;
        }
        int[] index = {0};
        for (Node item : container.getChildren()) {
            item.setOpacity(0);
            int delai = index[0] * 55;

            FadeTransition fade = new FadeTransition(Duration.millis(220), item);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.setDelay(Duration.millis(delai));

            TranslateTransition slide = new TranslateTransition(Duration.millis(220), item);
            slide.setFromY(8);
            slide.setToY(0);
            slide.setDelay(Duration.millis(delai));

            new ParallelTransition(fade, slide).play();
            index[0]++;
        }
    }
}
