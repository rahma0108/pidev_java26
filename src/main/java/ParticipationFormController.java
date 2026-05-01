import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ParticipationFormController {

    @FXML private Label eventTitleLabel;
    @FXML private TextArea commentArea;

    private Evenement event;
    private ParticipationService partService = new ParticipationService();
    private User currentUser;

    public void initData(Evenement event, User user) {
        this.event = event;
        this.currentUser = user;
        eventTitleLabel.setText("Participate in: " + event.getTitre());
    }

    @FXML
    private void handleSubmit() {
        if (currentUser == null) {
            System.err.println("No user logged in.");
            return;
        }

        Participation p = new Participation();
        p.setEvenementId(event.getId());
        p.setUserId(currentUser.getId());
        p.setStatut("en_attente");
        p.setCommentaire(commentArea.getText());

        partService.insert(p);
        closeStage();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        ((Stage) commentArea.getScene().getWindow()).close();
    }
}
