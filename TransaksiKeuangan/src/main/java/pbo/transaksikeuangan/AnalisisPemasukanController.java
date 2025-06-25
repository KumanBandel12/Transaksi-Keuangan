// src/main/java/pbo/transaksikeuangan/AnalisisPemasukanController.java
package pbo.transaksikeuangan;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalisisPemasukanController {
    @FXML private PieChart pemasukanPieChart;
    @FXML private LineChart<String, Number> pemasukanLineChart;
    @FXML private BarChart<String, Number> pemasukanBarChart;
    @FXML private ComboBox<String> periodeCombo;
    @FXML private ComboBox<String> jenisGrafikCombo; // Ini untuk memilih jenis chart (Pie, Line, Bar)
    @FXML private Label totalPemasukanLabel; // Label total pemasukan
    @FXML private Label rataRataPemasukanLabel; // Label rata-rata (jika masih diperlukan)
    @FXML private ListView<String> detailPemasukanList;
    @FXML private DatePicker tanggalMulai;
    @FXML private DatePicker tanggalAkhir;
    @FXML private Button filterButton;
    @FXML private Stage stage;
    @FXML private Scene scene;
    @FXML private Parent root;

    private AnalisisPemasukanManager analisisManager;
    private TransaksiManager transaksiManager;

    @FXML
    public void initialize() {
        transaksiManager = SharedDataManager.getInstance().getTransaksiManager();
        analisisManager = new AnalisisPemasukanManager(transaksiManager);

        periodeCombo.getItems().addAll("Harian", "Mingguan", "Bulanan", "Tahunan");
        periodeCombo.setValue("Bulanan");

        jenisGrafikCombo.getItems().addAll("Pie Chart", "Line Chart", "Bar Chart");
        jenisGrafikCombo.setValue("Bar Chart"); // Default ke Bar Chart sesuai deskripsi

        tanggalAkhir.setValue(LocalDate.now());
        tanggalMulai.setValue(LocalDate.now().minusMonths(1));

        updateAnalisis();

        periodeCombo.setOnAction(e -> updateAnalisis());
        jenisGrafikCombo.setOnAction(e -> updateJenisGrafik());
    }

    @FXML
    public void handleFilter() {
        updateAnalisis();
    }

    private void updateAnalisis() {
        LocalDate mulai = tanggalMulai.getValue();
        LocalDate akhir = tanggalAkhir.getValue();
        String periode = periodeCombo.getValue();

        if (mulai == null || akhir == null) {
            showAlert("Input Tanggal", "Tanggal mulai dan tanggal akhir harus diisi.");
            return;
        }
        if (mulai.isAfter(akhir)) {
            showAlert("Input Tanggal", "Tanggal mulai tidak boleh setelah tanggal akhir.");
            return;
        }

        updateStatistik(mulai, akhir);
        updateGrafik(mulai, akhir, periode);
        updateDetailList(mulai, akhir);
    }

    private void updateStatistik(LocalDate mulai, LocalDate akhir) {
        double totalPemasukan = analisisManager.getTotalPemasukan(mulai, akhir);
        // double rataRata = analisisManager.getRataRataPemasukan(mulai, akhir); // Tidak ada di deskripsi baru

        totalPemasukanLabel.setText(String.format("Rp %,.2f", totalPemasukan));
        // rataRataPemasukanLabel.setText(String.format("Rp %,.2f", rataRata)); // Tidak ada di deskripsi baru
    }

    private void updateGrafik(LocalDate mulai, LocalDate akhir, String periode) {
        String jenisGrafik = jenisGrafikCombo.getValue();

        pemasukanPieChart.setVisible(false);
        pemasukanLineChart.setVisible(false);
        pemasukanBarChart.setVisible(false);

        switch (jenisGrafik) {
            case "Pie Chart":
                updatePieChart(mulai, akhir);
                pemasukanPieChart.setVisible(true);
                break;
            case "Line Chart":
                updateLineChart(mulai, akhir, periode);
                pemasukanLineChart.setVisible(true);
                break;
            case "Bar Chart":
                updateBarChart(mulai, akhir, periode);
                pemasukanBarChart.setVisible(true);
                break;
        }
    }

    private void updatePieChart(LocalDate mulai, LocalDate akhir) {
        Map<String, Double> dataPemasukan = analisisManager.getPemasukanPerKategori(mulai, akhir);
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        if (dataPemasukan.isEmpty()) {
            pieChartData.add(new PieChart.Data("Tidak Ada Data", 1));
            pemasukanPieChart.setTitle("Distribusi Pemasukan (Tidak Ada Data)");
        } else {
            for (Map.Entry<String, Double> entry : dataPemasukan.entrySet()) {
                pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            pemasukanPieChart.setTitle("Distribusi Pemasukan");
        }
        pemasukanPieChart.setData(pieChartData);
    }

    private void updateLineChart(LocalDate mulai, LocalDate akhir, String periode) {
        Map<String, Double> dataTrend = analisisManager.getTrendPemasukan(mulai, akhir, periode);

        pemasukanLineChart.getData().clear();
        if (dataTrend.isEmpty()) {
            pemasukanLineChart.setTitle("Trend Pemasukan " + periode + " (Tidak Ada Data)");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Pemasukan " + periode);

        for (Map.Entry<String, Double> entry : dataTrend.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        pemasukanLineChart.getData().add(series);
        pemasukanLineChart.setTitle("Trend Pemasukan " + periode);
    }

    private void updateBarChart(LocalDate mulai, LocalDate akhir, String periode) {
        Map<String, Double> dataTrend = analisisManager.getTrendPemasukan(mulai, akhir, periode);

        pemasukanBarChart.getData().clear();
        if (dataTrend.isEmpty()) {
            pemasukanBarChart.setTitle("Pemasukan " + periode + " (Tidak Ada Data)");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Pemasukan " + periode);

        for (Map.Entry<String, Double> entry : dataTrend.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        pemasukanBarChart.getData().add(series);
        pemasukanBarChart.setTitle("Pemasukan " + periode);
    }

    private void updateDetailList(LocalDate mulai, LocalDate akhir) {
        List<String> detailPemasukan = analisisManager.getDetailPemasukan(mulai, akhir);
        detailPemasukanList.getItems().clear();
        detailPemasukanList.getItems().addAll(detailPemasukan);
    }

    private void updateJenisGrafik() {
        updateAnalisis();
    }

    @FXML
    public void switchToMainMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
        root = loader.load();
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    // Metode untuk beralih ke Analisis Pengeluaran
    @FXML
    public void switchToPengeluaranAnalisis(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/LaporanPengeluaran.fxml"));
            root = loader.load();
            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Analisis Pengeluaran");
            stage.show();
        } catch (IOException e) {
            // Tambahkan logging atau pesan error yang lebih spesifik
            System.err.println("Error loading LaporanPengeluaran.fxml: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Gagal membuka halaman Analisis Pengeluaran. Pastikan file FXML ada dan tidak rusak.");
        }
    }

    @FXML
    public void exportToExcel() {
        LocalDate mulai = tanggalMulai.getValue();
        LocalDate akhir = tanggalAkhir.getValue();

        if (mulai == null || akhir == null || mulai.isAfter(akhir)) {
            showAlert("Export Gagal", "Pastikan tanggal mulai dan tanggal akhir valid.");
            return;
        }

        analisisManager.exportToExcel(mulai, akhir);
        showAlert("Export Berhasil", "Data analisis pemasukan berhasil diekspor ke analisis_pemasukan.csv!");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void switchToPengaturan(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/Pengaturan.fxml"));
        root = loader.load();
        stage = (Stage)((Node) event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Pengaturan");
        stage.show();
    }
}
