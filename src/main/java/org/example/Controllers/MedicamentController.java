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
import org.example.models.Medicament;
import org.example.service.MedicamentService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MedicamentController {

    @FXML private TextField nomField;
    @FXML private TextField quantiteField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> triCombo;
    @FXML private GridPane cardGrid;

    private MedicamentService medicamentService;
    private ObservableList<Medicament> medicamentList;
    private ObservableList<Medicament> filteredList;

    @FXML
    public void initialize() {
        medicamentService = new MedicamentService();
        medicamentList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        // Configuration du tri par défaut
        triCombo.getItems().addAll(
                "Nom (A→Z)",
                "Nom (Z→A)",
                "Quantité (croissant)",
                "Quantité (décroissant)"
        );
        triCombo.setValue("Nom (A→Z)");

        rafraichirListe();
    }

    @FXML
    private void rechercherMedicament() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            filteredList.setAll(medicamentList);
        } else {
            List<Medicament> filtered = medicamentList.stream()
                    .filter(m -> m.getNom().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            filteredList.setAll(filtered);
        }
        trierMedicaments();
    }

    @FXML
    private void trierMedicaments() {
        String triType = triCombo.getValue();
        if (triType == null) return;

        List<Medicament> list = new ArrayList<>(filteredList);

        switch (triType) {
            case "Nom (A→Z)":
                list.sort(Comparator.comparing(Medicament::getNom, String.CASE_INSENSITIVE_ORDER));
                break;
            case "Nom (Z→A)":
                list.sort(Comparator.comparing(Medicament::getNom, String.CASE_INSENSITIVE_ORDER).reversed());
                break;
            case "Quantité (croissant)":
                list.sort(Comparator.comparingInt(Medicament::getQuantiteStock));
                break;
            case "Quantité (décroissant)":
                list.sort((m1, m2) -> Integer.compare(m2.getQuantiteStock(), m1.getQuantiteStock()));
                break;
            default:
                break;
        }

        afficherCartes(list);
    }

    private void afficherCartes(List<Medicament> medicaments) {
        cardGrid.getChildren().clear();

        int col = 0;
        int row = 0;

        for (Medicament m : medicaments) {
            VBox card = createMedicamentCard(m);
            cardGrid.add(card, col, row);
            col++;
            if (col > 2) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createMedicamentCard(Medicament m) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setMinWidth(250);

        // Couleur de la quantité
        String quantiteColor;
        if (m.getQuantiteStock() < 10) {
            quantiteColor = "#e74c3c"; // Rouge - stock faible
        } else if (m.getQuantiteStock() < 50) {
            quantiteColor = "#f39c12"; // Orange - stock moyen
        } else {
            quantiteColor = "#27ae60"; // Vert - stock suffisant
        }

        Label nameLabel = new Label(m.getNom());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);

        Label stockLabel = new Label("📦 Quantité: " + m.getQuantiteStock());
        stockLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + quantiteColor + "; -fx-font-weight: bold;");

        Label descLabel = new Label("📝 " + (m.getDescription() != null && !m.getDescription().isEmpty() ? m.getDescription() : "Aucune description"));
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        descLabel.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button editBtn = new Button("✏️ Modifier");
        editBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        editBtn.setOnAction(e -> {
            remplirFormulaire(m);
            ScrollPane scrollPane = (ScrollPane) cardGrid.getParent();
            scrollPane.setVvalue(0);
        });

        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
        deleteBtn.setOnAction(e -> supprimerMedicament(m));

        buttonBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(nameLabel, stockLabel, descLabel, buttonBox);
        return card;
    }

    private void supprimerMedicament(Medicament m) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer " + m.getNom() + " ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                medicamentService.supprimerMedicament(m.getId());
                rafraichirListe();
                showAlert("Succès", "Médicament supprimé!", Alert.AlertType.INFORMATION);
            } catch (SQLException e) {
                showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void remplirFormulaire(Medicament m) {
        nomField.setText(m.getNom());
        descriptionArea.setText(m.getDescription());
        quantiteField.setText(String.valueOf(m.getQuantiteStock()));
    }

    @FXML
    private void viderFormulaire() {
        nomField.clear();
        descriptionArea.clear();
        quantiteField.clear();
    }

    @FXML
    private void ajouterMedicament() {
        if (nomField.getText().isEmpty() || quantiteField.getText().isEmpty()) {
            showAlert("Erreur", "Nom et quantité obligatoires", Alert.AlertType.ERROR);
            return;
        }

        try {
            Medicament medicament = new Medicament();
            medicament.setNom(nomField.getText());
            medicament.setDescription(descriptionArea.getText());
            medicament.setQuantiteStock(Integer.parseInt(quantiteField.getText()));

            medicamentService.ajouterMedicament(medicament);
            rafraichirListe();
            viderFormulaire();
            showAlert("Succès", "Médicament ajouté!", Alert.AlertType.INFORMATION);
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Quantité invalide", Alert.AlertType.ERROR);
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void modifierMedicament() {
        String nom = nomField.getText();
        if (nom.isEmpty()) {
            showAlert("Attention", "Remplissez le formulaire", Alert.AlertType.WARNING);
            return;
        }

        try {
            Medicament existing = medicamentList.stream()
                    .filter(m -> m.getNom().equals(nom))
                    .findFirst()
                    .orElse(null);

            if (existing == null) {
                showAlert("Erreur", "Médicament non trouvé", Alert.AlertType.ERROR);
                return;
            }

            existing.setNom(nomField.getText());
            existing.setDescription(descriptionArea.getText());
            existing.setQuantiteStock(Integer.parseInt(quantiteField.getText()));

            medicamentService.modifierMedicament(existing);
            rafraichirListe();
            viderFormulaire();
            showAlert("Succès", "Médicament modifié!", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void supprimerMedicament() {
        String nom = nomField.getText();
        if (nom.isEmpty()) {
            showAlert("Attention", "Remplissez le formulaire", Alert.AlertType.WARNING);
            return;
        }

        Medicament toDelete = medicamentList.stream()
                .filter(m -> m.getNom().equals(nom))
                .findFirst()
                .orElse(null);

        if (toDelete != null) {
            supprimerMedicament(toDelete);
        }
    }

    @FXML
    private void rafraichirListe() {
        try {
            medicamentList.clear();
            medicamentList.addAll(medicamentService.getAllMedicaments());
            filteredList.setAll(medicamentList);
            searchField.clear();
            triCombo.setValue("Nom (A→Z)");
            rechercherMedicament();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void exporterPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setInitialFileName("medicaments_" + timestamp + ".pdf");

        File file = fileChooser.showSaveDialog(cardGrid.getScene().getWindow());
        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file);
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                document.add(new Paragraph("LISTE DES MÉDICAMENTS")
                        .setFontSize(18)
                        .setBold());
                document.add(new Paragraph("Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
                document.add(new Paragraph(" "));

                Table table = new Table(4);
                table.addCell("ID");
                table.addCell("Nom");
                table.addCell("Description");
                table.addCell("Quantité");

                List<Medicament> list = new ArrayList<>(filteredList);
                for (Medicament m : list) {
                    table.addCell(String.valueOf(m.getId()));
                    table.addCell(m.getNom());
                    table.addCell(m.getDescription() != null ? m.getDescription() : "");
                    table.addCell(String.valueOf(m.getQuantiteStock()));
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