package pbo.transaksikeuangan;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

public class PengaturanController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    private SharedDataManager sharedDataManager;
    private TransaksiManager transaksiManager;

    @FXML
    public void initialize() {
        sharedDataManager = SharedDataManager.getInstance();
        transaksiManager = sharedDataManager.getTransaksiManager();
    }

    @FXML
    public void switchToMainMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
        root = loader.load();
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Menu Utama");
        stage.show();
    }

    @FXML
    public void showAnalisis(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/AnalisisPemasukanView.fxml"));
        root = loader.load();
        stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Analisis Pemasukan");
        stage.show();
    }

    @FXML
    public void exportToExcel(ActionEvent event) {
        try {
            AnalisisPemasukanManager analisisManager = new AnalisisPemasukanManager(transaksiManager);
            LocalDate minDate = LocalDate.of(2000, 1, 1);
            LocalDate maxDate = LocalDate.of(2099, 12, 31);

            analisisManager.exportAllTransactionsToCsv(minDate, maxDate);

            showSuccessAlert("Export Berhasil", "Data transaksi berhasil diekspor ke 'semua_transaksi.csv'!");
        } catch (Exception e) {
            showAlert("Export Gagal", "Terjadi kesalahan saat mengekspor data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Logout");
        confirmAlert.setHeaderText("Logout dari Aplikasi");
        confirmAlert.setContentText("Apakah Anda yakin ingin logout?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SessionManager.logout();
            SharedDataManager.resetInstance();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/Login.fxml"));
            root = loader.load();
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        }
    }

    @FXML
    public void resetAllData(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Reset Data");
        confirmAlert.setHeaderText("PERINGATAN: Aksi ini tidak bisa dibatalkan!");
        confirmAlert.setContentText("Apakah Anda benar-benar yakin ingin menghapus SEMUA data transaksi dan mereset saldo ke 0?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            sharedDataManager.resetData();
            showSuccessAlert("Reset Berhasil", "Semua data berhasil direset!");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void kembaliKeMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


    @FXML
    public void switchToPengaturan(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/Pengaturan.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Pengaturan");
        stage.show();
    }
}
