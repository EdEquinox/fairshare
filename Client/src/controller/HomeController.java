package controller;

import communication.ClientService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import utils.NavigationManager;
import utils.Routes;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    public ClientService clientService;

    @FXML
    public void handleLoginAction() {
        NavigationManager.switchScene(Routes.LOGIN);
    }


    @FXML
    public void handleRegisterAction() {
        NavigationManager.switchScene(Routes.REGISTER);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // TODO: Do Thread to connect to server then
        //  if error show alert dialog
        //  if show database version and info like that to show to user that the server is up and running.
        this.clientService = ClientService.getInstance();
        // this.clientService.connectToServer();
    }
}
