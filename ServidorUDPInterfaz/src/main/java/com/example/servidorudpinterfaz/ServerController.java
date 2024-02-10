package com.example.servidorudpinterfaz;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ServerController {

    private static final int PORT = 5010;
    private DatagramSocket socket;
    private Set<String> registeredUsernames = new HashSet<>();
    private ArrayList<Integer> users = new ArrayList<>();
    @FXML
    private TextArea logTextArea;
    private InetAddress address;

    public void initialize() {
        try {
            socket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            notification("Error al iniciar el servidor en el puerto " + PORT);
            Platform.exit();
        }

        notification("Servidor iniciado en el puerto " + PORT);

        Thread serverThread = new Thread(this::runServer);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void runServer() {

        while (true) {
            byte[] incomingData = new byte[1024];

            DatagramPacket packet = new DatagramPacket(incomingData, incomingData.length);

            try {
                socket.receive(packet);
            } catch (IOException e) {
                notification("Error al recibir datos: " + e.getMessage());
            }

            String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            notification("Mensaje recibido: " + message);

            if (message.equalsIgnoreCase("STOP")) {
                notification("Servidor detenido por solicitud del cliente.");
                Platform.exit();
            }
            int puertoOrigen=packet.getPort();
            if (!users.contains(puertoOrigen)) {
                users.add(puertoOrigen);
            }
            forwardTextMessage(packet);
        }
    }

    private void notification(String message) {
        Platform.runLater(() -> logTextArea.appendText(message + "\n"));
    }



    private void forwardTextMessage(DatagramPacket packet) {
        int userPort = packet.getPort();
        byte[] byteMessage = packet.getData();

        for (int puertoDestino : users) {
            if (puertoDestino != userPort) {
                DatagramPacket envio = new DatagramPacket(byteMessage, byteMessage.length, packet.getAddress(), puertoDestino);
                try {
                    socket.send(envio);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }





}
