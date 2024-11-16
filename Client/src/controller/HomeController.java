package controller;

import communication.ClientService;
import communication.ClientServiceAware;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import utils.NavigationManager;
import utils.Routes;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements ClientServiceAware, Initializable {

    public ClientService clientService;

    public void handleLoginAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.LOGIN.toString().toLowerCase());
    }

    @FXML
    public void handleRegisterAction(ActionEvent actionEvent) {
        NavigationManager.switchScene(Routes.REGISTER.toString().toLowerCase());
    }

    @Override
    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.clientService.connectToServer();
    }
}
