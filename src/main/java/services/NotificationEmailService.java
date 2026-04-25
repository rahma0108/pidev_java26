package services;

import models.AINotification;
import models.User;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class NotificationEmailService {

    // Configuration Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EXPEDITEUR = System.getenv("MAIL_USER");
    private static final String MOT_DE_PASSE = System.getenv("MAIL_PASSWORD");

    // Methode principale
    public void envoyerNotification(User patient, AINotification notif) {
        if (patient == null || patient.getEmail() == null || patient.getEmail().isBlank()) {
            System.err.println("Email patient manquant - notification non envoyee.");
            return;
        }
        if (notif == null || notif.isEchec()) {
            System.err.println("Notification invalide - email non envoye.");
            return;
        }
        if (EXPEDITEUR == null || EXPEDITEUR.isBlank() || MOT_DE_PASSE == null || MOT_DE_PASSE.isBlank()) {
            System.err.println("Configuration SMTP manquante (MAIL_USER/MAIL_PASSWORD).");
            return;
        }

        // Envoi en thread separe pour ne pas bloquer l'UI
        Thread t = new Thread(() -> {
            try {
                Session session = creerSession();
                Message message = construireMessage(session, patient, notif);
                Transport.send(message);
                System.out.println("Email envoye a : " + patient.getEmail());
            } catch (Exception e) {
                System.err.println("Erreur envoi email : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // Creation de la session SMTP
    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EXPEDITEUR, MOT_DE_PASSE);
            }
        });
    }

    // Construction du message
    private Message construireMessage(Session session, User patient, AINotification notif)
            throws MessagingException, UnsupportedEncodingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EXPEDITEUR, "MediLink"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(patient.getEmail()));
        message.setSubject(formatSujet(notif));
        message.setText(formatCorps(patient, notif));
        return message;
    }

    // Formatage du sujet
    private String formatSujet(AINotification notif) {
        return switch (notif.getTypeNotificationSafe()) {
            case "urgent" -> "⚠ MediLink - Notification urgente";
            case "replanification" -> "📅 MediLink - Replanification suggeree";
            default -> "🔔 MediLink - Rappel de rendez-vous";
        };
    }

    // Formatage du corps
    private String formatCorps(User patient, AINotification notif) {
        String fullName = patient.getFullName() == null || patient.getFullName().isBlank()
                ? "Patient"
                : patient.getFullName();
        return "Bonjour " + fullName + ",\n\n"
                + (notif.getMessage() == null ? "" : notif.getMessage()) + "\n\n"
                + actionEnTexte(notif)
                + "\n\nCordialement,\nL'equipe MediLink";
    }

    private String actionEnTexte(AINotification notif) {
        return switch (notif.getActionRecommandeeSafe()) {
            case "confirmer" -> "👉 Action recommandee : confirmez votre rendez-vous.";
            case "replanifier" -> "👉 Action recommandee : replanifiez votre rendez-vous.";
            case "annuler" -> "👉 Action recommandee : annulez votre rendez-vous.";
            default -> "";
        };
    }
}
