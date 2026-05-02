package services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class StripePaymentService {
    private static final String STRIPE_BASE = "https://api.stripe.com/v1";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private final String secretKey;
    private final String currency;
    private final String successUrl;
    private final String cancelUrl;

    public StripePaymentService() {
        Properties p = loadProps();
        secretKey = getOrDefault(p, "stripe.secret.key", "");
        currency = getOrDefault(p, "stripe.currency", "eur");
        successUrl = getOrDefault(p, "stripe.checkout.success.url", "https://example.com/success");
        cancelUrl = getOrDefault(p, "stripe.checkout.cancel.url", "https://example.com/cancel");
        if (secretKey.isBlank()) {
            throw new StripePaymentException("stripe.secret.key est vide dans application.properties.");
        }
    }

    /** URL de retour après paiement (pour détecter la fin du flux dans une WebView intégrée). */
    public String getCheckoutSuccessUrl() {
        return successUrl;
    }

    /** URL de retour si l’utilisateur annule le paiement. */
    public String getCheckoutCancelUrl() {
        return cancelUrl;
    }

    public record CheckoutSessionResult(String sessionId, String checkoutUrl) {}
    public record CheckoutSessionState(String status, String paymentStatus) {}

    public CheckoutSessionResult createCheckoutSession(int amountMinorUnits, String label) {
        String body = form(
                "mode", "payment",
                "success_url", successUrl,
                "cancel_url", cancelUrl,
                "line_items[0][quantity]", "1",
                "line_items[0][price_data][currency]", currency,
                "line_items[0][price_data][unit_amount]", String.valueOf(amountMinorUnits),
                "line_items[0][price_data][product_data][name]", label == null || label.isBlank() ? "Don monetaire Medilink" : label
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(STRIPE_BASE + "/checkout/sessions"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        JsonObject json = sendJson(request);
        String sessionId = asString(json, "id");
        String checkoutUrl = asString(json, "url");
        if (sessionId == null || sessionId.isBlank() || checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new StripePaymentException("Session Checkout invalide dans la reponse Stripe.");
        }
        return new CheckoutSessionResult(sessionId, checkoutUrl);
    }

    public CheckoutSessionState getCheckoutSessionState(String sessionId) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(STRIPE_BASE + "/checkout/sessions/" + sessionId))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .GET()
                .build();
        JsonObject json = sendJson(request);
        String status = asString(json, "status");
        String paymentStatus = asString(json, "payment_status");
        if (status == null || status.isBlank() || paymentStatus == null || paymentStatus.isBlank()) {
            throw new StripePaymentException("status/payment_status Stripe manquant.");
        }
        return new CheckoutSessionState(status, paymentStatus);
    }

    private JsonObject sendJson(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new StripePaymentException("Erreur Stripe HTTP " + response.statusCode() + ": " + response.body());
            }
            return gson.fromJson(response.body(), JsonObject.class);
        } catch (IOException e) {
            throw new StripePaymentException("Erreur reseau Stripe: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StripePaymentException("Appel Stripe interrompu.", e);
        }
    }

    private static String form(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(URLEncoder.encode(kv[i], StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(kv[i + 1], StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String asString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }

    private static Properties loadProps() {
        Properties p = new Properties();
        try (InputStream in = StripePaymentService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException ignored) {
        }
        return p;
    }

    private static String getOrDefault(Properties p, String key, String fallback) {
        String value = p.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

