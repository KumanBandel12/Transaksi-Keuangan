package pbo.transaksikeuangan;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;

public class TransactionDetailController {

    @FXML private Label tanggalLabel;
    @FXML private Label jenisLabel;
    @FXML private Label kategoriLabel;
    @FXML private Label jumlahLabel;

    private Transaksi transaksi;
    private MenuController menuController;
    private int transactionIndex;

    public void initData(Transaksi transaksi, int index, MenuController menuController) {
        this.transaksi = transaksi;
        this.transactionIndex = index;
        this.menuController = menuController;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        tanggalLabel.setText("Tanggal: " + transaksi.getTanggal().format(formatter));
        jumlahLabel.setText(String.format("Rp %,d", transaksi.getJumlah()));

        if (transaksi.getStrategy() instanceof penambahanStrategy) {
            jenisLabel.setText("Jenis: Pemasukan");
            kategoriLabel.setVisible(false); // Sembunyikan label kategori untuk pemasukan
            kategoriLabel.setManaged(false);
        } else {
            jenisLabel.setText("Jenis: Pengeluaran");
            kategoriLabel.setText("Kategori: " + transaksi.getKategori().toString());
            kategoriLabel.setVisible(true);
            kategoriLabel.setManaged(true);
        }
    }

    @FXML
    void closePopup(ActionEvent event) {
        Stage stage = (Stage) jenisLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    void handleEdit(ActionEvent event) {
        closePopup(event); // Tutup popup dulu
        menuController.editTransaksi(transactionIndex); // Panggil method edit di MenuController
    }

    @FXML
    void handleDelete(ActionEvent event) {
        closePopup(event); // Tutup popup dulu
        menuController.hapusTransaksi(transactionIndex); // Panggil method hapus di MenuController
    }
}