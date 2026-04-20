package services;



import models.UrgenceDemande;

import utils.UrgenceCampagneFichierStore;



import java.io.IOException;

import java.util.List;



public final class UrgenceDemandeService {



    public int enregistrer(String message) throws IOException {

        return UrgenceCampagneFichierStore.enregistrerDemande(message);

    }



    public List<UrgenceDemande> listerEnAttente() throws IOException {

        return UrgenceCampagneFichierStore.listerDemandesEnAttente();

    }

    /** Toutes les demandes (y compris traitées), pour l’écran admin. */
    public List<UrgenceDemande> listerPourAdmin() throws IOException {
        return UrgenceCampagneFichierStore.listerDemandesPourAdmin();
    }



    public int compterEnAttente() throws IOException {

        return UrgenceCampagneFichierStore.compterEnAttente();

    }



    public void marquerSansCampagne(int id) throws IOException {

        UrgenceCampagneFichierStore.marquerSansCampagne(id);

    }

}

