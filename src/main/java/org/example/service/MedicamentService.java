package org.example.service;

import org.example.dao.MedicamentDAO;
import org.example.models.Medicament;
import java.sql.SQLException;
import java.util.List;

public class MedicamentService {
    private MedicamentDAO medicamentDAO;

    public MedicamentService() {
        this.medicamentDAO = new MedicamentDAO();
    }

    public void ajouterMedicament(Medicament medicament) throws SQLException {
        if (medicament.getNom() == null || medicament.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du médicament est obligatoire");
        }
        if (medicament.getQuantiteStock() < 0) {
            throw new IllegalArgumentException("La quantité ne peut pas être négative");
        }
        medicamentDAO.create(medicament);
    }

    public List<Medicament> getAllMedicaments() throws SQLException {
        return medicamentDAO.getAll();
    }

    public Medicament getMedicamentById(int id) throws SQLException {
        return medicamentDAO.getById(id);
    }

    public void modifierMedicament(Medicament medicament) throws SQLException {
        medicamentDAO.update(medicament);
    }

    public void supprimerMedicament(int id) throws SQLException {
        medicamentDAO.delete(id);
    }
}