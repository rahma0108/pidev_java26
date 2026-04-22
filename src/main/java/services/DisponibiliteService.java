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
 * Persistance alignée sur la table Symfony {@code disponibilites}.
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
                INSERT INTO disponibilites (date, heure_debut, heure_fin, status, created_at, medecin_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(d.getDate()));
            ps.setTime(2, Time.valueOf(d.getHeureDebut()));
            ps.setTime(3, Time.valueOf(d.getHeureFin()));
            ps.setString(4, toDbStatus(Disponibilite.STATUS_LIBRE));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setInt(6, d.getMedecin().getId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    d.setId(keys.getInt(1));
                }
            }
            d.setStatus(toAppStatus(toDbStatus(Disponibilite.STATUS_LIBRE)));
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
                UPDATE disponibilites
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
        String sql = "DELETE FROM disponibilites WHERE id = ?";
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
     * Retourne uniquement les créneaux réellement réservables :
     * - statut LIBRE
     * - date/heure de début strictement dans le futur.
     */
    public List<Disponibilite> listerReservables() throws ServiceException {
        String sql = """
                SELECT d.id, d.date, d.heure_debut, d.heure_fin, d.status, d.created_at, d.medecin_id,
                       um.id AS um_id, um.full_name AS um_name, um.email AS um_email, um.roles AS um_roles,
                       um.preferred_time AS um_pref, um.max_days_ahead AS um_max,
                       r.id AS rdv_id, r.statut AS rdv_statut
                FROM disponibilites d
                LEFT JOIN `user` um ON um.id = d.medecin_id
                LEFT JOIN rendez_vous r ON r.disponibilite_id = d.id
                WHERE UPPER(d.status) IN ('LIBRE', 'STATUS_LIBRE')
                  AND (d.date > ? OR (d.date = ? AND d.heure_debut > ?))
                ORDER BY d.date, d.heure_debut
                """;
        LocalDate nowDate = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(nowDate));
            ps.setDate(2, Date.valueOf(nowDate));
            ps.setTime(3, Time.valueOf(nowTime));
            try (ResultSet rs = ps.executeQuery()) {
                List<Disponibilite> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapDisponibilite(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors du chargement des créneaux réservables.", e);
        }
    }

    /**
     * Filtre optionnel par médecin (si {@code medecinId} fourni).
     */
    public List<Disponibilite> listerParMedecin(Integer medecinId) throws ServiceException {
        String sql = medecinId == null
                ? """
                SELECT d.id, d.date, d.heure_debut, d.heure_fin, d.status, d.created_at, d.medecin_id,
                       um.id AS um_id, um.full_name AS um_name, um.email AS um_email, um.roles AS um_roles,
                       um.preferred_time AS um_pref, um.max_days_ahead AS um_max,
                       r.id AS rdv_id, r.statut AS rdv_statut
                FROM disponibilites d
                LEFT JOIN `user` um ON um.id = d.medecin_id
                LEFT JOIN rendez_vous r ON r.disponibilite_id = d.id
                ORDER BY d.date, d.heure_debut
                """
                : """
                SELECT d.id, d.date, d.heure_debut, d.heure_fin, d.status, d.created_at, d.medecin_id,
                       um.id AS um_id, um.full_name AS um_name, um.email AS um_email, um.roles AS um_roles,
                       um.preferred_time AS um_pref, um.max_days_ahead AS um_max,
                       r.id AS rdv_id, r.statut AS rdv_statut
                FROM disponibilites d
                LEFT JOIN `user` um ON um.id = d.medecin_id
                LEFT JOIN rendez_vous r ON r.disponibilite_id = d.id
                WHERE d.medecin_id = ?
                ORDER BY d.date, d.heure_debut
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (medecinId != null) {
                ps.setInt(1, medecinId);
            }
            try (ResultSet rs = ps.executeQuery()) {
            List<Disponibilite> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapDisponibilite(rs));
            }
            return list;
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors du chargement des disponibilités.", e);
        }
    }

    public Optional<Disponibilite> findById(int id) throws ServiceException {
        String sql = """
                SELECT d.id, d.date, d.heure_debut, d.heure_fin, d.status, d.created_at, d.medecin_id,
                       um.id AS um_id, um.full_name AS um_name, um.email AS um_email, um.roles AS um_roles,
                       um.preferred_time AS um_pref, um.max_days_ahead AS um_max,
                       r.id AS rdv_id, r.statut AS rdv_statut
                FROM disponibilites d
                LEFT JOIN `user` um ON um.id = d.medecin_id
                LEFT JOIN rendez_vous r ON r.disponibilite_id = d.id
                WHERE d.id = ?
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
                SELECT d.id, d.date, d.heure_debut, d.heure_fin, d.status, d.created_at, d.medecin_id,
                       um.id AS um_id, um.full_name AS um_name, um.email AS um_email, um.roles AS um_roles,
                       um.preferred_time AS um_pref, um.max_days_ahead AS um_max,
                       r.id AS rdv_id, r.statut AS rdv_statut
                FROM disponibilites d
                LEFT JOIN `user` um ON um.id = d.medecin_id
                LEFT JOIN rendez_vous r ON r.disponibilite_id = d.id
                WHERE d.id = ?
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
        String sql = "UPDATE disponibilites SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, toDbStatus(status));
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
        if (existeChevauchement(d.getDate(), d.getHeureDebut(), d.getHeureFin(), d.getMedecin().getId(), excludeId)) {
            throw new ServiceException("Chevauchement avec un autre créneau du même médecin le même jour.");
        }
    }

    /** Chevauchement par médecin et par jour. */
    private boolean existeChevauchement(LocalDate date, LocalTime debut, LocalTime fin, int medecinId, Integer excludeId)
            throws ServiceException {
        String sql = """
                SELECT COUNT(*) FROM disponibilites
                WHERE date = ?
                AND medecin_id = ?
                AND UPPER(status) NOT IN ('ANNULEE', 'STATUS_ANNULEE')
                AND (? IS NULL OR id <> ?)
                AND heure_debut < ? AND heure_fin > ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            ps.setInt(2, medecinId);
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
        d.setStatus(toAppStatus(rs.getString("status")));
        Timestamp ca = rs.getTimestamp("created_at");
        d.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        d.setMedecin(mapMedecin(rs));
        Integer rendezVousId = (Integer) rs.getObject("rdv_id");
        if (rendezVousId != null) {
            models.RendezVous rendezVous = new models.RendezVous();
            rendezVous.setId(rendezVousId);
            rendezVous.setStatut(rs.getString("rdv_statut"));
            rendezVous.setDisponibilite(d);
            d.setRendezVous(rendezVous);
        }
        return d;
    }

    private static User mapMedecin(ResultSet rs) throws SQLException {
        if (rs.getObject("um_id") == null) {
            User u = new User();
            u.setId(0);
            u.setFullName("(medecin non assigne)");
            u.setEmail("");
            u.setRoles(Collections.emptyList());
            return u;
        }

        Integer maxDaysAhead = (Integer) rs.getObject("um_max");
        return new User(
                rs.getInt("um_id"),
                rs.getString("um_name"),
                rs.getString("um_email"),
                User.parseRoles(rs.getString("um_roles")),
                parsePreferredTime(rs, "um_pref"),
                maxDaysAhead
        );
    }

    private static LocalTime parsePreferredTime(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String toDbStatus(String appStatus) {
        if (appStatus == null) {
            return "libre";
        }
        String normalized = appStatus.trim().toUpperCase();
        return switch (normalized) {
            case "STATUS_RESERVEE", "RESERVEE", "RESERVEEE", "RESERVE" -> "reservee";
            case "STATUS_ANNULEE", "ANNULEE" -> "annulee";
            default -> "libre";
        };
    }

    private static String toAppStatus(String dbStatus) {
        if (dbStatus == null) {
            return Disponibilite.STATUS_LIBRE;
        }
        String normalized = dbStatus.trim().toUpperCase();
        if (normalized.equals("RESERVEE") || normalized.equals("STATUS_RESERVEE")) {
            return Disponibilite.STATUS_RESERVEE;
        }
        if (normalized.equals("ANNULEE") || normalized.equals("STATUS_ANNULEE")) {
            return Disponibilite.STATUS_ANNULEE;
        }
        return Disponibilite.STATUS_LIBRE;
    }
}
