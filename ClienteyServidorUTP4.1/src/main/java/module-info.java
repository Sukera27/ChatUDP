module com.example.clienteyservidorutp {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.clienteyservidorutp to javafx.fxml;
    exports com.example.clienteyservidorutp;
}