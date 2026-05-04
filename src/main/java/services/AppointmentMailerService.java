package services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import models.RendezVous;
import models.User;

import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class AppointmentMailerService {

    private static final String SMTP_HOST    = "smtp.gmail.com";
    private static final String SMTP_PORT    = "587";
    private static final String EXPEDITEUR   = System.getenv("MAIL_USER");
    private static final String MOT_DE_PASSE = System.getenv("MAIL_PASSWORD");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    // ── Méthodes publiques ────────────────────────────────────────────────

    public void sendReservationEmail(RendezVous rdv) {
        User patient = resolvePatient(rdv);
        if (patient == null) return;
        envoyer(
                patient.getEmail(),
                "🗓 MediLink — Rendez-vous enregistré",
                buildHtml(
                        patient.getFullName(),
                        "Votre rendez-vous est enregistré",
                        "Votre demande a bien été reçue. Votre médecin va confirmer prochainement.",
                        new String[][]{
                                {"Statut",   "En attente de confirmation"},
                                {"Date",     formatDate(rdv)},
                                {"Médecin",  resolveMedecin(rdv)},
                                {"Motif",    rdv.getMotif() != null && !rdv.getMotif().isBlank()
                                        ? rdv.getMotif() : "—"}
                        },
                        "#f59e0b", "⏳"
                )
        );
    }

    public void sendConfirmationEmail(RendezVous rdv) {
        User patient = resolvePatient(rdv);
        if (patient == null) return;
        envoyer(
                patient.getEmail(),
                "✅ MediLink — Rendez-vous confirmé",
                buildHtml(
                        patient.getFullName(),
                        "Votre rendez-vous est confirmé",
                        "Votre médecin a confirmé votre rendez-vous. Présentez-vous 5 minutes avant l'heure.",
                        new String[][]{
                                {"Statut",  "Confirmé"},
                                {"Date",    formatDate(rdv)},
                                {"Médecin", resolveMedecin(rdv)}
                        },
                        "#16a34a", "✅"
                )
        );
    }

    public void sendCancellationEmail(RendezVous rdv) {
        User patient = resolvePatient(rdv);
        if (patient == null) return;
        envoyer(
                patient.getEmail(),
                "❌ MediLink — Rendez-vous annulé",
                buildHtml(
                        patient.getFullName(),
                        "Votre rendez-vous a été annulé",
                        "Le créneau est désormais libéré. Vous pouvez réserver un nouveau rendez-vous depuis l'application.",
                        new String[][]{
                                {"Date",    formatDate(rdv)},
                                {"Médecin", resolveMedecin(rdv)}
                        },
                        "#dc2626", "❌"
                )
        );
    }

    public void sendModificationEmail(RendezVous oldRdv, RendezVous newRdv) {
        User patient = resolvePatient(newRdv);
        if (patient == null) return;
        envoyer(
                patient.getEmail(),
                "📝 MediLink — Rendez-vous modifié",
                buildHtml(
                        patient.getFullName(),
                        "Votre rendez-vous a été modifié",
                        "Les détails de votre rendez-vous ont été mis à jour.",
                        new String[][]{
                                {"Ancienne date", formatDate(oldRdv)},
                                {"Nouvelle date", formatDate(newRdv)},
                                {"Médecin",       resolveMedecin(newRdv)}
                        },
                        "#2563eb", "📝"
                )
        );
    }

    // ── Template HTML ─────────────────────────────────────────────────────

    private String buildHtml(
            String nomPatient,
            String titreEmail,
            String sousTitre,
            String[][] details,
            String couleurAccent,
            String icone
    ) {
        StringBuilder lignes = new StringBuilder();
        for (String[] row : details) {
            lignes.append("""
                <tr>
                  <td style="padding:10px 16px;color:#64748b;font-size:14px;
                             font-weight:600;width:40%%;border-bottom:1px solid #f1f5f9;">
                    %s
                  </td>
                  <td style="padding:10px 16px;color:#0f172a;font-size:14px;
                             border-bottom:1px solid #f1f5f9;">
                    %s
                  </td>
                </tr>
            """.formatted(row[0], row[1]));
        }

        return """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        </head>
        <body style="margin:0;padding:0;background-color:#f0f4f8;
                     font-family:'Segoe UI',Arial,sans-serif;">

          <!-- Wrapper -->
          <table width="100%%" cellpadding="0" cellspacing="0"
                 style="background:#f0f4f8;padding:40px 0;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="max-width:600px;width:100%%;">

                <!-- Header -->
                <tr>
                  <td style="background:linear-gradient(135deg,#1e40af 0%%,#1d4ed8 50%%,#2563eb 100%%);
                             border-radius:20px 20px 0 0;padding:40px 40px 32px;text-align:center;">
                    <div style="display:inline-block;background:rgba(255,255,255,0.15);
                                border-radius:50%%;width:72px;height:72px;line-height:72px;
                                font-size:32px;margin-bottom:16px;">
                      %s
                    </div>
                    <h1 style="color:white;margin:0;font-size:22px;font-weight:800;
                               letter-spacing:-0.3px;">MediLink</h1>
                    <p style="color:rgba(255,255,255,0.8);margin:6px 0 0;font-size:13px;">
                      Plateforme de gestion médicale
                    </p>
                  </td>
                </tr>

                <!-- Accent bar -->
                <tr>
                  <td style="background:%s;height:4px;"></td>
                </tr>

                <!-- Body -->
                <tr>
                  <td style="background:white;padding:36px 40px;">

                    <p style="color:#64748b;font-size:14px;margin:0 0 8px;">
                      Bonjour <strong style="color:#0f172a;">%s</strong>,
                    </p>
                    <h2 style="color:#0f172a;font-size:20px;font-weight:800;
                               margin:0 0 8px;letter-spacing:-0.3px;">
                      %s
                    </h2>
                    <p style="color:#475569;font-size:14px;line-height:1.6;margin:0 0 28px;">
                      %s
                    </p>

                    <!-- Détails -->
                    <div style="background:#f8fafc;border-radius:14px;
                                border:1px solid #e2e8f0;overflow:hidden;margin-bottom:28px;">
                      <div style="background:%s;padding:12px 16px;">
                        <span style="color:white;font-size:13px;font-weight:700;
                                     text-transform:uppercase;letter-spacing:0.5px;">
                          Détails du rendez-vous
                        </span>
                      </div>
                      <table width="100%%" cellpadding="0" cellspacing="0"
                             style="background:white;">
                        %s
                      </table>
                    </div>

                    <!-- CTA -->
                    <div style="text-align:center;margin-bottom:28px;">
                      <a href="https://medilink.app"
                         style="display:inline-block;background:linear-gradient(135deg,#1e40af,#2563eb);
                                color:white;text-decoration:none;padding:14px 32px;
                                border-radius:12px;font-weight:700;font-size:14px;
                                letter-spacing:0.2px;">
                        Accéder à MediLink
                      </a>
                    </div>

                    <!-- Note -->
                    <div style="background:#f0f9ff;border-left:4px solid #2563eb;
                                border-radius:0 8px 8px 0;padding:14px 16px;">
                      <p style="color:#1e40af;font-size:13px;margin:0;line-height:1.5;">
                        💡 En cas de question, contactez directement votre médecin
                        ou l'administration de la clinique.
                      </p>
                    </div>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="background:#1e293b;border-radius:0 0 20px 20px;
                             padding:24px 40px;text-align:center;">
                    <p style="color:rgba(255,255,255,0.9);font-size:15px;
                               font-weight:700;margin:0 0 4px;">MediLink</p>
                    <p style="color:rgba(255,255,255,0.5);font-size:12px;margin:0;">
                      Plateforme de gestion médicale intelligente
                    </p>
                    <p style="color:rgba(255,255,255,0.3);font-size:11px;
                               margin:12px 0 0;">
                      Cet email a été envoyé automatiquement, merci de ne pas y répondre.
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(
                icone,
                couleurAccent,
                nomPatient,
                titreEmail,
                sousTitre,
                couleurAccent,
                lignes.toString()
        );
    }

    // ── Envoi HTML ────────────────────────────────────────────────────────

    private void envoyer(String destinataire, String sujet, String html) {
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

                // Corps HTML
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(html, "text/html; charset=UTF-8");

                MimeMultipart multipart = new MimeMultipart("alternative");
                multipart.addBodyPart(htmlPart);
                message.setContent(multipart);

                Transport.send(message);
                System.out.println("[Mailer] Email HTML envoyé à : " + destinataire);

            } catch (Exception e) {
                System.err.println("[Mailer] Erreur envoi : " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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
        if (rdv.getDisponibilite() == null) return "Médecin inconnu";
        if (rdv.getDisponibilite().getMedecin() == null) return "Médecin inconnu";
        return rdv.getDisponibilite().getMedecin().getFullName();
    }

    private String formatDate(RendezVous rdv) {
        if (rdv == null || rdv.getDateHeure() == null) return "date inconnue";
        return rdv.getDateHeure().format(FMT);
    }
}