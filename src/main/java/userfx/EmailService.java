package userfx;


import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;

public class EmailService {

    private static final String SENDER_EMAIL    = "medilink.no.reply@gmail.com";
    private static final String SENDER_PASSWORD = "qexc jghx ixhm jfko";
    private static final String LOGO_PATH       =
            "src/main/resources/logo.png";

    // ─────────────────────────────────────────
    // Generate a random 6-digit code
    // ─────────────────────────────────────────
    public static String generateCode() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    // ─────────────────────────────────────────
    // Load logo as base64
    // ─────────────────────────────────────────
    private static String getLogoBase64() {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(LOGO_PATH));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.err.println("Logo not found, skipping: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────
    // Send the reset code email
    // ─────────────────────────────────────────
    public static void sendResetCode(String toEmail, String code) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        // Build multipart (HTML + logo attachment)
        MimeMultipart multipart = new MimeMultipart("related");

        // ── HTML body part
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(buildHtml(code), "text/html; charset=utf-8");
        multipart.addBodyPart(htmlPart);

        // ── Logo inline attachment
        String logoBase64 = getLogoBase64();
        if (logoBase64 != null) {
            MimeBodyPart logoPart = new MimeBodyPart();
            logoPart.setHeader("Content-Type", "image/png");
            logoPart.setHeader("Content-Transfer-Encoding", "base64");
            logoPart.setHeader("Content-ID", "<medilink_logo>");
            logoPart.setHeader("Content-Disposition", "inline");
            logoPart.setContent(logoBase64, "image/png");
            multipart.addBodyPart(logoPart);
        }

        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(SENDER_EMAIL, "MediLink"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(SENDER_EMAIL));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("🔐 Your MediLink Password Reset Code");
        message.setContent(multipart);

        Transport.send(message);
        System.out.println("Reset code sent to: " + toEmail);
    }

