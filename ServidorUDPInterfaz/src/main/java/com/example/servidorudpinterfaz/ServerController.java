package com.example.servidorudpinterfaz;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServerController {

    private static final int PORT = 5010;
    private DatagramSocket socket;
    private Set<String> registeredUsernames = new HashSet<>();
    private List<Integer> users = new ArrayList<>();
    private List<InetAddress> address = new ArrayList<>();
    @FXML
    private TextArea logTextArea;

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

                    InetAddress inetAddress = packet.getAddress();
                    int puertoOrigen = packet.getPort();
                    if (!users.contains(puertoOrigen) || !address.contains(inetAddress)) {
                        users.add(puertoOrigen);
                        address.add(inetAddress);
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
            } else {
                forwardTextMessage(packet);
            }
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
            String userName = parts[1];
            String fileName = parts[2];

            // Ruta donde se guardará la imagen.
            String savedImagePath = "C:\\Users\\rafa_\\Downloads\\DescargasServidor" + "\\" + fileName;
            // Creando el archivo.
            File f = new File(savedImagePath);
            // Creando el flujo a través del cual escribiremos el contenido del archivo.
            FileOutputStream outToFile = new FileOutputStream(f);

            // Recibiendo el archivo.
            saveImage(outToFile, serverSocket);
            forwardImagesToClients(f, userName);

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

    // Método para reenviar imágenes a los clientes conectados:
    private void forwardImagesToClients(File imageFile, String userName) {
        try {
            // Creando el socket del cliente.
            DatagramSocket clientSocket = new DatagramSocket();
            String fileName = imageFile.getName();
            // Combinando el nombre de usuario y el nombre del archivo.
            String combinedMessage = "IMAGE|" + userName + "|" + fileName;
            byte[] fileNameBytes = combinedMessage.getBytes();

            // Bucle por cada cliente:
            for (int i = 0; i < users.size(); i++) {
                InetAddress clientAddress = address.get(i); // Cambia esto con la dirección correcta
                int clientPort = users.get(i);

                // Enviando el nombre del archivo al servidor.
                DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, clientAddress, clientPort);
                clientSocket.send(fileStatPacket);
                notification("Archivo enviado a " + clientAddress + " " + clientPort);
                System.out.println("Archivo enviado a " + clientAddress + " " + clientPort);

                // Leyendo el archivo y enviándolo al servidor.
                byte[] fileByteArray = readFileToByteArray(imageFile);
                sendFile(clientSocket, fileByteArray, clientAddress, clientPort);
            }

            // Cerrando el socket del cliente.
            clientSocket.close();
        } catch (Exception e) {
            // Imprimiendo la traza de la excepción en caso de error.
            notification("Error al reenviar imágenes a los clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Logica para transformar imagen a bytes y enviarlo:
    private void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress address, int port) {
        try {
            notification("Enviando file to: " + address + " " + port);
            System.out.println("Enviando file");
            // Para ordenar.
            int sequenceNumber = 0;
            // Para ver si llegamos al final del archivo.
            boolean flag;
            // Para ver si el datagrama se recibió correctamente.
            int ackSequence = 0;

            for (int i = 0; i < fileByteArray.length; i = i + 1021) {
                sequenceNumber += 1;
                // Crear un mensaje
                byte[] message = new byte[1024];
                // Los primeros dos bytes de los datos son para control (integridad y orden del datagrama).
                message[0] = (byte) (sequenceNumber >> 8);
                message[1] = (byte) (sequenceNumber);

                // ¿Hemos llegado al final del archivo?
                if ((i + 1021) >= fileByteArray.length) {
                    // Llegamos al final del archivo (último datagrama a enviar).
                    flag = true;
                    message[2] = (byte) (1);
                } else {
                    // No hemos llegado al final del archivo, seguimos enviando datagramas.
                    flag = false;
                    message[2] = (byte) (0);
                }

                if (!flag) {
                    System.arraycopy(fileByteArray, i, message, 3, 1021);
                } else {
                    // Si es el último datagrama.
                    System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
                }

                // Los datos a enviar.
                DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port);
                socket.send(sendPacket);
                // Enviando los datos.
                notification("Sent: Sequence number = " + sequenceNumber);
                System.out.println("Sent: Sequence number = " + sequenceNumber);

                // ¿Se recibió el datagrama?
                boolean ackRec;
                while (true) {
                    // Cree otro paquete para el reconocimiento de datagramas.
                    byte[] ack = new byte[2];
                    DatagramPacket backpack = new DatagramPacket(ack, ack.length);

                    try {
                        // Esperando que el servidor envíe el acuse de recibo.
                        socket.setSoTimeout(50);
                        socket.receive(backpack);
                        // Calcular el número de secuencia.
                        ackSequence = ((ack[0] & 0xff) << 8) + (ack[1] & 0xff);
                        // Recibimos el ack.
                        ackRec = true;
                    } catch (SocketTimeoutException e) {
                        // No recibimos un acuse de recibo.
                        notification("Socket timed out waiting for ack");
                        System.out.println("Socket timed out waiting for ack");
                        ackRec = false;
                    }

                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        // Si el paquete se recibió correctamente se puede enviar el siguiente paquete.
                        notification("Ack received: Sequence Number = " + ackSequence);
                        System.out.println("Ack received: Sequence Number = " + ackSequence);
                        break;
                    } else {
                        // El paquete no fue recibido, por lo que lo reenviamos.
                        socket.send(sendPacket);
                        notification("Resending: Sequence Number = " + sequenceNumber);
                        System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        } catch (Exception e) {
            notification("Error al enviar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Método que toma un objeto de tipo File como parámetro:
    public byte[] readFileToByteArray(File file) {
        // Declara un objeto FileInputStream para leer bytes desde un archivo
        FileInputStream fis;
        // Crea un arreglo de bytes con la longitud del archivo
        byte[] bArray = new byte[(int) file.length()];
        try {
            // Inicializa el objeto FileInputStream con el archivo proporcionado
            fis = new FileInputStream(file);

            // Lee los bytes desde el archivo y almacena la cantidad de bytes leídos en la variable "bytesRead"
            int bytesRead = fis.read(bArray);

            // Mientras haya más bytes por leer y estén disponibles en el flujo de entrada
            while (bytesRead != -1 && fis.available() > 0) {
                // Lee los bytes restantes y actualiza la variable "bytesRead"
                bytesRead = fis.read(bArray, bytesRead, fis.available());
            }

            // Cierra el flujo de entrada después de leer todos los bytes
            fis.close();
        } catch (IOException ioExp) {
            // En caso de una excepción de E/S (IOException), imprime la traza de la excepción
            ioExp.printStackTrace();
        }
        // Devuelve el arreglo de bytes que contiene los datos del archivo
        return bArray;
    }

}
