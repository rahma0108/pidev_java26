package userfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Test DB connection
        java.sql.Connection conn = MyConnection.getInstance().getConnection();
        if (conn != null) System.out.println("Database connected!");

        // Open landing page first
        Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("MediLink Care");
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
