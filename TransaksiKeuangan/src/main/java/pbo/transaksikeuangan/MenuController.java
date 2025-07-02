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
    // --- Elemen UI ---
    @FXML private Label jumlahSaldoLabel;
    @FXML private Label tanggalLabel;
    @FXML private Label saldoPemasukanLabel;
    @FXML private Label saldoPengeluaranLabel;
    @FXML private ListView<String> historyList;
    @FXML private DatePicker historyDatePicker;
    @FXML private Label dailyPemasukanLabel;
    @FXML private Label dailyPengeluaranLabel;
    @FXML private ProgressIndicator loadingIndicator;

    // --- Atribut ---
    private TransaksiManager transaksiManager;
    private SharedDataManager sharedDataManager;
    private BalanceManager balanceManager;

    @FXML
    public void initialize() {
        sharedDataManager = SharedDataManager.getInstance();
        transaksiManager = sharedDataManager.getTransaksiManager();
        balanceManager = sharedDataManager.getBalanceManager();
        balanceManager.addObserver(this);

        historyDatePicker.setOnAction(event -> runUpdateTask());
        setupHistoryListClickHandler();
        refreshData();
    }

    /**
     * Metode pusat yang menjalankan semua perhitungan di background thread.
     */
    private void runUpdateTask() {
        LocalDate selectedDate = historyDatePicker.getValue();
        if (selectedDate == null) return;

        showLoading(true);

        Task<Map<String, Object>> calculationTask = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                // Semua kalkulasi berat terjadi di sini (di background)
                Map<String, Object> results = new HashMap<>();

                results.put("realTimeBalance", balanceManager.getSaldo());

                YearMonth currentMonth = YearMonth.from(selectedDate);
                List<Transaksi> monthlyTx = transaksiManager.getTransactionsByMonth(currentMonth);
                results.put("monthlyIncome", monthlyTx.stream().filter(t -> t.getStrategy() instanceof penambahanStrategy).mapToDouble(Transaksi::getJumlah).sum());
                results.put("monthlyExpense", monthlyTx.stream().filter(t -> t.getStrategy() instanceof penguranganStrategy).mapToDouble(Transaksi::getJumlah).sum());

                List<Transaksi> dailyTx = transaksiManager.getTransactionsByDate(selectedDate);
                results.put("dailyIncome", dailyTx.stream().filter(t -> t.getStrategy() instanceof penambahanStrategy).mapToDouble(Transaksi::getJumlah).sum());
                results.put("dailyExpense", dailyTx.stream().filter(t -> t.getStrategy() instanceof penguranganStrategy).mapToDouble(Transaksi::getJumlah).sum());

                results.put("historyItems", dailyTx.stream().map(Transaksi::toShortString).collect(Collectors.toList()));

                return results;
            }
        };

        calculationTask.setOnSucceeded(e -> {
            Map<String, Object> results = calculationTask.getValue();
            updateUIWithResults(selectedDate, results);
            showLoading(false);
        });

        calculationTask.setOnFailed(e -> {
            calculationTask.getException().printStackTrace();
            showAlert("Error", "Gagal memuat data transaksi.");
            showLoading(false);
        });

        new Thread(calculationTask).start();
    }

    /**
     * Method untuk memperbarui semua elemen UI dengan hasil dari background task.
     */
    private void updateUIWithResults(LocalDate date, Map<String, Object> results) {
        double realTimeBalance = (double) results.get("realTimeBalance");
        jumlahSaldoLabel.setText(String.format("Rp %,.0f", realTimeBalance));
        updateSaldoColor(realTimeBalance);

        saldoPemasukanLabel.setText(String.format("Rp %,.0f", (double) results.get("monthlyIncome")));
        saldoPengeluaranLabel.setText(String.format("Rp %,.0f", (double) results.get("monthlyExpense")));

        dailyPemasukanLabel.setText(String.format("Rp %,.0f", (double) results.get("dailyIncome")));
        dailyPengeluaranLabel.setText(String.format("Rp %,.0f", (double) results.get("dailyExpense")));

        tanggalLabel.setText(date.format(DateTimeFormatter.ofPattern("dd MMMM uuuu")));

        historyList.getItems().clear();
        List<String> historyItems = (List<String>) results.get("historyItems");
        if (historyItems.isEmpty()) {
            historyList.getItems().add("Tidak ada transaksi pada tanggal ini.");
        } else {
            historyList.getItems().addAll(historyItems);
        }
    }

    private void setupHistoryListClickHandler() {
        historyList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                int selectedIndex = historyList.getSelectionModel().getSelectedIndex();
                if (selectedIndex >= 0 && !historyList.getItems().get(selectedIndex).startsWith("Tidak ada")) {
                    showTransactionDetailPopup(selectedIndex);
                }
            }
        });
    }

    public void refreshData() {
        LocalDate today = LocalDate.now();
        historyDatePicker.setValue(today);
        runUpdateTask();
    }

    @FXML
    public void resetHistoryFilter() {
        refreshData();
    }

    @FXML
    private void onDateSelected() {
        runUpdateTask();
    }

    @Override
    public void onSaldoChanged(double saldoBaru) {
        Platform.runLater(this::runUpdateTask);
    }

    private void showTransactionDetailPopup(int viewIndex) {
        try {
            LocalDate selectedDate = historyDatePicker.getValue();
            List<Transaksi> dailyTx = transaksiManager.getTransactionsByDate(selectedDate);
            Transaksi selectedTransaksi = dailyTx.get(viewIndex);
            int originalIndex = transaksiManager.getRiwayat().indexOf(selectedTransaksi);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/TransactionDetail.fxml"));
            Parent root = loader.load();
            TransactionDetailController controller = loader.getController();
            controller.initData(selectedTransaksi, originalIndex, this);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();
        } catch (IOException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            showAlert("Error", "Tidak dapat menampilkan detail transaksi.");
        }
    }


    public void hapusTransaksi(int originalIndex) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, "Apakah Anda yakin ingin menghapus transaksi ini?", ButtonType.OK, ButtonType.CANCEL);
        confirmAlert.setTitle("Konfirmasi Hapus");
        confirmAlert.setHeaderText("Aksi ini tidak bisa dibatalkan.");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
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
        });
    }

    public void editTransaksi(int originalIndex) {
        try {
            Transaksi transaksiToEdit = transaksiManager.getRiwayat().get(originalIndex);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/TransaksiView.fxml"));
            Parent root = loader.load();
            TransaksiController controller = loader.getController();
            controller.setSharedDataManager(this.sharedDataManager);
            controller.initDataForEdit(transaksiToEdit, originalIndex);
            Stage stage = (Stage) historyList.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka halaman edit.");
        }
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
    }

    private void updateSaldoColor(double saldo) {
        if (jumlahSaldoLabel != null) {
            if (saldo >= 0) {
                jumlahSaldoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            } else {
                jumlahSaldoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #D32F2F;");
            }
        }
    }

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