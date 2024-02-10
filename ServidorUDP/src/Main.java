import java.io.IOException;
        import java.net.DatagramPacket;
        import java.net.DatagramSocket;
        import java.net.InetAddress;
        import java.net.SocketException;
        import java.util.ArrayList;
        import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        //Servidor
        byte[] bufer = new byte[1024];//para recibir el datagrama
        //Asocio el socket al puerto 6010
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(5010);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        //construyo datagrama a recibir
        DatagramPacket recibo = new DatagramPacket(bufer, bufer.length);
        List<Integer> listaPuertosDestino=new ArrayList();
        while (true) {
            System.out.println("Server: esperando Datagrama .......... ");
            socket.receive(recibo);

            // Obtener información del paquete recibido
            InetAddress iPOrigen = recibo.getAddress();
            int puertoOrigen = recibo.getPort();

            String paquete = new String(recibo.getData(), 0, recibo.getLength());

            System.out.println("Server: contenido del Paquete -> " + paquete.trim());
            System.out.println("Server: puerto origen del mensaje -> " + puertoOrigen);
            System.out.println("Server: IP de origen -> " + iPOrigen.getHostAddress());
            System.out.println("Server: puerto destino del mensaje -> " + socket.getLocalPort());
            System.out.println("=============================");

            if (!listaPuertosDestino.contains(puertoOrigen)) {
                listaPuertosDestino.add(puertoOrigen);
            }

            if (paquete.equalsIgnoreCase("STOP")) {
                break;
            }

            byte[] respuesta = paquete.getBytes();

            for (int puertoDestino : listaPuertosDestino) {
                if (puertoDestino != puertoOrigen) {
                    DatagramPacket envio = new DatagramPacket(respuesta, respuesta.length, iPOrigen, puertoDestino);
                    socket.send(envio);
                }
            }
        }

        socket.close();
    }
}