import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class GoogleAuthService {

    private static final String CLIENT_ID;
    private static final String CLIENT_SECRET;

    static {
        String id = "";
        String secret = "";
        try {
            java.util.Properties props = new java.util.Properties();
            props.load(GoogleAuthService.class.getResourceAsStream("/config.properties"));
            id     = props.getProperty("google.client.id", "");
            secret = props.getProperty("google.client.secret", "");
        } catch (Exception e) {
            System.err.println("Could not load Google credentials: " + e.getMessage());
        }
        CLIENT_ID     = id;
        CLIENT_SECRET = secret;
    }
    private static final List<String> SCOPES  = Arrays.asList(
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile"
    );
    private static final String TOKENS_DIR = System.getProperty("user.home") + "/medilink_tokens";

    /**
     * Opens Google OAuth browser window and returns user info.
     * Returns null if cancelled or failed.
     */
    public static Userinfo signIn() {
        try {
            final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();

            // Build client secrets from hardcoded credentials
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
            GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
            details.setClientId(CLIENT_ID);
            details.setClientSecret(CLIENT_SECRET);
            clientSecrets.setInstalled(details);

            // Build flow
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, jsonFactory, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
                .setAccessType("offline")
                .build();

            // Open browser for user to sign in
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888).build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

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

    /**
     * Clear saved tokens (force re-login next time)
     */
    public static void signOut() {
        try {
            File tokenDir = new File(TOKENS_DIR);
            if (tokenDir.exists()) {
                for (File f : tokenDir.listFiles()) f.delete();
            }
        } catch (Exception ignored) {}
    }
}
