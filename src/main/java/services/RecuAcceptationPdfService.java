package services;

import models.Don;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Génère un reçu PDF d’acceptation de don (statut valide), avec une présentation proche d’un reçu / facture FR.
 * Les dons monétaires et les dons en nature utilisent des libellés, tableaux et encadrés différents.
 */
public final class RecuAcceptationPdfService {

    private static final DateTimeFormatter DATE_HEURE_FR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ORG = "MediLink Care";
    private static final String ORG_LIGNE1 = "Plateforme de dons solidaires";

    /** Teal proche du modèle « Reçu » (#0d9488). */
    private static final float TEAL_R = 13f / 255f;
    private static final float TEAL_G = 148f / 255f;
    private static final float TEAL_B = 136f / 255f;

    private static final float GRIS_FOND_R = 0.93f;
    private static final float GRIS_FOND_G = 0.94f;
    private static final float GRIS_FOND_B = 0.96f;

    private RecuAcceptationPdfService() {
    }

    public static void ecrireFichier(Don don, String categorieLibelle, Path fichierCible) throws IOException {
        ecrireFichier(don, categorieLibelle, fichierCible, RecuPdfContext.defaut());
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
        ecrireFlux(don, categorieLibelle, RecuPdfContext.defaut(), out);
    }

    public static void ecrireFlux(Don don, String categorieLibelle, RecuPdfContext ctx, OutputStream out)
            throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            float margin = 42;
            float contentLeft = margin;
            float contentW = pageW - 2 * margin;

            boolean monetaire = estDonMonetaire(categorieLibelle, don);
            LocalDateTime gen = ctx.dateGeneration() != null ? ctx.dateGeneration() : LocalDateTime.now();
            String refRecu = referenceRecu(gen);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = dessinerEnteteRecu(doc, cs, pageH, pageW, margin, monetaire);
                y -= 8;
                y = ligneTeal(cs, contentLeft, y, contentW, 1.4f);
                y -= 14;

                y = dessinerColonnesEmetteurContributeur(cs, contentLeft, y, contentW, ctx);
                y -= 12;

                y = dessinerBlocMeta(cs, contentLeft, y, contentW, don, categorieLibelle, refRecu, gen, monetaire);
                y -= 14;

                y = dessinerSectionInfosAdditionnelles(cs, contentLeft, y, contentW, monetaire);
                y -= 12;

                if (monetaire) {
                    y = dessinerTableauEtTotauxMonetaire(cs, contentLeft, y, contentW, don, categorieLibelle);
                } else {
                    y = dessinerTableauEtTotauxNature(cs, contentLeft, y, contentW, don, categorieLibelle);
                }
                y -= 10;

                y = dessinerBlocDecisionIA(cs, contentLeft, y, contentW, don, ctx);
                y -= 8;

                y = dessinerPiedPage(cs, contentLeft, y, contentW, pageW, margin, monetaire, don);

