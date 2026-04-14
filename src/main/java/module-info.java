module com.medilink {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.medilink to javafx.fxml;
    opens com.medilink.controller to javafx.fxml;
    opens com.medilink.model to javafx.base;

    exports com.medilink;
    exports com.medilink.controller;
    exports com.medilink.model;
}
