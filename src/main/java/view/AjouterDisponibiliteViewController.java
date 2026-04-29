package view;

import controllers.DisponibiliteController;
import exceptions.ServiceException;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;
import javafx.util.Duration;
import models.Disponibilite;
import models.User;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.UnaryOperator;

public class AjouterDisponibiliteViewController {

    private static final DateTimeFormatter HEURE_FMT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter DATE_UI_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private Label pageTitleLabel;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField heureDebutTextField;
    @FXML
    private TextField heureFinTextField;
    @FXML
    private Button ajouterButton;

    private DisponibiliteController disponibiliteController;
    private Runnable afterSaveCallback;
    private Integer medecinIdContexte;
    private Disponibilite disponibiliteEnEdition;
    private boolean initialized;

    public void setAfterSaveCallback(Runnable afterSaveCallback) {
        this.afterSaveCallback = afterSaveCallback;
    }

    public void setMedecinIdContexte(Integer medecinIdContexte) {
        this.medecinIdContexte = medecinIdContexte;
    }

    public void setDisponibiliteAEditer(Disponibilite disponibilite) {
        this.disponibiliteEnEdition = disponibilite;
        if (initialized) {
            appliquerModeEdition();
        }
    }

    @FXML
    private void initialize() {
        try {
            disponibiliteController = new DisponibiliteController();
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Base de donnees", e.formatWithCauses());
            return;
        }
        configurerAideSaisie();
        initialized = true;
        appliquerModeEdition();
        ajouterButton.setOnAction(e -> handleEnregistrer());
    }

    private void appliquerModeEdition() {
        if (disponibiliteEnEdition == null) {
            if (pageTitleLabel != null) {
                pageTitleLabel.setText("Nouvelle disponibilite");
            }
            if (ajouterButton != null) {
                ajouterButton.setText("Enregistrer");
            }
            return;
        }

        if (pageTitleLabel != null) {
            pageTitleLabel.setText("Modifier une disponibilite");
        }
        if (ajouterButton != null) {
            ajouterButton.setText("Enregistrer");
        }
        if (datePicker != null) {
            datePicker.setValue(disponibiliteEnEdition.getDate());
        }
        if (heureDebutTextField != null && disponibiliteEnEdition.getHeureDebut() != null) {
            heureDebutTextField.setText(disponibiliteEnEdition.getHeureDebut().format(HEURE_FMT));
        }
        if (heureFinTextField != null && disponibiliteEnEdition.getHeureFin() != null) {
            heureFinTextField.setText(disponibiliteEnEdition.getHeureFin().format(HEURE_FMT));
        }
    }

