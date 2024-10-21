module pt.edequinox.fairshare {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;

    opens pt.edequinox.fairshare.client to javafx.fxml;
    exports pt.edequinox.fairshare.client;
    exports pt.edequinox.fairshare.model;
    opens pt.edequinox.fairshare.model to javafx.fxml;
}