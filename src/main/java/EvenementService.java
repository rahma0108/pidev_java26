import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {
    private Connection connection;

    public EvenementService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void insert(Evenement ev) {
        String query = "INSERT INTO evenements (titre, description, date_evenement, lieu, type, photo) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, ev.getTitre());
            ps.setString(2, ev.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(ev.getDateEvenement()));
            ps.setString(4, ev.getLieu());
            ps.setString(5, ev.getType());
            ps.setString(6, ev.getPhoto());
            ps.executeUpdate();
            System.out.println("Evenement added: " + ev.getTitre());
        } catch (SQLException e) {
            System.err.println("Insert Event error: " + e.getMessage());
        }
    }

    public List<Evenement> getAll() {
        List<Evenement> events = new ArrayList<>();
        String query = "SELECT * FROM evenements ORDER BY date_evenement DESC";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                events.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("GetAll Events error: " + e.getMessage());
        }
        return events;
    }

    public void update(Evenement ev) {
        String query = "UPDATE evenements SET titre=?, description=?, date_evenement=?, lieu=?, type=?, photo=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, ev.getTitre());
            ps.setString(2, ev.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(ev.getDateEvenement()));
            ps.setString(4, ev.getLieu());
            ps.setString(5, ev.getType());
            ps.setString(6, ev.getPhoto());
            ps.setInt(7, ev.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update Event error: " + e.getMessage());
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM evenements WHERE id = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete Event error: " + e.getMessage());
        }
    }

    private Evenement mapRow(ResultSet rs) throws SQLException {
        return new Evenement(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getTimestamp("date_evenement").toLocalDateTime(),
                rs.getString("lieu"),
                rs.getString("type"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("photo")
        );
    }
}
