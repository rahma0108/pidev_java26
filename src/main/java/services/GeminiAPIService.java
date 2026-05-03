package services;

import models.AnalyseDonIA;
import models.CampagneDonnee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Appelle l’API Google Generative Language (Gemini) via {@link HttpURLConnection}.
 * Authentification par en-tête {@code x-goog-api-key} (recommandé par Google, évite les 400 liés à l’encodage de la clé dans l’URL).
 */
public final class GeminiAPIService implements DonAnalyseLLM {

    /** Uniquement {@code v1beta} : l’API {@code v1} refuse souvent les mêmes ids (404) pour generateContent. */
    private static final String[] API_VERSIONS = {"v1beta"};

    /**
     * Si ListModels est vide : ids versionnés — pas {@code gemini-1.5-flash} seul (404 fréquent).
     */
    private static final String[] PRIORITE_SECOURS = {
            "gemini-2.0-flash-001",
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash-8b"
    };

    private static final Pattern JSON_NAME_MODEL = Pattern.compile(
            "\"name\"\\s*:\\s*\"models/([^\"]+)\"");

    private final String apiKey;
    private final String preferredModel;
    /** D’où vient la clé réellement utilisée (les variables d’environnement priment sur le fichier). */
    private final String cleSourceDescription;

    public GeminiAPIService() throws IOException {
        this(loadProperties());
    }

