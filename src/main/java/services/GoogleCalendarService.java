package services;

import models.RendezVous;

import java.awt.Desktop;
import java.net.URI;
import java.time.format.DateTimeFormatter;

public class GoogleCalendarService {

    private static final DateTimeFormatter GCAL_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public static void ouvrirDansGoogleCalendar(RendezVous rdv) {
        if (rdv == null || rdv.getDateHeure() == null) {
            System.err.println("[Calendar] RDV ou date manquant.");
            return;
        }

        try {
            String medecin = "Medecin inconnu";
            if (rdv.getDisponibilite() != null
                    && rdv.getDisponibilite().getMedecin() != null
                    && rdv.getDisponibilite().getMedecin().getFullName() != null
                    && !rdv.getDisponibilite().getMedecin().getFullName().isBlank()) {
                medecin = rdv.getDisponibilite().getMedecin().getFullName();
            }

            String titre = "Rendez-vous medical - " + medecin;
            String debut = rdv.getDateHeure().format(GCAL_FMT);
            String fin = rdv.getDateHeure().plusMinutes(30).format(GCAL_FMT);

            String details = "Rendez-vous MediLink";
            if (rdv.getMotif() != null && !rdv.getMotif().isBlank()) {
                details += " - Motif : " + rdv.getMotif();
            }

            String url = "https://calendar.google.com/calendar/render"
                    + "?action=TEMPLATE"
                    + "&text=" + encode(titre)
                    + "&dates=" + debut + "/" + fin
                    + "&details=" + encode(details)
                    + "&sf=true"
                    + "&output=xml";

            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                System.out.println("[Calendar] Ouvert : " + url);
            } else {
                System.err.println("[Calendar] Desktop non supporte.");
            }
        } catch (Exception e) {
            System.err.println("[Calendar] Erreur : " + e.getMessage());
        }
    }

    private static String encode(String text) {
        if (text == null) {
            return "";
        }
        return java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
    }
}
