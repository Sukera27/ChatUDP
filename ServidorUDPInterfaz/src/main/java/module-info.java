module com.example.servidorudpinterfaz {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.servidorudpinterfaz to javafx.fxml;
    exports com.example.servidorudpinterfaz;
}