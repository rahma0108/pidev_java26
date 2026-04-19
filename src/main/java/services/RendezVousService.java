package services;

import exceptions.ServiceException;
import models.Avis;
import models.Disponibilite;
import models.RendezVous;
import models.User;
import utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class RendezVousService {

    private final Connection connection;
    private final DisponibiliteService disponibiliteService;
    private final UserService userService;

    public RendezVousService() throws ServiceException {
        this(MyConnection.getInstance().getConn());
    }

    public RendezVousService(Connection connection) throws ServiceException {
        if (connection == null) {
            throw new ServiceException("Connexion JDBC indisponible (verifiez MySQL et application.properties).");
        }
        this.connection = connection;
        this.disponibiliteService = new DisponibiliteService(connection);
        this.userService = new UserService(connection);
    }

    public RendezVous reserver(int disponibiliteId, int patientId, String motif) throws ServiceException {
        User patient = userService.findById(patientId)
                .orElseThrow(() -> new ServiceException("Patient introuvable."));
        if (!patient.hasRole(User.ROLE_PATIENT)) {
            throw new ServiceException("Seul un utilisateur avec le role PATIENT peut reserver.");
        }

        try {
            connection.setAutoCommit(false);
            Optional<Disponibilite> optDispo = disponibiliteService.findByIdForUpdate(connection, disponibiliteId);
            Disponibilite dispo = optDispo.orElseThrow(() -> new ServiceException("Disponibilite introuvable."));

            if (!isStatutLibre(dispo.getStatus())) {
                throw new ServiceException("Cette disponibilite n'est plus libre.");
            }

            if (patient.getMaxDaysAhead() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), dispo.getDate());
                if (days > patient.getMaxDaysAhead()) {
                    throw new ServiceException("La date depasse la fenetre maxDaysAhead autorisee pour ce patient.");
                }
            }

            LocalDateTime dateHeure = dispo.toDateHeureDebut();
            LocalDateTime now = LocalDateTime.now();
            if (!dateHeure.isAfter(now)) {
                throw new ServiceException("Cette disponibilite est deja passee.");
            }
            if (rendezVousExistePourDisponibilite(connection, disponibiliteId)) {
                throw new ServiceException("Cette disponibilite est deja reservee.");
            }

            String insertRdv = """
                    INSERT INTO rendez_vous (disponibilite_id, date_heure, statut, motif, created_at, patient_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            RendezVous rdv = new RendezVous();
            try (PreparedStatement ps = connection.prepareStatement(insertRdv, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, disponibiliteId);
                ps.setTimestamp(2, Timestamp.valueOf(dateHeure));
                ps.setString(3, RendezVous.EN_ATTENTE);
                if (motif == null || motif.isBlank()) {
                    ps.setNull(4, Types.VARCHAR);
                } else {
                    ps.setString(4, motif);
                }
                ps.setTimestamp(5, Timestamp.valueOf(now));
                ps.setInt(6, patientId);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        rdv.setId(keys.getInt(1));
                    }
                }
            }

            disponibiliteService.updateStatus(connection, disponibiliteId, Disponibilite.STATUS_RESERVEE);

            rdv.setDisponibilite(dispo);
            rdv.setDateHeure(dateHeure);
            rdv.setStatut(RendezVous.EN_ATTENTE);
            rdv.setMotif(motif);
            rdv.setCreatedAt(now);
            rdv.setPatient(patient);
            dispo.setRendezVous(rdv);

            connection.commit();
            return rdv;
        } catch (SQLIntegrityConstraintViolationException e) {
            rollbackQuietly();
            throw new ServiceException("Cette disponibilite est deja reservee.", e);
        } catch (SQLException e) {
            rollbackQuietly();
            throw new ServiceException("Erreur lors de la reservation.", e);
        } catch (ServiceException e) {
            rollbackQuietly();
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void annuler(int rendezVousId) throws ServiceException {
        try {
            connection.setAutoCommit(false);
            Optional<RendezVous> opt = findByIdForUpdate(connection, rendezVousId);
            RendezVous rdv = opt.orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
            if (RendezVous.ANNULE.equals(rdv.getStatut())) {
                throw new ServiceException("Ce rendez-vous est deja annule.");
            }
            if (RendezVous.TERMINE.equals(rdv.getStatut())) {
                throw new ServiceException("Impossible d'annuler un rendez-vous termine.");
            }

            int dispoId = rdv.getDisponibilite().getId();

            try (PreparedStatement del = connection.prepareStatement("DELETE FROM rendez_vous WHERE id = ?")) {
                del.setInt(1, rendezVousId);
                int n = del.executeUpdate();
                if (n == 0) {
                    throw new ServiceException("Suppression impossible.");
                }
            }

            disponibiliteService.updateStatus(connection, dispoId, Disponibilite.STATUS_LIBRE);
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new ServiceException("Erreur lors de l'annulation.", e);
        } catch (ServiceException e) {
            rollbackQuietly();
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void confirmer(int rendezVousId, int medecinId) throws ServiceException {
        User medecin = userService.findById(medecinId)
                .orElseThrow(() -> new ServiceException("Medecin introuvable."));
        if (!medecin.hasRole(User.ROLE_MEDECIN)) {
            throw new ServiceException("Seul un medecin peut confirmer.");
        }
        try {
            connection.setAutoCommit(false);
            RendezVous rdv = findByIdForUpdate(connection, rendezVousId)
                    .orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
            User dispMed = rdv.getDisponibilite().getMedecin();
            if (dispMed != null && dispMed.getId() > 0 && dispMed.getId() != medecinId) {
                throw new ServiceException("Ce rendez-vous ne concerne pas ce medecin.");
            }
            if (!RendezVous.EN_ATTENTE.equals(rdv.getStatut())) {
                throw new ServiceException("Seuls les rendez-vous EN_ATTENTE peuvent etre confirmes.");
            }
            updateStatut(connection, rendezVousId, RendezVous.CONFIRME);
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new ServiceException("Erreur lors de la confirmation.", e);
        } catch (ServiceException e) {
            rollbackQuietly();
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public void terminer(int rendezVousId, int medecinId) throws ServiceException {
        User medecin = userService.findById(medecinId)
                .orElseThrow(() -> new ServiceException("Medecin introuvable."));
        if (!medecin.hasRole(User.ROLE_MEDECIN)) {
            throw new ServiceException("Seul un medecin peut terminer un rendez-vous.");
        }
        try {
            connection.setAutoCommit(false);
            RendezVous rdv = findByIdForUpdate(connection, rendezVousId)
                    .orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
            User dispMed = rdv.getDisponibilite().getMedecin();
            if (dispMed != null && dispMed.getId() > 0 && dispMed.getId() != medecinId) {
                throw new ServiceException("Ce rendez-vous ne concerne pas ce medecin.");
            }
            if (!RendezVous.EN_ATTENTE.equals(rdv.getStatut()) && !RendezVous.CONFIRME.equals(rdv.getStatut())) {
                throw new ServiceException("Seuls les rendez-vous EN_ATTENTE ou CONFIRME peuvent etre marques TERMINE.");
            }
            updateStatut(connection, rendezVousId, RendezVous.TERMINE);
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new ServiceException("Erreur lors de la cloture du rendez-vous.", e);
        } catch (ServiceException e) {
            rollbackQuietly();
            throw e;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    public Avis laisserAvis(int rendezVousId, int patientId, int note, String commentaire) throws ServiceException {
        if (note < 1 || note > 5) {
            throw new ServiceException("La note doit etre entre 1 et 5.");
        }
        RendezVous rdv = findById(rendezVousId)
                .orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
        if (rdv.getPatient() == null || rdv.getPatient().getId() != patientId) {
            throw new ServiceException("Seul le patient du rendez-vous peut laisser un avis.");
        }
        if (!RendezVous.CONFIRME.equals(rdv.getStatut()) && !RendezVous.TERMINE.equals(rdv.getStatut())) {
            throw new ServiceException("Avis autorise uniquement pour un rendez-vous CONFIRME ou TERMINE.");
        }
        if (!LocalDateTime.now().isAfter(rdv.getDateHeure())) {
            throw new ServiceException("L'avis n'est possible qu'apres la date/heure du rendez-vous.");
        }
        if (avisExistePourRdv(rendezVousId)) {
            throw new ServiceException("Un avis existe deja pour ce rendez-vous.");
        }

        LocalDateTime now = LocalDateTime.now();
        String sql = """
                INSERT INTO avis (rendez_vous_id, patient_id, note, commentaire, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rendezVousId);
            ps.setInt(2, patientId);
            ps.setInt(3, note);
            if (commentaire == null || commentaire.isBlank()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, commentaire);
            }
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.executeUpdate();
            Avis avis = new Avis();
            avis.setRendezVous(rdv);
            avis.setPatient(rdv.getPatient());
            avis.setNote(note);
            avis.setCommentaire(commentaire);
            avis.setCreatedAt(now);
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    avis.setId(keys.getInt(1));
                }
            }
            return avis;
        } catch (SQLException e) {
            throw new ServiceException("Erreur lors de l'enregistrement de l'avis.", e);
        }
    }

    public List<RendezVous> listerPourPatient(int patientId) throws ServiceException {
        String sql = baseSelectRdv() + " WHERE r.patient_id = ? ORDER BY r.date_heure DESC ";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RendezVous> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRendezVous(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur chargement rendez-vous patient.", e);
        }
    }

    public List<RendezVous> listerPourMedecin(int medecinId) throws ServiceException {
        String sql = baseSelectRdv() + " WHERE d.medecin_id = ? ORDER BY r.date_heure DESC ";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, medecinId);
            try (ResultSet rs = ps.executeQuery()) {
                List<RendezVous> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRendezVous(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur chargement rendez-vous medecin.", e);
        }
    }

    public Optional<RendezVous> findById(int id) throws ServiceException {
        String sql = baseSelectRdv() + " WHERE r.id = ? ";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRendezVous(rs));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lecture rendez-vous.", e);
        }
        return Optional.empty();
    }

    private Optional<RendezVous> findByIdForUpdate(Connection conn, int id) throws ServiceException {
        String sql = baseSelectRdv() + " WHERE r.id = ? FOR UPDATE ";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRendezVous(rs));
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur lecture rendez-vous (verrou).", e);
        }
        return Optional.empty();
    }

    private void updateStatut(Connection conn, int rendezVousId, String statut) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE rendez_vous SET statut = ? WHERE id = ?")) {
            ps.setString(1, statut);
            ps.setInt(2, rendezVousId);
            ps.executeUpdate();
        }
    }

    private boolean avisExistePourRdv(int rendezVousId) throws ServiceException {
        String sql = "SELECT COUNT(*) FROM avis WHERE rendez_vous_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, rendezVousId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur verification avis existant.", e);
        }
        return false;
    }

    private boolean rendezVousExistePourDisponibilite(Connection conn, int disponibiliteId) throws ServiceException {
        String sql = "SELECT COUNT(*) FROM rendez_vous WHERE disponibilite_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, disponibiliteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur verification reservation existante.", e);
        }
        return false;
    }

    private String baseSelectRdv() {
        return """
                SELECT r.id r_id, r.date_heure, r.statut, r.motif, r.created_at r_created,
                       r.patient_id, r.disponibilite_id,
                       d.id d_id, d.date d_date, d.heure_debut, d.heure_fin, d.status d_status, d.created_at d_created,
                       d.medecin_id AS um_id,
                       CASE
                           WHEN d.medecin_id IS NULL THEN '(medecin non assigne)'
                           ELSE CONCAT('Medecin #', d.medecin_id)
                       END AS um_name,
                       CAST(NULL AS CHAR) AS um_email,
                       CAST(NULL AS CHAR) AS um_roles,
                       CAST(NULL AS TIME) AS um_pref,
                       CAST(NULL AS SIGNED) AS um_max,
                       up.id up_id, up.full_name up_name, up.email up_email, up.roles up_roles,
                       up.preferred_time up_pref, up.max_days_ahead up_max
                FROM rendez_vous r
                JOIN disponibilites d ON d.id = r.disponibilite_id
                LEFT JOIN `user` up ON up.id = r.patient_id
                """;
    }

    private RendezVous mapRendezVous(ResultSet rs) throws SQLException {
        Integer umMax = (Integer) rs.getObject("um_max");
        Integer upMax = (Integer) rs.getObject("up_max");
        User medecin;
        if (rs.getObject("um_id") == null) {
            medecin = new User();
            medecin.setId(0);
            medecin.setFullName("(medecin non stocke en BDD)");
            medecin.setEmail("");
            medecin.setRoles(Collections.emptyList());
        } else {
            medecin = new User(
                    rs.getInt("um_id"),
                    rs.getString("um_name"),
                    rs.getString("um_email"),
                    User.parseRoles(rs.getString("um_roles")),
                    parsePreferredTime(rs, "um_pref"),
                    umMax
            );
        }

        User patient = null;
        if (rs.getObject("up_id") != null) {
            patient = new User(
                    rs.getInt("up_id"),
                    rs.getString("up_name"),
                    rs.getString("up_email"),
                    User.parseRoles(rs.getString("up_roles")),
                    parsePreferredTime(rs, "up_pref"),
                    upMax
            );
        }

        Disponibilite d = new Disponibilite();
        d.setId(rs.getInt("d_id"));
        d.setDate(rs.getDate("d_date").toLocalDate());
        d.setHeureDebut(rs.getTime("heure_debut").toLocalTime());
        d.setHeureFin(rs.getTime("heure_fin").toLocalTime());
        d.setStatus(rs.getString("d_status"));
        Timestamp dca = rs.getTimestamp("d_created");
        d.setCreatedAt(dca != null ? dca.toLocalDateTime() : null);
        d.setMedecin(medecin);

        RendezVous r = new RendezVous();
        r.setId(rs.getInt("r_id"));
        r.setDisponibilite(d);
        r.setDateHeure(rs.getTimestamp("date_heure").toLocalDateTime());
        r.setStatut(rs.getString("statut"));
        r.setMotif(rs.getString("motif"));
        Timestamp rca = rs.getTimestamp("r_created");
        r.setCreatedAt(rca != null ? rca.toLocalDateTime() : null);
        r.setPatient(patient);
        d.setRendezVous(r);
        return r;
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private static LocalTime parsePreferredTime(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static boolean isStatutLibre(String statut) {
        if (statut == null) {
            return false;
        }
        String normalized = statut.trim().toUpperCase();
        return normalized.equals("LIBRE") || normalized.equals("STATUS_LIBRE");
    }
}
