package utils;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MyConnection {
    private static MyConnection instance;
    private Connection conn;

    private String url = "jdbc:mysql://localhost:3306/medilink";
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
        try {
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Connexion réussie !");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }


    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConn() {
        return conn;
    }


}