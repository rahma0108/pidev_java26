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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            throw new ServiceException("Connexion JDBC indisponible (vérifiez MySQL et application.properties).");
        }
        this.connection = connection;
        this.disponibiliteService = new DisponibiliteService(connection);
        this.userService = new UserService(connection);
    }

    /**
     * Réservation : vérifie LIBRE, crée le RDV, copie dateHeure depuis le début du créneau, passe la dispo en RESERVEE.
     */
    public RendezVous reserver(int disponibiliteId, int patientId, String motif) throws ServiceException {
        User patient = userService.findById(patientId)
                .orElseThrow(() -> new ServiceException("Patient introuvable."));
        if (!patient.hasRole(User.ROLE_PATIENT)) {
            throw new ServiceException("Seul un utilisateur avec le rôle PATIENT peut réserver.");
        }

        try {
            connection.setAutoCommit(false);
            Optional<Disponibilite> optDispo = disponibiliteService.findByIdForUpdate(connection, disponibiliteId);
            Disponibilite dispo = optDispo.orElseThrow(() -> new ServiceException("Disponibilité introuvable."));

            if (!Disponibilite.STATUS_LIBRE.equals(dispo.getStatus())) {
                throw new ServiceException("Cette disponibilité n'est plus libre.");
            }

            if (dispo.getDate().isBefore(LocalDate.now())) {
                throw new ServiceException("Impossible de réserver une disponibilité passée.");
            }

            if (patient.getMaxDaysAhead() != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), dispo.getDate());
                if (days > patient.getMaxDaysAhead()) {
                    throw new ServiceException("La date dépasse la fenêtre maxDaysAhead autorisée pour ce patient.");
                }
            }

            LocalDateTime dateHeure = dispo.toDateHeureDebut();
            LocalDateTime now = LocalDateTime.now();

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

            connection.commit();
            return rdv;
        } catch (SQLException e) {
            rollbackQuietly();
            throw new ServiceException("Erreur lors de la réservation.", e);
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

    /**
     * Annulation : supprime le rendez-vous et remet la disponibilité en LIBRE.
     */
    public void annuler(int rendezVousId) throws ServiceException {
        try {
            connection.setAutoCommit(false);
            Optional<RendezVous> opt = findByIdForUpdate(connection, rendezVousId);
            RendezVous rdv = opt.orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
            if (RendezVous.ANNULE.equals(rdv.getStatut())) {
                throw new ServiceException("Ce rendez-vous est déjà annulé.");
            }
            if (RendezVous.TERMINE.equals(rdv.getStatut())) {
                throw new ServiceException("Impossible d'annuler un rendez-vous terminé.");
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
                .orElseThrow(() -> new ServiceException("Médecin introuvable."));
        if (!medecin.hasRole(User.ROLE_MEDECIN)) {
            throw new ServiceException("Seul un médecin peut confirmer.");
        }
        try {
            connection.setAutoCommit(false);
            RendezVous rdv = findByIdForUpdate(connection, rendezVousId)
                    .orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
            User dispMed = rdv.getDisponibilite().getMedecin();
            if (dispMed != null && dispMed.getId() > 0 && dispMed.getId() != medecinId) {
                throw new ServiceException("Ce rendez-vous ne concerne pas ce médecin.");
            }
            if (!RendezVous.EN_ATTENTE.equals(rdv.getStatut())) {
                throw new ServiceException("Seuls les rendez-vous EN_ATTENTE peuvent être confirmés.");
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
                .orElseThrow(() -> new ServiceException("Médecin introuvable."));
        if (!medecin.hasRole(User.ROLE_MEDECIN)) {
            throw new ServiceException("Seul un médecin peut terminer un rendez-vous.");
        }
        try {
            connection.setAutoCommit(false);
            RendezVous rdv = findByIdForUpdate(connection, rendezVousId)
                    .orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
            User dispMed = rdv.getDisponibilite().getMedecin();
            if (dispMed != null && dispMed.getId() > 0 && dispMed.getId() != medecinId) {
                throw new ServiceException("Ce rendez-vous ne concerne pas ce médecin.");
            }
            if (!RendezVous.CONFIRME.equals(rdv.getStatut())) {
                throw new ServiceException("Seuls les rendez-vous CONFIRME peuvent être marqués TERMINE.");
            }
            updateStatut(connection, rendezVousId, RendezVous.TERMINE);
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new ServiceException("Erreur lors de la clôture du rendez-vous.", e);
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

    /**
     * Avis : RDV CONFIRME ou TERMINE, après la date/heure du RDV, une seule fois par RDV, par le patient concerné.
     */
    public Avis laisserAvis(int rendezVousId, int patientId, int note, String commentaire) throws ServiceException {
        if (note < 1 || note > 5) {
            throw new ServiceException("La note doit être entre 1 et 5.");
        }
        RendezVous rdv = findById(rendezVousId)
                .orElseThrow(() -> new ServiceException("Rendez-vous introuvable."));
        if (rdv.getPatient().getId() != patientId) {
            throw new ServiceException("Seul le patient du rendez-vous peut laisser un avis.");
        }
        if (!RendezVous.CONFIRME.equals(rdv.getStatut()) && !RendezVous.TERMINE.equals(rdv.getStatut())) {
            throw new ServiceException("Avis autorisé uniquement pour un rendez-vous CONFIRME ou TERMINE.");
        }
        if (!LocalDateTime.now().isAfter(rdv.getDateHeure())) {
            throw new ServiceException("L'avis n'est possible qu'après la date/heure du rendez-vous.");
        }
        if (avisExistePourRdv(rendezVousId)) {
            throw new ServiceException("Un avis existe déjà pour ce rendez-vous.");
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
        /* Sans medecin_id sur disponibilite, impossible de filtrer par médecin en SQL */
        String sql = baseSelectRdv() + " ORDER BY r.date_heure DESC ";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<RendezVous> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapRendezVous(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new ServiceException("Erreur chargement rendez-vous médecin.", e);
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
            throw new ServiceException("Erreur vérification avis existant.", e);
        }
        return false;
    }

    private String baseSelectRdv() {
        return """
                SELECT r.id r_id, r.date_heure, r.statut, r.motif, r.created_at r_created,
                       r.patient_id, r.disponibilite_id,
                       d.id d_id, d.date d_date, d.heure_debut, d.heure_fin, d.status d_status, d.created_at d_created,
                       CAST(NULL AS SIGNED) AS um_id,
                       CAST(NULL AS CHAR) AS um_name,
                       CAST(NULL AS CHAR) AS um_email,
                       CAST(NULL AS CHAR) AS um_roles,
                       CAST(NULL AS TIME) AS um_pref,
                       CAST(NULL AS SIGNED) AS um_max,
                       up.id up_id, up.full_name up_name, up.email up_email, up.roles up_roles,
                       up.preferred_time up_pref, up.max_days_ahead up_max
                FROM rendez_vous r
                JOIN disponibilite d ON d.id = r.disponibilite_id
                JOIN `user` up ON up.id = r.patient_id
                """;
    }

    private RendezVous mapRendezVous(ResultSet rs) throws SQLException {
        Integer umMax = (Integer) rs.getObject("um_max");
        Integer upMax = (Integer) rs.getObject("up_max");
        User medecin;
        if (rs.getObject("um_id") == null) {
            medecin = new User();
            medecin.setId(0);
            medecin.setFullName("(médecin non stocké en BDD)");
            medecin.setEmail("");
            medecin.setRoles(Collections.emptyList());
        } else {
            medecin = new User(
                    rs.getInt("um_id"),
                    rs.getString("um_name"),
                    rs.getString("um_email"),
                    User.parseRoles(rs.getString("um_roles")),
                    rs.getTime("um_pref") != null ? rs.getTime("um_pref").toLocalTime() : null,
                    umMax
            );
        }
        User patient = new User(
                rs.getInt("up_id"),
                rs.getString("up_name"),
                rs.getString("up_email"),
                User.parseRoles(rs.getString("up_roles")),
                rs.getTime("up_pref") != null ? rs.getTime("up_pref").toLocalTime() : null,
                upMax
        );
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
        return r;
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }
}
