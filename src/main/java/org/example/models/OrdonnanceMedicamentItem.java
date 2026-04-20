package org.example.models;

public class OrdonnanceMedicamentItem {
    private int medicamentId;
    private String medicamentNom;
    private int quantite;
    private int stockDisponible;

    public OrdonnanceMedicamentItem() {}

    public OrdonnanceMedicamentItem(int medicamentId, String medicamentNom, int quantite) {
        this.medicamentId = medicamentId;
        this.medicamentNom = medicamentNom;
        this.quantite = quantite;
    }

    public int getMedicamentId() { return medicamentId; }
    public void setMedicamentId(int medicamentId) { this.medicamentId = medicamentId; }

    public String getMedicamentNom() { return medicamentNom; }
    public void setMedicamentNom(String medicamentNom) { this.medicamentNom = medicamentNom; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public int getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(int stockDisponible) { this.stockDisponible = stockDisponible; }
}