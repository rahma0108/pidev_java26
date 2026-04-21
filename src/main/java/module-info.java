module gdons {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires org.apache.pdfbox;
    requires org.apache.fontbox;
    requires commons.logging;

    opens test to javafx.graphics;
    opens controllers to javafx.fxml;
    opens models to javafx.base;
}
