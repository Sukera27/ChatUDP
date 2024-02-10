package com.example.clienteyservidorutp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class LoginController {

    @FXML
    private TextField usernameTextField;

    @FXML
    private Button button;

    public static DatagramSocket socket;

    private static final int serverPort = 5010;

    public static String username;




    public void initialize() {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            // Manejar el error de inicialización del socket
            e.printStackTrace();
            Platform.exit();
        }
    }

    @FXML
    private void loginButtonAction() {
        String username = usernameTextField.getText().trim();
        if (verificarNombreUsuario(username)) {
            chatView(username);
        }
    }
    private boolean verificarNombreUsuario(String username) {
        try {
            // Envía el nombre de usuario al servidor para su verificación.
            String message = "CHECK_USERNAME|" + username;
            byte[] sendData = message.getBytes();
            InetAddress serverAddress = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            socket.send(sendPacket);

            // Recibe la respuesta del servidor.
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            // Convierte la respuesta a String y devuelve true si el nombre de usuario está disponible.
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            if (response.equals("USERNAME_AVAILABLE")) {
                LoginController.username = username;

                return true;
            } else {
                // Muestra un mensaje al usuario indicando que el nombre no está disponible.
                System.out.println("El nombre de usuario '" + username + "' no está disponible. Introduce otro nombre.");
                return false;
            }
        } catch (IOException e) {
            // Muestra un mensaje al usuario indicando que ha ocurrido un error.
            System.out.println("Error al comunicarse con el servidor. Inténtalo de nuevo más tarde.");
            e.printStackTrace();
            return false;
        }
    }


    private void chatView(String user){
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
