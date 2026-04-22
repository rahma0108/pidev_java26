import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainFX {
    public static void main(String[] args) {
        Application.launch(FxBootstrap.class, args);
    }

    public static class FxBootstrap extends Application {
        @Override
        public void start(Stage stage) throws Exception {
            try {
                java.sql.Connection conn = userfx.MyConnection.getInstance().getConnection();
                if (conn != null) {
                    System.out.println("Database connected!");
                }
            } catch (Exception e) {
                System.err.println("Database init warning: " + e.getMessage());
            }

            try {
                Parent root = FXMLLoader.load(getClass().getResource("/landing.fxml"));
                Scene scene = new Scene(root, 1366, 860);
                stage.setScene(scene);
                stage.setTitle("MediLink Care");
                stage.setMinWidth(1200);
                stage.setMinHeight(760);
                stage.setResizable(true);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                Label msg = new Label("UI failed to load: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                StackPane fallback = new StackPane(msg);
                stage.setScene(new Scene(fallback, 900, 600));
                stage.setTitle("MediLink - Fallback UI");
                stage.show();
            }
        }
    }
}
