import java.io.*;
import java.util.Properties;

public class RememberMeHelper {

    private static final String FILE = System.getProperty("user.home") + "/medilink_session.properties";

    public static void save(String email) {
        try {
            Properties props = new Properties();
            props.setProperty("email", email);
            props.store(new FileWriter(FILE), "MediLink session");
        } catch (Exception e) {
            System.err.println("Could not save session: " + e.getMessage());
        }
    }

    public static String load() {
        try {
            Properties props = new Properties();
            props.load(new FileReader(FILE));
            return props.getProperty("email", "");
        } catch (Exception e) {
            return "";
        }
    }

    public static void clear() {
        try {
            new File(FILE).delete();
        } catch (Exception ignored) {}
    }
}
