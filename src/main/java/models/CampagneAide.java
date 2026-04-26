package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CampagneAide {

    private int id;
    private int demandeId;
    private String titre;
    private String corps;
    private Timestamp createdAt;

    public static CampagneAide fromResultSet(ResultSet rs) throws SQLException {
        CampagneAide c = new CampagneAide();
        c.setId(rs.getInt("id"));
        c.setDemandeId(rs.getInt("demande_id"));
        c.setTitre(rs.getString("titre"));
        c.setCorps(rs.getString("corps"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDemandeId() {
        return demandeId;
    }

    public void setDemandeId(int demandeId) {
        this.demandeId = demandeId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCorps() {
        return corps;
    }

    public void setCorps(String corps) {
        this.corps = corps;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
