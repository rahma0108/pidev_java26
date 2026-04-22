package services;

import models.UrgenceDemande;
import utils.MediLinkSessionUtil;
import utils.UrgenceCampagneFichierStore;

import java.io.IOException;
import java.util.List;

public final class UrgenceDemandeService {

    public int enregistrer(String message) throws IOException {
        return enregistrer(message, null);
    }

    public int enregistrer(String message, String pieceImagePath) throws IOException {
        return UrgenceCampagneFichierStore.enregistrerDemande(
                message, pieceImagePath, MediLinkSessionUtil.getOrCreateToken());
    }

    public List<UrgenceDemande> listerEnAttente() throws IOException {
        return UrgenceCampagneFichierStore.listerDemandesEnAttente();
    }

    /** Toutes les demandes (y compris traitées), pour l'écran admin. */
    public List<UrgenceDemande> listerPourAdmin() throws IOException {
        return UrgenceCampagneFichierStore.listerDemandesPourAdmin();
    }

    /** Demandes de l'utilisateur courant (même machine / session locale). */
    public List<UrgenceDemande> listerPourAuteurCourant() throws IOException {
        return UrgenceCampagneFichierStore.listerDemandesPourAuteur(MediLinkSessionUtil.getOrCreateToken());
    }

    public int compterEnAttente() throws IOException {
        return UrgenceCampagneFichierStore.compterEnAttente();
    }

    public void marquerSansCampagne(int id) throws IOException {
        UrgenceCampagneFichierStore.marquerSansCampagne(id);
    }

    public void mettreAJourParAuteurCourant(int demandeId, String nouveauMessage,
            boolean modifierPieceJointe, String nouveauCheminPieceAbsolu) throws IOException {
        UrgenceCampagneFichierStore.mettreAJourDemandeParAuteur(
                demandeId,
                MediLinkSessionUtil.getOrCreateToken(),
                nouveauMessage,
                modifierPieceJointe,
                nouveauCheminPieceAbsolu);
    }
}
