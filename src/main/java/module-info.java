/**
 * Module JPMS pour l'application MediLink / JavaFX (test).
 * Les {@code requires} suivent les Automatic-Module-Name des JAR (Maven Central) ou la dérivation JDK du nom de fichier.
 */
module gdons {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires javafx.media;

    requires java.sql;
    requires java.prefs;
    requires java.desktop;
    requires java.net.http;
    /* WebView / JSObject (netscape.javascript) */
    requires jdk.jsobject;
    /* TurnstileController : com.sun.net.httpserver */
    requires jdk.httpserver;

    requires org.apache.pdfbox;
    requires org.apache.fontbox;
    requires commons.logging;
    requires jakarta.mail;

    requires com.google.gson;
    /* google-api-client JAR */
    requires google.api.client;
    /* google-http-client JAR */
    requires com.google.api.client;
    requires com.google.api.client.json.gson;
    requires com.google.api.client.auth;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    /* google-api-services-oauth2-v2-rev157-1.25.0.jar → google.api.services.oauth2.v2.rev157 */
    requires google.api.services.oauth2.v2.rev157;

    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;

    opens controllers to javafx.fxml;
    opens userfx to javafx.fxml, javafx.graphics;
    opens view to javafx.fxml;
    opens com.medilink.controller to javafx.fxml;
    opens esprit.tn.pidev to javafx.fxml, javafx.graphics;
    opens test to javafx.fxml, javafx.graphics;
    opens models to javafx.base;
    opens ui to javafx.graphics;
}
