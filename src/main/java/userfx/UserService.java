package userfx;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private Connection connection;

    public UserService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // ─────────────────────────────────────────
    // LOGIN — email + mot de passe (clair) vs hash bcrypt Symfony ($2y$ / $2a$) ou texte brut legacy
    // ─────────────────────────────────────────
    public User login(String email, String password) {
        if (connection == null) {
            System.err.println("Login error: database connection is null. Check MyConnection configuration.");
            return null;
        }
        User user = getByEmail(email);
        if (user == null) {
            return null;
        }
        String stored = user.getPassword();
        if (stored == null || stored.isBlank()) {
            return null;
        }
        if (passwordMatches(password, stored)) {
            return user;
        }
        return null;
    }

    private static boolean passwordMatches(String raw, String stored) {
        String s = stored.trim();
        if (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$")) {
            return BCRYPT.matches(raw, s);
        }
        return raw.equals(s);
    }

    // ─────────────────────────────────────────
    // CREATE — insert a new user
    // ─────────────────────────────────────────
    public void insert(User user) {
        String query = "INSERT INTO `user` (email, password, full_name, roles, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRoles() != null ? user.getRoles() : "[\"ROLE_USER\"]");
            ps.setString(5, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            ps.executeUpdate();
            System.out.println("User inserted: " + user.getEmail());
        } catch (SQLException e) {
            System.err.println("Insert error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // READ ALL — get all users
    // ─────────────────────────────────────────
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM `user`";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("GetAll error: " + e.getMessage());
        }
        return users;
    }

    // ─────────────────────────────────────────
    // READ ONE — get user by id
    // ─────────────────────────────────────────
    public User getById(int id) {
        String query = "SELECT * FROM `user` WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("GetById error: " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────
    // UPDATE — update an existing user
    // ─────────────────────────────────────────
    public void update(User user) {
        String query = "UPDATE `user` SET email=?, password=?, full_name=?, roles=?, status=?, phone=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassword());   // ← added
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRoles());
            ps.setString(5, user.getStatus());
            ps.setString(6, user.getPhone());
            ps.setInt(7, user.getId());             // ← shifted from 6 to 7
            ps.executeUpdate();
            System.out.println("User updated: " + user.getId());
        } catch (SQLException e) {
            System.err.println("Update error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // DELETE — delete user by id
    // ─────────────────────────────────────────
    public void delete(int id) {
        String query = "DELETE FROM `user` WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("User deleted: " + id);
        } catch (SQLException e) {
            System.err.println("Delete error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // HELPER — map a ResultSet row to a User object
    // ─────────────────────────────────────────
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setRoles(rs.getString("roles"));
        u.setStatus(rs.getString("status"));
        if (hasColumn(rs, "phone")) {
            u.setPhone(rs.getString("phone"));
        }
        return u;
    }

    private static boolean hasColumn(ResultSet rs, String name) throws SQLException {
        var md = rs.getMetaData();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            if (name.equalsIgnoreCase(md.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }
    public User getByEmail(String email) {
        String query = "SELECT * FROM `user` WHERE email = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("GetByEmail error: " + e.getMessage());
        }
        return null;
    }
}