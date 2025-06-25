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

    @FXML
    private void handleLogin(ActionEvent event) throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        User user = userManager.validateUser(username, password);
        if (user != null) {
            SessionManager.setCurrentUser(user);

            // Initialize SharedDataManager for the logged-in user
            // This ensures each user has their own transaction history
            // For simplicity, we'll re-initialize TransaksiManager with 0 initial balance
            // In a real app, you might load user-specific balance/transactions here.
            SharedDataManager.resetInstance(); // Reset singleton to ensure new user data
            SharedDataManager.initializeForUser(username); // Initialize for specific user

            // Pindah ke Main Menu
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } else {
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
