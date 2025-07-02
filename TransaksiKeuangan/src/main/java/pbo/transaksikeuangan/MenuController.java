package pbo.transaksikeuangan;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MenuController implements saldoObserver {
    // --- Elemen UI Utama ---
    @FXML private Label jumlahSaldoLabel;
    @FXML private Label tanggalLabel;
    @FXML private Label saldoPemasukanLabel;
    @FXML private Label saldoPengeluaranLabel;

    // --- Elemen UI Riwayat ---
    @FXML private ListView<String> historyList;
    @FXML private DatePicker historyDatePicker;

    // --- Label Baru untuk Ringkasan Harian ---
    @FXML private Label dailyPemasukanLabel;
    @FXML private Label dailyPengeluaranLabel;

    // --- Elemen Loading ---
    @FXML private ProgressIndicator loadingIndicator;

    // --- Atribut lain ---
    private TransaksiManager transaksiManager;
    private SharedDataManager sharedDataManager;
    private BalanceManager balanceManager;

    @FXML
    public void initialize() {
        sharedDataManager = SharedDataManager.getInstance();
        transaksiManager = sharedDataManager.getTransaksiManager();
        balanceManager = sharedDataManager.getBalanceManager();
        balanceManager.addObserver(this);

        historyDatePicker.setOnAction(event -> onDateSelected());

        // Setup klik pada daftar riwayat untuk memunculkan popup
        setupHistoryListClickHandler();

        refreshData();
    }

    // Setup klik pada daftar riwayat
    private void setupHistoryListClickHandler() {
        historyList.setOnMouseClicked(event -> {
            // Cek jika pengguna double-click
            if (event.getClickCount() == 2) {
                int selectedIndex = historyList.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && !historyList.getItems().get(selectedIndex).startsWith("Tidak ada")) {
                    // Cari transaksi yang sesuai (perlu logika mapping dari list ke data asli)
                    showTransactionDetailPopup(selectedIndex);
                }
            }
        });
    }

    /**
     * Mengatur ulang tampilan ke kondisi default (menampilkan data hari ini).
     */
    public void refreshData() {
        LocalDate today = LocalDate.now();
        historyDatePicker.setValue(today);
        updateAllUI(today);
    }

    /**
     * Tombol untuk mereset filter kembali ke hari ini.
     */
    @FXML
    public void resetHistoryFilter() {
        refreshData();
    }

    /**
     * Dipanggil saat pengguna memilih tanggal baru di DatePicker.
     */
    @FXML
    private void onDateSelected() {
        LocalDate selectedDate = historyDatePicker.getValue();
        if (selectedDate == null) return;

        showLoading(true); // Tampilkan indikator loading

        Task<Map<String, Double>> calculationTask = new Task<>() {
            @Override
            protected Map<String, Double> call() throws Exception {
                // Semua kalkulasi berat terjadi di sini (background)
                Map<String, Double> results = new HashMap<>();
                // ... (lakukan semua kalkulasi saldo harian & bulanan) ...
                // results.put("dailyExpense", dailyExpense);
                // ...
                return results;
            }
        };

        calculationTask.setOnSucceeded(e -> {
            Map<String, Double> results = calculationTask.getValue();
            // Update UI dengan hasil kalkulasi (kembali ke UI thread)
            // saldoPengeluaranLabel.setText(...);
            showLoading(false); // Sembunyikan indikator
        });

        new Thread(calculationTask).start();
    }

    /**
     * Pusat dari semua pembaruan UI. Method ini akan dipanggil setiap kali
     * tanggal berubah atau data perlu di-refresh.
     */
    private void updateAllUI(LocalDate date) {
        // 1. Perbarui Saldo Total Real-Time
        double realTimeBalance = balanceManager.getSaldo();
        jumlahSaldoLabel.setText(String.format("Rp %,.0f", realTimeBalance));
        updateSaldoColor(realTimeBalance);

        // 2. Perbarui Ringkasan Bulanan
        updateMonthlySummary(date);

        // 3. Perbarui Ringkasan Harian (Fitur Baru)
        updateDailySummary(date);

        // 4. Perbarui Label Tanggal dan Daftar Riwayat
        tanggalLabel.setText(date.format(DateTimeFormatter.ofPattern("dd MMMM uuuu")));
        updateHistoryList(date);
    }

    private void updateMonthlySummary(LocalDate date) {
        YearMonth currentMonth = YearMonth.from(date);
        List<Transaksi> monthlyTransactions = transaksiManager.getTransactionsByMonth(currentMonth);

        double totalIncomeThisMonth = monthlyTransactions.stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .mapToDouble(Transaksi::getJumlah).sum();

        double totalExpenseThisMonth = monthlyTransactions.stream()
                .filter(t -> t.getStrategy() instanceof penguranganStrategy)
                .mapToDouble(Transaksi::getJumlah).sum();

        saldoPemasukanLabel.setText(String.format("Rp %,.0f", totalIncomeThisMonth));
        saldoPengeluaranLabel.setText(String.format("Rp %,.0f", totalExpenseThisMonth));
    }

    private void updateDailySummary(LocalDate date) {
        List<Transaksi> dailyTransactions = transaksiManager.getTransactionsByDate(date);

        double dailyIncome = dailyTransactions.stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .mapToDouble(Transaksi::getJumlah).sum();

        double dailyExpense = dailyTransactions.stream()
                .filter(t -> t.getStrategy() instanceof penguranganStrategy)
                .mapToDouble(Transaksi::getJumlah).sum();

        dailyPemasukanLabel.setText(String.format("Rp %,.0f", dailyIncome));
        dailyPengeluaranLabel.setText(String.format("Rp %,.0f", dailyExpense));
    }

    /**
     * Memperbarui daftar riwayat untuk menampilkan transaksi HANYA dari tanggal yang dipilih.
     */
    private void updateHistoryList(LocalDate filterDate) {
        Platform.runLater(() -> {
            historyList.getItems().clear();
            if (filterDate == null) return;

            // Logika ringkasan harian sudah dipindahkan ke label terpisah,
            // jadi ListView sekarang hanya menampilkan daftar transaksi.
            List<Transaksi> dailyTransactions = transaksiManager.getTransactionsByDate(filterDate);

            if (dailyTransactions.isEmpty()) {
                historyList.getItems().add("Tidak ada transaksi pada tanggal ini.");
            } else {
                for (Transaksi t : dailyTransactions) {
                    historyList.getItems().add(t.toShortString());
                }
            }
        });
    }

    /**
     * Method observer yang dipanggil setelah ada transaksi baru.
     */
    @Override
    public void onSaldoChanged(double saldoBaru) {
        Platform.runLater(() -> {
            LocalDate currentDate = historyDatePicker.getValue();
            updateAllUI(currentDate != null ? currentDate : LocalDate.now());
        });
    }

    private void showTransactionDetailPopup(int viewIndex) {
        try {
            LocalDate selectedDate = historyDatePicker.getValue();
            List<Transaksi> dailyTx = transaksiManager.getTransactionsByDate(selectedDate);
            Transaksi selectedTransaksi = dailyTx.get(viewIndex);

            // Mencari index asli dari transaksi di riwayat utama
            int originalIndex = transaksiManager.getRiwayat().indexOf(selectedTransaksi);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/TransactionDetail.fxml"));
            Parent root = loader.load();

            TransactionDetailController controller = loader.getController();
            controller.initData(selectedTransaksi, originalIndex, this);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Detail Transaksi");
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            showAlert("Error", "Tidak bisa menampilkan detail transaksi.");
        }
    }

    public void hapusTransaksi(int originalIndex) {
        // Fitur #3: Pesan Konfirmasi yang Lebih Baik
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Konfirmasi Hapus");
        confirmAlert.setHeaderText("Anda akan menghapus transaksi ini secara permanen.");
        confirmAlert.setContentText("Apakah Anda yakin?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showLoading(true);
            Task<Void> task = new Task<>() {
                @Override protected Void call() {
                    transaksiManager.hapusTransaksi(originalIndex);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showLoading(false);
                refreshData();
            });
            new Thread(task).start();
        }
    }

    public void editTransaksi(int originalIndex) {
        try {
            // Dapatkan transaksi asli berdasarkan index-nya di riwayat utama
            Transaksi transaksiToEdit = transaksiManager.getRiwayat().get(originalIndex);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/TransaksiView.fxml"));
            Parent root = loader.load();

            // Kirim data transaksi yang akan diedit ke TransaksiController
            TransaksiController controller = loader.getController();
            controller.setSharedDataManager(this.sharedDataManager); // Kirim shared data manager
            controller.initDataForEdit(transaksiToEdit, originalIndex);

            Stage stage = (Stage) historyList.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka halaman edit transaksi.");
        }
    }

    // --- KONTROL INDIKATOR LOADING ---
    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }

    private void updateSaldoColor(double saldo) {
        if (saldo >= 0) {
            jumlahSaldoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        } else {
            jumlahSaldoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D32F2F;");
        }
    }

    // --- Method Navigasi ---
    @FXML
    public void showTransaksi(ActionEvent event) throws IOException {
        switchToScene(event, "/pbo/transaksikeuangan/TransaksiView.fxml", "Tulis Transaksi");
    }

    @FXML
    public void showAnalisis(ActionEvent event) throws IOException {
        switchToScene(event, "/pbo/transaksikeuangan/AnalisisPemasukanView.fxml", "Analisis Pemasukan");
    }

    @FXML
    public void showPengaturan(ActionEvent event) throws IOException {
        switchToScene(event, "/pbo/transaksikeuangan/Pengaturan.fxml", "Pengaturan");
    }

    @FXML
    public void showCalendar(ActionEvent event) throws IOException {
        switchToScene(event, "/pbo/transaksikeuangan/CalendarView.fxml", "Kalender Transaksi");
    }

    private void switchToScene(ActionEvent event, String fxmlFile, String title) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            if (loader.getController() instanceof TransaksiController) {
                TransaksiController controller = loader.getController();
                controller.setSharedDataManager(sharedDataManager);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka halaman: " + title);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}