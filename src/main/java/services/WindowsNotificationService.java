package services;

import java.io.File;

public class WindowsNotificationService {

    private static final String APP_ID = "MediLink";

    public static void enregistrerApp() {
        new Thread(() -> {
            try {
                String logoPath = "";
                try {
                    java.net.URL logoUrl = WindowsNotificationService.class
                            .getResource("/logo.png");
                    if (logoUrl != null) {
                        logoPath = new java.io.File(logoUrl.toURI())
                                .getAbsolutePath();
                    }
                } catch (Exception ignored) {}

                String script =
                        "$appId = 'MediLink'\n" +
                        "$appName = 'MediLink'\n" +
                        "$logoPath = '" + logoPath.replace("\\", "\\\\") + "'\n" +
                        "$regPath = \"HKCU:\\Software\\Classes\\AppUserModelId\\$appId\"\n" +
                        "if (-not (Test-Path $regPath)) {\n" +
                        "    New-Item -Path $regPath -Force | Out-Null\n" +
                        "}\n" +
                        "Set-ItemProperty -Path $regPath -Name 'DisplayName' -Value $appName\n" +
                        "Set-ItemProperty -Path $regPath -Name 'IconUri' -Value $logoPath\n" +
                        "Write-Output 'MediLink enregistre'\n";

                File scriptFile = File.createTempFile("medilink_reg_", ".ps1");
                scriptFile.deleteOnExit();
                try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(scriptFile),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    fw.write('\uFEFF');
                    fw.write(script);
                }

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell",
                        "-ExecutionPolicy", "Bypass",
                        "-File", scriptFile.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();
                System.out.println("[WinNotif] Enregistrement : " + output.trim());

            } catch (Exception e) {
                System.err.println("[WinNotif] Erreur enregistrement : " + e.getMessage());
            }
        }).start();
    }

    public static void envoyer(String titre, String message) {
        new Thread(() -> {
            try {
                String t = nettoyer(titre);
                String m = nettoyer(message);

                String logoPath = "";
                try {
                    java.net.URL logoUrl = WindowsNotificationService.class
                            .getResource("/logo.png");
                    if (logoUrl != null) {
                        logoPath = new java.io.File(logoUrl.toURI()).toURI().toString();
                    }
                } catch (Exception ignored) {}

                String appLogo = logoPath.isBlank()
                        ? ""
                        : "<image placement=\"appLogoOverride\" hint-crop=\"circle\" src=\"" + logoPath + "\"/>";

                String xml = "<toast scenario=\"reminder\">"
                        + "<visual><binding template=\"ToastGeneric\">"
                        + appLogo
                        + "<text>" + t + "</text>"
                        + "<text>" + m + "</text>"
                        + "</binding></visual>"
                        + "<actions>"
                        + "<action content=\"OK\" arguments=\"ok\"/>"
                        + "</actions>"
                        + "</toast>";

                String script =
                        "[Windows.UI.Notifications.ToastNotificationManager, " +
                        "Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null\n" +
                        "[Windows.Data.Xml.Dom.XmlDocument, " +
                        "Windows.Data.Xml.Dom, ContentType = WindowsRuntime] | Out-Null\n" +
                        "$xml = New-Object Windows.Data.Xml.Dom.XmlDocument\n" +
                        "$xml.LoadXml('" + xml + "')\n" +
                        "$toast = [Windows.UI.Notifications.ToastNotification]::new($xml)\n" +
                        "$toast.ExpirationTime = [DateTimeOffset]::Now.AddDays(1)\n" +
                        "$notifier = [Windows.UI.Notifications.ToastNotificationManager]" +
                        "::CreateToastNotifier('" + APP_ID + "')\n" +
                        "$notifier.Show($toast)\n" +
                        "Write-Output 'OK'\n";

                File scriptFile = File.createTempFile("medilink_notif_", ".ps1");
                scriptFile.deleteOnExit();
                try (java.io.OutputStreamWriter fw = new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(scriptFile),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    fw.write('\uFEFF');
                    fw.write(script);
                }

                ProcessBuilder pb = new ProcessBuilder(
                        "powershell",
                        "-ExecutionPolicy", "Bypass",
                        "-File", scriptFile.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();

                System.out.println("[WinNotif] Exit code : " + exitCode);
                System.out.println("[WinNotif] Output : " + output.trim());

            } catch (Exception e) {
                System.err.println("[WinNotif] Erreur : " + e.getMessage());
            }
        }).start();
    }

    public static void rdvReserve(String medecin, String date) {
        envoyer(
                "MediLink - Rendez-vous enregistre",
                "Avec " + medecin + " le " + date + " - En attente de confirmation."
        );
    }

    public static void rdvConfirme(String medecin, String date) {
        envoyer(
                "MediLink - Rendez-vous confirme",
                "Avec " + medecin + " le " + date + "."
        );
    }

    public static void rdvAnnule(String medecin) {
        envoyer(
                "MediLink - Rendez-vous annule",
                "Votre rendez-vous avec " + medecin + " a ete annule."
        );
    }

    public static void rdvProchain(String heure, String medecin) {
        envoyer(
                "MediLink - Rappel",
                "Rendez-vous demain a " + heure + " avec " + medecin + "."
        );
    }

    public static void tester() {
        envoyer("MediLink - Test", "Notification de test depuis MediLink.");
    }

    private static String nettoyer(String texte) {
        if (texte == null) return "";
        return texte
                .replace("&", "and")
                .replace("<", "")
                .replace(">", "")
                .replace("\"", "")
                .replace("'", "")
                .replace("✅", "")
                .replace("❌", "")
                .replace("🗓", "")
                .replace("⏰", "")
                .replace("📝", "");
    }
}
