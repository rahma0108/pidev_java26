package test;

import models.Don;
import services.DonService;
import java.sql.Date;

public class Main {
    public static void main(String[] args) throws Exception {

        DonService ds = new DonService();

        Don d = new Don(1, "Vêtements pour enfants", 10, "unités",
                "Bon état", "Neuf / Non ouvert", "Moyen",
                "en_attente", Date.valueOf("2026-12-31"));
        ds.add(d);

        System.out.println("Don ajouté avec succès !");
    }
}