package pbo.transaksikeuangan;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;

    private final UserManager userManager = UserManager.getInstance();

    @FXML
    private void handleRegister(ActionEvent event) throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username dan password tidak boleh kosong.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Password dan konfirmasi password tidak cocok.");
            return;
        }

        boolean isRegistered = userManager.registerUser(username, password);

        if (isRegistered) {
            // Jika berhasil, kembali ke halaman login
            AlertHelper.showInformation("Registrasi Berhasil", "Akun berhasil dibuat. Silakan login.");
            handleGoToLogin(event);
        } else {
            errorLabel.setText("Username '" + username + "' sudah digunakan.");
        }
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}