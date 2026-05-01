package userfx;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.time.format.DateTimeFormatter;

public class EventCardController {

    @FXML private Label titleLabel;
    @FXML private Label locationLabel;
    @FXML private Button participateBtn;

    private Evenement event;
    private EventFrontController parentController;

    public void setData(Evenement ev, EventFrontController parent) {
        this.event = ev;
        this.parentController = parent;

        titleLabel.setText(ev.getTitre());
        locationLabel.setText("📍 " + ev.getLieu());
        
        String dateStr = ev.getDateEvenement().format(DateTimeFormatter.ofPattern("dd MMM"));
        participateBtn.setText("Join - " + dateStr);

        // Add hover effect
        javafx.scene.Node card = titleLabel.getParent().getParent();
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); -fx-background-radius: 24; -fx-border-color: rgba(16, 185, 129, 0.5); -fx-border-radius: 24; -fx-border-width: 1; -fx-padding: 25;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: rgba(15, 23, 42, 0.6); -fx-background-radius: 24; -fx-border-color: rgba(255, 255, 255, 0.1); -fx-border-radius: 24; -fx-border-width: 1; -fx-padding: 25;"));
    }

    @FXML
    private void handleParticipate() {
        parentController.openParticipationForm(event);
    }

    @FXML
    private void handleQR() {
        String data = "Event: " + event.getTitre() + "\nDate: " + event.getDateEvenement() + "\nLocation: " + event.getLieu();
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + 
                       java.net.URLEncoder.encode(data, java.nio.charset.StandardCharsets.UTF_8);
        
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(qrUrl));
        
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("QR Code - " + event.getTitre());
        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(15, new Label("Scan to see event details:"), iv);
        vbox.setAlignment(javafx.geometry.Pos.CENTER);
        vbox.setPadding(new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
