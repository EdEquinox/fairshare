package controller;

import communication.ClientService;
import communication.ClientServiceAware;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements ClientServiceAware, Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @Override
    public void setClientService(ClientService clientService) {

    }
}
