package services;

import models.AnalyseDonIA;

import java.util.Locale;

/**
 * Prompt unique et parsing JSON de la réponse (décision / raison / traduction).
 */
public final class DonIAPrompt {

    private DonIAPrompt() {
    }

    public static String construirePromptUtilisateur(
            String titre, String categorie, String description, int quantite) {

        String t = titre != null ? titre : "";
        String c = categorie != null ? categorie : "";
        String d = description != null ? description : "";

        return """
                Tu es un modérateur pour une plateforme de dons MÉDICAUX (hôpital / association santé).

                Don à analyser :
                - titre : %s
                - categorie : %s
                - description : %s
                - quantite : %d

                Tâches :
                1) Décision parmi EXACTEMENT une de ces trois valeurs : ACCEPTE, REFUSE, VERIFIER
                2) Raison : une phrase courte en français (max ~350 caractères).
                3) Traduction : si le titre ou la description est en arabe, dialecte tunisien ou « francarabe », \
                traduis le contenu pertinent en français clair ; sinon mets une chaîne vide "".

                Règles de décision :
                - ACCEPTE : utile en contexte médical (médicaments, matériel soin, consommables médicaux, fauteuil roulant, \
                bavettes, seringues, tensiomètre, insuline, etc.) ; titre / catégorie / description cohérents ; quantité raisonnable (>0, pas absurde).
                - REFUSE : aucun lien médical (ex. voiture, téléphone, vêtements loisirs, nourriture non adaptée, électroménager grand public) ; \
                incohérence forte titre vs catégorie ; quantité 0, négative ou aberrante ; description demandant un contre-don / paiement / échange.
                - VERIFIER : cas limite possiblement utile ; infos incomplètes ou ambiguës.

                Réponds UNIQUEMENT par un objet JSON valide UTF-8, sans markdown, sans texte avant ou après, de la forme exacte :
                {"decision":"ACCEPTE","raison":"...","traduction":"..."}
                Les clés doivent être decision, raison, traduction (minuscules).
                """.formatted(escapePourPrompt(t), escapePourPrompt(c), escapePourPrompt(d), quantite);
    }

    private static String escapePourPrompt(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Échappe une chaîne pour l’insérer dans une valeur JSON entre guillemets. */
    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String extraireJsonObjet(String texte) {
        int a = texte.indexOf('{');
        int b = texte.lastIndexOf('}');
        if (a >= 0 && b > a) {
            return texte.substring(a, b + 1);
        }
        return texte.trim();
    }

    public static AnalyseDonIA parserJsonAnalyse(String jsonObject) {
        String decision = extraireValeurChaine(jsonObject, "decision");
        String raison = extraireValeurChaine(jsonObject, "raison");
        String traduction = extraireValeurChaine(jsonObject, "traduction");

        if (decision != null) {
            decision = decision.trim().toUpperCase(Locale.ROOT);
            if (!decision.equals("ACCEPTE") && !decision.equals("REFUSE") && !decision.equals("VERIFIER")) {
                decision = "VERIFIER";
            }
        } else {
            decision = "VERIFIER";
        }
        if (raison == null) {
            raison = "";
        }
        if (traduction == null) {
            traduction = "";
        }
        return new AnalyseDonIA(decision, raison.trim(), traduction.trim());
    }

    public static String extraireValeurChaine(String json, String key) {
        String needle = "\"" + key + "\"";
        int k = json.indexOf(needle);
        if (k < 0) {
            return null;
        }
        int colon = json.indexOf(':', k + needle.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        return unescapeJsonString(json, i + 1);
    }

    public static String unescapeJsonString(String json, int start) {
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                switch (c) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '\\' -> out.append('\\');
                    case '"' -> out.append('"');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                    }
                    default -> out.append(c);
                }
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
