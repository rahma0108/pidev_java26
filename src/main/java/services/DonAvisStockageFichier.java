package services;

import models.Don;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Fichiers {@code don_&lt;id&gt;_ia.properties} : un par don.
 * <p>
 * Écriture : dossier explicite ou, par défaut, {@code &lt;user.dir&gt;/gdons_avis_ia} (sous le projet
 * lorsque l’app est lancée depuis ce dossier — même chemin pour donneur et admin).
 * <p>
 * Lecture : ce dossier, puis ancien emplacement {@code user.home/.gdons} pour compatibilité.
 */
public final class DonAvisStockageFichier {

    private static final String DEFAUT_SOUS_PROJET = "gdons_avis_ia";
    private static final String LEGACY_SOUS_HOME = ".gdons";

    private static volatile Path racineEcritureCache;

    private DonAvisStockageFichier() {
    }

    private static String premierNonVide(String... ss) {
        if (ss == null) {
            return null;
        }
        for (String s : ss) {
            if (s != null && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }

    private static String lireCleApplicationProperties(String cle) {
        try (InputStream in = DonAvisStockageFichier.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                return null;
            }
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty(cle);
            return v != null ? v.trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Dossier où l’on enregistre (et le premier où l’on lit). */
    private static Path racineEcriture() throws IOException {
        Path r = racineEcritureCache;
        if (r != null) {
            return r;
        }
        synchronized (DonAvisStockageFichier.class) {
            if (racineEcritureCache != null) {
                return racineEcritureCache;
            }
            String custom = premierNonVide(
                    System.getProperty("don.ia.storage.dir"),
                    System.getenv("DON_IA_STORAGE_DIR"),
                    lireCleApplicationProperties("don.ia.storage.dir"));
            if (custom != null && !custom.isBlank()) {
                Path p = Paths.get(custom);
                racineEcritureCache = p.isAbsolute()
                        ? p.normalize()
                        : Paths.get(System.getProperty("user.dir", ".")).resolve(p).normalize().toAbsolutePath();
            } else {
                racineEcritureCache = Paths.get(System.getProperty("user.dir", "."), DEFAUT_SOUS_PROJET)
                        .normalize().toAbsolutePath();
            }
            Files.createDirectories(racineEcritureCache);
            return racineEcritureCache;
        }
    }

    /** Ordre : racine d’écriture, puis ancien ~/.gdons si différent. */
    private static List<Path> racinesLecture() throws IOException {
        Set<Path> vu = new LinkedHashSet<>();
        List<Path> out = new ArrayList<>();
        Path w = racineEcriture();
        vu.add(w);
        out.add(w);
        Path legacy = Paths.get(System.getProperty("user.home"), LEGACY_SOUS_HOME).toAbsolutePath().normalize();
        if (!vu.contains(legacy)) {
            out.add(legacy);
        }
        return out;
    }

    /** Pour messages d’erreur utilisateur. */
    public static String cheminRacinePourMessage() {
        try {
            return racineEcriture().toString();
        } catch (IOException e) {
            return "(dossier indisponible)";
        }
    }

    public static boolean existeFichierAvis(int donId) {
        if (donId <= 0) {
            return false;
        }
        try {
            Path f = racineEcriture().resolve("don_" + donId + "_ia.properties");
            return Files.isRegularFile(f);
        } catch (IOException e) {
            return false;
        }
    }

    private static Path fichierDans(Path racine, int donId) {
        return racine.resolve("don_" + donId + "_ia.properties");
    }

    /** Enregistre ou supprime le fichier si tout est vide. */
    public static void enregistrer(Don d) throws IOException {
        if (d == null || d.getId() <= 0) {
            return;
        }
        if (!aContenuIa(d)) {
            supprimer(d.getId());
            return;
        }
        Properties pr = new Properties();
        if (nonVide(d.getDecisionIA())) {
            pr.setProperty("decision", d.getDecisionIA());
        }
        if (nonVide(d.getRaisonIA())) {
            pr.setProperty("raison", d.getRaisonIA());
        }
        if (nonVide(d.getTraductionIA())) {
            pr.setProperty("traduction", d.getTraductionIA());
        }
        Path f = fichierDans(racineEcriture(), d.getId());
        try (OutputStream os = Files.newOutputStream(f)) {
            pr.store(os, "Avis IA — don id=" + d.getId());
        }
    }

    private static boolean aContenuIa(Don d) {
        return nonVide(d.getDecisionIA()) || nonVide(d.getRaisonIA()) || nonVide(d.getTraductionIA());
    }

    private static boolean nonVide(String s) {
        return s != null && !s.isBlank();
    }

    /** Lit le premier fichier trouvé parmi les racines connues. */
    public static void fusionnerVersDon(Don d) throws IOException {
        if (d == null || d.getId() <= 0) {
            return;
        }
        for (Path racine : racinesLecture()) {
            Path f = fichierDans(racine, d.getId());
            if (!Files.isRegularFile(f)) {
                continue;
            }
            Properties pr = new Properties();
            try (InputStream is = Files.newInputStream(f)) {
                pr.load(is);
            }
            String dec = pr.getProperty("decision");
            String r = pr.getProperty("raison");
            String t = pr.getProperty("traduction");
            if (dec != null) {
                d.setDecisionIA(dec);
            }
            if (r != null) {
                d.setRaisonIA(r);
            }
            if (t != null) {
                d.setTraductionIA(t);
            }
            return;
        }
    }

    public static void supprimer(int donId) throws IOException {
        if (donId <= 0) {
            return;
        }
        Files.deleteIfExists(fichierDans(racineEcriture(), donId));
        Path leg = Paths.get(System.getProperty("user.home"), LEGACY_SOUS_HOME).resolve("don_" + donId + "_ia.properties");
        Files.deleteIfExists(leg);
    }
}
