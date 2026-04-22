package com.medilink.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class MainController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Button btnFront;
    @FXML
    private Button btnBack;

    @FXML
    public void initialize() {
        loadPage("Front.fxml");
        setActiveNav(true);
    }

    @FXML
    private void onShowFront() {
        loadPage("Front.fxml");
        setActiveNav(true);
    }

    @FXML
    private void onShowBack() {
        loadPage("Back.fxml");
        setActiveNav(false);
    }

    private void loadPage(String fxml) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/com/medilink/" + fxml));
            rootPane.setCenter(page);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(boolean frontActive) {
        btnFront.getStyleClass().remove("nav-button-active");
        btnBack.getStyleClass().remove("nav-button-active");
        if (frontActive) {
            btnFront.getStyleClass().add("nav-button-active");
        } else {
            btnBack.getStyleClass().add("nav-button-active");
        }
    }
}
