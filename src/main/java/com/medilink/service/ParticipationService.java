package com.medilink.service;

import com.medilink.model.Participation;
import com.medilink.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService {

    public void insert(Participation p) throws SQLException {
        String sql = "INSERT INTO participations(evenement_id, statut, date_inscription, user_id, commentaire) VALUES(?, 'en_attente', CURRENT_TIMESTAMP, ?, ?)";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getEvenementId());
            ps.setInt(2, p.getUserId());
            ps.setString(3, p.getCommentaire());
            ps.executeUpdate();
        }
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE participations SET statut=? WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM participations WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Participation> getAll() throws SQLException {
        String sql = "SELECT * FROM participations";
        return loadFromQuery(sql, null);
    }

    public List<Participation> getByEvent(int eventId) throws SQLException {
        String sql = "SELECT * FROM participations WHERE evenement_id = ? ORDER BY date_inscription DESC";
        return loadFromQuery(sql, eventId);
    }

    private List<Participation> loadFromQuery(String sql, Integer eventId) throws SQLException {
        List<Participation> participations = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (eventId != null) {
                ps.setInt(1, eventId);
            }
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("date_inscription");
                LocalDateTime dateInscription = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();
                Participation p = new Participation(
                        rs.getInt("id"),
                        rs.getInt("evenement_id"),
                        rs.getString("statut"),
                        dateInscription,
                        rs.getInt("user_id"),
                        rs.getString("commentaire")
                );
                participations.add(p);
            }
            }
        }
        return participations;
    }
}
