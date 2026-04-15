package org.example.dao;

import org.example.models.Medicament;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicamentDAO {

    public void create(Medicament medicament) throws SQLException {
        String sql = "INSERT INTO medicaments (nom, description, quantite_stock) VALUES (?, ?, ?)";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, medicament.getNom());
            pstmt.setString(2, medicament.getDescription());
            pstmt.setInt(3, medicament.getQuantiteStock());
            pstmt.executeUpdate();
        }
    }

    public List<Medicament> getAll() throws SQLException {
        List<Medicament> medicaments = new ArrayList<>();
        String sql = "SELECT * FROM medicaments";
        try (Connection conn = MyConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Medicament m = new Medicament();
                m.setId(rs.getInt("id"));
                m.setNom(rs.getString("nom"));
                m.setDescription(rs.getString("description"));
                m.setQuantiteStock(rs.getInt("quantite_stock"));
                medicaments.add(m);
            }
        }
        return medicaments;
    }

    public Medicament getById(int id) throws SQLException {
        String sql = "SELECT * FROM medicaments WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Medicament m = new Medicament();
                    m.setId(rs.getInt("id"));
                    m.setNom(rs.getString("nom"));
                    m.setDescription(rs.getString("description"));
                    m.setQuantiteStock(rs.getInt("quantite_stock"));
                    return m;
                }
            }
        }
        return null;
    }

    public void update(Medicament medicament) throws SQLException {
        String sql = "UPDATE medicaments SET nom = ?, description = ?, quantite_stock = ? WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, medicament.getNom());
            pstmt.setString(2, medicament.getDescription());
            pstmt.setInt(3, medicament.getQuantiteStock());
            pstmt.setInt(4, medicament.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM medicaments WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}