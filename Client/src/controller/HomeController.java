package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class HomeController {

    @FXML
    public void handleStartAction(ActionEvent event) {
        // Quando o botão for clicado, você pode carregar uma nova tela ou exibir uma mensagem
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informação");
        alert.setHeaderText("Início da Aplicação");
        alert.setContentText("Você iniciou a aplicação com sucesso!");
        alert.showAndWait();

        // Aqui você pode carregar outra tela se necessário, por exemplo:
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/layouts/OutraTela.fxml"));
        // Parent root = loader.load();
        // Scene scene = new Scene(root);
        // Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        // stage.setScene(scene);
        // stage.show();
    }

    public void handleLoginAction(ActionEvent actionEvent) {
    
    }

    public void handleRegisterAction(ActionEvent actionEvent) {

    }
}