                if (y < 72) {
                    y = 72;
                }
                y = ligneTeal(cs, contentLeft, y, contentW, 0.8f);
                y -= 10;
                cs.setNonStrokingColor(0.35f, 0.38f, 0.42f);
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 7.5f);
                String disclaimer = monetaire
                        ? "Document genere par " + ORG + ". Il atteste de l'acceptation administrative du don "
                        + "monetaire et de son enregistrement dans l'outil. Il ne vaut pas recu fiscal au sens Cerfa "
                        + "sauf cadre legal applicable ; renseignez-vous aupres de votre service des impots."
                        : "Document genere par " + ORG + ". Il atteste de l'acceptation administrative du don en "
                        + "nature dans l'outil. Il ne constitue pas un recu de deduction fiscale sauf mention "
                        + "contraire de l'organisme habilite.";
                y = paragraphe(cs, contentLeft, y, contentW, safeLatin1(disclaimer));
            }
            doc.save(out);
        }
    }

    private static boolean estDonMonetaire(String categorieLibelle, Don don) {
        String lib = categorieLibelle != null ? categorieLibelle.toLowerCase(Locale.ROOT) : "";
        if (lib.contains("argent")) {
            return true;
        }
        if (don.getCategorie() != null && don.getCategorie().toLowerCase(Locale.ROOT).contains("argent")) {
            return true;
        }
        String u = don.getUnite() != null ? don.getUnite().toLowerCase(Locale.ROOT) : "";
        return lib.contains("monétaire") || lib.contains("monetaire") || u.equals("tnd") && lib.isBlank();
    }

    /** En-tête type modèle : « Reçu » à gauche (teal), logo à droite, filet. */
    private static float dessinerEnteteRecu(
            PDDocument doc, PDPageContentStream cs, float pageH, float pageW, float margin, boolean monetaire)
            throws IOException {
        float yTop = pageH - margin;
        String sousTitre = monetaire ? "Don monetaire - acceptation" : "Don en nature - acceptation";

        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 26);
        cs.newLineAtOffset(margin, yTop - 28);
        cs.showText(safeLatin1("Reçu"));
        cs.endText();

        cs.setNonStrokingColor(0.22f, 0.28f, 0.36f);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
        cs.newLineAtOffset(margin, yTop - 46);
        cs.showText(safeLatin1(sousTitre));
        cs.endText();

        byte[] logoBytes = lireLogoMesilink();
        float bottomBand = yTop - 58;
        if (logoBytes != null && logoBytes.length > 0) {
            PDImageXObject img = PDImageXObject.createFromByteArray(doc, logoBytes, "mesilink-logo");
            float maxW = 100;
            float maxH = 52;
            float iw = img.getWidth();
            float ih = img.getHeight();
            float scale = Math.min(maxW / iw, maxH / ih);
            iw *= scale;
            ih *= scale;
            float ix = pageW - margin - iw;
            float iy = yTop - ih - 8;
            cs.drawImage(img, ix, iy, iw, ih);
            bottomBand = Math.min(bottomBand, iy - 6);
        }

        float yLine = bottomBand - 6;
        ligneTeal(cs, margin, yLine, pageW - 2 * margin, 1.8f);
        return yLine - 18;
    }

    private static float ligneTeal(PDPageContentStream cs, float x, float y, float w, float lw) throws IOException {
        cs.setStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.setLineWidth(lw);
        cs.moveTo(x, y);
        cs.lineTo(x + w, y);
        cs.stroke();
        return y;
    }

    /** Émetteur (gauche) / Contributeur (droite), style facture. */
    private static float dessinerColonnesEmetteurContributeur(
            PDPageContentStream cs, float x, float y, float w, RecuPdfContext ctx) throws IOException {
        float mid = x + w * 0.52f;
        float colW = w * 0.48f - 8;
        float y0 = y;

        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y0);
        cs.showText(safeLatin1("Emetteur"));
        cs.endText();
        float yL = y0 - 12;
        cs.setNonStrokingColor(0.15f, 0.17f, 0.2f);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        yL = texte(cs, x, yL, safeLatin1(ORG));
        cs.setFont(PDType1Font.HELVETICA, 9);
        yL = texte(cs, x, yL, safeLatin1(ORG_LIGNE1));

        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(mid, y0);
        cs.showText(safeLatin1("Contributeur"));
        cs.endText();
        float yR = y0 - 12;
        cs.setNonStrokingColor(0.15f, 0.17f, 0.2f);
        cs.setFont(PDType1Font.HELVETICA, 9);
        yR = paragraphe(cs, mid, yR, colW, ctx.ligneContributeur());
        return Math.min(yL, yR) - 8;
    }

    private static float dessinerBlocMeta(
            PDPageContentStream cs,
            float x,
            float y,
            float w,
            Don don,
            String categorieLibelle,
            String refRecu,
            LocalDateTime gen,
            boolean monetaire) throws IOException {
        float pad = 10;
        float lineH = 11;
        float boxH = pad * 2 + 8 * lineH;

        float yBottom = y - boxH;
        cs.setNonStrokingColor(GRIS_FOND_R, GRIS_FOND_G, GRIS_FOND_B);
        cs.addRect(x, yBottom, w, boxH);
        cs.fill();
        cs.setStrokingColor(0.82f, 0.85f, 0.88f);
        cs.setLineWidth(0.6f);
        cs.addRect(x, yBottom, w, boxH);
        cs.stroke();

        float yt = y - pad - 2;
        float xL = x + pad;
        float xV = x + w * 0.42f;
        cs.setFont(PDType1Font.HELVETICA, 8.5f);
        cs.setNonStrokingColor(0.25f, 0.28f, 0.32f);

        yt = metaLigne(cs, xL, xV, yt, "Date d'emission du recu", gen.format(DATE_HEURE_FR));
        yt = metaLigne(cs, xL, xV, yt, "Reference du recu", refRecu);
        yt = metaLigne(cs, xL, xV, yt, "Type de don", monetaire ? "Monetaire (TND)" : "En nature");
        yt = metaLigne(cs, xL, xV, yt, "Categorie", n(categorieLibelle));
        if (monetaire) {
            yt = metaLigne(cs, xL, xV, yt, "Montant enregistre",
                    don.getQuantite() + " " + n(don.getUnite()));
            String refPaiement = extraireRefPaiementStripe(don);
            if (refPaiement != null && !refPaiement.isBlank()) {
                yt = metaLigne(cs, xL, xV, yt, "Reference de paiement", refPaiement);
            }
        } else {
            if (don.getDateSoumission() != null) {
                yt = metaLigne(cs, xL, xV, yt, "Date de soumission",
                        don.getDateSoumission().toLocalDate().format(DATE_FR));
            }
            if (don.getDateExpiration() != null) {
                yt = metaLigne(cs, xL, xV, yt, "Date d'expiration indiquee",
                        don.getDateExpiration().toLocalDate().format(DATE_FR));
            }
        }
        yt = metaLigne(cs, xL, xV, yt, "Statut", "Valide (accepte)");
        return yBottom - 10;
    }

    private static float metaLigne(PDPageContentStream cs, float xL, float xV, float y, String label, String val)
            throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
        cs.setNonStrokingColor(0.35f, 0.4f, 0.45f);
        cs.beginText();
        cs.newLineAtOffset(xL, y);
        cs.showText(safeLatin1(label + " :"));
        cs.endText();
        cs.setFont(PDType1Font.HELVETICA, 8.5f);
        cs.setNonStrokingColor(0.12f, 0.14f, 0.18f);
        cs.beginText();
        cs.newLineAtOffset(xV, y);
        cs.showText(safeLatin1(n(val)));
        cs.endText();
        return y - 11;
    }

    /** Référence Stripe {@code cs_...} uniquement si présente dans les détails ; sinon {@code null} (rien n'est affiché). */
    private static String extraireRefPaiementStripe(Don don) {
        String d = don.getDetailsSupplementaires();
        if (d == null || d.isBlank()) {
            return null;
        }
        String low = d.toLowerCase(Locale.ROOT);
        if (!low.contains("cs_")) {
            return null;
        }
        int i = low.indexOf("cs_");
        int j = i;
        while (j < d.length() && (Character.isLetterOrDigit(d.charAt(j)) || d.charAt(j) == '_')) {
            j++;
        }
        if (j <= i) {
            return null;
        }
        return d.substring(i, Math.min(i + 48, j));
    }

    private static float dessinerSectionInfosAdditionnelles(
            PDPageContentStream cs, float x, float y, float w, boolean monetaire) throws IOException {
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(safeLatin1("Informations additionnelles"));
        cs.endText();
        y -= 12;
        cs.setNonStrokingColor(0.2f, 0.22f, 0.26f);
        cs.setFont(PDType1Font.HELVETICA, 9);
        String txt = monetaire
                ? "Merci pour votre don monetaire. Ce recu atteste de l'acceptation administrative du versement "
                + "enregistre sur la plateforme. Conservez ce document avec vos pieces comptables si votre "
                + "organisme le prevoit."
                : "Merci pour votre don en nature. Ce recu atteste de l'acceptation administrative des biens "
                + "decrits ci-dessous. Les modalites de mise a disposition ou de livraison sont gerees hors "
                + "de ce document.";
        return paragraphe(cs, x, y, w, safeLatin1(txt));
    }

    private static float dessinerTableauEtTotauxMonetaire(
            PDPageContentStream cs, float x, float y, float w, Don don, String categorieLibelle) throws IOException {
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(safeLatin1("Detail du versement (don monetaire)"));
        cs.endText();
        y -= 14;

        float[] cols = {0.52f, 0.12f, 0.14f, 0.22f};
        String[] headers = {"Description", "Qté", "Unité", "Montant"};
        String desc = n(don.getTitre());
        if ("—".equals(desc)) {
            desc = "Don monetaire - " + n(categorieLibelle);
        }
        String[][] rows = {{desc, "1", n(don.getUnite()), don.getQuantite() + " " + n(don.getUnite())}};
        y = dessinerTableau(cs, x, y, w, cols, headers, rows);

        y -= 8;
        float xTot = x + w * 0.58f;
        float wTot = w * 0.42f;
        cs.setNonStrokingColor(0.94f, 0.96f, 0.97f);
        cs.addRect(xTot, y - 36, wTot, 36);
        cs.fill();
        cs.setStrokingColor(0.8f, 0.84f, 0.88f);
        cs.addRect(xTot, y - 36, wTot, 36);
        cs.stroke();

        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.setNonStrokingColor(0.25f, 0.28f, 0.32f);
        y = texte(cs, xTot + 8, y - 4, safeLatin1("Total verse"));
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        y = texte(cs, xTot + 8, y - 4, safeLatin1(don.getQuantite() + " " + n(don.getUnite())));
        return y - 12;
    }

    private static float dessinerTableauEtTotauxNature(
            PDPageContentStream cs, float x, float y, float w, Don don, String categorieLibelle) throws IOException {
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(safeLatin1("Detail du don en nature"));
        cs.endText();
        y -= 14;

        float[] cols = {0.40f, 0.12f, 0.14f, 0.17f, 0.17f};
        String[] headers = {"Article / description", "Qté", "Unité", "État", "Urgence"};
        String[][] rows = {{
                resumeArticle(don),
                String.valueOf(don.getQuantite()),
                n(don.getUnite()),
                n(don.getEtat()),
                n(don.getNiveauUrgence())
        }};
        y = dessinerTableau(cs, x, y, w, cols, headers, rows);

        y -= 8;
        cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
        cs.setNonStrokingColor(0.35f, 0.38f, 0.42f);
        y = paragraphe(cs, x, y, w, safeLatin1(
                "Categorie : " + n(categorieLibelle) + " - Details : " + n(don.getDetailsSupplementaires())));
        y -= 6;
        cs.setFont(PDType1Font.HELVETICA, 9);
        y = paragraphe(cs, x, y, w, safeLatin1("Description complete : " + n(don.getDescription())));
        return y;
    }

    private static String resumeArticle(Don don) {
        String a = don.getArticleDescription();
        if (a != null && !a.isBlank()) {
            return a.length() > 120 ? a.substring(0, 117) + "..." : a;
        }
        return n(don.getTitre());
    }

    /**
     * Tableau avec en-tête teal ; lignes de données sur fond blanc.
     */
    private static float dessinerTableau(
            PDPageContentStream cs,
            float x,
            float y,
            float w,
            float[] colFracs,
            String[] headers,
            String[][] dataRows) throws IOException {
        int nCol = headers.length;
        float[] colW = new float[nCol];
        for (int i = 0; i < nCol; i++) {
            colW[i] = w * colFracs[i];
        }
        float rowH = 16;
        float headerH = 18;

        float tableTop = y;
        float yHeaderBottom = y - headerH;
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.addRect(x, yHeaderBottom, w, headerH);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
        float cx = x + 4;
        for (int i = 0; i < nCol; i++) {
            cs.beginText();
            cs.newLineAtOffset(cx, y - 12);
            cs.showText(safeLatin1(headers[i]));
            cs.endText();
            cx += colW[i];
        }

        float yData = yHeaderBottom;
        for (String[] row : dataRows) {
            yData -= rowH;
            cs.setNonStrokingColor(0.99f, 0.99f, 1f);
            cs.addRect(x, yData, w, rowH);
            cs.fill();
            cs.setStrokingColor(0.88f, 0.9f, 0.93f);
            cs.setLineWidth(0.4f);
            cs.addRect(x, yData, w, rowH);
            cs.stroke();

            cs.setNonStrokingColor(0.12f, 0.14f, 0.18f);
            cs.setFont(PDType1Font.HELVETICA, 8);
            cx = x + 4;
            for (int i = 0; i < nCol && i < row.length; i++) {
                String cell = row[i] == null ? "—" : row[i];
                List<String> wrapped = wrap(safeLatin1(cell), Math.max(12, (int) (colW[i] / 4.2f)));
                float yy = yData + rowH - 10;
                for (String ln : wrapped) {
                    cs.beginText();
                    cs.newLineAtOffset(cx, yy);
                    cs.showText(ln);
                    cs.endText();
                    yy -= 9;
                    if (yy < yData + 2) {
                        break;
                    }
                }
                cx += colW[i];
            }
        }

        cs.setStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.setLineWidth(0.8f);
        cs.addRect(x, yData, w, tableTop - yData);
        cs.stroke();

        return yData - 6;
    }

    private static float dessinerBlocDecisionIA(
            PDPageContentStream cs, float x, float y, float w, Don don, RecuPdfContext ctx) throws IOException {
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 9);
        cs.newLineAtOffset(x, y);
        cs.showText(safeLatin1("Decision administrative"));
        cs.endText();
        y -= 10;
        cs.setNonStrokingColor(0.2f, 0.22f, 0.26f);
        cs.setFont(PDType1Font.HELVETICA, 8.5f);
        y = paragraphe(cs, x, y, w, safeLatin1(ctx.ligneAcceptation()));
        if (don.getDecisionIA() != null && !don.getDecisionIA().isBlank()) {
            y -= 4;
            cs.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
            y = texte(cs, x, y, safeLatin1("Avis analyse IA (decision) :"));
            cs.setFont(PDType1Font.HELVETICA, 8.5f);
            y = paragraphe(cs, x + 6, y, w - 6, safeLatin1(don.getDecisionIA()));
        }
        if (don.getRaisonIA() != null && !don.getRaisonIA().isBlank()) {
            y -= 2;
            cs.setFont(PDType1Font.HELVETICA_BOLD, 8.5f);
            y = texte(cs, x, y, safeLatin1("Commentaire IA :"));
            cs.setFont(PDType1Font.HELVETICA, 8.5f);
            y = paragraphe(cs, x + 6, y, w - 6, safeLatin1(don.getRaisonIA()));
        }
        return y;
    }

    private static float dessinerPiedPage(
            PDPageContentStream cs,
            float x,
            float y,
            float w,
            float pageW,
            float margin,
            boolean monetaire,
            Don don) throws IOException {
        float barH = 5;
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.addRect(margin, y - barH, pageW - 2 * margin, barH);
        cs.fill();
        y -= barH + 10;

        float cw = (w - 16) / 3f;
        float x1 = x;
        float x2 = x + cw + 8;
        float x3 = x + 2 * (cw + 8);

        cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.newLineAtOffset(x1, y);
        cs.showText(safeLatin1("Siege social"));
        cs.endText();
        cs.setFont(PDType1Font.HELVETICA, 7.5f);
        cs.setNonStrokingColor(0.28f, 0.3f, 0.34f);
        float y1 = y - 10;
        y1 = texte(cs, x1, y1, safeLatin1(ORG));
        y1 = texte(cs, x1, y1, safeLatin1(ORG_LIGNE1));
        y1 = paragraphe(cs, x1, y1, cw, safeLatin1(
                "Les mentions legales et l'adresse complete de l'organisme sont communiquees sur les supports "
                        + "officiels habilites ; elles ne sont pas reproduites sur ce document genere."));

        cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.newLineAtOffset(x2, y);
        cs.showText(safeLatin1("Coordonnees"));
        cs.endText();
        cs.setFont(PDType1Font.HELVETICA, 7.5f);
        cs.setNonStrokingColor(0.28f, 0.3f, 0.34f);
        float y2 = y - 10;
        y2 = paragraphe(cs, x2, y2, cw, safeLatin1(
                "Pour toute question relative a ce recu, utilisez les canaux prevus dans l'application "
                        + "MediLink Care ou les coordonnees publiees par votre organisme partenaire."));

        cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
        cs.setNonStrokingColor(TEAL_R, TEAL_G, TEAL_B);
        cs.beginText();
        cs.newLineAtOffset(x3, y);
        cs.showText(safeLatin1("Paiement / tracabilite"));
        cs.endText();
        cs.setFont(PDType1Font.HELVETICA, 7.5f);
        cs.setNonStrokingColor(0.28f, 0.3f, 0.34f);
        float y3 = y - 10;
        if (monetaire) {
            String ref = extraireRefPaiementStripe(don);
            String msg = ref != null && !ref.isBlank()
                    ? "Paiement en ligne traite via prestataire ; reference technique : " + ref + "."
                    : "Paiement en ligne : aucune reference de transaction n'est indiquee sur ce recu "
                            + "lorsqu'elle n'apparait pas dans le bloc principal.";
            y3 = paragraphe(cs, x3, y3, cw, safeLatin1(msg));
        } else {
            y3 = paragraphe(cs, x3, y3, cw, safeLatin1(
                    "Don en nature : pas d'information de paiement sur ce recu administratif."));
        }

        return Math.min(y1, Math.min(y2, y3)) - 6;
    }

    /** Contexte affiché sur le reçu (contributeur, décision). */
    public record RecuPdfContext(String ligneContributeur, String ligneAcceptation, LocalDateTime dateGeneration) {
        /** Contexte sans identifiant de session ni don : textes generiques pour le reçu. */
        public static RecuPdfContext defaut() {
            return new RecuPdfContext(
                    "Don enregistre depuis l'application " + ORG + " sur cet appareil "
                            + "(sans identification nominative du donateur sur ce document).",
                    "Acceptation effectuee par l'equipe d'administration de la plateforme " + ORG
                            + " (validation du don decrit sur ce recu).",
                    LocalDateTime.now());
        }
    }

    /** Reference du recu basee sur la date/heure d'emission (sans id interne du don). */
    private static String referenceRecu(LocalDateTime gen) {
        return String.format(
                "MLK-%d-%02d%02d-%02d%02d%02d",
                gen.getYear(),
                gen.getMonthValue(),
                gen.getDayOfMonth(),
                gen.getHour(),
                gen.getMinute(),
                gen.getSecond());
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

    private static float texte(PDPageContentStream cs, float x, float y, String ligne) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(ligne);
        cs.endText();
        return y - 11;
    }

    private static float paragraphe(PDPageContentStream cs, float x, float y, float maxW, String text)
            throws IOException {
        List<String> lignes = wrap(safeLatin1(text), 95);
        for (String ligne : lignes) {
            y = texte(cs, x, y, ligne);
            y += 1;
        }
        return y - 6;
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

    private static String safeLatin1(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') {
                b.append(' ');
            } else if (c == '’' || c == '‘' || c == '‚') {
                b.append('\'');
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
