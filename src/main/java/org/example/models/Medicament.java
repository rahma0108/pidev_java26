package org.example.models;

public class Medicament {
    private int id;
    private String nom;
    private String description;
    private int quantiteStock;

    // Constructeur sans paramètres (OBLIGATOIRE pour PropertyValueFactory)
    public Medicament() {}

    // Constructeur avec paramètres
    public Medicament(int id, String nom, String description, int quantiteStock) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.quantiteStock = quantiteStock;
    }

    // Getters et Setters (OBLIGATOIRES pour PropertyValueFactory)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(int quantiteStock) { this.quantiteStock = quantiteStock; }
}