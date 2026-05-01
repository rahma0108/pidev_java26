package userfx;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class GoogleAuthService {

    private static final String CLIENT_ID;
    private static final String CLIENT_SECRET;
    private static final List<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
    );

    static {
        String id = "";
        String secret = "";
        try {
            Properties props = new Properties();
            props.load(GoogleAuthService.class.getResourceAsStream("/config.properties"));
            id     = props.getProperty("google.client.id", "");
            secret = props.getProperty("google.client.secret", "");
        } catch (Exception e) {
            System.err.println("Could not load Google credentials: " + e.getMessage());
        }
        CLIENT_ID     = id;
        CLIENT_SECRET = secret;
    }

    public static Userinfo signIn() {
        try {
            // ── Always clear saved tokens so browser opens every time ──
            clearTokens();

            final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // Build client secrets
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
            details.setClientId(CLIENT_ID);
            details.setClientSecret(CLIENT_SECRET);
            clientSecrets.setInstalled(details);

            // Build flow — force account selection every time with prompt=select_account
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    transport, jsonFactory, clientSecrets, SCOPES)
                    .setAccessType("online") // online = no refresh token saved
                    .build();

            // Open browser every time — no stored credentials
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(8888).build();

            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver) {
                @Override
                protected void onAuthorization(
                        com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl authorizationUrl)
                        throws java.io.IOException {
                    // Add prompt=select_account to force Google account picker
                    authorizationUrl.set("prompt", "select_account");
                    super.onAuthorization(authorizationUrl);
                }
            }.authorize("user");

            // Get user info
            Oauth2 oauth2 = new Oauth2.Builder(transport, jsonFactory, credential)
                    .setApplicationName("MediLink")
                    .build();

            return oauth2.userinfo().get().execute();

        } catch (Exception e) {
            System.err.println("Google Sign In error: " + e.getMessage());
            return null;
        }
    }

    // Clear any saved tokens so browser always opens
    private static void clearTokens() {
        try {
            File tokenDir = new File(System.getProperty("user.home") + "/medilink_tokens");
            if (tokenDir.exists()) {
                for (File f : tokenDir.listFiles()) {
                    f.delete();
                }
            }
        } catch (Exception ignored) {}
    }
}