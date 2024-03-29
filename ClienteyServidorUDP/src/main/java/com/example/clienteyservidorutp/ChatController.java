package com.example.clienteyservidorutp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.*;

public class ChatController {
    @FXML
    private TextArea messageTextArea;

    @FXML
    private TextFlow messageTextFlow;

    @FXML
    private TextField inputTextField;

    @FXML
    private Button sendButton;

    @FXML
    private Button imagenButton;

    private static InetAddress serverAddress;
    private static final int serverPort = 5010;

    private static final int clientPort = 6010;



    // Establece el nombre de usuario.


    @FXML
    private void initialize() {

        try {

            serverAddress = InetAddress.getByName("localhost");
            // Iniciar hilo para recibir mensajes
            Thread receiveThread = new Thread(this::receiveMessage);
            receiveThread.setDaemon(true);
            receiveThread.start();


        } catch (  UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void sendMessage() {
        String message = inputTextField.getText();
        message = LoginController.username + ": " + message;

        // Construir el datagrama a enviar
        byte[] messageBytes = message.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, serverPort);

        // Enviar el datagrama
        try {
            LoginController.socket.send(sendPacket);
            // Mostrar el mensaje en el TextArea
            appendMessage(message, true); // Indicamos que el mensaje fue enviado por el usuario local

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
                LoginController.socket.receive(receivedPacket);

                String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());

                // Verificar si el mensaje no comienza con "CHECK_USERNAME"
                if (!receivedMessage.startsWith("CHECK_USERNAME")) {
                    // Verificar si el mensaje contiene el delimitador ": "
                    if (receivedMessage.contains(": ")) {
                        String[] parts = receivedMessage.split(": ", 2);
                        String sender = parts[0]; // Nombre de usuario
                        String content = parts[1]; // Contenido del mensaje

                        appendMessage(sender + ": " + content, false); // Indicamos que el mensaje no fue enviado por el usuario local

                    } else if (receivedMessage.startsWith("IMAGE")) {
                        byte [] data = receivedPacket.getData();
                        // Converting the name to string
                        String combinedMessage  = new String(data, 0, receivedPacket.getLength());
                        String[] parts = combinedMessage.split("\\|");
                        String receivedUserName = parts[1];
                        String fileName = parts[2];

                        // Se construye la ruta donde se guardará la imagen.
                        String savedImagePath = "C:\\Users\\rafa_\\Downloads\\DescargasCliente" + "\\" + fileName;
                        // Se crea el objeto File para representar el archivo de imagen.
                        File f = new File(savedImagePath);
                        // Se crea un FileOutputStream para escribir el contenido del archivo.
                        FileOutputStream outToFile = new FileOutputStream(f);
                        // Se llama a la función saveImage para recibir y guardar la imagen.
                        saveImage(outToFile, LoginController.socket);
                        // Si el mensaje es una imagen, llamar al método saveImage
                        // Llamar al método appendImage para agregar la imagen al TextFlow
                        appendImage(savedImagePath, false); // Indicamos que la imagen fue enviada por el usuario remoto


                    } else {
                        // Si el mensaje no contiene el delimitador y no es una imagen, mostrar el mensaje completo
                        appendMessage(receivedMessage, false); // Indicamos que el mensaje no fue enviado por el usuario local
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    // Método auxiliar para agregar mensajes al TextFlow con un estilo específico
    private void appendMessage(String message, boolean sentByUser) {
        Platform.runLater(() -> {
            Text text = new Text(message + "\n");
            if (message.startsWith("IMAGE")) {
                text.setFill(Color.TRANSPARENT); // Establecer el color del texto como transparente para el texto "IMAGE username"
            } else {
                text.setFill(sentByUser ? Color.GREEN : Color.GRAY); // Establecer el color de la fuente según el remitente
            }
            messageTextFlow.getChildren().add(text);
        });
    }


    // Método auxiliar para agregar imágenes al TextFlow
    private void appendImage(String image, boolean sentByUser) {
        Platform.runLater(() -> {
            // Crear un Text para representar el texto "IMAGE username"
            Text imageText = new Text("IMAGE " + LoginController.username + "\n");
            imageText.setFill(Color.TRANSPARENT); // Establecer el color del texto como transparente

            // Crear un ImageView para la imagen
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(100); // Ajustar el ancho de la imagen
            imageView.setPreserveRatio(true); // Mantener la relación de aspecto de la imagen

            // Crear un contenedor HBox para la imagen y el texto
            HBox imageBox = new HBox(imageText, imageView);
            imageBox.setAlignment(sentByUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            messageTextFlow.getChildren().add(imageBox);
        });
    }



    // Método para enviar imagen:
    @FXML
    private void sendImageButton() {
        try {
            // Crear un objeto FileChooser para permitir al usuario seleccionar un archivo de imagen.
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose Image File");

            // Mostrar el diálogo de selección de archivo y obtener el archivo seleccionado.
            File selectedFile = fileChooser.showOpenDialog(null);

            // Verificar si se seleccionó un archivo antes de continuar.
            if (selectedFile != null) {
                // Llamar al método sendImage() para procesar y enviar la imagen seleccionada.
                sendImage(selectedFile);
            }
        } catch (Exception e) {
            // Manejar cualquier excepción imprevista e imprimir la traza de la pila.
            e.printStackTrace();
        }
    }
    // Logica para enviar imagen:
    private void sendImage(File imageFile) {
        try {
            // Crear un socket Datagram para la comunicación de red.
            DatagramSocket clientSocket = new DatagramSocket();
            // Obtener la dirección IP del servidor desde la clase UDPLoginController.
            InetAddress serverAddress = InetAddress.getByName("localhost");

            // Enviar el nombre del archivo al servidor en forma de paquete Datagram.
            String fileName = imageFile.getName();
            String combinedMessage = "IMAGE|" + LoginController.username + "|" + fileName;
            byte[] fileNameBytes = combinedMessage.getBytes();
            DatagramPacket fileStatPacket = new DatagramPacket(fileNameBytes, fileNameBytes.length, serverAddress, serverPort);
            clientSocket.send(fileStatPacket);

            // Leer el contenido del archivo a un arreglo de bytes y enviarlo al servidor.
            byte[] fileByteArray = readFileToByteArray(imageFile);
            sendFile(clientSocket, fileByteArray, serverAddress);
        } catch (Exception ex) {
            // Manejar cualquier excepción imprevista e imprimir la traza de la pila.
            ex.printStackTrace();
        }
    }

    // Logica para transformar imagen a bytes y enviarlo:
    private static void sendFile(DatagramSocket socket, byte[] fileByteArray, InetAddress serverAddress) {
        try {
            System.out.println("Sending file");
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
                DatagramPacket sendPacket = new DatagramPacket(message, message.length, serverAddress, serverPort);
                socket.send(sendPacket);
                // Enviando los datos.
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
                        System.out.println("Socket timed out waiting for ack");
                        ackRec = false;
                    }

                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        // Si el paquete se recibió correctamente se puede enviar el siguiente paquete.
                        System.out.println("Ack received: Sequence Number = " + ackSequence);
                        break;
                    } else {
                        // El paquete no fue recibido, por lo que lo reenviamos.
                        socket.send(sendPacket);
                        System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Método que toma un objeto de tipo File como parámetro:
    public static byte[] readFileToByteArray(File file) {
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

    private static void saveImage(FileOutputStream outToFile, DatagramSocket socket) {
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
                    System.out.println("Expected sequence number: " + (foundLast + 1) + " but received " + sequenceNumber + ". DISCARDING");
                    // Reenviar el acuse de recibo.
                    sendAck(foundLast, socket, address, port);
                }
                // Verificar el último datagrama.
                if (flag) {
                    System.out.println("Image received");
                    outToFile.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) {
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
            System.out.println("Sent ack: Sequence Number = " + foundLast);
        } catch (Exception e) {
            // Se imprime la traza de la excepción en caso de error durante el envío del acuse de recibo.
            e.printStackTrace();
        }
    }






}
