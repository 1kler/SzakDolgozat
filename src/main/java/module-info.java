module com.example.onepix {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;


    opens com.example.onepix to javafx.fxml;
    exports com.example.onepix;
}