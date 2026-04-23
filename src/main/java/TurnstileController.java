import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class TurnstileController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private Button cancelBtn;

    private String capturedToken = null;
    private boolean verified = false;
    private Runnable onSuccess;
    private Runnable onCancel;

    public void setCallbacks(Runnable onSuccess, Runnable onCancel) {
        this.onSuccess = onSuccess;
        this.onCancel  = onCancel;
    }

    public boolean isVerified() { return verified; }
    public String getToken()    { return capturedToken; }

    @FXML
    public void initialize() {
        WebEngine engine = webView.getEngine();

        // Load the turnstile HTML from resources
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

        // Listen for location changes — captures token from javafx://turnstile-success?token=...
        engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc == null) return;

            if (newLoc.startsWith("javafx://turnstile-success")) {
                // Extract token from URL
                String token = newLoc.replace("javafx://turnstile-success?token=", "");
                capturedToken = token;

                // Verify with Cloudflare server
                statusLabel.setText("Verifying with Cloudflare...");
                new Thread(() -> {
                    boolean ok = TurnstileService.verify(token);
                    Platform.runLater(() -> {
                        if (ok) {
                            verified = true;
                            statusLabel.setStyle("-fx-text-fill: #5DCAA5;");
                            statusLabel.setText("✓ Verified successfully!");
                            // Close dialog after short delay
                            new Thread(() -> {
                                try { Thread.sleep(800); } catch (Exception ignored) {}
                                Platform.runLater(() -> {
                                    closeStage();
                                    if (onSuccess != null) onSuccess.run();
                                });
                            }).start();
                        } else {
                            statusLabel.setStyle("-fx-text-fill: #E24B4A;");
                            statusLabel.setText("✗ Verification failed. Try again.");
                            // Reload the widget
                            engine.reload();
                        }
                    });
                }).start();

            } else if (newLoc.startsWith("javafx://turnstile-error")) {
                statusLabel.setStyle("-fx-text-fill: #E24B4A;");
                statusLabel.setText("✗ Error. Please try again.");

            } else if (newLoc.startsWith("javafx://turnstile-expired")) {
                statusLabel.setStyle("-fx-text-fill: #EF9F27;");
                statusLabel.setText("⚠ Expired. Please verify again.");
                engine.reload();
            }
        });
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
