package services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import models.RendezVous;
import models.User;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class AppointmentMailerService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EXPEDITEUR = System.getenv("MAIL_USER");
    private static final String MOT_DE_PASSE = System.getenv("MAIL_PASSWORD");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy a HH:mm");

    public void sendReservationEmail(RendezVous rdv) {
        User patient = resolvePatient(rdv);
        if (patient == null) return;

        String sujet = "MediLink - Votre rendez-vous est enregistre";
        String corps = "Bonjour " + patient.getFullName() + ",\n\n"
                + "Votre rendez-vous a bien ete enregistre.\n\n"
                + "Details :\n"
                + "- Statut  : En attente de confirmation\n"
                + "- Date    : " + formatDate(rdv) + "\n"
                + "- Medecin : " + resolveMedecin(rdv) + "\n"
                + formatMotif(rdv)
                + "\nVous recevrez un email des que le medecin confirme.\n\n"
                + "Cordialement,\nL'equipe MediLink";

        envoyer(patient.getEmail(), sujet, corps);
    }

    public void sendConfirmationEmail(RendezVous rdv) {
        User patient = resolvePatient(rdv);
        if (patient == null) return;

        String sujet = "MediLink - Votre rendez-vous est confirme";
        String corps = "Bonjour " + patient.getFullName() + ",\n\n"
                + "Votre rendez-vous a ete confirme par votre medecin.\n\n"
                + "Details :\n"
                + "- Statut  : Confirme\n"
                + "- Date    : " + formatDate(rdv) + "\n"
                + "- Medecin : " + resolveMedecin(rdv) + "\n\n"
                + "Pensez a vous presenter 5 minutes avant l'heure.\n\n"
                + "Cordialement,\nL'equipe MediLink";

        envoyer(patient.getEmail(), sujet, corps);
    }

    public void sendCancellationEmail(RendezVous rdv) {
        User patient = resolvePatient(rdv);
        if (patient == null) return;

        String sujet = "MediLink - Votre rendez-vous a ete annule";
        String corps = "Bonjour " + patient.getFullName() + ",\n\n"
                + "Votre rendez-vous du " + formatDate(rdv)
                + " avec " + resolveMedecin(rdv) + " a ete annule.\n\n"
                + "Le creneau est desormais libere.\n"
                + "Vous pouvez reserver un nouveau rendez-vous depuis l'application.\n\n"
                + "Cordialement,\nL'equipe MediLink";

        envoyer(patient.getEmail(), sujet, corps);
    }

    public void sendModificationEmail(RendezVous oldRdv, RendezVous newRdv) {
        User patient = resolvePatient(newRdv);
        if (patient == null) return;

        String sujet = "MediLink - Votre rendez-vous a ete modifie";
        String corps = "Bonjour " + patient.getFullName() + ",\n\n"
                + "Votre rendez-vous a ete modifie.\n\n"
                + "Ancienne date : " + formatDate(oldRdv) + "\n"
                + "Nouvelle date : " + formatDate(newRdv) + "\n"
                + "Medecin       : " + resolveMedecin(newRdv) + "\n\n"
                + "Cordialement,\nL'equipe MediLink";

        envoyer(patient.getEmail(), sujet, corps);
    }

    private void envoyer(String destinataire, String sujet, String corps) {
        if (!configValide()) {
            System.err.println("[Mailer] MAIL_USER/MAIL_PASSWORD manquants.");
            return;
        }
        if (destinataire == null || destinataire.isBlank()) {
            System.err.println("[Mailer] Email destinataire manquant.");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(EXPEDITEUR, MOT_DE_PASSE);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EXPEDITEUR, "MediLink"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(destinataire));
                message.setSubject(sujet);
                message.setText(corps);

                Transport.send(message);
                System.out.println("[Mailer] Email envoye a : " + destinataire);

            } catch (Exception e) {
                System.err.println("[Mailer] Erreur envoi : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private boolean configValide() {
        return EXPEDITEUR != null && !EXPEDITEUR.isBlank()
                && MOT_DE_PASSE != null && !MOT_DE_PASSE.isBlank();
    }

    private User resolvePatient(RendezVous rdv) {
        if (rdv == null) return null;
        User patient = rdv.getPatient();
        if (patient == null || patient.getEmail() == null
                || patient.getEmail().isBlank()) {
            System.err.println("[Mailer] Patient ou email manquant.");
            return null;
        }
        return patient;
    }

    private String resolveMedecin(RendezVous rdv) {
        if (rdv.getDisponibilite() == null) return "Medecin inconnu";
        if (rdv.getDisponibilite().getMedecin() == null) return "Medecin inconnu";
        return rdv.getDisponibilite().getMedecin().getFullName();
    }

    private String formatDate(RendezVous rdv) {
        if (rdv.getDateHeure() == null) return "date inconnue";
        return rdv.getDateHeure().format(FMT);
    }

    private String formatMotif(RendezVous rdv) {
        String motif = rdv.getMotif();
        if (motif == null || motif.isBlank()) return "";
        return "- Motif   : " + motif + "\n";
    }
}
