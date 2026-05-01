package userfx;


import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.io.OutputStream;
import java.net.InetSocketAddress;

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

    public class JavaBridge {
        public void onTokenReceived(String token) {
            System.out.println("Token received!");
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

        // Set Chrome user agent so Cloudflare accepts it
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36"
        );

        // Start local HTTP server
        try {
            startLocalServer();
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            // ── FALLBACK: skip captcha if server fails ──
            statusLabel.setText("Verification unavailable. Proceeding...");
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (Exception ignored) {}
                Platform.runLater(() -> {
                    verified = true;
                    closeStage();
                    if (onSuccess != null) onSuccess.run();
                });
            }).start();
            return;
        }

        // Inject bridge when loaded
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            System.out.println("WebView state: " + state);
            if (state == Worker.State.SUCCEEDED) {
                try {
                    JSObject window = (JSObject) engine.executeScript("window");
                    window.setMember("javaBridge", bridge);
                    System.out.println("Bridge injected!");
                } catch (Exception e) {
                    System.err.println("Bridge error: " + e.getMessage());
                }
            }
            if (state == Worker.State.FAILED) {
                Platform.runLater(() -> {
                    statusLabel.setText("Cannot load. Check internet connection.");
                });
            }
        });

        engine.load("http://localhost:" + PORT + "/turnstile");
    }

    private void startLocalServer() throws Exception {
        localServer = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

        localServer.createContext("/turnstile", exchange -> {
            String html = buildHtml();
            byte[] bytes = html.getBytes("UTF-8");
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });

        localServer.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(2));
        localServer.start();
        System.out.println("Turnstile server started on port " + PORT);
    }

    private String buildHtml() {
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: #0d1b2a;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
            font-family: Arial, sans-serif;
        }
        .title {
            color: #7fc4fd;
            font-size: 14px;
            font-weight: bold;
            margin-bottom: 20px;
            letter-spacing: 0.05em;
        }
        #status {
            color: #5DCAA5;
            font-size: 12px;
            margin-top: 14px;
            min-height: 18px;
            text-align: center;
        }
        .hint {
            color: #445566;
            font-size: 11px;
            margin-top: 6px;
        }
    </style>
</head>
<body>
    <p class="title">Human Verification</p>
    <div id="cf-turnstile"></div>
    <p id="status">Please complete the checkbox</p>
    <p class="hint">Powered by Cloudflare Turnstile</p>

    <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
    <script>
        function waitForTurnstile(attempts) {
            if (typeof turnstile !== 'undefined') {
                turnstile.render('#cf-turnstile', {
                    sitekey: '0x4AAAAAADBxKaQ8USAMMwhZ',
                    theme: 'dark',
                    callback: function(token) {
                        document.getElementById('status').textContent = 'Verified! Please wait...';
                        document.getElementById('status').style.color = '#5DCAA5';
                        if (window.javaBridge) {
                            window.javaBridge.onTokenReceived(token);
                        }
                    },
                    'error-callback': function() {
                        document.getElementById('status').textContent = 'Error. Please retry.';
                        document.getElementById('status').style.color = '#E24B4A';
                        if (window.javaBridge) window.javaBridge.onError();
                    },
                    'expired-callback': function() {
                        document.getElementById('status').textContent = 'Expired. Refreshing...';
                        if (window.javaBridge) window.javaBridge.onExpired();
                    }
                });
            } else if (attempts > 0) {
                setTimeout(function() { waitForTurnstile(attempts - 1); }, 500);
            } else {
                document.getElementById('status').textContent = 'Failed to load. Check internet.';
                document.getElementById('status').style.color = '#E24B4A';
            }
        }
        window.addEventListener('load', function() {
            waitForTurnstile(20);
        });
    </script>
</body>
</html>""";
    }

    private void stopServer() {
        if (localServer != null) {
            localServer.stop(0);
            System.out.println("Server stopped.");
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