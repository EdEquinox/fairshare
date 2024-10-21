package pt.edequinox.fairshare.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import pt.edequinox.fairshare.client.model.ClientManager;

public class LandingUI extends BorderPane {

    ClientManager clientManager;
    Button loginBtn, exitBtn, registerBtn;
    TextField emailTF, passwordTF;
    Text welcomeText;

    public LandingUI(ClientManager clientManager) {
        this.clientManager = clientManager;
        createViews();
        registerHandlers();
        update();
    }

    private void createViews() {

        welcomeText = new Text("Welcome to FairShare");

        loginBtn = new Button("Login");
        exitBtn = new Button("Exit");
        registerBtn = new Button("Register");

        emailTF = new TextField();
        passwordTF = new TextField();

        HBox hbox =  new HBox(loginBtn, exitBtn, registerBtn);
        VBox vbox = new VBox(emailTF, passwordTF, hbox);

        this.setCenter(vbox);

    }

    private void registerHandlers() {

        clientManager.addPropertyChangeListener(evt -> {
            Platform.runLater(this::update);
        });

        loginBtn.setOnAction(e -> {
            this.setCenter(new MainMenuUI(clientManager));
            clientManager.login(emailTF.getText(), passwordTF.getText());
        });

    }

    private void update() {
    }
}
