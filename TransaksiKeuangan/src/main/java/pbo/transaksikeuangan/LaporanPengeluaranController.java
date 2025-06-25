package pbo.transaksikeuangan;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LaporanPengeluaranController implements Initializable {

    @FXML private PieChart pieChartPengeluaran;
    @FXML private Label detailLabel;
    @FXML private Label totalPengeluaranLabel;
    @FXML private ComboBox<String> periodeCombo;
    @FXML private ComboBox<String> jenisGrafikCombo;
    @FXML private DatePicker tanggalMulai;
    @FXML private DatePicker tanggalAkhir;
    @FXML private Button filterButton;

    private TransaksiManager transaksiManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Pastikan SharedDataManager sudah diinisialisasi (misalnya setelah login)
        try {
            transaksiManager = SharedDataManager.getInstance().getTransaksiManager();
        } catch (IllegalStateException e) {
            // Tangani kasus jika SharedDataManager belum diinisialisasi
            showAlert("Error Inisialisasi", "Sistem belum siap. Silakan login terlebih dahulu.");
            // Opsional: Kembali ke halaman login atau keluar aplikasi
            return;
        }
        // Inisialisasi ComboBoxes
        periodeCombo.getItems().addAll("Harian", "Mingguan", "Bulanan", "Tahunan");
        periodeCombo.setValue("Bulanan"); // Set nilai default
        jenisGrafikCombo.getItems().addAll("Pie Chart", "Line Chart", "Bar Chart");
        jenisGrafikCombo.setValue("Pie Chart"); // Set nilai default
        // Inisialisasi DatePickers
        tanggalAkhir.setValue(LocalDate.now());
        tanggalMulai.setValue(LocalDate.now().minusMonths(1));
        // Inisialisasi Label Detail
        detailLabel.setText("Arahkan kursor ke chart untuk melihat detail");
        // Panggil muatDataPieChart setelah semua komponen terinisialisasi
        muatDataPieChart();
        // Tambahkan listener untuk memperbarui chart saat ada perubahan
        periodeCombo.setOnAction(e -> muatDataPieChart());
        jenisGrafikCombo.setOnAction(e -> muatDataPieChart());
        tanggalMulai.setOnAction(e -> muatDataPieChart());
        tanggalAkhir.setOnAction(e -> muatDataPieChart());
        filterButton.setOnAction(e -> muatDataPieChart()); // Pastikan tombol filter terhubung
    }

    private void muatDataPieChart() {
        // Validasi DatePicker sebelum digunakan
        LocalDate mulaiFilter = tanggalMulai.getValue();
        LocalDate akhirFilter = tanggalAkhir.getValue();
        if (mulaiFilter == null || akhirFilter == null) {
            showAlert("Input Tanggal", "Tanggal mulai dan tanggal akhir harus diisi.");
            // Kosongkan chart dan label jika tanggal tidak valid
            pieChartPengeluaran.setData(FXCollections.observableArrayList());
            pieChartPengeluaran.setTitle("Ringkasan Pengeluaran (Tanggal Tidak Valid)");
            totalPengeluaranLabel.setText("Rp 0,00");
            detailLabel.setText("Arahkan kursor ke chart untuk melihat detail");
            return;
        }

        // Validasi jika tanggal mulai setelah tanggal akhir
        if (mulaiFilter.isAfter(akhirFilter)) {
            showAlert("Input Tanggal", "Tanggal mulai tidak boleh setelah tanggal akhir. Silakan perbaiki.");
            // Kosongkan chart dan label jika tanggal tidak valid
            pieChartPengeluaran.setData(FXCollections.observableArrayList());
            pieChartPengeluaran.setTitle("Ringkasan Pengeluaran (Rentang Tanggal Tidak Valid)");
            totalPengeluaranLabel.setText("Rp 0,00");
            detailLabel.setText("Arahkan kursor ke chart untuk melihat detail");
            return;
        }
        // Pastikan transaksiManager tidak null
        if (transaksiManager == null) {
            showAlert("Error", "Transaksi Manager belum diinisialisasi.");
            return;
        }

        List<Transaksi> riwayat = transaksiManager.getRiwayat();
        final LocalDate finalMulai = mulaiFilter;
        final LocalDate finalAkhir = akhirFilter;
        List<Transaksi> pengeluaran = riwayat.stream()
                .filter(t -> t.getStrategy() instanceof penguranganStrategy)
                .filter(t -> t.getTanggal() != null &&
                        !t.getTanggal().isBefore(finalMulai) &&
                        !t.getTanggal().isAfter(finalAkhir))
                .collect(Collectors.toList());
        double totalPengeluaran = pengeluaran.stream()
                .mapToDouble(Transaksi::getJumlah)
                .sum();
        totalPengeluaranLabel.setText(String.format("Rp %,.2f", totalPengeluaran));
        Map<String, Double> totalPerKategori = pengeluaran.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getKategori() != null ? t.getKategori().toString() : "Tidak Berkategori", // Handle null kategori
                        Collectors.summingDouble(Transaksi::getJumlah)
                ));
        ObservableList<PieChart.Data> dataPieChart = FXCollections.observableArrayList();
        if (totalPerKategori.isEmpty()) {
            dataPieChart.add(new PieChart.Data("Tidak Ada Data", 1)); // Tampilkan slice "Tidak Ada Data"
            pieChartPengeluaran.setTitle("Ringkasan Pengeluaran (Tidak Ada Data)");
        } else {
            totalPerKategori.forEach((kategori, total) -> {
                dataPieChart.add(new PieChart.Data(kategori, total));
            });
            pieChartPengeluaran.setTitle("Ringkasan Pengeluaran per Kategori");
        }
        pieChartPengeluaran.setData(dataPieChart);

        // Tambahkan event handler untuk setiap slice PieChart
        for (final PieChart.Data data : pieChartPengeluaran.getData()) {
            Node node = data.getNode();
            node.setOnMouseEntered(event -> {
                double nilai = data.getPieValue();
                double persentase = (totalPengeluaran > 0) ? (nilai / totalPengeluaran) * 100 : 0;
                String detailText = String.format(
                        "Kategori: %s\nJumlah: Rp %,.2f (%.1f%%)",
                        data.getName(),
                        nilai,
                        persentase
                );
                detailLabel.setText(detailText);
            });
            node.setOnMouseExited(event -> {
                detailLabel.setText("Arahkan kursor ke chart untuk melihat detail");
            });
        }
    }

    @FXML
    public void kembaliKeMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Menu Utama");
        stage.show();
    }

    @FXML
    public void switchToPemasukanAnalisis(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/AnalisisPemasukanView.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Analisis Pemasukan");
        stage.show();
    }

    @FXML
    public void handleFilter(ActionEvent event) {
        muatDataPieChart();
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
        Parent root = loader.load();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Pengaturan");
        stage.show();
    }
}
