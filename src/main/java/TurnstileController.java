import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;

public class TurnstileController {

    @FXML private WebView webView;
    @FXML private Label statusLabel;
    @FXML private Button cancelBtn;

    private boolean verified = false;
    private Runnable onSuccess;
    private Runnable onCancel;
    private HttpServer localServer;
    private static final int PORT = 7654;

    public void setCallbacks(Runnable onSuccess, Runnable onCancel) {
        this.onSuccess = onSuccess;
        this.onCancel  = onCancel;
    }

    public boolean isVerified() { return verified; }

    // ── Java bridge exposed to JavaScript ──
    public class JavaBridge {
        public void onTokenReceived(String token) {
            System.out.println("Turnstile token received!");
            Platform.runLater(() ->
                    statusLabel.setText("Verifying with Cloudflare..."));

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
                                stopServer();
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
        // Allow WebView to load all content
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

// Disable SSL check for WebView (dev only)
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        // Start local HTTP server to serve the HTML
        try {
            startLocalServer();
        } catch (Exception e) {
            System.err.println("Could not start local server: " + e.getMessage());
            statusLabel.setText("Error starting verification server.");
            return;
        }

        // Inject Java bridge when page loads
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                System.out.println("JavaBridge injected!");
            }
            if (state == Worker.State.FAILED) {
                Platform.runLater(() ->
                        statusLabel.setText("Failed to load. Check internet."));
            }
        });

        // Load from local HTTP server (not file://)
        engine.load("http://localhost:" + PORT + "/turnstile");
    }

    private void startLocalServer() throws Exception {
        localServer = HttpServer.create(new InetSocketAddress(PORT), 0);

        localServer.createContext("/turnstile", exchange -> {
            // Read HTML from resources
            InputStream is = getClass().getResourceAsStream("/turnstile.html");
            byte[] bytes = is != null ? is.readAllBytes() : "<h1>Not found</h1>".getBytes();

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        localServer.setExecutor(null);
        localServer.start();
        System.out.println("Local Turnstile server started on port " + PORT);
    }

    private void stopServer() {
        if (localServer != null) {
            localServer.stop(0);
            System.out.println("Local server stopped.");
        }
    }

    @FXML
    public void handleCancel() {
        verified = false;
        stopServer();
        closeStage();
        if (onCancel != null) onCancel.run();
    }

    private void closeStage() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}