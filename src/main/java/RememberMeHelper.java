import java.io.*;
import java.util.Properties;

public class RememberMeHelper {

    private static final String FILE = System.getProperty("user.home") + "/medilink_session.properties";

    public static void save(String email, String password) {
        try {
            Properties props = new Properties();
            props.setProperty("email", email);
            props.setProperty("password", password);
            props.setProperty("remember", "true");
            props.store(new FileWriter(FILE), "MediLink session");
        } catch (Exception e) {
            System.err.println("Could not save session: " + e.getMessage());
        }
    }

    public static String loadEmail() {
        try {
            Properties props = new Properties();
            props.load(new FileReader(FILE));
            if ("true".equals(props.getProperty("remember")))
                return props.getProperty("email", "");
        } catch (Exception ignored) {}
        return "";
    }

    public static String loadPassword() {
        try {
            Properties props = new Properties();
            props.load(new FileReader(FILE));
            if ("true".equals(props.getProperty("remember")))
                return props.getProperty("password", "");
        } catch (Exception ignored) {}
        return "";
    }

    public static boolean isRemembered() {
        try {
            Properties props = new Properties();
            props.load(new FileReader(FILE));
            return "true".equals(props.getProperty("remember"));
        } catch (Exception ignored) {}
        return false;
    }

    public static void clear() {
        try { new File(FILE).delete(); } catch (Exception ignored) {}
    }
}