package userfx;


import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportHelper {

    public static void exportUsers(List<User> users, Stage stage) {
        // File chooser so user picks where to save
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export User List");
        chooser.setInitialFileName("medilink_users_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".txt");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Text File", "*.txt"),
            new FileChooser.ExtensionFilter("CSV File", "*.csv")
        );

        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return; // user cancelled

        boolean isCsv = file.getName().endsWith(".csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {

            if (isCsv) {
                // CSV format
                pw.println("ID,Full Name,Email,Role,Status,Phone");
                for (User u : users) {
                    pw.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        cleanRole(u.getRoles()),
                        u.getStatus() != null ? u.getStatus() : "ACTIVE",
                        u.getPhone() != null ? u.getPhone() : ""
                    );
                }
            } else {
                // Pretty text format
                String line = "═".repeat(70);
                String thin = "─".repeat(70);

                pw.println(line);
                pw.println("  MEDILINK — USER LIST EXPORT");
                pw.println("  Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                pw.println("  Total Users: " + users.size());
                pw.println(line);
                pw.println();

                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    pw.printf("  #%-4d %s%n", i + 1, u.getFullName());
                    pw.printf("        Email  : %s%n", u.getEmail());
                    pw.printf("        Role   : %s%n", cleanRole(u.getRoles()));
                    pw.printf("        Status : %s%n", u.getStatus() != null ? u.getStatus() : "ACTIVE");
                    if (u.getPhone() != null && !u.getPhone().isEmpty())
                        pw.printf("        Phone  : %s%n", u.getPhone());
                    pw.println("  " + thin);
                }

                pw.println();
                pw.println("  End of report — MediLink Care");
                pw.println(line);
            }

            PopupHelper.showSuccess("Exported " + users.size() + " users to:\n" + file.getName());

        } catch (Exception e) {
            PopupHelper.showError("Export failed:\n" + e.getMessage());
        }
    }

    private static String cleanRole(String roles) {
        if (roles == null) return "USER";
        return roles.replace("[","").replace("]","")
                    .replace("\"","").replace("ROLE_","");
    }
}
