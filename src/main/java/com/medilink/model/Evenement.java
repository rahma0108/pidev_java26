package com.medilink.model;

import java.time.LocalDate;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private LocalDate dateEvenement;
    private String lieu;
    private String type;
    private String photo;

    public Evenement() {
    }

    public Evenement(int id, String titre, String description, LocalDate dateEvenement, String lieu, String type, String photo) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.type = type;
        this.photo = photo;
    }

    public Evenement(String titre, String description, LocalDate dateEvenement, String lieu, String type, String photo) {
        this.titre = titre;
        this.description = description;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.type = type;
        this.photo = photo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDateEvenement() {
        return dateEvenement;
    }

    public void setDateEvenement(LocalDate dateEvenement) {
        this.dateEvenement = dateEvenement;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Override
    public String toString() {
        return id + " - " + titre;
    }
}
