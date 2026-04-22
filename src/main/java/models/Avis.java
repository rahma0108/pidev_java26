package models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Avis laissé par un patient sur un rendez-vous (une seule fois par RDV).
 */
public class Avis {

    private int id;
    private RendezVous rendezVous;
    private User patient;
    private int note;
    private String commentaire;
    private LocalDateTime createdAt;

    public Avis() {
    }

    public Avis(int id, RendezVous rendezVous, User patient, int note,
                String commentaire, LocalDateTime createdAt) {
        this.id = id;
        this.rendezVous = rendezVous;
        this.patient = patient;
        this.note = note;
        this.commentaire = commentaire;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RendezVous getRendezVous() {
        return rendezVous;
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVous = rendezVous;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Avis avis = (Avis) o;
        return id == avis.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
