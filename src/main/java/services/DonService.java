package services;

import interfaces.IService;
import models.Don;
import utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DonService implements IService<Don> {

    Connection conn;

    public DonService() {
        conn = MyConnection.getInstance().getConn();
    }

    private void ensureConn() throws SQLException {
        if (conn == null) {
            throw new SQLException(
                    "Pas de connexion à la base. Vérifiez que MySQL est démarré, que la base « medilink » existe "
                            + "et les paramètres dans application.properties (URL, utilisateur, mot de passe).");
        }
    }

    @Override
    public void add(Don d) throws SQLException {
        ensureConn();
        String req = "INSERT INTO dons (categorie_id, article_description, quantite, unite, details_supplementaires, etat, niveau_urgence, statut, date_expiration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, d.getCategorieId());
        ps.setString(2, d.getArticleDescription());
        ps.setInt(3, d.getQuantite());
        ps.setString(4, d.getUnite());
        ps.setString(5, d.getDetailsSupplementaires());
        ps.setString(6, d.getEtat());
        ps.setString(7, d.getNiveauUrgence());
        ps.setString(8, "en_attente");
        ps.setDate(9, d.getDateExpiration());
        ps.executeUpdate();
        System.out.println("Don ajouté !");
    }

    @Override
    public List<Don> getAll() throws SQLException {
        ensureConn();
        List<Don> list = new ArrayList<>();
        String req = "SELECT * FROM dons";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
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
            list.add(d);
        }
        return list;
    }

    @Override
    public void update(Don d) throws SQLException {
        ensureConn();
        String req = "UPDATE dons SET categorie_id=?, article_description=?, quantite=?, unite=?, details_supplementaires=?, etat=?, niveau_urgence=?, statut=?, date_expiration=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(req);
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
        System.out.println("Don modifié !");
    }

    @Override
    public void delete(int id) throws SQLException {
        ensureConn();
        String req = "DELETE FROM dons WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Don supprimé !");
    }
}