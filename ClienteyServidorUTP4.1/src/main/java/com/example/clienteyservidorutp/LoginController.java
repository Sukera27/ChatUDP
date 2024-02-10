package com.example.clienteyservidorutp;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;

public class LoginController {

    @FXML
    private TextField usernameTextField;

    @FXML
    private Button button;


    @FXML
    private void loginButtonAction() {
        String username = usernameTextField.getText().trim();
        if (!username.isEmpty()) {
            // Cargar la nueva interfaz (Chat.fxml)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent root;
            try {
                root = loader.load();
                Scene scene = new Scene(root);

                // Obtener la etapa actual y establecer la nueva escena
                Stage stage = (Stage) button.getScene().getWindow();
                stage.setScene(scene);

                // Configurar el controlador del chat si es necesario
                ChatController chatController = loader.getController();
                // Puedes pasar información o configuraciones al controlador del chat aquí si es necesario

                // Mostrar la nueva interfaz
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                // Manejar la excepción de carga del archivo FXML
            }
        }
    }



}
