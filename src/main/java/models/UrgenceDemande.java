package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UrgenceDemande {

    private int id;
    private String message;
    private String statut;
    private Timestamp createdAt;

    public static UrgenceDemande fromResultSet(ResultSet rs) throws SQLException {
        UrgenceDemande u = new UrgenceDemande();
        u.setId(rs.getInt("id"));
        u.setMessage(rs.getString("message"));
        u.setStatut(rs.getString("statut"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
