package com.example.servidorudpinterfaz;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
            // Verificar si el mensaje contiene el comando "STOP" independientemente del nombre de usuario
            if (message.toLowerCase().contains("stop")) {
                notification("Servidor detenido por solicitud del cliente.");
                Platform.exit();
                return; // Terminar el método runServer
            }

            String[] messageParts = message.split("\\|");

            if (messageParts[0].equals("CHECK_USERNAME")) {
                // Verifica si el nombre de usuario está disponible
                String username = messageParts[1];
                if (!registeredUsernames.contains(username)) {
                    // Envía una respuesta al cliente indicando que el nombre de usuario está disponible
                    sendResponse(packet.getAddress(), packet.getPort(), "USERNAME_AVAILABLE");

                    // Agrega el nombre de usuario a la lista de disponibles
                    registeredUsernames.add(username);

                    int puertoOrigen = packet.getPort();
                    if (!users.contains(puertoOrigen)) {
                        users.add(puertoOrigen);
                    }

                    // Imprime en la consola la información del cliente.
                    notification("Cliente aceptado - IP: " + packet.getAddress() + ", PORT: " + packet.getPort());
                    System.out.println("Cliente aceptado - IP: " + packet.getAddress() + ", PORT: " + packet.getPort());
                } else {
                    // Envía una respuesta al cliente indicando que el nombre de usuario no está disponible.
                    sendResponse(packet.getAddress(),packet.getPort(), "USERNAME_UNAVAILABLE");
                }

            } else if (messageParts[0].equals("IMAGE")) {
                // Si el mensaje es de tipo imagen, llamar al método receiveImage para manejarlo
                receiveImage(packet, socket);
            }


            forwardTextMessage(packet);
        }
    }

    private void notification(String message) {
        Platform.runLater(() -> logTextArea.appendText(message + "\n"));
    }



    private void forwardTextMessage(DatagramPacket packet) {
        byte[] byteMessage = packet.getData();
        int messageLength = packet.getLength();
        InetAddress clientAddress = packet.getAddress();

        for (int puertoDestino : users) {
            if (puertoDestino != packet.getPort()) {
                DatagramPacket envio = new DatagramPacket(byteMessage, messageLength, clientAddress, puertoDestino);
                try {
                    socket.send(envio);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    // Método para enviar una respuesta a un cliente a través de un socket UDP:
    private void sendResponse(InetAddress clientAddress, int clientPort, String response) {
        try {
            // Convierte la respuesta (String) en un arreglo de bytes.
            byte[] sendData = response.getBytes();

            // Crea un paquete UDP que contiene los datos a enviar, la longitud de los datos,
            // la dirección del cliente y el puerto del cliente.
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);

            // Envía el paquete a través del socket UDP.
            socket.send(sendPacket);

            // Imprime en la consola la información sobre la respuesta enviada.
            notification("Respuesta enviada al cliente - IP: " + clientAddress + ", Puerto: " + clientPort + ", Respuesta: " + response);
            System.out.println("Respuesta enviada al cliente - IP: " + clientAddress + ", Puerto: " + clientPort + ", Respuesta: " + response);
        } catch (IOException e) {
            // En caso de error, imprime la traza de la excepción.
            notification("Error al enviar respuesta al cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método para recibir mensajes de imagen:
    private void receiveImage(DatagramPacket receivePacket, DatagramSocket serverSocket) {
        try {
            // Leyendo el nombre en bytes.
            byte [] data = receivePacket.getData();
            // Convirtiendo el nombre a cadena de texto.
            String combinedMessage  = new String(data, 0, receivePacket.getLength());
            String[] parts = combinedMessage.split("\\|");
            String userName = parts[0];
            String fileName = parts[1];

            // Ruta donde se guardará la imagen.
            String savedImagePath = "C:\\Users\\rafa_\\Downloads\\DescargasServidor" + "\\" + fileName;
            // Creando el archivo.
            File f = new File(savedImagePath);
            // Creando el flujo a través del cual escribiremos el contenido del archivo.
            FileOutputStream outToFile = new FileOutputStream(f);

            // Recibiendo el archivo.
            saveImage(outToFile, serverSocket);



        } catch (FileNotFoundException e) {
            // Lanzando una excepción de tiempo de ejecución en caso de no encontrar el archivo.
            throw new RuntimeException(e);
        }
    }

    // Logica para recibir imagen y guardarlo:
    private void saveImage(FileOutputStream outToFile, DatagramSocket socket) {
        try {
            // ¿Hemos llegado al final del archivo?
            boolean flag;
            // Orden de las secuencias.
            int sequenceNumber;
            // La última secuencia encontrada.
            int foundLast = 0;

            while (true) {
                // Donde se almacena los datos del datagrama recibido.
                byte[] message = new byte[1024];
                // Donde almacenamos los datos que se escribirán en el archivo.
                byte[] fileByteArray = new byte[1021];
                // Recibir paquete y obtener los datos.
                DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
                socket.receive(receivedPacket);
                // Datos que se escribirán en el archivo.
                message = receivedPacket.getData();
                // Obtener puerto y dirección para enviar el acuse de recibo.
                InetAddress address = receivedPacket.getAddress();
                int port = receivedPacket.getPort();
                // Obtener número de secuencia.
                sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
                // Verificar si llegamos al último datagrama (fin del archivo).
                flag = (message[2] & 0xff) == 1;
                // Si el número de secuencia es el último visto + 1, entonces es correcto.
                // Obtenemos los datos del mensaje y escribimos el acuse de recibo de que se ha recibido correctamente.
                if (sequenceNumber == (foundLast + 1)) {
                    // Establecer el último número de secuencia como el que acabamos de recibir.
                    foundLast = sequenceNumber;
                    // Obtener datos del mensaje.
                    System.arraycopy(message, 3, fileByteArray, 0, 1021);
                    // Escribir los datos recuperados en el archivo e imprimir el número de secuencia recibido.
                    outToFile.write(fileByteArray);
                    //System.out.println("Received: Sequence number:" + foundLast);
                    // Enviar acuse de recibo.
                    sendAck(foundLast, socket, address, port);
                } else {
                    notification("Número de secuencia esperado: " + (foundLast + 1) + " pero se recibió " + sequenceNumber + ". DESCARTANDO");
                    System.out.println("Número de secuencia esperado: " + (foundLast + 1) + " pero se recibió " + sequenceNumber + ". DESCARTANDO");
                    // Reenviar el acuse de recibo.
                    sendAck(foundLast, socket, address, port);
                }
                // Verificar el último datagrama.
                if (flag) {
                    notification("Imagen recibida");
                    System.out.println("Imagen recibida");
                    outToFile.close();
                    break;
                }
            }
        } catch (IOException e) {
            notification("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Método que envía un acuse de recibo (acknowledgement) a través de un socket UDP:
    private void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) {
        try {
            // Se crea un array de bytes para almacenar el paquete de acuse de recibo (acknowledgement).
            byte[] ackPacket = new byte[2];

            // Se asignan los bytes correspondientes al número de secuencia del último paquete recibido.
            ackPacket[0] = (byte) (foundLast >> 8);
            ackPacket[1] = (byte) (foundLast);

            // Se crea un DatagramPacket que contiene el paquete de acuse de recibo, la longitud del paquete,
            // la dirección IP de destino y el puerto de destino.
            DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);

            // Se envía el paquete de acuse de recibo a través del socket.
            socket.send(acknowledgement);

            // Se imprime un mensaje (comentado) indicando el número de secuencia enviado en el acuse de recibo.
            notification("Acuse de recibo enviado: Número de secuencia = " + foundLast);
            System.out.println("Acuse de recibo enviado: Número de secuencia = " + foundLast);
        } catch (Exception e) {
            // Se imprime la traza de la excepción en caso de error durante el envío del acuse de recibo.
            notification("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }











}
