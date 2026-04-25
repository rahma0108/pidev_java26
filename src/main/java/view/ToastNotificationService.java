package view;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastNotificationService {

    public enum Type {
        SUCCES, ERREUR, INFO, WARNING
    }

    public static void show(Stage ownerStage, Type type, String message) {
        Platform.runLater(() -> {
            String icone = icone(type);
            String couleur = couleur(type);

            Label iconeLabel = new Label(icone);
            iconeLabel.setStyle("-fx-font-size: 18px;");

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(260);
            messageLabel.setStyle(
                    "-fx-font-size: 13px;"
                            + "-fx-text-fill: white;"
            );

            HBox contenu = new HBox(10, iconeLabel, messageLabel);
            contenu.setAlignment(Pos.CENTER_LEFT);
            contenu.setPadding(new Insets(14, 18, 14, 18));
            contenu.setStyle(
                    "-fx-background-color: " + couleur + ";"
                            + "-fx-background-radius: 12;"
                            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 10, 0, 0, 4);"
            );
            contenu.setMaxWidth(320);

            VBox root = new VBox(contenu);
            root.setStyle("-fx-background-color: transparent;");
            root.setPadding(new Insets(4));

            Stage toastStage = new Stage();
            toastStage.initStyle(StageStyle.TRANSPARENT);
            if (ownerStage != null) {
                toastStage.initOwner(ownerStage);
            }

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            toastStage.setScene(scene);
            toastStage.setAlwaysOnTop(true);
            toastStage.setResizable(false);

            positionner(toastStage, ownerStage);

            toastStage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), root);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            PauseTransition pause = new PauseTransition(Duration.seconds(3));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> toastStage.close());

            SequentialTransition animation = new SequentialTransition(
                    fadeIn, pause, fadeOut
            );
            animation.play();
        });
    }

    public static void succes(Stage owner, String message) {
        show(owner, Type.SUCCES, message);
    }

    public static void erreur(Stage owner, String message) {
        show(owner, Type.ERREUR, message);
    }

    public static void info(Stage owner, String message) {
        show(owner, Type.INFO, message);
    }

    public static void warning(Stage owner, String message) {
        show(owner, Type.WARNING, message);
    }

    private static void positionner(Stage toastStage, Stage ownerStage) {
        if (ownerStage == null) {
            toastStage.setX(20);
            toastStage.setY(20);
            return;
        }
        toastStage.setOnShown(e -> {
            double x = ownerStage.getX()
                    + ownerStage.getWidth()
                    - toastStage.getWidth() - 20;
            double y = ownerStage.getY() + 20;
            toastStage.setX(x);
            toastStage.setY(y);
        });
    }

    private static String icone(Type type) {
        return switch (type) {
            case SUCCES -> "✅";
            case ERREUR -> "❌";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };
    }

    private static String couleur(Type type) {
        return switch (type) {
            case SUCCES -> "#16a34a";
            case ERREUR -> "#dc2626";
            case WARNING -> "#d97706";
            case INFO -> "#2563eb";
        };
    }
}
