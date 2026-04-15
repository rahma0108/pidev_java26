module gdons {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens test to javafx.graphics;
    opens controllers to javafx.fxml;
    opens models to javafx.base;

    /* FXML + point d’entrée JavaFX (obligatoire pour MediLinkFxApp et les *.fxml sous view/) */
    opens view to javafx.fxml;
    opens esprit.tn.pidev to javafx.fxml;

    exports esprit.tn.pidev;
    exports view;
    exports models;
    exports services;
    exports controllers;
    exports exceptions;
    exports utils;
    exports interfaces;
}
