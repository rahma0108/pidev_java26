import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;

public class TurnstileController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private Button cancelBtn;

    private boolean verified = false;
    private Runnable onSuccess;
    private Runnable onCancel;

    public void setCallbacks(Runnable onSuccess, Runnable onCancel) {
        this.onSuccess = onSuccess;
        this.onCancel  = onCancel;
    }

    public boolean isVerified() { return verified; }

    // ── Java bridge exposed to JavaScript ──
    public class JavaBridge {
        public void onTokenReceived(String token) {
            System.out.println("Token received: " + token.substring(0, 20) + "...");
            statusLabel.setText("Verifying with Cloudflare...");

            new Thread(() -> {
                boolean ok = TurnstileService.verify(token);
                Platform.runLater(() -> {
                    if (ok) {
                        verified = true;
                        statusLabel.setStyle("-fx-text-fill: #5DCAA5;");
                        statusLabel.setText("Verified! Logging in...");
                        new Thread(() -> {
                            try { Thread.sleep(700); } catch (Exception ignored) {}
                            Platform.runLater(() -> {
                                closeStage();
                                if (onSuccess != null) onSuccess.run();
                            });
                        }).start();
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #E24B4A;");
                        statusLabel.setText("Verification failed. Try again.");
                        webView.getEngine().reload();
                    }
                });
            }).start();
        }

        public void onError() {
            Platform.runLater(() -> {
                statusLabel.setStyle("-fx-text-fill: #E24B4A;");
                statusLabel.setText("Error. Please try again.");
            });
        }

        public void onExpired() {
            Platform.runLater(() -> {
                statusLabel.setStyle("-fx-text-fill: #EF9F27;");
                statusLabel.setText("Expired. Refreshing...");
                webView.getEngine().reload();
            });
        }
    }

    private final JavaBridge bridge = new JavaBridge();

    @FXML
    public void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);

        // Inject Java bridge into JavaScript when page loads
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                // Inject the Java object into JS as 'javaBridge'
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                System.out.println("JavaBridge injected successfully");
            }
            if (state == Worker.State.FAILED) {
                Platform.runLater(() ->
                        statusLabel.setText("Failed to load. Check internet connection."));
            }
        });

        // Load HTML
        try {
            URL url = getClass().getResource("/turnstile.html");
            if (url != null) {
                engine.load(url.toExternalForm());
            } else {
                statusLabel.setText("Could not load verification page.");
            }
        } catch (Exception e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        verified = false;
        closeStage();
        if (onCancel != null) onCancel.run();
    }

    private void closeStage() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}