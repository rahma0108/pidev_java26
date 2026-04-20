package utils;

import models.CampagneAide;
import models.UrgenceDemande;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Stockage local des demandes d'urgence et des campagnes publiées (aucune table MySQL).
 * Fichier : {@code ~/.medilink/urgence_campagnes.store}
 */
public final class UrgenceCampagneFichierStore {

    private static final ReentrantLock LOCK = new ReentrantLock();

    private UrgenceCampagneFichierStore() {
    }

    private static final class Snapshot implements Serializable {
        private static final long serialVersionUID = 2L;
        ArrayList<DemandeSer> demandes = new ArrayList<>();
        ArrayList<CampagneSer> campagnes = new ArrayList<>();
        int nextDemandeId = 1;
        int nextCampagneId = 1;
    }

    private static final class DemandeSer implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        String message;
        String statut;
        long createdAtEpochMilli;
    }

    private static final class CampagneSer implements Serializable {
        private static final long serialVersionUID = 1L;
        int id;
        int demandeId;
        String titre;
        String corps;
        long createdAtEpochMilli;
    }

    private static Path fichier() {
        return Path.of(System.getProperty("user.home"), ".medilink", "urgence_campagnes.store");
    }

    private static void ensureNextIds(Snapshot s) {
        int md = 0;
        int mc = 0;
        for (DemandeSer d : s.demandes) {
            md = Math.max(md, d.id);
        }
        for (CampagneSer c : s.campagnes) {
            mc = Math.max(mc, c.id);
        }
        s.nextDemandeId = Math.max(s.nextDemandeId, md + 1);
        s.nextCampagneId = Math.max(s.nextCampagneId, mc + 1);
    }

    private static Snapshot loadOrCreate() throws IOException {
        Path p = fichier();
        if (!Files.isRegularFile(p)) {
            return new Snapshot();
        }
        try (InputStream in = Files.newInputStream(p);
             ObjectInputStream ois = new ObjectInputStream(in)) {
            Object o = ois.readObject();
            if (o instanceof Snapshot snap) {
                if (snap.demandes == null) {
                    snap.demandes = new ArrayList<>();
                }
                if (snap.campagnes == null) {
                    snap.campagnes = new ArrayList<>();
                }
                ensureNextIds(snap);
                return snap;
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Données urgences illisibles (fichier corrompu ou version incompatible).", e);
        }
        throw new IOException("Données urgences invalides.");
    }

    private static void save(Snapshot s) throws IOException {
        Path p = fichier();
        Files.createDirectories(p.getParent());
        Path tmp = p.resolveSibling("urgence_campagnes.store.tmp");
        try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(s);
        }
        Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
    }

    public static int enregistrerDemande(String message) throws IOException {
        LOCK.lock();
        try {
            Snapshot s = loadOrCreate();
            DemandeSer d = new DemandeSer();
            d.id = s.nextDemandeId++;
            d.message = message;
            d.statut = "en_attente";
            d.createdAtEpochMilli = System.currentTimeMillis();
            s.demandes.add(d);
            save(s);
            return d.id;
        } finally {
            LOCK.unlock();
        }
    }

    public static List<UrgenceDemande> listerDemandesEnAttente() throws IOException {
        LOCK.lock();
        try {
            Snapshot s = loadOrCreate();
            return s.demandes.stream()
                    .filter(x -> "en_attente".equals(x.statut))
                    .sorted(Comparator.comparingLong(x -> x.createdAtEpochMilli))
                    .map(UrgenceCampagneFichierStore::toDemande)
                    .collect(Collectors.toList());
        } finally {
            LOCK.unlock();
        }
    }

    public static int compterEnAttente() throws IOException {
        return listerDemandesEnAttente().size();
    }

    /**
     * Toutes les demandes pour l’admin : en attente d’abord (FIFO), puis traitées (plus récentes en premier).
     */
    public static List<UrgenceDemande> listerDemandesPourAdmin() throws IOException {
        LOCK.lock();
        try {
            Snapshot s = loadOrCreate();
            return s.demandes.stream()
                    .map(UrgenceCampagneFichierStore::toDemande)
                    .sorted((a, b) -> {
                        boolean aWait = "en_attente".equals(a.getStatut());
                        boolean bWait = "en_attente".equals(b.getStatut());
                        if (aWait != bWait) {
                            return aWait ? -1 : 1;
                        }
                        long ta = a.getCreatedAt() != null ? a.getCreatedAt().getTime() : 0L;
                        long tb = b.getCreatedAt() != null ? b.getCreatedAt().getTime() : 0L;
                        if (aWait) {
                            return Long.compare(ta, tb);
                        }
                        return Long.compare(tb, ta);
                    })
                    .collect(Collectors.toList());
        } finally {
            LOCK.unlock();
        }
    }

    public static void marquerSansCampagne(int demandeId) throws IOException {
        LOCK.lock();
        try {
            Snapshot s = loadOrCreate();
            for (DemandeSer d : s.demandes) {
                if (d.id == demandeId && "en_attente".equals(d.statut)) {
                    d.statut = "sans_campagne";
                    break;
                }
            }
            save(s);
        } finally {
            LOCK.unlock();
        }
    }

    public static void publierCampagne(int demandeId, String titre, String corps) throws IOException {
        LOCK.lock();
        try {
            Snapshot s = loadOrCreate();
            DemandeSer cible = null;
            for (DemandeSer d : s.demandes) {
                if (d.id == demandeId && "en_attente".equals(d.statut)) {
                    cible = d;
                    break;
                }
            }
            if (cible == null) {
                throw new IOException("Demande introuvable ou déjà traitée.");
            }
            CampagneSer c = new CampagneSer();
            c.id = s.nextCampagneId++;
            c.demandeId = demandeId;
            c.titre = titre;
            c.corps = corps;
            c.createdAtEpochMilli = System.currentTimeMillis();
            s.campagnes.add(c);
            cible.statut = "campagne_publiee";
            save(s);
        } finally {
            LOCK.unlock();
        }
    }

    public static List<CampagneAide> listerCampagnesPourAccueil(int limite) throws IOException {
        LOCK.lock();
        try {
            Snapshot s = loadOrCreate();
            int max = Math.max(1, Math.min(limite, 50));
            return s.campagnes.stream()
                    .sorted(Comparator.comparingLong((CampagneSer c) -> c.createdAtEpochMilli).reversed())
                    .limit(max)
                    .map(UrgenceCampagneFichierStore::toCampagne)
                    .collect(Collectors.toList());
        } finally {
            LOCK.unlock();
        }
    }

    private static UrgenceDemande toDemande(DemandeSer d) {
        UrgenceDemande u = new UrgenceDemande();
        u.setId(d.id);
        u.setMessage(d.message);
        u.setStatut(d.statut);
        u.setCreatedAt(new Timestamp(d.createdAtEpochMilli));
        return u;
    }

    private static CampagneAide toCampagne(CampagneSer c) {
        CampagneAide a = new CampagneAide();
        a.setId(c.id);
        a.setDemandeId(c.demandeId);
        a.setTitre(c.titre);
        a.setCorps(c.corps);
        a.setCreatedAt(new Timestamp(c.createdAtEpochMilli));
        return a;
    }
}
