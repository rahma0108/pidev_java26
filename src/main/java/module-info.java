module gdons {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires java.prefs;
    requires java.desktop;
    requires java.net.http;
    requires com.google.gson;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;
    requires commons.logging;

    opens test to javafx.graphics;
    opens controllers to javafx.fxml;
    opens models to javafx.base;
}
