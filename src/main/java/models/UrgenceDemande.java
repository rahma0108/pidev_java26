package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class UrgenceDemande {

    private int id;
    private String message;
    private String pieceImagePath;
    private String statut;
    private Timestamp createdAt;
    /** Jeton machine (sans compte) : rattache la demande à « Mes campagnes ». */
    private String auteurToken;

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

    public String getPieceImagePath() {
        return pieceImagePath;
    }

    public void setPieceImagePath(String pieceImagePath) {
        this.pieceImagePath = pieceImagePath;
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

    public String getAuteurToken() {
        return auteurToken;
    }

    public void setAuteurToken(String auteurToken) {
        this.auteurToken = auteurToken;
    }
}
