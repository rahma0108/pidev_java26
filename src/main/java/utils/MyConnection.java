package utils;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MyConnection {
    private static final int LOGIN_TIMEOUT_SECONDS = 5;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 10000;

    private static MyConnection instance;
    private Connection conn;
    private String lastErrorMessage;

    private String url = "jdbc:mysql://127.0.0.1:3306/medilink";
    private String user = "root";
    private String password = "";

    private static void loadProperties(MyConnection c) {
        try (InputStream in = MyConnection.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                return;
            }
            Properties p = new Properties();
            p.load(in);
            c.url = p.getProperty("spring.datasource.url", c.url);
            c.user = p.getProperty("spring.datasource.username", c.user);
            c.password = p.getProperty("spring.datasource.password", c.password);
        } catch (Exception ignored) {
            // garde les valeurs par défaut
        }
    }

    private MyConnection() {
        loadProperties(this);
        url = avecTimeouts(url);
        ouvrirConnexionSiNecessaire();
    }


    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public synchronized Connection getConn() {
        ouvrirConnexionSiNecessaire();
        return conn;
    }

    public synchronized String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public synchronized String getActiveUrl() {
        return url;
    }

    private static String avecTimeouts(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return jdbcUrl;
        }
        if (jdbcUrl.contains("connectTimeout=") || jdbcUrl.contains("socketTimeout=")) {
            return jdbcUrl;
        }
        String separateur = jdbcUrl.contains("?") ? "&" : "?";
        return jdbcUrl + separateur
                + "connectTimeout=" + CONNECT_TIMEOUT_MS
                + "&socketTimeout=" + SOCKET_TIMEOUT_MS
                + "&allowPublicKeyRetrieval=true"
                + "&useSSL=false"
                + "&serverTimezone=UTC";
    }

    private void ouvrirConnexionSiNecessaire() {
        try {
            if (conn != null && !conn.isClosed()) {
                return;
            }
        } catch (SQLException ignored) {
            // Reconnexion forcée juste après.
        }
        SQLException lastEx = null;
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        for (String candidateUrl : urlsCandidats(url)) {
            try {
                conn = DriverManager.getConnection(candidateUrl, user, password);
                url = candidateUrl;
                lastErrorMessage = null;
                System.out.println("Connexion réussie via " + candidateUrl);
                return;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1049 && essayerCreationBaseManquante(candidateUrl)) {
                    url = candidateUrl;
                    return;
                }
                lastEx = e;
            }
        }
        conn = null;
        lastErrorMessage = formatSqlError(lastEx);
        System.out.println("Erreur de connexion : " + (lastEx == null ? "inconnue" : lastEx.getMessage()));
    }

    private boolean essayerCreationBaseManquante(String candidateUrl) {
        try {
            String dbName = extraireNomBase(candidateUrl);
            String serverUrl = extraireUrlServeur(candidateUrl);
            if (dbName == null || serverUrl == null) {
                return false;
            }
            try (Connection serverConn = DriverManager.getConnection(serverUrl, user, password);
                 Statement st = serverConn.createStatement()) {
                st.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
            }
            conn = DriverManager.getConnection(candidateUrl, user, password);
            lastErrorMessage = null;
            System.out.println("Base manquante créée automatiquement : " + dbName);
            return true;
        } catch (SQLException ex) {
            conn = null;
            lastErrorMessage = formatSqlError(ex);
            return false;
        }
    }

    private static String formatSqlError(SQLException e) {
        if (e == null) {
            return "Erreur SQL inconnue";
        }
        return e.getMessage() + " (SQLState=" + e.getSQLState() + ", Code=" + e.getErrorCode() + ")";
    }

    private static String extraireNomBase(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:mysql://")) {
            return null;
        }
        String sansPrefixe = jdbcUrl.substring("jdbc:mysql://".length());
        int slash = sansPrefixe.indexOf('/');
        if (slash < 0 || slash == sansPrefixe.length() - 1) {
            return null;
        }
        String apresSlash = sansPrefixe.substring(slash + 1);
        int q = apresSlash.indexOf('?');
        String db = (q >= 0) ? apresSlash.substring(0, q) : apresSlash;
        return db.isBlank() ? null : db;
    }

    private static String extraireUrlServeur(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:mysql://")) {
            return null;
        }
        int q = jdbcUrl.indexOf('?');
        String base = (q >= 0) ? jdbcUrl.substring(0, q) : jdbcUrl;
        int slash = base.lastIndexOf('/');
        if (slash < "jdbc:mysql://".length()) {
            return null;
        }
        String serveur = base.substring(0, slash);
        return serveur + "?connectTimeout=" + CONNECT_TIMEOUT_MS
                + "&socketTimeout=" + SOCKET_TIMEOUT_MS
                + "&allowPublicKeyRetrieval=true"
                + "&useSSL=false"
                + "&serverTimezone=UTC";
    }

    private static List<String> urlsCandidats(String baseUrl) {
        List<String> urls = new ArrayList<>();
        if (baseUrl == null || baseUrl.isBlank()) {
            return urls;
        }
        urls.add(baseUrl);
        if (baseUrl.contains("://localhost")) {
            urls.add(baseUrl.replace("://localhost", "://127.0.0.1"));
        } else if (baseUrl.contains("://127.0.0.1")) {
            urls.add(baseUrl.replace("://127.0.0.1", "://localhost"));
        }
        return urls;
    }


}
