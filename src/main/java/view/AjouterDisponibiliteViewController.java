package view;

import controllers.DisponibiliteController;
import exceptions.ServiceException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;
import javafx.util.StringConverter;
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
                ViewAlertUtil.info("Succes", "Disponibilite enregistree.");
            } else {
                disponibiliteController.modifierDisponibilite(d);
                ViewAlertUtil.info("Succes", "Disponibilite modifiee.");
            }
            if (afterSaveCallback != null) {
                afterSaveCallback.run();
            }
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
}
