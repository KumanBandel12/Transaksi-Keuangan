package pbo.transaksikeuangan;

import javafx.scene.control.Alert;

public class AlertHelper {
    public static void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}