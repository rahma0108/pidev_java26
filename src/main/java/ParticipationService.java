import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {
    private Connection connection;

    public ParticipationService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void insert(Participation p) {
        String query = "INSERT INTO participations (evenement_id, user_id, statut, commentaire) VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, p.getEvenementId());
            ps.setInt(2, p.getUserId());
            ps.setString(3, p.getStatut() != null ? p.getStatut() : "en_attente");
            ps.setString(4, p.getCommentaire());
            ps.executeUpdate();
            System.out.println("Participation added for user " + p.getUserId());
        } catch (SQLException e) {
            System.err.println("Insert Participation error: " + e.getMessage());
        }
    }

    public List<Participation> getByEvent(int eventId) {
        List<Participation> participations = new ArrayList<>();
        String query = "SELECT p.*, u.full_name as user_name FROM participations p " +
                       "JOIN user u ON p.user_id = u.id " +
                       "WHERE p.evenement_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Participation p = new Participation(
                        rs.getInt("id"),
                        rs.getInt("evenement_id"),
                        rs.getInt("user_id"),
                        rs.getString("statut"),
                        rs.getTimestamp("date_inscription").toLocalDateTime(),
                        rs.getString("commentaire")
                );
                p.setUserName(rs.getString("user_name"));
                participations.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Get Participations by Event error: " + e.getMessage());
        }
        return participations;
    }

    public boolean isUserParticipating(int userId, int eventId) {
        String query = "SELECT COUNT(*) FROM participations WHERE user_id = ? AND evenement_id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Check participation error: " + e.getMessage());
        }
        return false;
    }
}
