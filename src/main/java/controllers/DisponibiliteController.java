package controllers;

import exceptions.ServiceException;
import models.Disponibilite;
import services.DisponibiliteService;

import java.util.List;

public class DisponibiliteController {

    private final DisponibiliteService disponibiliteService;

    public DisponibiliteController() throws ServiceException {
        this.disponibiliteService = new DisponibiliteService();
    }

    public DisponibiliteController(DisponibiliteService disponibiliteService) {
        this.disponibiliteService = disponibiliteService;
    }

    public void ajouterDisponibilite(Disponibilite disponibilite) throws ServiceException {
        disponibiliteService.ajouter(disponibilite);
    }

    public void modifierDisponibilite(Disponibilite disponibilite) throws ServiceException {
        disponibiliteService.modifier(disponibilite);
    }

    public void supprimerDisponibilite(int id) throws ServiceException {
        disponibiliteService.supprimer(id);
    }

    /**
     * @return toutes les disponibilités (tous médecins), triées par date et heure.
     */
    public List<Disponibilite> afficherDisponibilites() throws ServiceException {
        return disponibiliteService.listerToutes();
    }

    /**
     * Filtre optionnel par médecin.
     */
    public List<Disponibilite> afficherDisponibilitesMedecin(int medecinId) throws ServiceException {
        return disponibiliteService.listerParMedecin(medecinId);
    }
}
