package utils;

import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Identifiant anonyme persistant sur la machine (préférences Java), pour lier les demandes d'urgence
 * à « Mes campagnes » sans compte utilisateur.
 */
public final class MediLinkSessionUtil {

    private static final String NODE = "tn/esprit/gdons";
    private static final String KEY_TOKEN = "public_session_token";

    private MediLinkSessionUtil() {
    }

    public static String getOrCreateToken() {
        Preferences p = Preferences.userRoot().node(NODE);
        String t = p.get(KEY_TOKEN, null);
        if (t == null || t.isBlank()) {
            t = UUID.randomUUID().toString();
            p.put(KEY_TOKEN, t);
        }
        return t;
    }
}
