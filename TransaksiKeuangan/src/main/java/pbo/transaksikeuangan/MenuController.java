// src/main/java/pbo/transaksikeuangan/MenuController.java
package pbo.transaksikeuangan;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*; // Import all control classes
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Import for stream operations

public class MenuController implements saldoObserver {
    @FXML private Label jumlahSaldoLabel;
    // @FXML private Label tanggalLabel; // Label tanggal ini tidak lagi diperlukan jika menggunakan DatePicker
    @FXML private Label saldoPemasukanLabel;
    @FXML private Label saldoPengeluaranLabel;
    @FXML private ListView<String> historyList;
    @FXML private Button pensilButton;

    // START: Tambahan untuk DatePicker
    @FXML private DatePicker tanggalMulai;
    @FXML private DatePicker tanggalAkhir;
    // END: Tambahan untuk DatePicker

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

        // Inisialisasi DatePicker
        tanggalAkhir.setValue(LocalDate.now());
        tanggalMulai.setValue(LocalDate.now().minusMonths(1)); // Default 1 bulan terakhir

        // Tambahkan listener untuk DatePicker
        tanggalMulai.setOnAction(event -> updateFilteredHistory());
        tanggalAkhir.setOnAction(event -> updateFilteredHistory());

        updateAllLabels();
        updateHistoryList(); // Panggil ini untuk pertama kali dengan rentang tanggal default

        System.out.println("MenuController initialized - Saldo: " + balanceManager.getSaldo());
    }

    // Metode baru untuk memfilter riwayat saat tombol filter ditekan
    @FXML
    public void handleFilterDate(ActionEvent event) {
        updateFilteredHistory();
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

    @FXML
    public void showTransaksi(ActionEvent event) throws IOException {
        switchToTransaksiView(event);
    }

    @FXML
    public void showAnalisis(ActionEvent event) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/AnalisisPemasukanView.fxml"));
            root = loader.load();
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Analisis Pemasukan");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka halaman analisis: " + e.getMessage());
        }
    }

    @FXML
    public void showPengaturan(ActionEvent event) {
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

            calculateTotals(); // Panggil calculateTotals() untuk memperbarui total pemasukan/pengeluaran berdasarkan filter tanggal

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

        LocalDate mulai = tanggalMulai.getValue();
        LocalDate akhir = tanggalAkhir.getValue();

        // Pastikan tanggal tidak null sebelum memfilter
        if (mulai == null || akhir == null) {
            // Jika tanggal null, hitung total dari semua transaksi
            for (Transaksi t : transaksiManager.getRiwayat()) {
                if (t.getStrategy() instanceof penambahanStrategy) {
                    totalPemasukan += t.getJumlah();
                } else if (t.getStrategy() instanceof penguranganStrategy) {
                    totalPengeluaran += t.getJumlah();
                }
            }
        } else {
            // Filter transaksi berdasarkan rentang tanggal
            for (Transaksi t : transaksiManager.getRiwayat()) {
                if (t.getTanggal() != null && !t.getTanggal().isBefore(mulai) && !t.getTanggal().isAfter(akhir)) {
                    if (t.getStrategy() instanceof penambahanStrategy) {
                        totalPemasukan += t.getJumlah();
                    } else if (t.getStrategy() instanceof penguranganStrategy) {
                        totalPengeluaran += t.getJumlah();
                    }
                }
            }
        }
    }

    private void updateHistoryList() {
        Platform.runLater(() -> {
            historyList.getItems().clear();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<Transaksi> riwayat = transaksiManager.getRiwayat();

            LocalDate mulai = tanggalMulai.getValue();
            LocalDate akhir = tanggalAkhir.getValue();

            List<Transaksi> filteredRiwayat;

            // Filter riwayat berdasarkan tanggal
            if (mulai == null || akhir == null) {
                // Jika tanggal null, tampilkan semua riwayat
                filteredRiwayat = riwayat;
            } else {
                filteredRiwayat = riwayat.stream()
                        .filter(t -> t.getTanggal() != null && !t.getTanggal().isBefore(mulai) && !t.getTanggal().isAfter(akhir))
                        .collect(Collectors.toList());
            }


            if (filteredRiwayat.isEmpty()) {
                historyList.getItems().add("Belum ada transaksi di rentang tanggal ini.");
            } else {
                // Tampilkan riwayat dari yang terbaru ke terlama
                for (int i = filteredRiwayat.size() - 1; i >= 0; i--) {
                    Transaksi t = filteredRiwayat.get(i);
                    String tanggalStr = (t.getTanggal() != null) ? t.getTanggal().format(formatter) : "Tanpa Tanggal";
                    String historyItem = String.format("[%s] %s", tanggalStr, t.toShortString());
                    historyList.getItems().add(historyItem);
                }
            }
        });
    }

    // Metode ini akan dipanggil oleh action listener DatePicker dan tombol filter
    private void updateFilteredHistory() {
        updateAllLabels(); // Perbarui total dan saldo
        updateHistoryList(); // Perbarui daftar riwayat
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