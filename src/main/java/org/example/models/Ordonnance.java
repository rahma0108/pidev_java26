package org.example.models;

import java.time.LocalDateTime;

public class Ordonnance {
    private int id;
    private LocalDateTime dateCreation;
    private String instructions;
    private int medecinId;
    private int patientId;

    // Champs pour l'affichage (non stockés en base)
    private String nomMedecin;
    private String nomPatient;

    public Ordonnance() {}

    public Ordonnance(int id, LocalDateTime dateCreation, String instructions, int medecinId, int patientId) {
        this.id = id;
        this.dateCreation = dateCreation;
        this.instructions = instructions;
        this.medecinId = medecinId;
        this.patientId = patientId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public int getMedecinId() { return medecinId; }
    public void setMedecinId(int medecinId) { this.medecinId = medecinId; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getNomMedecin() { return nomMedecin; }
    public void setNomMedecin(String nomMedecin) { this.nomMedecin = nomMedecin; }

    public String getNomPatient() { return nomPatient; }
    public void setNomPatient(String nomPatient) { this.nomPatient = nomPatient; }
}