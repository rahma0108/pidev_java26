package services;



import models.Don;

import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.MethodOrderer;

import org.junit.jupiter.api.Order;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestMethodOrder;



import java.sql.Date;

import java.sql.SQLException;

import java.util.List;

import java.util.Optional;



import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.junit.jupiter.api.Assertions.assertTrue;



@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

class DonServiceTest {



    private static DonService donService;

    private static String marqueurUnique;

    private static int idInsere = -1;



    @BeforeAll

    static void init() {

        donService = new DonService();

        marqueurUnique = "JUnit_DON_" + System.nanoTime();

    }



    @Test

    @Order(1)

    void testAjouter() throws SQLException {

        Don d = new Don();

        d.setCategorieId(1);

        d.setArticleDescription(marqueurUnique);

        d.setQuantite(10);

        d.setUnite("pcs");

        d.setDetailsSupplementaires("créé par test unitaire");

        d.setEtat("neuf");

        d.setNiveauUrgence("normale");

        d.setStatut("en_attente");

        d.setDateExpiration(Date.valueOf("2026-12-31"));



        assertDoesNotThrow(() -> donService.add(d));



        List<Don> tous = donService.getAll();

        assertNotNull(tous);

        Optional<Don> trouve = tous.stream()

                .filter(x -> marqueurUnique.equals(x.getArticleDescription()))

                .findFirst();

        assertTrue(trouve.isPresent(), "Le don inséré doit apparaître après getAll()");

        idInsere = trouve.get().getId();

        assertTrue(idInsere > 0, "L'id généré par la base doit être > 0");

    }



    @Test

    @Order(2)

    void testAfficher() throws SQLException {

        assertTrue(idInsere > 0, "testAjouter doit avoir réussi avant testAfficher");



        List<Don> tous = donService.getAll();

        assertNotNull(tous);

        assertFalse(tous.isEmpty(), "La liste ne doit pas être vide pour ce test");



        boolean present = tous.stream().anyMatch(x -> x.getId() == idInsere);

        assertTrue(present, "Le don créé doit être présent dans la liste");

    }



    @Test

    @Order(3)

    void testModifier() throws SQLException {

        assertTrue(idInsere > 0, "testAjouter doit avoir réussi avant testModifier");



        Don d = new Don();

        d.setId(idInsere);

        d.setCategorieId(1);

        d.setArticleDescription(marqueurUnique + "_modifié");

        d.setQuantite(20);

        d.setUnite("kg");

        d.setDetailsSupplementaires("mis à jour");

        d.setEtat("bon");

        d.setNiveauUrgence("haute");

        d.setStatut("valide");

        d.setDateExpiration(Date.valueOf("2027-01-15"));



        assertDoesNotThrow(() -> donService.update(d));



        List<Don> tous = donService.getAll();

        Optional<Don> lu = tous.stream().filter(x -> x.getId() == idInsere).findFirst();

        assertTrue(lu.isPresent());

        assertTrue(lu.get().getArticleDescription().endsWith("_modifié"));

        assertFalse(marqueurUnique.equals(lu.get().getArticleDescription()));

    }



    @Test

    @Order(4)

    void testSupprimer() throws SQLException {

        assertTrue(idInsere > 0, "testAjouter doit avoir réussi avant testSupprimer");



        assertDoesNotThrow(() -> donService.delete(idInsere));



        List<Don> tous = donService.getAll();

        assertNotNull(tous);

        boolean encoreLa = tous.stream().anyMatch(x -> x.getId() == idInsere);

        assertFalse(encoreLa, "Le don ne doit plus exister après suppression");

    }

}

