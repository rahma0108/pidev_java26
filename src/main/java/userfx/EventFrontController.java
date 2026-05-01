package userfx;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.List;

public class EventFrontController {

    @FXML private FlowPane eventFlowPane;

    private EvenementService eventService = new EvenementService();

    @FXML
    public void initialize() {
        loadEvents();
    }

    private void loadEvents() {
        eventFlowPane.getChildren().clear();
        List<Evenement> events = eventService.getAll();

        for (Evenement e : events) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/event_card.fxml"));
                Parent card = loader.load();
                
                EventCardController ctrl = loader.getController();
                ctrl.setData(e, this);
                
                eventFlowPane.getChildren().add(card);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void openParticipationForm(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/participation_form.fxml"));
            Parent root = loader.load();
            
            ParticipationFormController ctrl = loader.getController();
            ctrl.initData(event, HomeController.getLoggedInUser());
            
            Stage stage = new Stage();
            stage.setTitle("Confirm Participation");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            ThemeManager.apply(scene);
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/home.fxml"));
            ThemeManager.applyWithFade(eventFlowPane.getScene(), root, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
