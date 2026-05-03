package services;

import models.Don;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import utils.MediLinkSessionUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Génère un reçu PDF attestant que le don a été accepté par l’administration (statut valide).
 * Mise en page soignée : en-tête MediLink Care, détails du don, contributeur, référence du reçu.
 */
public final class RecuAcceptationPdfService {

    private static final DateTimeFormatter DATE_HEURE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ORG = "MediLink Care";
    private static final String ORG_TAGLINE = "Plateforme de dons solidaires — reçu d’acceptation";

    private RecuAcceptationPdfService() {
    }

    public static void ecrireFichier(Don don, String categorieLibelle, Path fichierCible) throws IOException {
        ecrireFichier(don, categorieLibelle, fichierCible, RecuPdfContext.depuisSessionMediLink());
    }

    public static void ecrireFichier(Don don, String categorieLibelle, Path fichierCible, RecuPdfContext ctx)
            throws IOException {
        Objects.requireNonNull(fichierCible, "fichierCible");
        Path parent = fichierCible.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(fichierCible)) {
            ecrireFlux(don, categorieLibelle, ctx, out);
        }
    }

    public static void ecrireFlux(Don don, String categorieLibelle, OutputStream out) throws IOException {
        ecrireFlux(don, categorieLibelle, RecuPdfContext.depuisSessionMediLink(), out);
    }

    public static void ecrireFlux(Don don, String categorieLibelle, RecuPdfContext ctx, OutputStream out)
            throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float margin = 48;
            float contentLeft = margin;
            float contentW = pageW - 2 * margin;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = dessinerEnTete(doc, cs, pageH, pageW, margin);

                y -= 8;
                y = ligneFine(cs, contentLeft, y, contentW);
                y -= 18;

                String refRecu = referenceRecu(don);
                LocalDateTime gen = ctx.dateGeneration() != null ? ctx.dateGeneration() : LocalDateTime.now();

                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                y = texte(cs, contentLeft, y, safeLatin1("Certificat d’acceptation de don"));
                y -= 6;
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                y = texte(cs, contentLeft, y, safeLatin1(
                        "Le présent document atteste que la proposition de don ci-dessous a été acceptée "
                                + "par l’administration de la plateforme " + ORG + "."));
                y -= 16;

                cs.setFont(PDType1Font.HELVETICA, 10);
                y = paire(cs, contentLeft, y, line(), "Référence du reçu", refRecu);
                y = paire(cs, contentLeft, y, line(), "Date et heure d’émission", gen.format(DATE_HEURE_FR));
                y = paire(cs, contentLeft, y, line(), "Identifiant du don (n°)", String.valueOf(don.getId()));
                y -= 8;

                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                y = texte(cs, contentLeft, y, safeLatin1("Contributeur"));
                y -= 4;
                cs.setFont(PDType1Font.HELVETICA, 10);
                y = paragraphe(cs, contentLeft, y, contentW, ctx.ligneContributeur());
                if (ctx.ligneReferenceSession() != null && !ctx.ligneReferenceSession().isBlank()) {
                    y = paragraphe(cs, contentLeft, y, contentW, ctx.ligneReferenceSession());
                }
                y -= 10;

                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                y = texte(cs, contentLeft, y, safeLatin1("Décision administrative"));
                y -= 4;
                cs.setFont(PDType1Font.HELVETICA, 10);
                y = paragraphe(cs, contentLeft, y, contentW, ctx.ligneAcceptation());
                y -= 12;

                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                y = texte(cs, contentLeft, y, safeLatin1("Détail du don accepté"));
                y -= 6;
                cs.setFont(PDType1Font.HELVETICA, 10);
                y = paire(cs, contentLeft, y, line(), "Intitulé / titre", n(don.getTitre()));
                y = paire(cs, contentLeft, y, line(), "Article ou description", n(don.getArticleDescription()));
                y = paire(cs, contentLeft, y, line(), "Description complète", n(don.getDescription()));
                y = paire(cs, contentLeft, y, line(), "Catégorie", n(categorieLibelle));
                y = paire(cs, contentLeft, y, line(), "Quantité", don.getQuantite() + " " + n(don.getUnite()));
                y = paire(cs, contentLeft, y, line(), "État du bien", n(don.getEtat()));
                y = paire(cs, contentLeft, y, line(), "Niveau d’urgence", n(don.getNiveauUrgence()));
                y = blocLibelleValeurLong(cs, contentLeft, y, contentW, line(), "Détails complémentaires",
                        n(don.getDetailsSupplementaires()));
                if (don.getDateSoumission() != null) {
                    y = paire(cs, contentLeft, y, line(), "Date de soumission du don",
                            don.getDateSoumission().toLocalDate().format(DATE_FR));
                }
                if (don.getDateExpiration() != null) {
                    y = paire(cs, contentLeft, y, line(), "Date d’expiration indiquée",
                            don.getDateExpiration().toLocalDate().format(DATE_FR));
                }
                y = paire(cs, contentLeft, y, line(), "Statut enregistré", "valide (accepté)");

                if (don.getDecisionIA() != null && !don.getDecisionIA().isBlank()) {
                    y -= 6;
                    y = blocLibelleValeurLong(cs, contentLeft, y, contentW, line(), "Avis analyse IA (décision)",
                            n(don.getDecisionIA()));
                }
                if (don.getRaisonIA() != null && !don.getRaisonIA().isBlank()) {
                    y = blocLibelleValeurLong(cs, contentLeft, y, contentW, line(), "Commentaire IA",
                            n(don.getRaisonIA()));
                }

                if (y < 100) {
                    y = 100;
                }
                y = ligneFine(cs, contentLeft, y, contentW);
                y -= 14;
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                y = paragraphe(cs, contentLeft, y, contentW,
                        "Document généré par l’application " + ORG + " / gdons. Il atteste de l’acceptation "
                                + "administrative du don dans l’outil ; il ne constitue pas un reçu fiscal "
                                + "sauf disposition contraire des autorités compétentes. "
                                + "Pour toute question : contact via votre espace " + ORG + ".");
            }
            doc.save(out);
        }
    }

    /** Contexte affiché sur le reçu (contributeur, décision). */
    public record RecuPdfContext(
            String ligneContributeur,
            String ligneReferenceSession,
            String ligneAcceptation,
            LocalDateTime dateGeneration
    ) {
        public static RecuPdfContext depuisSessionMediLink() {
            String token = MediLinkSessionUtil.getOrCreateToken();
            String extrait = token.replace("-", "");
            if (extrait.length() > 10) {
                extrait = extrait.substring(0, 4) + "…" + extrait.substring(extrait.length() - 4);
            }
            return new RecuPdfContext(
                    "Proposition de don enregistrée depuis l’application " + ORG
                            + " sur cet appareil (contributeur non nominatif).",
                    "Référence anonyme de session locale : " + extrait,
                    "Acceptation effectuée par l’équipe d’administration de la plateforme " + ORG
                            + " (validation du dossier don n° indiqué ci-dessus).",
                    LocalDateTime.now()
            );
        }
    }

    private static String referenceRecu(Don don) {
        int id = don.getId() <= 0 ? 0 : don.getId();
        return "MLK-" + LocalDateTime.now().getYear() + "-" + String.format("%06d", id);
    }

    private static float line() {
        return 13f;
    }

    private static byte[] lireLogoMesilink() {
        try (InputStream in = RecuAcceptationPdfService.class.getResourceAsStream("/images/mesilink-logo.png")) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * En-tête du reçu : logo officiel centré sur fond blanc (recommandé pour le visuel MediLink CARE),
     * sinon marque vectorielle sur bandeau clair.
     */
    private static float dessinerEnTete(PDDocument doc, PDPageContentStream cs, float pageH, float pageW, float margin)
            throws IOException {
        byte[] logoBytes = lireLogoMesilink();
        if (logoBytes != null && logoBytes.length > 0) {
            return dessinerEnTeteAvecLogoOfficiel(doc, cs, pageH, pageW, margin, logoBytes);
        }
        return dessinerEnTeteSansLogo(cs, pageH, pageW, margin);
    }

    /** Logo image : centré en haut, fond blanc, filet et texte foncés (le logo contient déjà « MediLink CARE »). */
    private static float dessinerEnTeteAvecLogoOfficiel(
            PDDocument doc, PDPageContentStream cs, float pageH, float pageW, float margin, byte[] logoBytes)
            throws IOException {
        PDImageXObject img = PDImageXObject.createFromByteArray(doc, logoBytes, "mesilink-logo");
        float maxW = 240;
        float maxH = 100;
        float iw = img.getWidth();
        float ih = img.getHeight();
        float scale = Math.min(maxW / iw, maxH / ih);
        iw *= scale;
        ih *= scale;
        float ix = (pageW - iw) / 2f;
        float iy = pageH - margin - ih - 10;

        cs.drawImage(img, ix, iy, iw, ih);

        float yFilet = iy - 14;
        cs.setStrokingColor(0.12f, 0.40f, 0.68f);
        cs.setLineWidth(1.2f);
        cs.moveTo(margin, yFilet);
        cs.lineTo(pageW - margin, yFilet);
        cs.stroke();

        cs.setNonStrokingColor(0.22f, 0.28f, 0.36f);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
        cs.newLineAtOffset(margin, yFilet - 18);
        cs.showText(safeLatin1(ORG_TAGLINE));
        cs.endText();

        return yFilet - 36;
    }

    /** Fallback sans fichier image : bandeau clair + pictogramme + texte. */
    private static float dessinerEnTeteSansLogo(PDPageContentStream cs, float pageH, float pageW, float margin)
            throws IOException {
        float bandH = 72;
        float bandBottom = pageH - bandH;
        cs.setNonStrokingColor(0.94f, 0.95f, 0.97f);
        cs.addRect(0, bandBottom, pageW, bandH);
        cs.fill();

        float cx = margin + 22;
        float cy = pageH - bandH / 2f - 6;
        dessinerMarqueMediLinkFondClair(cs, cx, cy);

        cs.setNonStrokingColor(0.12f, 0.18f, 0.28f);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.newLineAtOffset(margin + 50, pageH - 32);
        cs.showText(safeLatin1(ORG));
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
        cs.newLineAtOffset(margin + 50, pageH - 50);
        cs.showText(safeLatin1(ORG_TAGLINE));
        cs.endText();

        return bandBottom - 20;
    }

    /** Pictogramme sur fond clair : disque bleu-teal et croix blanche. */
    private static void dessinerMarqueMediLinkFondClair(PDPageContentStream cs, float cx, float cy) throws IOException {
        float r = 18;
        remplirCercle(cs, cx, cy, r, 0.12f, 0.38f, 0.68f);

        cs.setStrokingColor(1, 1, 1);
        cs.setLineWidth(2.2f);
        float h = 11;
        float w = 11;
        cs.moveTo(cx, cy - h);
        cs.lineTo(cx, cy + h);
        cs.moveTo(cx - w, cy);
        cs.lineTo(cx + w, cy);
        cs.stroke();

        cs.setLineWidth(1f);
        cs.setStrokingColor(1, 1, 1);
        contourCercle(cs, cx, cy, r);
        cs.stroke();
    }

    private static void remplirCercle(PDPageContentStream cs, float cx, float cy, float r, float red, float g, float b)
            throws IOException {
        float k = 0.552284749831f * r;
        cs.setNonStrokingColor(red, g, b);
        cs.moveTo(cx + r, cy);
        cs.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
        cs.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
        cs.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
        cs.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
        cs.closePath();
        cs.fill();
    }

    private static void contourCercle(PDPageContentStream cs, float cx, float cy, float r) throws IOException {
        float k = 0.552284749831f * r;
        cs.moveTo(cx + r, cy);
        cs.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r);
        cs.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy);
        cs.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r);
        cs.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy);
        cs.closePath();
    }

    private static float ligneFine(PDPageContentStream cs, float x, float y, float w) throws IOException {
        cs.setStrokingColor(0.78f, 0.82f, 0.86f);
        cs.setLineWidth(0.6f);
        float yy = y;
        cs.moveTo(x, yy);
        cs.lineTo(x + w, yy);
        cs.stroke();
        return yy - 2;
    }

    private static float paire(PDPageContentStream cs, float x, float y, float lh, String label, String value)
            throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        y = texte(cs, x, y, safeLatin1(label + " :"));
        y -= 2;
        cs.setFont(PDType1Font.HELVETICA, 10);
        y = texte(cs, x + 10, y, safeLatin1(n(value)));
        return y - lh * 0.35f;
    }

    private static float blocLibelleValeurLong(
            PDPageContentStream cs, float x, float y, float maxW, float lh, String label, String value)
            throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        y = texte(cs, x, y, safeLatin1(label + " :"));
        y -= 2;
        cs.setFont(PDType1Font.HELVETICA, 10);
        return paragraphe(cs, x + 10, y, maxW - 10, n(value));
    }

    private static float paragraphe(PDPageContentStream cs, float x, float y, float maxW, String text)
            throws IOException {
        List<String> lignes = wrap(safeLatin1(text), 92);
        for (String ligne : lignes) {
            y = texte(cs, x, y, ligne);
            y -= 2;
        }
        return y - 8;
    }

    private static float texte(PDPageContentStream cs, float x, float y, String ligne) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(ligne);
        cs.endText();
        return y - 12;
    }

    private static List<String> wrap(String s, int maxChars) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isEmpty()) {
            out.add("");
            return out;
        }
        String[] mots = s.split("\\s+");
        StringBuilder ligne = new StringBuilder();
        for (String mot : mots) {
            if (mot.isEmpty()) {
                continue;
            }
            if (ligne.length() + mot.length() + 1 > maxChars) {
                if (ligne.length() > 0) {
                    out.add(ligne.toString());
                    ligne = new StringBuilder();
                }
                while (mot.length() > maxChars) {
                    out.add(mot.substring(0, maxChars));
                    mot = mot.substring(maxChars);
                }
            }
            if (ligne.length() > 0) {
                ligne.append(' ');
            }
            ligne.append(mot);
        }
        if (ligne.length() > 0) {
            out.add(ligne.toString());
        }
        return out;
    }

    /**
     * Filtre les caractères non représentables en Helvetica standard (PDFDocEncoding / Latin-1 courant).
     */
    private static String safeLatin1(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') {
                b.append(' ');
            } else if (c >= 32 && c <= 255) {
                b.append(c);
            } else {
                b.append('?');
            }
        }
        return b.toString();
    }

    private static String n(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
