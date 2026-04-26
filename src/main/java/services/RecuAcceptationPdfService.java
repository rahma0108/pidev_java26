package services;

import models.Don;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Génère un reçu PDF attestant que le don a été accepté par l’administration (statut valide).
 */
public final class RecuAcceptationPdfService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RecuAcceptationPdfService() {
    }

    public static void ecrireFichier(Don don, String categorieLibelle, Path fichierCible) throws IOException {
        Objects.requireNonNull(fichierCible, "fichierCible");
        Path parent = fichierCible.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(fichierCible)) {
            ecrireFlux(don, categorieLibelle, out);
        }
    }

    public static void ecrireFlux(Don don, String categorieLibelle, OutputStream out) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float margin = 56;
            float y = page.getMediaBox().getHeight() - margin;
            float x = margin;
            float line = 16;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(safeLatin1("MediLink - Reçu d'acceptation de don"));
                cs.endText();
                y -= line * 2;

                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(safeLatin1(
                        "Le présent document atteste que le don ci-dessous a été accepté par l'administration."));
                cs.endText();
                y -= line * 2.2f;

                cs.setFont(PDType1Font.HELVETICA, 11);
                y = ligne(cs, x, y, line, "Date du reçu", LocalDate.now().format(DATE_FR));
                y = ligne(cs, x, y, line, "Article / description", tronquer(don.getArticleDescription(), 95));
                y = ligne(cs, x, y, line, "Catégorie", tronquer(categorieLibelle != null ? categorieLibelle : "-", 95));
                y = ligne(cs, x, y, line, "Quantité", don.getQuantite() + " " + n(don.getUnite()));
                y = ligne(cs, x, y, line, "État", tronquer(don.getEtat(), 95));
                y = ligne(cs, x, y, line, "Niveau d'urgence", n(don.getNiveauUrgence()));
                y = ligne(cs, x, y, line, "Détails", tronquer(don.getDetailsSupplementaires(), 95));
                if (don.getDateSoumission() != null) {
                    y = ligne(cs, x, y, line, "Date de soumission du don",
                            don.getDateSoumission().toLocalDate().format(DATE_FR));
                }
                if (don.getDateExpiration() != null) {
                    y = ligne(cs, x, y, line, "Date d'expiration indiquée",
                            don.getDateExpiration().toLocalDate().format(DATE_FR));
                }
                y = ligne(cs, x, y, line, "Statut", "valide (accepté)");

                y -= line * 2;
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9);
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(safeLatin1(
                        "Document généré par l'application MediLink / gdons - sans valeur fiscale."));
                cs.endText();
            }
            doc.save(out);
        }
    }

    private static float ligne(PDPageContentStream cs, float x, float y, float line, String label, String value)
            throws IOException {
        String text = label + " : " + value;
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(safeLatin1(text));
        cs.endText();
        return y - line * 1.35f;
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
        return s == null || s.isBlank() ? "-" : s;
    }

    private static String tronquer(String s, int max) {
        String t = n(s);
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 3) + "...";
    }
}
