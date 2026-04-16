import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("My App");
        stage.show();
    }

    //public static void main(String[] args) {
    //    launch(args);
    //}
    public static void main(String[] args) {
        // Test DB connection before launching UI
        java.sql.Connection conn = MyConnection.getInstance().getConnection();
        if (conn != null) {
            System.out.println("✅ Database connected!");
        } else {
            System.out.println("❌ Connection failed!");
        }
        launch(args);
    }
}