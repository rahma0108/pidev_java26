package services;



import models.CampagneAide;

import utils.UrgenceCampagneFichierStore;



import java.io.IOException;

import java.util.List;



public final class CampagneAideService {



    public void publier(int demandeId, String titre, String corps) throws IOException {

        UrgenceCampagneFichierStore.publierCampagne(demandeId, titre, corps);

    }



    public List<CampagneAide> listerPourAccueil(int limite) throws IOException {

        return UrgenceCampagneFichierStore.listerCampagnesPourAccueil(limite);

    }

}

