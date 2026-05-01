package userfx;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.*;

import java.util.Optional;

public class PopupHelper {

    // ── Base styled dialog ──
    private static Stage createStyledStage(String title, String icon,
                                            String iconColor, String iconBg,
                                            String message, boolean hasCancel) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setTitle(title);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(36, 40, 32, 40));
        root.setStyle("-fx-background-color: #0d1b2a; -fx-background-radius: 20;");
        root.setPrefWidth(380);

        // Icon circle
        StackPane iconPane = new StackPane();
        Circle circle = new Circle(36);
        circle.setFill(Color.web(iconBg));
        circle.setStroke(Color.web(iconColor, 0.4));
        circle.setStrokeWidth(1.5);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 26px;");
        iconPane.getChildren().addAll(circle, iconLabel);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Message
        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(300);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #8899aa; -fx-text-alignment: center;");
        msgLabel.setAlignment(Pos.CENTER);

        // Separator line
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.08);");

        // Buttons
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);

        final boolean[] confirmed = {false};

        if (hasCancel) {
            Button cancelBtn = new Button("Cancel");
            cancelBtn.setPrefWidth(120);
            cancelBtn.setPrefHeight(40);
            cancelBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.06);" +
                "-fx-text-fill: #aabbcc; -fx-font-size: 13px;" +
                "-fx-background-radius: 10; -fx-cursor: hand;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 10; -fx-border-width: 1;");
            cancelBtn.setOnAction(e -> stage.close());
            buttons.getChildren().add(cancelBtn);
        }

        Button confirmBtn = new Button(hasCancel ? "Confirm" : "OK");
        confirmBtn.setPrefWidth(hasCancel ? 120 : 160);
        confirmBtn.setPrefHeight(40);
        confirmBtn.setStyle(
            "-fx-background-color: " + iconColor + ";" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand;" +
            "-fx-border-color: transparent;");
        confirmBtn.setOnAction(e -> {
            confirmed[0] = true;
            stage.close();
        });
        buttons.getChildren().add(confirmBtn);

        root.getChildren().addAll(iconPane, titleLabel, msgLabel, sep, buttons);

        // Store result
        root.setUserData(confirmed);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);

        // Center on screen
        stage.centerOnScreen();

        return stage;
    }

    // ── DELETE confirmation ──
    public static boolean confirmDelete(String itemName) {
        Stage stage = createStyledStage(
            "Delete Confirmation",
            "🗑️",
            "#E24B4A",
            "rgba(226,75,74,0.15)",
            "Are you sure you want to delete \"" + itemName + "\"?\nThis action cannot be undone.",
            true
        );

        // Change confirm button text to Delete
        VBox root = (VBox) stage.getScene().getRoot();
        HBox buttons = (HBox) root.getChildren().get(root.getChildren().size() - 1);
        Button confirmBtn = (Button) buttons.getChildren().get(buttons.getChildren().size() - 1);
        confirmBtn.setText("Delete");
        confirmBtn.setStyle(
            "-fx-background-color: #E24B4A;" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: transparent;");

        stage.showAndWait();
        boolean[] confirmed = (boolean[]) root.getUserData();
        return confirmed[0];
    }

    // ── SUCCESS popup ──
    public static void showSuccess(String message) {
        Stage stage = createStyledStage(
            "Success",
            "✓",
            "#1D9E75",
            "rgba(29,158,117,0.15)",
            message,
            false
        );
        stage.showAndWait();
    }

    // ── ERROR popup ──
    public static void showError(String message) {
        Stage stage = createStyledStage(
            "Error",
            "✕",
            "#E24B4A",
            "rgba(226,75,74,0.15)",
            message,
            false
        );
        stage.showAndWait();
    }

    // ── INFO popup ──
    public static void showInfo(String title, String message) {
        Stage stage = createStyledStage(
            title,
            "ℹ",
            "#185FA5",
            "rgba(24,95,165,0.15)",
            message,
            false
        );
        stage.showAndWait();
    }

    // ── COMING SOON popup ──
    public static void showComingSoon(String featureName) {
        Stage stage = createStyledStage(
            "Coming Soon",
            "🚀",
            "#534AB7",
            "rgba(83,74,183,0.15)",
            featureName + " is under development.\nCheck back soon!",
            false
        );

        // Change OK button text
        VBox root = (VBox) stage.getScene().getRoot();
        HBox buttons = (HBox) root.getChildren().get(root.getChildren().size() - 1);
        Button confirmBtn = (Button) buttons.getChildren().get(0);
        confirmBtn.setText("Got it");
        confirmBtn.setStyle(
            "-fx-background-color: #534AB7;" +
            "-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: transparent;");

        stage.showAndWait();
    }

    // ── LOGOUT confirmation ──
    public static boolean confirmLogout() {
        Stage stage = createStyledStage(
            "Logout",
            "🚪",
            "#185FA5",
            "rgba(24,95,165,0.15)",
            "Are you sure you want to logout\nfrom MediLink Care?",
            true
        );

        VBox root = (VBox) stage.getScene().getRoot();
        HBox buttons = (HBox) root.getChildren().get(root.getChildren().size() - 1);
        Button confirmBtn = (Button) buttons.getChildren().get(buttons.getChildren().size() - 1);
        confirmBtn.setText("Logout");

        stage.showAndWait();
        boolean[] confirmed = (boolean[]) root.getUserData();
        return confirmed[0];
    }
}
