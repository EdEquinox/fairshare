package pt.edequinox.fairshare.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    static String ip;

    @Override
    public void start(Stage stage) throws IOException {

        GridPane root = new GridPane();
        root.add(new Label("Hello, JavaFX!"), 0, 0);
        Scene scene = new Scene(root, 300, 200);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        ip = args[0];
        launch(args);
    }
}