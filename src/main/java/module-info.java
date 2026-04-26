module gdons {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires java.sql;
    requires java.net.http;
    requires java.prefs;
    requires jbcrypt;
    requires com.google.gson;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;
    requires commons.logging;

    opens test to javafx.graphics;
    opens controllers to javafx.fxml;
    opens userfx to javafx.fxml;
    opens view to javafx.fxml;
    opens com.medilink.controller to javafx.fxml;
    opens models to javafx.base;
}