    private void configurerAideSaisie() {
        datePicker.setEditable(true);
        datePicker.getEditor().setPromptText("jj/mm/aaaa");
        datePicker.getEditor().setTextFormatter(creerDateFormatter());
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_UI_FMT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                String s = text.trim();
                try {
                    return LocalDate.parse(s, DATE_UI_FMT);
                } catch (DateTimeParseException ex) {
                    return LocalDate.parse(s);
                }
            }
        });

        heureDebutTextField.setPromptText("--:--");
        heureFinTextField.setPromptText("--:--");
        heureDebutTextField.setTextFormatter(creerHeureFormatter());
        heureFinTextField.setTextFormatter(creerHeureFormatter());
    }

    private static TextFormatter<String> creerDateFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }

            String digits = next.replace("/", "");
            if (!digits.matches("\\d{0,8}")) {
                return null;
            }

            String formatted = digits;
            if (digits.length() > 2) {
                formatted = digits.substring(0, 2) + "/" + digits.substring(2);
            }
            if (digits.length() > 4) {
                formatted = digits.substring(0, 2) + "/" + digits.substring(2, 4) + "/" + digits.substring(4);
            }

            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        };
        return new TextFormatter<>(filter);
    }

    private static TextFormatter<String> creerHeureFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }

            String digits = next.replace(":", "");
            if (!digits.matches("\\d{0,4}")) {
                return null;
            }

            String formatted = digits;
            if (digits.length() > 2) {
                formatted = digits.substring(0, 2) + ":" + digits.substring(2);
            }

            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        };
        return new TextFormatter<>(filter);
    }

    private void handleEnregistrer() {
        if (disponibiliteController == null) {
            return;
        }
        try {
            if (medecinIdContexte == null) {
                throw new IllegalArgumentException("Aucun medecin connecte pour creer cette disponibilite.");
            }

            LocalDate date = datePicker.getValue();
            if (date == null && datePicker.getEditor().getText() != null && !datePicker.getEditor().getText().isBlank()) {
                date = datePicker.getConverter().fromString(datePicker.getEditor().getText().trim());
                datePicker.setValue(date);
            }
            if (date == null) {
                throw new IllegalArgumentException("Choisissez une date.");
            }

            LocalTime debut = parseHeure(heureDebutTextField.getText(), "heure de debut");
            LocalTime fin = parseHeure(heureFinTextField.getText(), "heure de fin");

            User medecin = new User();
            medecin.setId(medecinIdContexte);

            Disponibilite d = new Disponibilite();
            if (disponibiliteEnEdition != null) {
                d.setId(disponibiliteEnEdition.getId());
            }
            d.setMedecin(medecin);
            d.setDate(date);
            d.setHeureDebut(debut);
            d.setHeureFin(fin);

            if (disponibiliteEnEdition == null) {
                disponibiliteController.ajouterDisponibilite(d);
            } else {
                disponibiliteController.modifierDisponibilite(d);
            }
            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }
            showSuccessToast(
                    disponibiliteEnEdition == null
                            ? "Disponibilite enregistree avec succes"
                            : "Disponibilite modifiee avec succes",
                    "Le creneau du " + DATE_UI_FMT.format(date)
                            + " de " + debut.format(HEURE_FMT)
                            + " a " + fin.format(HEURE_FMT)
                            + " est maintenant disponible."
            );
            closeWindow();
        } catch (IllegalArgumentException e) {
            ViewAlertUtil.erreur("Saisie", e.getMessage());
        } catch (ServiceException e) {
            ViewAlertUtil.erreur("Disponibilite", e.formatWithCauses());
        }
    }

    private static LocalTime parseHeure(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Indiquez " + label + ".");
        }
        String s = raw.trim();
        try {
            return LocalTime.parse(s);
        } catch (DateTimeParseException e1) {
            try {
                return LocalTime.parse(s, HEURE_FMT);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Format invalide pour " + label + " (ex: 09:30).");
            }
        }
    }

    private void closeWindow() {
        if (ajouterButton != null && ajouterButton.getScene() != null && ajouterButton.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    private void showSuccessToast(String title, String details) {
        if (ajouterButton == null || ajouterButton.getScene() == null) {
            return;
        }

        Window currentWindow = ajouterButton.getScene().getWindow();
        Stage ownerStage = currentWindow instanceof Stage currentStage && currentStage.getOwner() instanceof Stage owner
                ? owner
                : currentWindow instanceof Stage currentStage ? currentStage : null;

        Platform.runLater(() -> {
            Label iconLabel = new Label("\u2705");
            iconLabel.setStyle("-fx-font-size: 18px;");

            Label titleLabel = new Label(title);
            titleLabel.setWrapText(true);
            titleLabel.setStyle(
                    "-fx-font-size: 13px;"
                            + "-fx-font-weight: 700;"
                            + "-fx-text-fill: #166534;"
            );

            Label detailsLabel = new Label(details);
            detailsLabel.setWrapText(true);
            detailsLabel.setMaxWidth(320);
            detailsLabel.setStyle(
                    "-fx-font-size: 12px;"
                            + "-fx-text-fill: #166534;"
            );

            VBox textBox = new VBox(4, titleLabel, detailsLabel);
            HBox content = new HBox(10, iconLabel, textBox);
            content.setAlignment(Pos.TOP_LEFT);
            content.setPadding(new Insets(14, 16, 14, 16));
            content.setStyle(
                    "-fx-background-color: #dcfce7;"
                            + "-fx-border-color: #22c55e;"
                            + "-fx-border-width: 1;"
                            + "-fx-border-radius: 14;"
                            + "-fx-background-radius: 14;"
                            + "-fx-effect: dropshadow(gaussian, rgba(22,101,52,0.16), 14, 0, 0, 4);"
            );
            content.setMaxWidth(380);

            VBox root = new VBox(content);
            root.setPadding(new Insets(6));
            root.setStyle("-fx-background-color: transparent;");
            root.setOpacity(0);

            Stage toastStage = new Stage();
            toastStage.initStyle(StageStyle.TRANSPARENT);
            if (ownerStage != null) {
                toastStage.initOwner(ownerStage);
            }

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            toastStage.setScene(scene);
            toastStage.setAlwaysOnTop(true);
            toastStage.setResizable(false);

            toastStage.setOnShown(event -> {
                Window anchor = ownerStage != null ? ownerStage : currentWindow;
                double x = anchor.getX() + anchor.getWidth() - toastStage.getWidth() - 24;
                double y = anchor.getY() + 24;
                toastStage.setX(x);
                toastStage.setY(y);
            });

            toastStage.show();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(220), root);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            PauseTransition pause = new PauseTransition(Duration.seconds(3));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(260), root);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(event -> toastStage.close());

            new SequentialTransition(fadeIn, pause, fadeOut).play();
        });
    }
}
