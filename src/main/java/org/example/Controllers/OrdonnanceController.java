package org.example.Controllers;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.models.Ordonnance;
import org.example.service.OrdonnanceService;
import org.example.utils.MyConnection;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OrdonnanceController {

    @FXML private ComboBox<String> medecinCombo;
    @FXML private ComboBox<String> patientCombo;
    @FXML private TextArea instructionsArea;
    @FXML private TextField searchField;
    @FXML private GridPane cardGrid;

    private OrdonnanceService ordonnanceService;
    private ObservableList<Ordonnance> ordonnanceList;
    private ObservableList<Ordonnance> filteredList;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter dateFileFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private Map<String, Integer> medecinIdMap = new HashMap<>();
    private Map<String, Integer> patientIdMap = new HashMap<>();

    @FXML
    public void initialize() {
        ordonnanceService = new OrdonnanceService();
        ordonnanceList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        chargerMedecins();
        chargerPatients();
        rafraichirListe();
    }

    private void chargerMedecins() {
        try {
            String sql = "SELECT id, full_name FROM user WHERE roles LIKE '%ROLE_MEDECIN%' ORDER BY full_name";
            Statement stmt = MyConnection.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            medecinCombo.getItems().clear();
            medecinIdMap.clear();
            while (rs.next()) {
                String nom = rs.getString("full_name");
                int id = rs.getInt("id");
                medecinCombo.getItems().add(nom);
                medecinIdMap.put(nom, id);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur chargement médecins: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void chargerPatients() {
        try {
            String sql = "SELECT id, full_name FROM user WHERE roles LIKE '%ROLE_PATIENT%' ORDER BY full_name";
            Statement stmt = MyConnection.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            patientCombo.getItems().clear();
            patientIdMap.clear();
            while (rs.next()) {
                String nom = rs.getString("full_name");
                int id = rs.getInt("id");
                patientCombo.getItems().add(nom);
                patientIdMap.put(nom, id);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur chargement patients: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void rechercherOrdonnance() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            filteredList.setAll(ordonnanceList);
        } else {
            List<Ordonnance> filtered = ordonnanceList.stream()
                    .filter(o -> o.getNomPatient().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            filteredList.setAll(filtered);
        }
        afficherCartes(filteredList);
    }

    private void afficherCartes(ObservableList<Ordonnance> ordonnances) {
        cardGrid.getChildren().clear();

        int col = 0;
        int row = 0;

        for (Ordonnance o : ordonnances) {
            VBox card = createOrdonnanceCard(o);
            cardGrid.add(card, col, row);
            col++;
            if (col > 2) { // 3 colonnes par ligne
                col = 0;
                row++;
            }
        }
    }

    private VBox createOrdonnanceCard(Ordonnance o) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setMinWidth(250);

        // Entête avec ID et date
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("#" + o.getId());
        idLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");

        Label dateLabel = new Label(o.getDateCreation().format(dateFormatter));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerBox.getChildren().addAll(idLabel, spacer, dateLabel);

        // Médecin
        Label medecinLabel = new Label("👨‍⚕️ Médecin: " + o.getNomMedecin());
        medecinLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        // Patient
        Label patientLabel = new Label("👤 Patient: " + o.getNomPatient());
        patientLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2980b9; -fx-font-weight: bold;");

        // Instructions
        Label instructionsTitle = new Label("📋 Instructions:");
        instructionsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        Label instructionsLabel = new Label(o.getInstructions() != null && !o.getInstructions().isEmpty() ? o.getInstructions() : "Aucune instruction");
        instructionsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34495e;");
        instructionsLabel.setWrapText(true);

        VBox instructionsBox = new VBox(5);
        instructionsBox.getChildren().addAll(instructionsTitle, instructionsLabel);

        // Boutons d'action
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        editBtn.setOnAction(e -> {
            remplirFormulaire(o);
            ScrollPane scrollPane = (ScrollPane) cardGrid.getParent();
            scrollPane.setVvalue(0);
        });

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        deleteBtn.setOnAction(e -> supprimerOrdonnance(o));

        buttonBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(headerBox, medecinLabel, patientLabel, instructionsBox, buttonBox);
        return card;
    }

    private void supprimerOrdonnance(Ordonnance o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer l'ordonnance #" + o.getId() + " ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                ordonnanceService.supprimerOrdonnance(o.getId());
                rafraichirListe();
                showAlert("Succès", "Ordonnance supprimée!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void remplirFormulaire(Ordonnance o) {
        medecinCombo.setValue(o.getNomMedecin());
        patientCombo.setValue(o.getNomPatient());
        instructionsArea.setText(o.getInstructions());
    }

    @FXML
    private void viderFormulaire() {
        medecinCombo.setValue(null);
        patientCombo.setValue(null);
        instructionsArea.clear();
    }

    @FXML
    private void ajouterOrdonnance() {
        if (medecinCombo.getValue() == null || patientCombo.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un médecin et un patient", Alert.AlertType.ERROR);
            return;
        }

        try {
            Ordonnance ordonnance = new Ordonnance();
            ordonnance.setDateCreation(LocalDateTime.now());
            ordonnance.setInstructions(instructionsArea.getText());
            ordonnance.setMedecinId(medecinIdMap.get(medecinCombo.getValue()));
            ordonnance.setPatientId(patientIdMap.get(patientCombo.getValue()));

            ordonnanceService.ajouterOrdonnance(ordonnance);
            rafraichirListe();
            viderFormulaire();
            showAlert("Succès", "Ordonnance ajoutée!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void modifierOrdonnance() {
        if (medecinCombo.getValue() == null || patientCombo.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un médecin et un patient", Alert.AlertType.ERROR);
            return;
        }

        // Chercher l'ordonnance à modifier (par le nom du patient sélectionné)
        String patientNom = patientCombo.getValue();
        Ordonnance toModify = ordonnanceList.stream()
                .filter(o -> o.getNomPatient().equals(patientNom))
                .findFirst()
                .orElse(null);

        if (toModify == null) {
            showAlert("Erreur", "Ordonnance non trouvée", Alert.AlertType.ERROR);
            return;
        }

        try {
            toModify.setInstructions(instructionsArea.getText());
            toModify.setMedecinId(medecinIdMap.get(medecinCombo.getValue()));
            toModify.setPatientId(patientIdMap.get(patientCombo.getValue()));

            ordonnanceService.modifierOrdonnance(toModify);
            rafraichirListe();
            viderFormulaire();
            showAlert("Succès", "Ordonnance modifiée!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void supprimerOrdonnance() {
        String patientNom = patientCombo.getValue();
        if (patientNom == null) {
            showAlert("Attention", "Sélectionnez une ordonnance dans la liste", Alert.AlertType.WARNING);
            return;
        }

        Ordonnance toDelete = ordonnanceList.stream()
                .filter(o -> o.getNomPatient().equals(patientNom))
                .findFirst()
                .orElse(null);

        if (toDelete != null) {
            supprimerOrdonnance(toDelete);
        }
    }

    @FXML
    private void rafraichirListe() {
        try {
            ordonnanceList.clear();
            ordonnanceList.addAll(ordonnanceService.getAllOrdonnances());
            filteredList.setAll(ordonnanceList);
            searchField.clear();
            afficherCartes(ordonnanceList);
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void exporterPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        fileChooser.setInitialFileName("ordonnances_" + LocalDateTime.now().format(dateFileFormatter) + ".pdf");

        File file = fileChooser.showSaveDialog(cardGrid.getScene().getWindow());
        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                document.add(new Paragraph("LISTE DES ORDONNANCES")
                        .setFontSize(18)
                        .setBold());
                document.add(new Paragraph("Généré le: " + LocalDateTime.now().format(dateFormatter)));
                document.add(new Paragraph(" "));

                Table table = new Table(5);
                table.addCell("ID");
                table.addCell("Date");
                table.addCell("Médecin");
                table.addCell("Patient");
                table.addCell("Instructions");

                for (Ordonnance o : filteredList) {
                    table.addCell(String.valueOf(o.getId()));
                    table.addCell(o.getDateCreation().format(dateFormatter));
                    table.addCell(o.getNomMedecin());
                    table.addCell(o.getNomPatient());
                    table.addCell(o.getInstructions() != null ? o.getInstructions() : "");
                }

                document.add(table);
                document.close();

                showAlert("Succès", "PDF exporté: " + file.getName(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur export: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void retourAccueil() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"));
            Stage stage = (Stage) cardGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion Médicale");
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}