package com.medilink.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainController {

    @FXML
    private BorderPane rootPane;

    @FXML
    public void initialize() {
        loadPage("Front.fxml");
    }

    @FXML
    private void onShowFront() {
        loadPage("Front.fxml");
    }

    @FXML
    private void onShowBack() {
        loadPage("Back.fxml");
    }

    private void loadPage(String fxml) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/com/medilink/" + fxml));
            rootPane.setCenter(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