    private GeminiAPIService(Properties p) {
        String envGemini = normalizeKey(System.getenv("GEMINI_API_KEY"));
        String envGoogle = normalizeKey(System.getenv("GOOGLE_API_KEY"));
        String fromFile = normalizeKey(p.getProperty("gemini.api.key", ""));
        if (!envGemini.isBlank()) {
            this.apiKey = envGemini;
            this.cleSourceDescription = "variable d’environnement GEMINI_API_KEY (prioritaire — ignore gemini.api.key)";
        } else if (!envGoogle.isBlank()) {
            this.apiKey = envGoogle;
            this.cleSourceDescription = "variable d’environnement GOOGLE_API_KEY (prioritaire — ignore gemini.api.key)";
        } else {
            this.apiKey = fromFile;
            this.cleSourceDescription = "application.properties (gemini.api.key)";
        }
        String rawModel = p.getProperty("gemini.model", "gemini-2.0-flash-001").trim();
        this.preferredModel = sanitizeModelId(rawModel);
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Clé API Gemini absente : créez une clé sur https://aistudio.google.com/app/apikey "
                            + "puis définissez GEMINI_API_KEY ou gemini.api.key dans application.properties.");
        }
        if (!formatCleGoogleSembleValide(apiKey)) {
            throw new IllegalStateException(
                    "La clé dans gemini.api.key ne ressemble pas à une clé Google (attendu : commence par AIza, "
                            + "lettres/chiffres/_/-, longueur typique ~39 caractères). "
                            + "Recopie-la depuis https://aistudio.google.com/app/apikey sans modifier les caractères "
                            + "(attention au tiret bas _ et à ne pas confondre O / 0).");
        }
    }

    public GeminiAPIService(String apiKey, String model) {
        this.apiKey = normalizeKey(apiKey);
        this.preferredModel = sanitizeModelId(model != null && !model.isBlank() ? model : "gemini-2.0-flash-001");
        this.cleSourceDescription = "constructeur explicite";
    }

    private static Properties loadProperties() throws IOException {
        Properties p = new Properties();
        try (var in = GeminiAPIService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        return p;
    }

    /** Retire espaces, BOM, guillemets autour de la clé (copier-coller depuis l’IDE). */
    static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\uFEFF", "").trim();
        if (s.length() >= 2) {
            char a = s.charAt(0);
            char z = s.charAt(s.length() - 1);
            if ((a == '"' && z == '"') || (a == '\'' && z == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
            }
        }
        // Espaces / tabulations au milieu (Word, PDF) — une vraie clé Google n’en contient pas
        s = s.replaceAll("[\\s\u00A0]+", "");
        return s;
    }

    /** Vérifie le format typique d’une clé API Google (évite typos évidentes avant l’appel réseau). */
    static boolean formatCleGoogleSembleValide(String k) {
        if (k == null || k.length() < 35 || k.length() > 48) {
            return false;
        }
        if (!k.startsWith("AIza")) {
            return false;
        }
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    /** Évite les chemins ou préfixes erronés ; retire -latest (non supporté tel quel sur l’endpoint REST). */
    static String sanitizeModelId(String model) {
        String m = model.replace("\uFEFF", "").trim();
        int hash = m.indexOf('#');
        if (hash >= 0) {
            m = m.substring(0, hash).trim();
        }
        if (m.startsWith("models/")) {
            m = m.substring("models/".length());
        }
        int colon = m.indexOf(':');
        if (colon > 0) {
            m = m.substring(0, colon);
        }
        if (m.endsWith("-latest")) {
            m = m.substring(0, m.length() - 7);
        }
        return m;
    }

    @Override
    public AnalyseDonIA analyserDon(
            String titre,
            String categorie,
            String description,
            int quantite) throws IOException {

        String prompt = DonIAPrompt.construirePromptUtilisateur(titre, categorie, description, quantite);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\""
                + DonIAPrompt.escapeJson(prompt)
                + "\"}]}]}";

        IOException dernier = null;
        for (String model : modelsToTryOrdre()) {
            for (String ver : API_VERSIONS) {
                String url = "https://generativelanguage.googleapis.com/" + ver + "/models/"
                        + model + ":generateContent";
                try {
                    return postGenerateContent(url, body, model);
                } catch (IllegalStateException ex) {
                    throw ex;
                } catch (IOException ex) {
                    dernier = ex;
                    String msg = ex.getMessage() != null ? ex.getMessage() : "";
                    if (msg.contains("HTTP 400")
                            || msg.contains("HTTP 403")
                            || msg.contains("HTTP 404")
                            || msg.contains("HTTP 429")) {
                        continue;
                    }
                    throw ex;
                }
            }
        }
        if (dernier != null) {
            throw enrichirExceptionFinale(dernier);
        }
        throw new IOException("Gemini : aucun modèle n’a répondu.");
    }

    public CampagneDonnee genererCampagneUrgence(String messageUrgence) throws IOException {
        String prompt = CampagneIAPrompt.construirePromptCampagne(messageUrgence);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\""
                + DonIAPrompt.escapeJson(prompt)
                + "\"}]}]}";
        IOException dernier = null;
        for (String model : modelsToTryOrdre()) {
            for (String ver : API_VERSIONS) {
                String url = "https://generativelanguage.googleapis.com/" + ver + "/models/"
                        + model + ":generateContent";
                try {
                    String texte = postGenerateContentRaw(url, body, model);
                    return CampagneIAPrompt.parserCampagne(DonIAPrompt.extraireJsonObjet(texte));
                } catch (IllegalStateException ex) {
                    throw ex;
                } catch (IOException ex) {
                    dernier = ex;
                    String msg = ex.getMessage() != null ? ex.getMessage() : "";
                    if (msg.contains("HTTP 400")
                            || msg.contains("HTTP 403")
                            || msg.contains("HTTP 404")
                            || msg.contains("HTTP 429")) {
                        continue;
                    }
                    throw ex;
                }
            }
        }
        if (dernier != null) {
            throw enrichirExceptionFinale(dernier);
        }
        throw new IOException("Gemini : aucun modèle n’a répondu.");
    }

    /**
     * Interroge {@code GET v1beta/models} pour ne proposer que des ids réellement disponibles (évite HTTP 404
     * quand Google retire ou renomme un modèle comme {@code gemini-1.5-flash}).
     */
    private String[] modelsToTryOrdre() {
        List<String> dispo = null;
        try {
            dispo = listerModelesAvecGenerateContent();
        } catch (IOException ex) {
            System.err.println("[Gemini] ListModels indisponible, secours statique : " + ex.getMessage());
        }
        LinkedHashSet<String> candidats = new LinkedHashSet<>();
        candidats.add(preferredModel);
        for (String p : PRIORITE_SECOURS) {
            candidats.add(p);
        }
        if (dispo != null && !dispo.isEmpty()) {
            LinkedHashSet<String> ordre = new LinkedHashSet<>();
            for (String variante : variantesNomModele(preferredModel)) {
                String resolu = resoudreIdDepuisListe(variante, dispo);
                if (resolu != null) {
                    ordre.add(resolu);
                    break;
                }
            }
            List<String> flash = new ArrayList<>();
            for (String id : dispo) {
                if (id.toLowerCase().contains("flash") && !id.toLowerCase().contains("embedding")) {
                    flash.add(id);
                }
            }
            flash.sort(Comparator.comparingInt(GeminiAPIService::prioriteIdListe).thenComparing(Comparator.naturalOrder()));
            ordre.addAll(flash);
            for (String c : candidats) {
                if (c.equals(preferredModel)) {
                    continue;
                }
                for (String variante : variantesNomModele(c)) {
                    String resolu = resoudreIdDepuisListe(variante, dispo);
                    if (resolu != null) {
                        ordre.add(resolu);
                        break;
                    }
                }
            }
            for (String id : dispo) {
                ordre.add(id);
            }
            return ordre.toArray(new String[0]);
        }
        return candidats.toArray(new String[0]);
    }

    /** Ex. {@code gemini-2.0-flash-001} → tente aussi {@code gemini-2.0-flash} pour le préfixe dans ListModels. */
    static List<String> variantesNomModele(String model) {
        LinkedHashSet<String> v = new LinkedHashSet<>();
        if (model == null || model.isBlank()) {
            return List.of();
        }
        String m = model.trim();
        while (m != null && !m.isEmpty()) {
            v.add(m);
            int d = m.lastIndexOf('-');
            if (d <= 0) {
                break;
            }
            String tail = m.substring(d + 1);
            if (tail.matches("\\d+")) {
                m = m.substring(0, d);
            } else {
                break;
            }
        }
        return new ArrayList<>(v);
    }

    /**
     * Google liste souvent des ids versionnés ({@code gemini-1.5-flash-8b}) alors que la config utilise le nom court.
     */
    static String resoudreIdDepuisListe(String baseSouhaite, List<String> dispo) {
        if (baseSouhaite == null || baseSouhaite.isBlank() || dispo == null) {
            return null;
        }
        String base = baseSouhaite.trim();
        if (dispo.contains(base)) {
            return base;
        }
        String prefix = base + "-";
        List<String> matches = new ArrayList<>();
        for (String id : dispo) {
            if (id.startsWith(prefix)) {
                matches.add(id);
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        matches.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        return matches.get(0);
    }

    /** Préfère 2.0 stable pour l’auto-sélection quand la config demande un nom générique. */
    private static int prioriteIdListe(String id) {
        if (id == null) {
            return 99;
        }
        String s = id.toLowerCase();
        if (s.contains("embedding") || s.contains("aqa")) {
            return 50;
        }
        if (s.contains("2.0-flash") && !s.contains("exp")) {
            return 0;
        }
        if (s.contains("2.5-flash")) {
            return 1;
        }
        if (s.contains("1.5-flash")) {
            return 2;
        }
        return 10;
    }

    /** Liste les modèles via {@code v1beta} uniquement (cohérent avec {@link #API_VERSIONS}). */
    private List<String> listerModelesAvecGenerateContent() throws IOException {
        LinkedHashSet<String> merge = new LinkedHashSet<>();
        listerModelesDepuisApiVersion("v1beta", merge);
        return new ArrayList<>(merge);
    }

    private void listerModelesDepuisApiVersion(String apiVersion, LinkedHashSet<String> mergeInto) throws IOException {
        List<String> tous = new ArrayList<>(mergeInto);
        String pageToken = null;
        for (int page = 0; page < 8; page++) {
            StringBuilder url = new StringBuilder(
                    "https://generativelanguage.googleapis.com/" + apiVersion + "/models?pageSize=100");
            if (pageToken != null) {
                url.append("&pageToken=").append(URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
            }
            HttpURLConnection conn = (HttpURLConnection) URI.create(url.toString()).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-goog-api-key", apiKey);
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(60_000);
            int code = conn.getResponseCode();
            String body = lireCorps(conn, code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code < 200 || code >= 300) {
                if (code == 403) {
                    verifierCleRevokeeOuLever(body, "ListModels " + apiVersion);
                }
                throw new IOException("Gemini ListModels " + apiVersion + " HTTP " + code + " : " + body);
            }
            extraireModelesGenerateContentDepuisJson(body, tous);
            pageToken = extraireNextPageToken(body);
            if (pageToken == null || pageToken.isBlank()) {
                break;
            }
        }
        mergeInto.addAll(tous);
    }

    /** Fenêtre assez large : dans la réponse ListModels, {@code supportedGenerationMethods} peut être loin du {@code name}. */
    private static final int FENETRE_METHODES_APRES_NAME = 14_000;

    private static void extraireModelesGenerateContentDepuisJson(String json, List<String> out) {
        Matcher m = JSON_NAME_MODEL.matcher(json);
        while (m.find()) {
            int start = m.start();
            int finBloc = Math.min(json.length(), start + FENETRE_METHODES_APRES_NAME);
            String bloc = json.substring(start, finBloc);
            if (!contientGenerateContentOuMethodes(bloc)) {
                continue;
            }
            String id = m.group(1);
            if (!out.contains(id)) {
                out.add(id);
            }
        }
        if (out.isEmpty()) {
            secoursExtraireIdsGeminiFlash(json, out);
        }
    }

    /** Si la structure JSON change et que le filtre {@code generateContent} ne matche plus, dernier recours. */
    private static void secoursExtraireIdsGeminiFlash(String json, List<String> out) {
        Matcher m = JSON_NAME_MODEL.matcher(json);
        while (m.find()) {
            String id = m.group(1);
            if (!id.startsWith("gemini-") || id.toLowerCase().contains("embedding")) {
                continue;
            }
            if (id.contains("flash") || id.contains("Flash")) {
                if (!out.contains(id)) {
                    out.add(id);
                }
            }
        }
    }

    private static boolean contientGenerateContentOuMethodes(String bloc) {
        return bloc != null && bloc.toLowerCase().contains("generatecontent");
    }

    private static String extraireNextPageToken(String json) {
        if (json == null) {
            return null;
        }
        int i = json.indexOf("\"nextPageToken\"");
        if (i < 0) {
            return null;
        }
        int colon = json.indexOf(':', i);
        int q1 = json.indexOf('"', colon + 1);
        int q2 = q1 >= 0 ? json.indexOf('"', q1 + 1) : -1;
        if (q1 < 0 || q2 <= q1) {
            return null;
        }
        return json.substring(q1 + 1, q2);
    }

    private AnalyseDonIA postGenerateContent(String url, String body, String modelUsed) throws IOException {
        String texte = postGenerateContentRaw(url, body, modelUsed);
        return DonIAPrompt.parserJsonAnalyse(DonIAPrompt.extraireJsonObjet(texte));
    }

    private String postGenerateContentRaw(String url, String body, String modelUsed) throws IOException {
        final int maxTentatives = 4;
        for (int tentative = 0; tentative < maxTentatives; tentative++) {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(60_000);
            conn.setReadTimeout(120_000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("x-goog-api-key", apiKey);

            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(payload.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int code = conn.getResponseCode();
            String response = lireCorps(conn, code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code == 429 || estIndisponibiliteTemporaire(code, response)) {
                if (tentative < maxTentatives - 1) {
                    long attente = attenteRetryMs(response, tentative);
                    attendreSilencieusement(attente);
                    continue;
                }
            }
            if (code < 200 || code >= 300) {
                if (code == 403) {
                    verifierCleRevokeeOuLever(response, "generateContent");
                }
                if (reponseIndiqueCleInvalide(response)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Clé API Gemini refusée par Google (« API key not valid »).\n\n");
                    sb.append("Source de la clé utilisée par l’application : ").append(cleSourceDescription).append(".\n");
                    sb.append("Si tu viens de coller une nouvelle clé dans application.properties mais que la source ci-dessus ");
                    sb.append("est une variable d’environnement, c’est l’ancienne clé qui est encore envoyée : ");
                    sb.append("supprime ou mets à jour GEMINI_API_KEY / GOOGLE_API_KEY (Windows : Paramètres → Système → ");
                    sb.append("Informations système → Paramètres avancés → Variables d’environnement), puis redémarre l’IDE.\n\n");
                    sb.append("Sinon : crée une clé sur https://aistudio.google.com/app/apikey , colle-la en entier ");
                    sb.append("(souvent ~39 caractères, commence par AIza) dans gemini.api.key, une ligne, sans guillemets. ");
                    sb.append("Vérifie chaque caractère : une faute (tiret bas _ manquant, O au lieu de 0) invalide la clé. ");
                    sb.append("Dans Google Cloud Console → Identifiants → ta clé : API « Generative Language API » autorisée ; ");
                    sb.append("restrictions d’appli : pas « Sites web » seul — « Aucune » (test) ou « Adresses IP ».");
                    if (apiKey.length() > 0 && apiKey.length() < 36) {
                        sb.append("\n\nIndice : la clé chargée est très courte — vérifie qu’elle n’est pas tronquée au copier-coller.");
                    }
                    throw new IllegalStateException(sb.toString());
                }
                if (code == 429) {
                    throw new IOException(
                            "Gemini : quota ou limite de débit dépassée (HTTP 429) pour le modèle « " + modelUsed + " ».\n"
                                    + "• Réessaie dans ~1 min, ou mets gemini.model=gemini-1.5-flash (souvent encore dispo en gratuit ; gemini-2.0-flash a souvent limit: 0 en gratuit).\n"
                                    + "• Si « limit: 0 » persiste : facturation Google Cloud sur le projet, ou autre projet / autre clé AI Studio : "
                                    + "https://ai.google.dev/gemini-api/docs/rate-limits\n\n"
                                    + "Détail API : " + response);
                }
                if (estIndisponibiliteTemporaire(code, response)) {
                    throw new IOException(
                            "Gemini temporairement indisponible (HTTP " + code + ") pour le modèle « " + modelUsed + " ».\n"
                                    + "Le service est en surcharge côté Google. Réessaie dans quelques secondes, "
                                    + "ou passe sur gemini-1.5-flash.\n\n"
                                    + "Détail API : " + response);
                }
                if (code == 403) {
                    throw new IOException(messageHttp403(modelUsed, url, response));
                }
                throw new IOException("Gemini API HTTP " + code + " (modèle=" + modelUsed + ", url=" + url + ") : " + response);
            }

            String texte = extraireTexteGemini(response);
            if (texte == null || texte.isBlank()) {
                throw new IOException("Réponse Gemini sans texte exploitable : " + response);
            }
            return texte;
        }
        throw new IOException("Gemini : échec inattendu après tentatives.");
    }

    private static boolean estIndisponibiliteTemporaire(int code, String response) {
        if (code == 503 || code == 502 || code == 504) {
            return true;
        }
        if (response == null) {
            return false;
        }
        String r = response.toLowerCase();
        return r.contains("\"status\":\"unavailable\"")
                || r.contains("experiencing high demand")
                || r.contains("please try again later");
    }

    private static long attenteRetryMs(String response, int tentative) {
        long fromApi = parseRetryDelayMs(response);
        long bornedApi = Math.max(1_000L, Math.min(fromApi, 12_000L));
        long backoff = switch (tentative) {
            case 0 -> 1_200L;
            case 1 -> 2_500L;
            default -> 4_500L;
        };
        return Math.max(bornedApi, backoff);
    }

    private static void attendreSilencieusement(long attenteMs) throws IOException {
        try {
            Thread.sleep(attenteMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrompu pendant l’attente d’une nouvelle tentative Gemini.", e);
        }
    }

    /** Extrait le délai suggéré par Google (« Please retry in 17.2s » ou retryDelay « 17s »). */
    private static long parseRetryDelayMs(String response) {
        if (response == null) {
            return 0;
        }
        String marker = "Please retry in ";
        int i = response.indexOf(marker);
        if (i >= 0) {
            int start = i + marker.length();
            int end = start;
            while (end < response.length() && (Character.isDigit(response.charAt(end)) || response.charAt(end) == '.')) {
                end++;
            }
            if (end > start) {
                try {
                    return (long) (Double.parseDouble(response.substring(start, end)) * 1000);
                } catch (NumberFormatException ignored) {
                    return 20_000L;
                }
            }
        }
        int rd = response.indexOf("\"retryDelay\"");
        if (rd >= 0) {
            int q1 = response.indexOf('"', response.indexOf(':', rd) + 1);
            if (q1 >= 0) {
                int q2 = response.indexOf('"', q1 + 1);
                if (q2 > q1) {
                    String val = response.substring(q1 + 1, q2);
                    if (val.endsWith("s") && val.length() > 1) {
                        try {
                            return (long) (Double.parseDouble(val.substring(0, val.length() - 1)) * 1000);
                        } catch (NumberFormatException ignored) {
                            return 20_000L;
                        }
                    }
                }
            }
        }
        return 20_000L;
    }

    private IOException enrichirExceptionFinale(IOException dernier) {
        String msg = dernier.getMessage() != null ? dernier.getMessage() : "";
        if (msg.contains("HTTP 403")) {
            int detail = msg.indexOf(" : ");
            String corps = detail > 0 && detail + 3 < msg.length() ? msg.substring(detail + 3) : msg;
            try {
                verifierCleRevokeeOuLever(corps, "Gemini");
            } catch (IllegalStateException ex) {
                return new IOException(ex.getMessage(), ex);
            }
            return new IOException(messageHttp403("(tous modèles essayés)", "", corps), dernier);
        }
        if (msg.contains("HTTP 404")) {
            return new IOException(msg + message404HintFinale(), dernier);
        }
        return dernier;
    }

    private static String message404HintFinale() {
        return "\n\n---\nConseil : les ids changent souvent (ex. gemini-1.5-flash-8b au lieu de gemini-1.5-flash). "
                + "Dans application.properties, essaye par exemple :\n"
                + "  gemini.model=gemini-2.5-flash\n"
                + "ou  gemini.model=gemini-2.0-flash-001\n"
                + "ou  gemini.model=gemini-1.5-flash-8b\n"
                + "puis redémarre l’application (l’app résout aussi les variantes listées par Google).";
    }

    /**
     * HTTP 403 : souvent Generative Language API désactivée, restrictions de clé (référents HTTP / appli Android
     * alors que l’app est un client Java bureau), ou projet sans accès au modèle.
     */
    private String messageHttp403(String modelUsed, String url, String response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Gemini API HTTP 403 — accès refusé");
        if (modelUsed != null && !modelUsed.isBlank()) {
            sb.append(" (modèle=").append(modelUsed).append(")");
        }
        sb.append(".\n\n");
        sb.append("Vérifications (Google Cloud / AI Studio) :\n");
        sb.append("• Active l’API « Generative Language API » sur le projet lié à la clé : ");
        sb.append("https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com\n");
        sb.append("• Clé API : restrictions « Applications » → pour un test depuis ton PC, mets « Aucune » ");
        sb.append("ou « Adresses IP » — pas seulement « Sites web (référents HTTP) » (incompatible avec JavaFX).\n");
        sb.append("• Source de la clé utilisée : ").append(cleSourceDescription).append(".\n");
        sb.append("  Si une variable GEMINI_API_KEY ou GOOGLE_API_KEY est définie dans l’OS, elle remplace application.properties.\n");
        sb.append("• Nouvelle clé : https://aistudio.google.com/app/apikey — puis redémarrer l’application.\n\n");
        if (url != null && !url.isBlank()) {
            sb.append("URL : ").append(url).append("\n");
        }
        sb.append("Réponse Google : ").append(response != null ? response : "(vide)");
        return sb.toString();
    }

    /**
     * Google désactive les clés publiées (GitHub, capture d’écran, chat). Il faut en créer une nouvelle.
     */
    private static void verifierCleRevokeeOuLever(String response, String contexte) {
        if (response == null) {
            return;
        }
        String r = response.toLowerCase(Locale.ROOT);
        if (r.contains("leaked") || r.contains("reported as leaked") || r.contains("has been restricted")) {
            throw new IllegalStateException(messageCleRevokee(contexte, response));
        }
    }

    private static String messageCleRevokee(String contexte, String corpsApi) {
        return "Clé API Gemini révoquée ou restreinte par Google (" + contexte + ").\n\n"
                + "Une clé qui a fuité (commit Git, capture d’écran, forum) ne fonctionne plus.\n\n"
                + "À faire :\n"
                + "1) Crée une NOUVELLE clé : https://aistudio.google.com/app/apikey\n"
                + "2) Colle-la dans src/main/resources/application.properties → gemini.api.key=…\n"
                + "   (ou variable d’environnement GEMINI_API_KEY — elle remplace le fichier.)\n"
                + "3) Ne commite JAMAIS la clé. Vérifie l’historique Git et supprime l’ancienne clé côté Google.\n\n"
                + "Réponse API : " + (corpsApi != null ? corpsApi : "");
    }

    private static boolean reponseIndiqueCleInvalide(String response) {
        if (response == null) {
            return false;
        }
        String r = response.toLowerCase();
        return r.contains("api key not valid")
                || r.contains("api_key_invalid")
                || r.contains("invalid api key");
    }

    private static String extraireTexteGemini(String json) {
        int cand = json.indexOf("\"candidates\"");
        if (cand < 0) {
            return null;
        }
        int textKey = json.indexOf("\"text\"", cand);
        if (textKey < 0) {
            return null;
        }
        int colon = json.indexOf(':', textKey);
        if (colon < 0) {
            return null;
        }
        int startQuote = json.indexOf('"', colon + 1);
        if (startQuote < 0) {
            return null;
        }
        return DonIAPrompt.unescapeJsonString(json, startQuote + 1);
    }

    private static String lireCorps(HttpURLConnection conn, java.io.InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
