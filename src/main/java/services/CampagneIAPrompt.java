package services;

import models.CampagneDonnee;

/**
 * Prompt et parsing JSON pour une campagne d'aide (sang, matériel, appel à dons, etc.).
 */
public final class CampagneIAPrompt {

    private CampagneIAPrompt() {
    }

    public static String construirePromptCampagne(String messageUrgence) {
        String m = messageUrgence != null ? messageUrgence : "";
        return """
                Tu es un assistant pour MediLink Care, plateforme de solidarité santé en Tunisie / francophone.

                Message d'URGENCE laissé par une personne (peut être en français, en tunisien / derja, en arabe, ou mélange) :
                ---
                %s
                ---

                Étape 1 — Comprends d'abord le message tel quel :
                - Identifie le BESOIN RÉEL exprimé : aide financière (dinars, flous, masruf, مصروف, تبرع مالي, etc.), \
                don de sang, médicament, matériel médical, orientation / contact, autre.
                - Si le texte demande de l'ARGENT ou des FRAIS (chiffre en dinars, opération payante, cagnotte, etc.), \
                la campagne doit parler UNIQUEMENT de solidarité financière / entraide pour ces frais. \
                N'invente PAS un besoin en sang ni un appel au don de sang dans ce cas.
                - Si le texte demande explicitement du SANG ou des poches de sang, alors seulement tu peux parler du don de sang.
                - Ne remplace jamais un type d'aide par un autre (ex. : pas de campagne « don de sang » si la demande est de l'aide monétaire).

                Étape 2 — Montants et chiffres :
                - Si le message indique un MONTANT ou un OBJECTIF financier (ex. chiffre + dinars/dinar/TND, « 3000 », « 3 mille », « ثلاثة آلاف », « تلاثة الف », millimes, etc.), \
                tu DOIS le faire figurer explicitement dans la campagne : au minimum dans le corps, idéalement aussi dans le titre si ça reste lisible (max ~120 caractères pour le titre).
                - Reproduis le montant de façon claire en français (ex. « 3000 dinars », « 3000 DT ») en restant fidèle au chiffre compris dans le message ; ne l'arrondis pas, ne le change pas, ne l'omet pas.
                - Expressions comme « 3 milles / trois milles / 3 mille » signifient souvent trois mille (3000) dinars en contexte tunisien ; interprète correctement sans diviser par mille par erreur.
                - Si aucun montant n'est indiqué dans le message, n'invente pas de chiffre.

                Étape 3 — Rédige une courte campagne publique pour l'accueil du site :
                - titre : une ligne percutante (max ~120 caractères), en français, alignée sur le besoin réel du message (y compris le montant si pertinent et court).
                - corps : 2 à 5 phrases en français, ton humain et respectueux ; \
                rappelle le type d'aide demandé (financier, sang, matériel, etc.) de façon cohérente avec le message ; \
                pas de promesse médicale illégale ; pas de données personnelles inventées ; \
                pour une aide financière, évoque la solidarité (contribution, canaux de confiance) en intégrant le montant lorsqu'il est présent dans le message.

                Réponds UNIQUEMENT par un objet JSON UTF-8 valide, sans markdown, sans texte avant ou après :
                {"titre":"...","corps":"..."}
                Les clés doivent être exactement titre et corps (minuscules).
                """.formatted(DonIAPrompt.escapePourPrompt(m));
    }

    public static CampagneDonnee parserCampagne(String jsonObject) {
        String titre = DonIAPrompt.extraireValeurChaine(jsonObject, "titre");
        String corps = DonIAPrompt.extraireValeurChaine(jsonObject, "corps");
        if (titre == null || titre.isBlank()) {
            titre = "Campagne d'entraide";
        }
        if (corps == null || corps.isBlank()) {
            corps = "Merci de votre solidarité.";
        }
        return new CampagneDonnee(titre.trim(), corps.trim());
    }
}
