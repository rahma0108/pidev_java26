package org.example.dao;

import org.example.models.Ordonnance;
import org.example.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdonnanceDAO {

    public void create(Ordonnance ordonnance) throws SQLException {
        String sql = "INSERT INTO ordonnance (date_creation, instructions, medecin_id, patient_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(ordonnance.getDateCreation()));
            pstmt.setString(2, ordonnance.getInstructions());
            pstmt.setInt(3, ordonnance.getMedecinId());
            pstmt.setInt(4, ordonnance.getPatientId());
            pstmt.executeUpdate();
        }
    }

    public List<Ordonnance> getAll() throws SQLException {
        List<Ordonnance> ordonnances = new ArrayList<>();
        String sql = "SELECT o.*, " +
                "m.full_name as medecin_nom, " +
                "p.full_name as patient_nom " +
                "FROM ordonnance o " +
                "LEFT JOIN user m ON o.medecin_id = m.id " +
                "LEFT JOIN user p ON o.patient_id = p.id " +
                "ORDER BY o.id DESC";

        try (Connection conn = MyConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Ordonnance o = new Ordonnance();
                o.setId(rs.getInt("id"));
                o.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                o.setInstructions(rs.getString("instructions"));
                o.setMedecinId(rs.getInt("medecin_id"));
                o.setPatientId(rs.getInt("patient_id"));

                String medecinNom = rs.getString("medecin_nom");
                o.setNomMedecin(medecinNom != null ? medecinNom : "Médecin inconnu");

                String patientNom = rs.getString("patient_nom");
                o.setNomPatient(patientNom != null ? patientNom : "Patient inconnu");

                ordonnances.add(o);
            }
        }
        return ordonnances;
    }

    public Ordonnance getById(int id) throws SQLException {
        String sql = "SELECT o.*, " +
                "m.full_name as medecin_nom, " +
                "p.full_name as patient_nom " +
                "FROM ordonnance o " +
                "LEFT JOIN user m ON o.medecin_id = m.id " +
                "LEFT JOIN user p ON o.patient_id = p.id " +
                "WHERE o.id = ?";

        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Ordonnance o = new Ordonnance();
                    o.setId(rs.getInt("id"));
                    o.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                    o.setInstructions(rs.getString("instructions"));
                    o.setMedecinId(rs.getInt("medecin_id"));
                    o.setPatientId(rs.getInt("patient_id"));
                    o.setNomMedecin(rs.getString("medecin_nom"));
                    o.setNomPatient(rs.getString("patient_nom"));
                    return o;
                }
            }
        }
        return null;
    }

    public void update(Ordonnance ordonnance) throws SQLException {
        String sql = "UPDATE ordonnance SET instructions = ?, medecin_id = ?, patient_id = ? WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ordonnance.getInstructions());
            pstmt.setInt(2, ordonnance.getMedecinId());
            pstmt.setInt(3, ordonnance.getPatientId());
            pstmt.setInt(4, ordonnance.getId());
            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ordonnance WHERE id = ?";
        try (Connection conn = MyConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}