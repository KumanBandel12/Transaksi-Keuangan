package pbo.transaksikeuangan;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserManager userManager = UserManager.getInstance();

    // Dalam LoginController.java

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        System.out.println("DEBUG: Attempting login for username: " + username); // Tambahkan ini

        User user = userManager.validateUser(username, password);
        if (user != null) {
            System.out.println("DEBUG: Login successful for user: " + user.getUsername()); // Tambahkan ini
            SessionManager.setCurrentUser(user);
            SharedDataManager.resetInstance();
            SharedDataManager.initializeForUser(username);

            // Pindah ke Main Menu
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } else {
            System.out.println("DEBUG: Login failed for username: " + username + ". Invalid credentials."); // Tambahkan ini
            errorLabel.setText("Username atau password salah.");
        }
    }

    @FXML
    private void handleGoToRegister(ActionEvent event) throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/pbo/transaksikeuangan/Register.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
