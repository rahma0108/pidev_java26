package org.example.service;

import org.example.dao.OrdonnanceDAO;
import org.example.models.Ordonnance;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class OrdonnanceService {
    private OrdonnanceDAO ordonnanceDAO;

    public OrdonnanceService() {
        this.ordonnanceDAO = new OrdonnanceDAO();
    }

    public void ajouterOrdonnance(Ordonnance ordonnance) throws SQLException {
        if (ordonnance.getDateCreation() == null) {
            ordonnance.setDateCreation(LocalDateTime.now());
        }
        if (ordonnance.getMedecinId() <= 0) {
            throw new IllegalArgumentException("L'ID du médecin est invalide");
        }
        if (ordonnance.getPatientId() <= 0) {
            throw new IllegalArgumentException("L'ID du patient est invalide");
        }
        ordonnanceDAO.create(ordonnance);
    }

    public List<Ordonnance> getAllOrdonnances() throws SQLException {
        return ordonnanceDAO.getAll();
    }

    public Ordonnance getOrdonnanceById(int id) throws SQLException {
        return ordonnanceDAO.getById(id);
    }

    public void modifierOrdonnance(Ordonnance ordonnance) throws SQLException {
        ordonnanceDAO.update(ordonnance);
    }

    public void supprimerOrdonnance(int id) throws SQLException {
        ordonnanceDAO.delete(id);
    }
}