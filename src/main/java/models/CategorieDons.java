package models;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Entité alignée sur la table {@code categories_dons} (id, nom, description, icone, couleur, created_at).
 */
public class CategorieDons {

    private int id;
    private String nom;
    private String description;
    private String icone;
    private String couleur;
    private Timestamp createdAt;

    public CategorieDons() {
    }

    public static CategorieDons fromResultSet(ResultSet rs) throws SQLException {
        CategorieDons c = new CategorieDons();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setDescription(rs.getString("description"));
        c.setIcone(rs.getString("icone"));
        c.setCouleur(rs.getString("couleur"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
