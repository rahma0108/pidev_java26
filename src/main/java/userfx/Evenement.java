package userfx;


import java.time.LocalDateTime;

public class Evenement {
    private int id;
    private String titre;
    private String description;
    private LocalDateTime dateEvenement;
    private String lieu;
    private String type;
    private LocalDateTime createdAt;
    private String photo;

    public Evenement() {}

    public Evenement(int id, String titre, String description, LocalDateTime dateEvenement, String lieu, String type, LocalDateTime createdAt, String photo) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.type = type;
        this.createdAt = createdAt;
        this.photo = photo;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateEvenement() { return dateEvenement; }
    public void setDateEvenement(LocalDateTime dateEvenement) { this.dateEvenement = dateEvenement; }
    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    @Override
    public String toString() {
        return titre;
    }
}
