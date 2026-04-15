package services;

import exceptions.ServiceException;
import models.User;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.Optional;

public class UserService {

    private final Connection connection;

    public UserService() throws ServiceException {
        this(MyConnection.getInstance().getConn());
    }

    public UserService(Connection connection) throws ServiceException {
        if (connection == null) {
            throw new ServiceException("Connexion JDBC indisponible (vérifiez MySQL et application.properties).");
        }
        this.connection = connection;
    }

    public Optional<User> findById(int id) throws ServiceException {
        String sql = """
                SELECT id, full_name, email, roles, preferred_time, max_days_ahead
                FROM `user` WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lecture utilisateur.", e);
        }
        return Optional.empty();
    }

    public Optional<User> findFirstByRole(String role) throws ServiceException {
        String sql = """
                SELECT id, full_name, email, roles, preferred_time, max_days_ahead
                FROM `user`
                WHERE UPPER(roles) LIKE ?
                ORDER BY id
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, "%" + role.toUpperCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lecture utilisateur par rôle.", e);
        }
        return Optional.empty();
    }

    static User mapUser(ResultSet rs) throws SQLException {
        Time pref = rs.getTime("preferred_time");
        int maxDays = rs.getInt("max_days_ahead");
        boolean maxNull = rs.wasNull();
        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                User.parseRoles(rs.getString("roles")),
                pref != null ? pref.toLocalTime() : null,
                maxNull ? null : maxDays
        );
    }
}
