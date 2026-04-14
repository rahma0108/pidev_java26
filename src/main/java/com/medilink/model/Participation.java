package com.medilink.model;

import java.time.LocalDateTime;

public class Participation {
    private int id;
    private int evenementId;
    private String statut;
    private LocalDateTime dateInscription;
    private int userId;
    private String commentaire;

    public Participation() {
    }

    public Participation(int id, int evenementId, String statut, LocalDateTime dateInscription, int userId, String commentaire) {
        this.id = id;
        this.evenementId = evenementId;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.userId = userId;
        this.commentaire = commentaire;
    }

    public Participation(int evenementId, String statut, LocalDateTime dateInscription, int userId, String commentaire) {
        this.evenementId = evenementId;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.userId = userId;
        this.commentaire = commentaire;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
}
