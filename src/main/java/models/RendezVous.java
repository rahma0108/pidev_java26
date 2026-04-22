package models;

import java.time.LocalDateTime;
import java.util.Objects;

public class RendezVous {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String CONFIRME = "CONFIRME";
    public static final String ANNULE = "ANNULE";
    public static final String TERMINE = "TERMINE";

    private int id;
    private Disponibilite disponibilite;
    private LocalDateTime dateHeure;
    private String statut;
    private String motif;
    private LocalDateTime createdAt;
    private User patient;

    public RendezVous() {
    }

    public RendezVous(int id, Disponibilite disponibilite, LocalDateTime dateHeure,
                      String statut, String motif, LocalDateTime createdAt, User patient) {
        this.id = id;
        this.disponibilite = disponibilite;
        this.dateHeure = dateHeure;
        this.statut = statut;
        this.motif = motif;
        this.createdAt = createdAt;
        this.patient = patient;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Disponibilite getDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(Disponibilite disponibilite) {
        this.disponibilite = disponibilite;
    }

    public LocalDateTime getDateHeure() {
        return dateHeure;
    }

    public void setDateHeure(LocalDateTime dateHeure) {
        this.dateHeure = dateHeure;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RendezVous rendezVous = (RendezVous) o;
        return id == rendezVous.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
