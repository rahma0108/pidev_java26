package services;

import interfaces.IService;
import models.Don;
import utils.MyConnection;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonService implements IService<Don> {
    private static final String TABLE_DONS = "dons";
    private static final String TABLE_PAIEMENTS = "paiements_dons";

    Connection conn;
    private volatile boolean schemaVerifiee = false;

    public DonService() {
        conn = MyConnection.getInstance().getConn();
    }

    private static PreparedStatement preparedStatementInsertAvecCles(Connection conn, String sql) throws SQLException {
        try {
            return conn.prepareStatement(sql, new String[]{"id"});
        } catch (SQLException e) {
            return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
    }

    /** Clés générées : parcourt toutes les colonnes du ResultSet (selon drivers), puis LAST_INSERT_ID(). */
    private static int lireIdGenereMySql(PreparedStatement ps, Connection conn) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                ResultSetMetaData md = keys.getMetaData();
                for (int c = 1; c <= md.getColumnCount(); c++) {
                    long v = keys.getLong(c);
                    if (v > 0) {
                        return (int) v;
                    }
                }
            }
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            if (rs.next()) {
                long v = rs.getLong(1);
                if (v > 0) {
                    return (int) v;
                }
            }
        }
        return 0;
    }

    private void ensureConn() throws SQLException {
        conn = MyConnection.getInstance().getConn();
        if (conn == null) {
            String detail = MyConnection.getInstance().getLastErrorMessage();
            String urlActive = MyConnection.getInstance().getActiveUrl();
            throw new SQLException(
                    "Pas de connexion à la base. Vérifiez que MySQL est démarré, que la base « medilink » existe "
                            + "et les paramètres dans application.properties (URL, utilisateur, mot de passe)."
                            + (urlActive == null || urlActive.isBlank() ? "" : " URL testée: " + urlActive + ".")
                            + (detail == null || detail.isBlank() ? "" : " Détail pilote JDBC: " + detail));
        }
        verifierSchemaSiNecessaire();
    }

    private synchronized void verifierSchemaSiNecessaire() throws SQLException {
        if (schemaVerifiee) {
            return;
        }
        creerTableDonsSiNecessaire();
        creerTablePaiementsSiNecessaire();
        try (Statement st = conn.createStatement()) {
            st.executeQuery("SELECT 1 FROM " + TABLE_DONS + " LIMIT 1");
            schemaVerifiee = true;
        } catch (SQLException ex) {
            // MySQL: "Table '...dons' doesn't exist in engine" (souvent code 1932)
            // table présente en métadonnées mais absente/corrompue côté moteur.
            if (estTableInvalide(ex)) {
                reparerTableDons();
                schemaVerifiee = true;
                return;
            }
            throw ex;
        }
    }

    private void creerTableDonsSiNecessaire() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE_DONS + " ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "categorie_id INT NOT NULL,"
                + "article_description VARCHAR(255) NOT NULL,"
                + "quantite INT NOT NULL,"
                + "unite VARCHAR(50) NOT NULL,"
                + "details_supplementaires TEXT,"
                + "etat VARCHAR(50) NOT NULL,"
                + "niveau_urgence VARCHAR(50) NOT NULL,"
                + "statut VARCHAR(50) NOT NULL DEFAULT 'en_attente',"
                + "date_expiration DATE,"
                + "date_soumission DATE"
                + ")";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        }
    }

    private void creerTablePaiementsSiNecessaire() throws SQLException {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE_PAIEMENTS + " ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "don_id INT NOT NULL,"
                + "provider VARCHAR(30) NOT NULL,"
                + "montant_tnd INT NOT NULL,"
                + "devise VARCHAR(10) NOT NULL,"
                + "stripe_checkout_session_id VARCHAR(255) NOT NULL,"
                + "checkout_status VARCHAR(50) NOT NULL,"
                + "payment_status VARCHAR(50) NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uq_stripe_checkout_session (stripe_checkout_session_id),"
                + "CONSTRAINT fk_paiements_don FOREIGN KEY (don_id) REFERENCES " + TABLE_DONS + "(id) ON DELETE CASCADE"
                + ")";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        }
    }

    private void reparerTableDons() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS " + TABLE_DONS);
        }
        creerTableDonsSiNecessaire();
    }

    private static boolean estTableInvalide(SQLException ex) {
        if (ex == null) {
            return false;
        }
        if (ex.getErrorCode() == 1932) {
            return true;
        }
        String msg = ex.getMessage();
        return msg != null && msg.toLowerCase().contains("doesn't exist in engine");
    }

    @Override
    public void add(Don d) throws SQLException {
        ensureConn();
        String req = "INSERT INTO " + TABLE_DONS + " (categorie_id, article_description, quantite, unite, details_supplementaires, etat, niveau_urgence, statut, date_expiration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = preparedStatementInsertAvecCles(conn, req)) {
            ps.setInt(1, d.getCategorieId());
            ps.setString(2, d.getArticleDescription());
            ps.setInt(3, d.getQuantite());
            ps.setString(4, d.getUnite());
            ps.setString(5, d.getDetailsSupplementaires());
            ps.setString(6, d.getEtat());
            ps.setString(7, d.getNiveauUrgence());
            ps.setString(8, d.getStatut() != null ? d.getStatut() : "en_attente");
            ps.setDate(9, d.getDateExpiration());
            ps.executeUpdate();
            int newId = lireIdGenereMySql(ps, conn);
            if (newId <= 0) {
                throw new SQLException(
                        "Enregistrement du don incomplet côté base : impossible d’associer l’avis IA. "
                                + "Vérifiez la table dons (clé primaire AUTO_INCREMENT).");
            }
            d.setId(newId);
        }
        System.out.println("Don ajouté !");
    }

    public void addPaiementDonStripe(
            int donId,
            int montantTnd,
            String checkoutSessionId,
            String checkoutStatus,
            String paymentStatus) throws SQLException {
        ensureConn();
        String req = "INSERT INTO " + TABLE_PAIEMENTS
                + " (don_id, provider, montant_tnd, devise, stripe_checkout_session_id, checkout_status, payment_status)"
                + " VALUES (?, 'stripe', ?, 'TND', ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, donId);
            ps.setInt(2, montantTnd);
            ps.setString(3, checkoutSessionId);
            ps.setString(4, checkoutStatus == null ? "" : checkoutStatus);
            ps.setString(5, paymentStatus == null ? "" : paymentStatus);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Don> getAll() throws SQLException {
        ensureConn();
        List<Don> list = new ArrayList<>();
        String req = "SELECT * FROM " + TABLE_DONS;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Don d = mapperDepuisResultSet(rs);
                try {
                    DonAvisStockageFichier.fusionnerVersDon(d);
                } catch (IOException ignored) {
                    // fichier absent ou illisible : pas d’avis IA
                }
                list.add(d);
            }
        }
        return list;
    }

    private static Don mapperDepuisResultSet(ResultSet rs) throws SQLException {
        Don d = new Don();
        d.setId(rs.getInt("id"));
        d.setCategorieId(rs.getInt("categorie_id"));
        d.setArticleDescription(rs.getString("article_description"));
        d.setQuantite(rs.getInt("quantite"));
        d.setUnite(rs.getString("unite"));
        d.setDetailsSupplementaires(rs.getString("details_supplementaires"));
        d.setEtat(rs.getString("etat"));
        d.setNiveauUrgence(rs.getString("niveau_urgence"));
        d.setStatut(rs.getString("statut"));
        d.setDateExpiration(rs.getDate("date_expiration"));
        d.setDateSoumission(rs.getDate("date_soumission"));
        return d;
    }

    @Override
    public void update(Don d) throws SQLException {
        ensureConn();
        String req = "UPDATE " + TABLE_DONS + " SET categorie_id=?, article_description=?, quantite=?, unite=?, details_supplementaires=?, etat=?, niveau_urgence=?, statut=?, date_expiration=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(req)) {
            ps.setInt(1, d.getCategorieId());
            ps.setString(2, d.getArticleDescription());
            ps.setInt(3, d.getQuantite());
            ps.setString(4, d.getUnite());
            ps.setString(5, d.getDetailsSupplementaires());
            ps.setString(6, d.getEtat());
            ps.setString(7, d.getNiveauUrgence());
            ps.setString(8, d.getStatut());
            ps.setDate(9, d.getDateExpiration());
            ps.setInt(10, d.getId());
            ps.executeUpdate();
        }
        try {
            DonAvisStockageFichier.enregistrer(d);
        } catch (IOException ex) {
            throw new SQLException("Impossible d’enregistrer l’avis IA local (~/.gdons/).", ex);
        }
        System.out.println("Don modifié !");
    }

    @Override
    public void delete(int id) throws SQLException {
        ensureConn();
        String req = "DELETE FROM " + TABLE_DONS + " WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
        try {
            DonAvisStockageFichier.supprimer(id);
        } catch (IOException ex) {
            throw new SQLException("Don supprimé en base, mais le fichier d’avis IA local n’a pas pu être effacé.", ex);
        }
        System.out.println("Don supprimé !");
    }

    /** Passe le statut à « valide » uniquement si le don est encore « en attente ». */
    public boolean validerDon(int id) throws SQLException {
        ensureConn();
        String req = "UPDATE " + TABLE_DONS + " SET statut='valide' WHERE id=? AND statut='en_attente'";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);
        int n = ps.executeUpdate();
        if (n > 0) {
            System.out.println("Don validé !");
        }
        return n > 0;
    }

    /** Passe le statut à « rejete » uniquement si le don est encore « en attente ». */
    public boolean rejeterDon(int id) throws SQLException {
        ensureConn();
        String req = "UPDATE " + TABLE_DONS + " SET statut='rejete' WHERE id=? AND statut='en_attente'";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);
        int n = ps.executeUpdate();
        if (n > 0) {
            System.out.println("Don rejeté !");
        }
        return n > 0;
    }
}
