module gdons {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires com.google.gson;
    requires jbcrypt;
    requires jakarta.mail;

    opens test to javafx.graphics;
    opens controllers to javafx.fxml;
    opens models to javafx.base;
    opens view to javafx.fxml;
    opens userfx to javafx.fxml;
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
