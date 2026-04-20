package controllers;

import exceptions.ServiceException;
import models.Avis;
import models.RendezVous;
import services.RendezVousService;

import java.util.List;

public class RendezVousController {

    private final RendezVousService rendezVousService;

    public RendezVousController() throws ServiceException {
        this.rendezVousService = new RendezVousService();
    }

    public RendezVousController(RendezVousService rendezVousService) {
        this.rendezVousService = rendezVousService;
    }

    public RendezVous reserverRendezVous(int disponibiliteId, int patientId, String motif) throws ServiceException {
        return rendezVousService.reserver(disponibiliteId, patientId, motif);
    }

    public void annulerRendezVous(int rendezVousId) throws ServiceException {
        rendezVousService.annuler(rendezVousId);
    }

    public void confirmerRendezVous(int rendezVousId, int medecinId) throws ServiceException {
        rendezVousService.confirmer(rendezVousId, medecinId);
    }

    public void terminerRendezVous(int rendezVousId, int medecinId) throws ServiceException {
        rendezVousService.terminer(rendezVousId, medecinId);
    }

    /**
     * Règles patient : RDV CONFIRME ou TERMINE, après la date, pas déjà noté.
     */
    public Avis laisserAvis(int rendezVousId, int patientId, int note, String commentaire) throws ServiceException {
        return rendezVousService.laisserAvis(rendezVousId, patientId, note, commentaire);
    }

    public List<RendezVous> listerTousRendezVous() throws ServiceException {
        return rendezVousService.listerTous();
    }

    public List<RendezVous> listerPourPatient(int patientId) throws ServiceException {
        return rendezVousService.listerPourPatient(patientId);
    }

    public List<RendezVous> listerPourMedecin(int medecinId) throws ServiceException {
        return rendezVousService.listerPourMedecin(medecinId);
    }
}
