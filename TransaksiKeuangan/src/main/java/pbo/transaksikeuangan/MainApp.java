package pbo.transaksikeuangan;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        try {
            // Load the Login FXML as the initial scene
            Parent root = FXMLLoader.load(getClass().getResource("/pbo/transaksikeuangan/Login.fxml"));
            Scene scene = new Scene(root, 800, 600);

            // Optional: Add CSS for styling (e.g., for green theme)
            // scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

            stage.setTitle("Sistem Manajemen Keuangan");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading FXML: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
