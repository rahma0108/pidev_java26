package esprit.tn.pidev;

import controllers.DisponibiliteController;
import controllers.RendezVousController;
import exceptions.ServiceException;

/**
 * Test console JDBC / contrôleurs métier uniquement (aucune fenêtre).
 * Pour l'interface graphique, lancer {@link MediLinkFxApp}.
 */
public class MediLinkApp {

    public static void main(String[] args) {
        try {
            utils.MyConnection.getInstance();
            new DisponibiliteController();
            new RendezVousController();
            System.out.println("MediLink : connexion OK, contrôleurs initialisés.");
            System.out.println("Branche ton UI (JavaFX, etc.) sur DisponibiliteController et RendezVousController.");
        } catch (ServiceException e) {
            System.err.println("Erreur : " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            System.exit(1);
        }
    }
}
