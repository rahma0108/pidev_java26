import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public class EventBackController {

    @FXML private FlowPane cardsContainer;
    @FXML private TextField filterField;
    @FXML private Label messageLabel;
    @FXML private Label aiTitleLabel;
    @FXML private Label aiDescriptionLabel;
    @FXML private Label totalEventsStatLabel;
    @FXML private Label upcomingEventsStatLabel;
    @FXML private Label totalParticipantsStatLabel;

    private EvenementService eventService = new EvenementService();
    private ParticipationService partService = new ParticipationService();

    @FXML
    public void initialize() {
        loadCards(null);
        if (filterField != null) {
            filterField.textProperty().addListener((obs, old, val) -> loadCards(val.trim()));
        }
    }

    private void loadCards(String filter) {
        cardsContainer.getChildren().clear();
        List<Evenement> events = eventService.getAll();

        int total = events.size();
        long upcoming = events.stream().filter(e -> e.getDateEvenement().isAfter(LocalDateTime.now())).count();
        
        if (totalEventsStatLabel != null) totalEventsStatLabel.setText("Total events: " + total);
        if (upcomingEventsStatLabel != null) upcomingEventsStatLabel.setText("Upcoming: " + upcoming);

        for (Evenement ev : events) {
            if (filter != null && !filter.isEmpty()) {
                String f = filter.toLowerCase();
                boolean matches = ev.getTitre().toLowerCase().contains(f) ||
                                 ev.getLieu().toLowerCase().contains(f) ||
                                 ev.getType().toLowerCase().contains(f);
                if (!matches) continue;
            }
            VBox card = createCard(ev);
            cardsContainer.getChildren().add(card);
            EffectsHelper.slideInUp(card, cardsContainer.getChildren().size() * 50);
        }
    }

    private VBox createCard(Evenement ev) {
        VBox card = new VBox(10);
        card.setPrefWidth(260);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                "-fx-border-width: 0.5; -fx-border-radius: 12; -fx-background-radius: 12;");

        EffectsHelper.addHoverGlow(card, "#185FA5");

        Label title = new Label(ev.getTitre());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        
        Label date = new Label("📅 " + ev.getDateEvenement().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #185FA5;");
        
        Label location = new Label("📍 " + ev.getLieu());
        location.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Separator sep = new Separator();

        HBox buttons = new HBox(6);
        buttons.setAlignment(Pos.CENTER);
        
        Button aiBtn     = makeBtn("AI",     "#534AB7");
        Button editBtn   = makeBtn("Edit",   "#185FA5");
        Button deleteBtn = makeBtn("Delete", "#c0392b");
        Button partBtn   = makeBtn("Parts",  "#0F6E56");
        Button qrBtn     = makeBtn("QR",     "#34495e");

        aiBtn.setOnAction(e     -> showAIInsight(ev));
        editBtn.setOnAction(e   -> showEditPopup(ev));
        deleteBtn.setOnAction(e -> handleDelete(ev));
        partBtn.setOnAction(e   -> showParticipants(ev));
        qrBtn.setOnAction(e     -> showQRCode(ev));

        buttons.getChildren().addAll(aiBtn, editBtn, deleteBtn, partBtn, qrBtn);
        card.getChildren().addAll(title, date, location, sep, buttons);
        return card;
    }

    private void showQRCode(Evenement ev) {
        String data = "Event: " + ev.getTitre() + "\nDate: " + ev.getDateEvenement() + "\nLocation: " + ev.getLieu();
        String qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + 
                       java.net.URLEncoder.encode(data, java.nio.charset.StandardCharsets.UTF_8);
        
        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(new javafx.scene.image.Image(qrUrl));
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("QR Code - " + ev.getTitre());
        VBox vbox = new VBox(15, new Label("Scan to see event details:"), iv);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void handleAdd() {
        showEventPopup(null);
    }

    private void showEditPopup(Evenement ev) {
        showEventPopup(ev);
    }

    private void showEventPopup(Evenement ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(ev == null ? "Add New Event" : "Edit Event");
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: white;");
        pane.setPrefWidth(450);

        VBox content = new VBox(12);
        content.setPadding(new Insets(24));

        Label titleHeader = new Label(ev == null ? "Add New Event" : "Edit Event");
        titleHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");

        TextField titleF   = styledField(ev != null ? ev.getTitre() : "", "Title");

        HBox descHeader = new HBox(10);
        descHeader.setAlignment(Pos.CENTER_LEFT);
        Button generateAIBtn = new Button("✨ AI Generate");
        generateAIBtn.setStyle("-fx-background-color: #534AB7; -fx-text-fill: white; -fx-font-size: 10px; -fx-background-radius: 5;");
        descHeader.getChildren().addAll(fieldLabel("Description"), generateAIBtn);

        TextArea descF     = new TextArea(ev != null ? ev.getDescription() : "");
        descF.setPromptText("Description");
        descF.setPrefHeight(80);
        descF.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd; -fx-font-size: 13px;");
        
        generateAIBtn.setOnAction(e -> {
            String title = titleF.getText().trim();
            if (title.isEmpty()) {
                messageLabel.setText("Enter a title first!");
                return;
            }
            generateAIBtn.setDisable(true);
            generateAIBtn.setText("⌛ Generating...");
            new Thread(() -> {
                try {
                    HuggingFaceService ai = new HuggingFaceService();
                    String generated = ai.generateEventDescription(title);
                    Platform.runLater(() -> {
                        descF.setText(generated);
                        generateAIBtn.setDisable(false);
                        generateAIBtn.setText("✨ AI Generate");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        messageLabel.setText("AI Error: " + ex.getMessage());
                        generateAIBtn.setDisable(false);
                        generateAIBtn.setText("✨ AI Generate");
                    });
                }
            }).start();
        });

        DatePicker dateF   = new DatePicker(ev != null ? ev.getDateEvenement().toLocalDate() : null);
        TextField lieuF    = styledField(ev != null ? ev.getLieu() : "", "Location (or pick on map)");
        
        // Map integration
        WebView webView = new WebView();
        webView.setPrefHeight(200);
        webView.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-background-color: white;");
        WebEngine webEngine = webView.getEngine();
        
        var mapUrl = getClass().getResource("/map.html");
        if (mapUrl != null) {
            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaConnector", new MapConnector(lieuF));
                }
            });
            webEngine.load(mapUrl.toExternalForm());
        } else {
            System.err.println("map.html NOT FOUND in resources!");
        }

        TextField typeF    = styledField(ev != null ? ev.getType() : "", "Type");
        TextField photoF   = styledField(ev != null ? ev.getPhoto() : "", "Photo URL");

        content.getChildren().addAll(titleHeader, new Separator(),
                fieldLabel("Title"), titleF,
                descHeader, descF,
                fieldLabel("Date"), dateF,
                fieldLabel("Location (Map)"), webView, lieuF,
                fieldLabel("Type"), typeF,
                fieldLabel("Photo URL"), photoF);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500); // Limit height so buttons stay visible
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        pane.setContent(scrollPane);
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == saveBtn) {
                try {
                    Evenement e = (ev == null) ? new Evenement() : ev;
                    e.setTitre(titleF.getText());
                    e.setDescription(descF.getText());
                    e.setDateEvenement(dateF.getValue().atStartOfDay());
                    e.setLieu(lieuF.getText());
                    e.setType(typeF.getText());
                    e.setPhoto(photoF.getText());

                    if (ev == null) eventService.insert(e);
                    else eventService.update(e);
                    
                    loadCards(null);
                } catch (Exception ex) {
                    messageLabel.setText("Error: Check time format (HH:mm)");
                }
            }
        });
    }

    private void handleDelete(Evenement ev) {
        if (PopupHelper.confirmDelete(ev.getTitre())) {
            eventService.delete(ev.getId());
            loadCards(null);
        }
    }

    private void showParticipants(Evenement ev) {
        List<Participation> parts = partService.getByEvent(ev.getId());
        StringBuilder sb = new StringBuilder("Participants:\n");
        for (Participation p : parts) {
            sb.append("- ").append(p.getUserName()).append(" (").append(p.getStatut()).append(")\n");
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Participants - " + ev.getTitre());
        alert.setHeaderText(null);
        alert.setContentText(sb.length() > 14 ? sb.toString() : "No participants yet.");
        alert.showAndWait();
    }

    private void showAIInsight(Evenement ev) {
        aiTitleLabel.setText(ev.getTitre());
        aiDescriptionLabel.setText("Analyzing event potential...");
        new Thread(() -> {
            String insight = ClaudeAI.ask("Analyze this event. Give a 2-sentence marketing insight for an organizer.",
                    "Title: " + ev.getTitre() + "\nDescription: " + ev.getDescription() + "\nType: " + ev.getType());
            Platform.runLater(() -> aiDescriptionLabel.setText(insight));
        }).start();
    }

    @FXML
    public void goToDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/dashboard.fxml"));
            ThemeManager.applyWithFade(cardsContainer.getScene(), root, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── HELPERS ──
    private Button makeBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-font-size: 10px; -fx-cursor: hand; -fx-border-color: transparent;");
        return btn;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");
        return l;
    }

    private TextField styledField(String value, String placeholder) {
        TextField tf = new TextField(value);
        tf.setPromptText(placeholder);
        tf.setPrefHeight(36);
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd; -fx-font-size: 13px;");
        return tf;
    }

    public static class MapConnector {
        private TextField field;
        public MapConnector(TextField field) { this.field = field; }
        public void onLocationSelected(String address) {
            Platform.runLater(() -> field.setText(address));
        }
    }
}
