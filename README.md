# Aplicación de Cliente y Servidor UDP

## Descripción
Esta es una aplicación de chat cliente-servidor que utiliza el protocolo UDP para la comunicación. Permite a los usuarios iniciar sesión con un nombre de usuario único y enviar mensajes de texto o imágenes a otros usuarios conectados al servidor.

## Componentes
- **Cliente:** Interfaz de usuario para que los usuarios inicien sesión y envíen mensajes.
- **Servidor:** Procesa las solicitudes de inicio de sesión, gestiona la comunicación entre los clientes y distribuye los mensajes.

## Estructura del Proyecto
El proyecto está dividido en dos partes principales:
- **Cliente:** Contiene la interfaz gráfica para el cliente.
- **Servidor:** Implementa la lógica del servidor y la comunicación con los clientes `posee interfaz gráfica con info`.

## Uso
### Cliente
1. Ejecuta la clase `LoginController` para iniciar el cliente.

   ![image](https://github.com/Sukera27/ChatUDP/assets/122563964/dde343b7-8888-46c2-b3e4-f7708f3a49e7)

3. Ingresa un nombre de usuario único en el campo proporcionado.
4. Si introduce un usuario ya utilizado muestra un label de error.

   ![image](https://github.com/Sukera27/ChatUDP/assets/122563964/3a5e11c0-74b0-4029-b7b1-944fafaa7e9e)

6. Haz clic en "Iniciar Sesión" para conectarte al servidor.
7. Una vez iniciada sesión, podrás enviar mensajes de texto o imágenes desde la interfaz del chat.

   ![image](https://github.com/Sukera27/ChatUDP/assets/122563964/842ed74f-3ca9-4338-a8cf-46e5536e5956)

### Servidor
1. Ejecuta la clase `ServerController` para iniciar el servidor.
2. El servidor estará escuchando en el puerto predeterminado (5010) para las solicitudes de inicio de sesión y los mensajes de los clientes.

![image](https://github.com/Sukera27/ChatUDP/assets/122563964/63a66501-f55b-4b5b-9ba0-66b636e0fc76)