    // ─────────────────────────────────────────
    // HTML email template
    // ─────────────────────────────────────────
    private static String buildHtml(String code) {
        // Split code into individual digits for styling
        String[] digits = code.split("");
        StringBuilder digitBoxes = new StringBuilder();
        for (String d : digits) {
            digitBoxes.append(
                    "<span style='" +
                            "display:inline-block;" +
                            "width:48px; height:56px;" +
                            "line-height:56px;" +
                            "margin:0 4px;" +
                            "background:#f0f7ff;" +
                            "border:2px solid #185FA5;" +
                            "border-radius:10px;" +
                            "font-size:28px;" +
                            "font-weight:700;" +
                            "color:#185FA5;" +
                            "text-align:center;" +
                            "font-family:monospace;'>" + d + "</span>"
            );
        }

        return "<!DOCTYPE html>" +
                "<html lang='en'><head><meta charset='UTF-8'/>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
                "<title>MediLink Password Reset</title></head>" +
                "<body style='margin:0;padding:0;background:#f4f8fb;font-family:Arial,sans-serif;'>" +

                // ── Outer wrapper
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4f8fb;padding:40px 0;'>" +
                "<tr><td align='center'>" +

                // ── Card
                "<table width='600' cellpadding='0' cellspacing='0' style='" +
                "background:#ffffff;border-radius:20px;" +
                "box-shadow:0 4px 24px rgba(24,95,165,0.10);overflow:hidden;'>" +

                // ── Header with gradient
                "<tr><td style='" +
                "background:linear-gradient(135deg,#0d1b2a 0%,#185FA5 60%,#1D9E75 100%);" +
                "padding:36px 40px 28px;text-align:center;'>" +

                // Logo
                "<img src='cid:medilink_logo' alt='MediLink' height='56' " +
                "style='margin-bottom:16px;display:block;margin-left:auto;margin-right:auto;'/>" +

                // Heartbeat line decoration
                "<div style='margin:0 auto 8px;max-width:320px;'>" +
                "<svg width='320' height='32' viewBox='0 0 320 32' xmlns='http://www.w3.org/2000/svg'>" +
                "<polyline points='0,16 60,16 80,4 100,28 120,4 140,28 160,16 320,16' " +
                "fill='none' stroke='rgba(255,255,255,0.35)' stroke-width='2' stroke-linecap='round'/>" +
                "</svg></div>" +

                "<h1 style='color:#ffffff;font-size:22px;font-weight:700;" +
                "margin:0;letter-spacing:0.5px;'>Password Reset Request</h1>" +
                "<p style='color:rgba(255,255,255,0.7);font-size:13px;margin:6px 0 0;'>" +
                "Secure account recovery • MediLink Health Platform</p>" +
                "</td></tr>" +

                // ── Body
                "<tr><td style='padding:40px 48px 32px;'>" +

                // Greeting
                "<p style='color:#1a2a3a;font-size:16px;margin:0 0 12px;'>Hello,</p>" +
                "<p style='color:#445566;font-size:15px;line-height:1.7;margin:0 0 28px;'>" +
                "We received a request to reset your <strong style='color:#185FA5;'>MediLink</strong> " +
                "account password. Use the secure code below to proceed. " +
                "This code is valid for <strong>10 minutes</strong>.</p>" +

                // Code label
                "<p style='text-align:center;color:#667788;font-size:13px;" +
                "text-transform:uppercase;letter-spacing:1.5px;margin:0 0 12px;'>" +
                "Your Reset Code</p>" +

                // Digit boxes
                "<div style='text-align:center;margin:0 0 28px;'>" +
                digitBoxes +
                "</div>" +

                // Warning box
                "<table width='100%' cellpadding='0' cellspacing='0' style='" +
                "background:#fff8f0;border-radius:12px;" +
                "border-left:4px solid #EF9F27;margin-bottom:28px;'>" +
                "<tr><td style='padding:16px 20px;'>" +
                "<p style='margin:0;color:#854F0B;font-size:13px;line-height:1.6;'>" +
                "<strong>⚠ Security Notice:</strong> Never share this code with anyone. " +
                "MediLink staff will never ask for your reset code. " +
                "If you did not request this, please ignore this email — your account is safe.</p>" +
                "</td></tr></table>" +

                // Health tip divider
                "<table width='100%' cellpadding='0' cellspacing='0' style='" +
                "background:#f0f7ff;border-radius:12px;margin-bottom:28px;'>" +
                "<tr><td style='padding:16px 20px;'>" +
                "<p style='margin:0;color:#185FA5;font-size:13px;line-height:1.6;'>" +
                "<strong>💡 Health Tip:</strong> Use a strong, unique password for your health account. " +
                "Combine uppercase letters, numbers and symbols to keep your medical data secure.</p>" +
                "</td></tr></table>" +

                "</td></tr>" +

                // ── Footer
                "<tr><td style='" +
                "background:#f4f8fb;padding:24px 48px;border-top:1px solid #e8eef4;" +
                "border-radius:0 0 20px 20px;text-align:center;'>" +

                // Cross icon
                "<div style='margin-bottom:12px;'>" +
                "<svg width='28' height='28' viewBox='0 0 28 28' xmlns='http://www.w3.org/2000/svg'>" +
                "<rect x='11' y='2' width='6' height='24' rx='3' fill='#185FA5' opacity='0.7'/>" +
                "<rect x='2' y='11' width='24' height='6' rx='3' fill='#185FA5' opacity='0.7'/>" +
                "</svg></div>" +

                "<p style='color:#667788;font-size:12px;margin:0 0 6px;line-height:1.6;'>" +
                "This email was sent by <strong>MediLink Health Platform</strong>.<br/>" +
                "If you have questions, contact our support team.</p>" +
                "<p style='color:#aabbcc;font-size:11px;margin:0;'>" +
                "© 2025 MediLink. All rights reserved. &nbsp;|&nbsp; " +
                "Your health, our priority. ❤</p>" +
                "</td></tr>" +

                "</table>" + // end card
                "</td></tr></table>" + // end outer
                "</body></html>";
    }
}