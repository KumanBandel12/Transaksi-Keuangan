module pbo.transaksikeuangan {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens pbo.transaksikeuangan to javafx.fxml;
    exports pbo.transaksikeuangan;
}