// src/main/java/pbo/transaksikeuangan/MenuController.java
package pbo.transaksikeuangan;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class MenuController implements saldoObserver {
    @FXML private Label jumlahSaldoLabel;
    @FXML private Label tanggalLabel;
    @FXML private Label saldoPemasukanLabel; // Tambahkan ini jika belum ada di FXML
    @FXML private Label saldoPengeluaranLabel; // Tambahkan ini jika belum ada di FXML
    @FXML private ListView<String> historyList;
    @FXML private Button pensilButton; // Tombol tambah di MainMenu

    @FXML private Stage stage;
    @FXML private Scene scene;
    @FXML private Parent root;

    private BalanceManager balanceManager;
    private TransaksiManager transaksiManager;
    private SharedDataManager sharedDataManager;
    private double totalPemasukan = 0;
    private double totalPengeluaran = 0;

    @FXML
    public void initialize() {
        sharedDataManager = SharedDataManager.getInstance();
        balanceManager = sharedDataManager.getBalanceManager();
        transaksiManager = sharedDataManager.getTransaksiManager();

        balanceManager.addObserver(this);

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        tanggalLabel.setText(today.format(formatter));

        updateAllLabels();
        updateHistoryList();

        System.out.println("MenuController initialized - Saldo: " + balanceManager.getSaldo());
    }

    @FXML
    public void switchToTransaksiView(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/TransaksiView.fxml"));
            root = loader.load();

            TransaksiController transaksiController = loader.getController();
            if (transaksiController != null) {
                transaksiController.setSharedDataManager(sharedDataManager);
            }

            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Tulis Transaksi");
            stage.show();

        } catch (IOException e) {
            showAlert("Error", "Gagal membuka halaman transaksi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metode ini dipanggil oleh tombol "Transaksi" di footer dan tombol pensil
    @FXML
    public void showTransaksi(ActionEvent event) throws IOException {
        switchToTransaksiView(event);
    }

    // Metode ini dipanggil oleh tombol "Analisis" di footer
    @FXML
    public void showAnalisis(ActionEvent event) throws IOException {
        try {
            // Pastikan path ini benar
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/AnalisisPemasukanView.fxml"));
            root = loader.load(); // Error kemungkinan terjadi di sini
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Analisis Pemasukan");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace(); // Tambahkan ini untuk melihat error detail
            showAlert("Error", "Gagal membuka halaman analisis: " + e.getMessage());
        }
    }

    // Metode ini dipanggil oleh tombol "Pengaturan" di footer
    @FXML
    public void showPengaturan(ActionEvent event) {
        // Karena Pengaturan.fxml juga menggunakan MenuController, kita bisa langsung load
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/Pengaturan.fxml"));
            root = loader.load();
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Pengaturan");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Gagal membuka halaman pengaturan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metode ini dipanggil oleh tombol "Export Data ke CSV" di Pengaturan.fxml
    @FXML
    public void exportToExcel() {
        try {
            AnalisisPemasukanManager tempAnalisisManager = new AnalisisPemasukanManager(transaksiManager);
            LocalDate minDate = LocalDate.of(2000, 1, 1);
            LocalDate maxDate = LocalDate.of(2099, 12, 31);

            tempAnalisisManager.exportAllTransactionsToCsv(minDate, maxDate);

            showSuccessAlert("Export Berhasil", "Data transaksi berhasil diekspor ke 'semua_transaksi.csv'!");
        } catch (Exception e) {
            showAlert("Export Gagal", "Terjadi kesalahan saat mengekspor data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metode ini dipanggil oleh tombol "Logout" di Pengaturan.fxml
    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Logout");
        confirmAlert.setHeaderText("Logout dari Aplikasi");
        confirmAlert.setContentText("Apakah Anda yakin ingin logout?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SessionManager.logout();
            SharedDataManager.resetInstance(); // Clear shared data on logout

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/Login.fxml"));
            root = loader.load();
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        }
    }

    // Metode ini dipanggil oleh tombol "Reset Data" di Pengaturan.fxml
    @FXML
    public void resetAllData() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Reset");
        confirmAlert.setHeaderText("Reset Semua Data");
        confirmAlert.setContentText("Apakah Anda yakin ingin menghapus semua data transaksi dan reset saldo ke 0?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                sharedDataManager.resetData();

                balanceManager = sharedDataManager.getBalanceManager();
                transaksiManager = sharedDataManager.getTransaksiManager();
                balanceManager.addObserver(this);

                updateAllLabels();
                updateHistoryList();

                showSuccessAlert("Berhasil", "Semua data berhasil direset!");
            }
        });
    }

    private void updateAllLabels() {
        Platform.runLater(() -> {
            double currentSaldo = balanceManager.getSaldo();
            jumlahSaldoLabel.setText(String.format("Rp %,.0f", currentSaldo));

            calculateTotals();

            saldoPemasukanLabel.setText(String.format("Rp %,.0f", totalPemasukan));
            saldoPengeluaranLabel.setText(String.format("Rp %,.0f", totalPengeluaran));

            updateSaldoColor(currentSaldo);
        });
    }

    private void updateSaldoColor(double saldo) {
        if (saldo >= 0) {
            jumlahSaldoLabel.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
        } else {
            jumlahSaldoLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;");
        }
    }

    private void calculateTotals() {
        totalPemasukan = 0;
        totalPengeluaran = 0;

        for (Transaksi t : transaksiManager.getRiwayat()) {
            if (t.getStrategy() instanceof penambahanStrategy) {
                totalPemasukan += t.getJumlah();
            } else if (t.getStrategy() instanceof penguranganStrategy) {
                totalPengeluaran += t.getJumlah();
            }
        }
    }

    private void updateHistoryList() {
        Platform.runLater(() -> {
            historyList.getItems().clear();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.util.List<Transaksi> riwayat = transaksiManager.getRiwayat();

            if (riwayat.isEmpty()) {
                historyList.getItems().add("Belum ada transaksi");
            } else {
                for (int i = riwayat.size() - 1; i >= 0; i--) {
                    Transaksi t = riwayat.get(i);
                    String tanggalStr = (t.getTanggal() != null) ? t.getTanggal().format(formatter) : "Tanpa Tanggal";
                    String historyItem = String.format("[%s] %s", tanggalStr, t.toShortString());
                    historyList.getItems().add(historyItem);
                }
            }
        });
    }

    @Override
    public void onSaldoChanged(double saldoBaru) {
        updateAllLabels();
        updateHistoryList();
    }

    public void refreshData() {
        updateAllLabels();
        updateHistoryList();
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
}
