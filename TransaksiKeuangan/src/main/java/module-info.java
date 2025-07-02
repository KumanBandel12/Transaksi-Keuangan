module pbo.transaksikeuangan {
    requires javafx.controls;
    requires javafx.fxml;


    opens pbo.transaksikeuangan to javafx.fxml;
    exports pbo.transaksikeuangan;
}