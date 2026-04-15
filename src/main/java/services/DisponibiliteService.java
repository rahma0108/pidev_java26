package services;

import exceptions.ServiceException;
import models.Disponibilite;
import models.User;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Persistance alignée sur une table {@code disponibilite} <strong>sans</strong> colonne {@code medecin_id}
 * (schéma minimal : id, date, heure_debut, heure_fin, status, created_at).
 * <p>L’id médecin saisi dans l’UI sert à la validation / chevauchement côté application mais n’est pas stocké.</p>
 */
public class DisponibiliteService {

    private final Connection connection;

    public DisponibiliteService() throws ServiceException {
        this(MyConnection.getInstance().getConn());
    }

    public DisponibiliteService(Connection connection) throws ServiceException {
        if (connection == null) {
            throw new ServiceException("Connexion JDBC indisponible (vérifiez MySQL et application.properties).");
        }
        this.connection = connection;
    }

    public void ajouter(Disponibilite d) throws ServiceException {
        valider(d, null);
        String sql = """
                INSERT INTO disponibilite (date, heure_debut, heure_fin, status, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));
            ps.setString(4, Disponibilite.STATUS_LIBRE);
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getInt(1));
                }
            }
            d.setStatus(Disponibilite.STATUS_LIBRE);
            d.setCreatedAt(now);
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors de l'ajout de la disponibilité.", e);
        }
    }

    public void modifier(Disponibilite d) throws ServiceException {
        if (d.getId() <= 0) {
            throw new ServiceException("Identifiant de disponibilité invalide.");
        }
        if (countRendezVousLies(d.getId()) > 0) {
            throw new ServiceException("Modification interdite : un rendez-vous est lié à cette disponibilité.");
        }
        valider(d, d.getId());
        String sql = """
                UPDATE disponibilite
                SET date = ?, heure_debut = ?, heure_fin = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));
            ps.setInt(4, d.getId());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new ServiceException("Disponibilité introuvable.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors de la modification de la disponibilité.", e);
        }
    }

    public void supprimer(int id) throws ServiceException {
        if (countRendezVousLies(id) > 0) {
            throw new ServiceException("Suppression interdite : un rendez-vous est lié à cette disponibilité.");
        }
        String sql = "DELETE FROM disponibilite WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted == 0) {
                throw new ServiceException("Disponibilité introuvable.");
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors de la suppression de la disponibilité.", e);
        }
    }

    public List<Disponibilite> listerToutes() throws ServiceException {
        return listerParMedecin(null);
    }

    /**
     * Sans colonne {@code medecin_id} en base, le filtre par médecin est ignoré : toutes les lignes sont retournées.
     */
    public List<Disponibilite> listerParMedecin(Integer medecinId) throws ServiceException {
        String sql = """
                SELECT id, date, heure_debut, heure_fin, status, created_at
                FROM disponibilite
                ORDER BY date, heure_debut
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Disponibilite> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapDisponibilite(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors du chargement des disponibilités.", e);
        }
    }

    public Optional<Disponibilite> findById(int id) throws ServiceException {
        String sql = """
                SELECT id, date, heure_debut, heure_fin, status, created_at
                FROM disponibilite
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDisponibilite(rs));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lecture disponibilité.", e);
        }
        return Optional.empty();
    }

    Optional<Disponibilite> findByIdForUpdate(Connection conn, int id) throws ServiceException {
        String sql = """
                SELECT id, date, heure_debut, heure_fin, status, created_at
                FROM disponibilite
                WHERE id = ?
                FOR UPDATE
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDisponibilite(rs));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lecture disponibilité (verrou).", e);
        }
        return Optional.empty();
    }

    void updateStatus(Connection conn, int disponibiliteId, String status) throws ServiceException {
        String sql = "UPDATE disponibilite SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, disponibiliteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ServiceException("Erreur mise à jour statut disponibilité.", e);
        }
    }

    public int countRendezVousLies(int disponibiliteId) throws ServiceException {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE disponibilite_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, disponibiliteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur vérification rendez-vous liés.", e);
        }
        return 0;
    }

    private void valider(Disponibilite d, Integer excludeId) throws ServiceException {
        if (d.getMedecin() == null || d.getMedecin().getId() <= 0) {
            throw new ServiceException("Indiquez un médecin dans le formulaire (non enregistré en base sans colonne dédiée).");
        }
        if (d.getDate() == null || d.getHeureDebut() == null || d.getHeureFin() == null) {
            throw new ServiceException("Date et plage horaire obligatoires.");
        }
        LocalDate today = LocalDate.now();
        if (d.getDate().isBefore(today)) {
            throw new ServiceException("La disponibilité doit être aujourd'hui ou dans le futur.");
        }
        if (!d.getHeureFin().isAfter(d.getHeureDebut())) {
            throw new ServiceException("heureFin doit être strictement après heureDebut.");
        }
        if (existeChevauchement(d.getDate(), d.getHeureDebut(), d.getHeureFin(), excludeId)) {
            throw new ServiceException("Chevauchement avec un autre créneau le même jour (tous créneaux confondus).");
        }
    }

    /** Chevauchement global : pas de filtre par médecin en base. */
    private boolean existeChevauchement(LocalDate date, LocalTime debut, LocalTime fin, Integer excludeId)
            throws ServiceException {
        String sql = """
                SELECT COUNT(*) FROM disponibilite
                WHERE date = ? AND status <> ?
                AND (? IS NULL OR id <> ?)
                AND heure_debut < ? AND heure_fin > ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setString(2, Disponibilite.STATUS_ANNULEE);
            if (excludeId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, excludeId);
                ps.setInt(4, excludeId);
            }
            ps.setTime(5, Time.valueOf(fin));
            ps.setTime(6, Time.valueOf(debut));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur contrôle de chevauchement.", e);
        }
        return false;
    }

    private Disponibilite mapDisponibilite(ResultSet rs) throws SQLException {
        Disponibilite d = new Disponibilite();
        d.setId(rs.getInt("id"));
        d.setDate(rs.getDate("date").toLocalDate());
        d.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        d.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        d.setStatus(rs.getString("status"));
        Timestamp ca = rs.getTimestamp("created_at");
        d.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        d.setMedecin(stubMedecinNonLie());
        return d;
    }

    private static User stubMedecinNonLie() {
        User u = new User();
        u.setId(0);
        u.setFullName("(médecin non stocké en BDD)");
        u.setEmail("");
        u.setRoles(Collections.emptyList());
        return u;
    }
}
