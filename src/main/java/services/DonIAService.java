package services;

import models.AnalyseDonIA;
import models.Don;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

/**
 * Orchestre l’analyse LLM d’un don puis l’insertion JDBC via {@link DonService}.
 * Fournisseur : {@code don.ai.provider} = {@code gemini} (défaut) ou {@code claude}.
 */
public final class DonIAService {

    private final DonService donService;
    private final DonAnalyseLLM llm;

    public DonIAService() throws IOException {
        Properties p = loadProperties();
        this.donService = new DonService();
        this.llm = creerLLM(p);
    }

    public DonIAService(DonService donService, DonAnalyseLLM llm) {
        this.donService = donService;
        this.llm = llm;
    }

    private static Properties loadProperties() throws IOException {
        Properties p = new Properties();
        try (InputStream in = DonIAService.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                p.load(in);
            }
        }
        return p;
    }

    private static DonAnalyseLLM creerLLM(Properties p) throws IOException {
        String provider = p.getProperty("don.ai.provider", "gemini").trim().toLowerCase(Locale.ROOT);
        if ("claude".equals(provider) || "anthropic".equals(provider)) {
            return new ClaudeAPIService();
        }
        return new GeminiAPIService();
    }

    /**
     * Complète titre / description à partir des champs existants si besoin, appelle le LLM, remplit les champs IA sur le don puis insère en base.
     */
    public void enregistrerDonAvecAnalyse(Don don) throws SQLException, IOException {
        preparerChampsAnalyse(don);
        String titre = nonVide(don.getTitre(), don.getArticleDescription());
        String categorie = nonVide(don.getCategorie(), "");
        String description = nonVide(don.getDescription(), don.getDetailsSupplementaires());

        AnalyseDonIA ia = llm.analyserDon(titre, categorie, description, don.getQuantite());
        don.setDecisionIA(ia.decision());
        don.setRaisonIA(ia.raison());
        don.setTraductionIA(ia.traduction());

        donService.add(don);
        DonAvisStockageFichier.enregistrer(don);
        if (!DonAvisStockageFichier.existeFichierAvis(don.getId())) {
            throw new IOException(
                    "L’avis IA n’a pas pu être enregistré sur disque. Dossier attendu : "
                            + DonAvisStockageFichier.cheminRacinePourMessage());
        }
    }

    /**
     * Analyse uniquement (sans écriture BDD).
     */
    public AnalyseDonIA analyserSansPersister(Don don) throws IOException {
        preparerChampsAnalyse(don);
        String titre = nonVide(don.getTitre(), don.getArticleDescription());
        String categorie = nonVide(don.getCategorie(), "");
        String description = nonVide(don.getDescription(), don.getDetailsSupplementaires());
        return llm.analyserDon(titre, categorie, description, don.getQuantite());
    }

    private static void preparerChampsAnalyse(Don don) {
        if (isBlank(don.getTitre()) && !isBlank(don.getArticleDescription())) {
            don.setTitre(don.getArticleDescription());
        }
        if (isBlank(don.getDescription()) && !isBlank(don.getDetailsSupplementaires())) {
            don.setDescription(don.getDetailsSupplementaires());
        }
    }

    private static String nonVide(String a, String b) {
        if (!isBlank(a)) {
            return a;
        }
        return b != null ? b : "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
