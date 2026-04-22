package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class Disponibilite {

    public static final String STATUS_LIBRE = "STATUS_LIBRE";
    public static final String STATUS_RESERVEE = "STATUS_RESERVEE";
    public static final String STATUS_ANNULEE = "STATUS_ANNULEE";

    private int id;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private String status;
    private LocalDateTime createdAt;
    private User medecin;
    private RendezVous rendezVous;

    public Disponibilite() {
    }

    public Disponibilite(int id, LocalDate date, LocalTime heureDebut, LocalTime heureFin,
                         String status, LocalDateTime createdAt, User medecin) {
        this.id = id;
        this.date = date;
        this.heureDebut = heureDebut;
        this.heureFin = heureFin;
        this.status = status;
        this.createdAt = createdAt;
        this.medecin = medecin;
    }

    public LocalDateTime toDateHeureDebut() {
        if (date == null || heureDebut == null) {
            return null;
        }
        return LocalDateTime.of(date, heureDebut);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getMedecin() {
        return medecin;
    }

    public void setMedecin(User medecin) {
        this.medecin = medecin;
    }

    public RendezVous getRendezVous() {
        return rendezVous;
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVous = rendezVous;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Disponibilite that = (Disponibilite) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
