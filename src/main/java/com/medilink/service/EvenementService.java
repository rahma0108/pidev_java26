package com.medilink.service;

import com.medilink.model.Evenement;
import com.medilink.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EvenementService {

    public void insert(Evenement e) throws SQLException {
        String sql = "INSERT INTO evenements(titre, description, date_evenement, lieu, type, photo) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setDate(3, Date.valueOf(e.getDateEvenement()));
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getType());
            ps.setString(6, e.getPhoto());
            ps.executeUpdate();
        }
    }

    public void update(Evenement e) throws SQLException {
        String sql = "UPDATE evenements SET titre=?, description=?, date_evenement=?, lieu=?, type=?, photo=? WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setDate(3, Date.valueOf(e.getDateEvenement()));
            ps.setString(4, e.getLieu());
            ps.setString(5, e.getType());
            ps.setString(6, e.getPhoto());
            ps.setInt(7, e.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM evenements WHERE id=?";
        try (Connection con = DBConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Evenement> getAll() throws SQLException {
        String sql = "SELECT * FROM evenements";
        List<Evenement> evenements = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Evenement e = new Evenement(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDate("date_evenement").toLocalDate(),
                        rs.getString("lieu"),
                        rs.getString("type"),
                        rs.getString("photo")
                );
                evenements.add(e);
            }
        }
        return evenements;
    }
}
