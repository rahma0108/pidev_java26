package userfx;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import utils.MyConnection;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Test DB connection
        java.sql.Connection conn = MyConnection.getInstance().getConn();
        if (conn != null) System.out.println("Database connected!");

        // Load landing page
        Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
        Scene scene = new Scene(root);

        // Load stylesheets
        var captchaUrl = getClass().getResource("/captcha_style.css");
        if (captchaUrl != null)
            scene.getStylesheets().add(captchaUrl.toExternalForm());

        stage.setScene(scene);
        stage.setTitle("MediLink Care");

        // ── Resizable + minimum size ──
        stage.setResizable(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        // Start maximized
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
