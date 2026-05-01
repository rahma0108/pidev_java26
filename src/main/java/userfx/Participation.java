package userfx;


import java.time.LocalDateTime;

public class Participation {
    private int id;
    private int evenementId;
    private int userId;
    private String statut;
    private LocalDateTime dateInscription;
    private String commentaire;

    // Optional fields for easy display
    private String userName;
    private String eventTitle;

    public Participation() {}

    public Participation(int id, int evenementId, int userId, String statut, LocalDateTime dateInscription, String commentaire) {
        this.id = id;
        this.evenementId = evenementId;
        this.userId = userId;
        this.statut = statut;
        this.dateInscription = dateInscription;
        this.commentaire = commentaire;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) { this.dateInscription = dateInscription; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
}
