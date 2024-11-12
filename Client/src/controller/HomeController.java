package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import utils.NavigationManager;

public class HomeController {

    public void handleLoginAction(ActionEvent actionEvent) {
    
    }

    @FXML
    public void handleRegisterAction(ActionEvent actionEvent) {
        // Navegar para a tela de registro
        NavigationManager.switchScene((Node) actionEvent.getSource(), "register.fxml");
    }
}
