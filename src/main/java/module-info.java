module com.remotedesktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;
    requires java.desktop;

    opens com.remotedesktop.client to javafx.fxml;
    opens com.remotedesktop.server to javafx.fxml;

    exports com.remotedesktop.client;
    exports com.remotedesktop.server;
    exports com.remotedesktop.common;
}
