package com.example.clienteyservidorutp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.*;

public class ChatController {
    @FXML
    private TextArea messageTextArea;

    @FXML
    private TextField inputTextField;

    @FXML
    private Button sendButton;

    private static DatagramSocket socket;
    private static InetAddress serverAddress;
    private static final int serverPort = 5010;
    private static final int clientPort = 6010;

    @FXML
    private void initialize() {
        try {
            // Inicializando el socket y obteniendo la dirección IP del servidor
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName("localhost");


            // Iniciar hilo para recibir mensajes
            Thread receiveThread = new Thread(this::receiveMessage);
            receiveThread.setDaemon(true);
            receiveThread.start();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void sendMessage() {
        String message = inputTextField.getText();

        // Construir el datagrama a enviar
        byte[] messageBytes = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, serverPort);

        // Enviar el datagrama
        try {
            socket.send(sendPacket);
            // Mostrar el mensaje en el TextArea
            messageTextArea.appendText("Mensaje enviado: " + message + "\n");

            // Limpiar el TextField después de enviar el mensaje
            inputTextField.clear();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @FXML
    private void receiveMessage() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivedPacket);

                String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());

                Platform.runLater(() -> {
                    messageTextArea.appendText("Mensaje recibido: " + receivedMessage + "\n");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean validateUsername(String username) {
        byte[] validationRequest = ("validate;" + username).getBytes();
        DatagramPacket validationPacket = new DatagramPacket(validationRequest, validationRequest.length, serverAddress, serverPort);

        try {
            socket.send(validationPacket);

            // Esperar la respuesta del servidor
            byte[] responseBuffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            String responseMessage = new String(responsePacket.getData(), 0, responsePacket.getLength());
            return responseMessage.equals("valid");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void handleClose() {
        // Cerrar el socket cuando se cierre la aplicación
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
