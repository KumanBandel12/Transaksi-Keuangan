package pbo.transaksikeuangan;

import java.time.LocalDate; // Import LocalDate

public class TransactionWriter {

    // Metode untuk transaksi Pemasukan
    public Transaksi createTransaction(int saldoSebelum, int jumlah, saldoStrategy strategy, LocalDate tanggal) {
        if (!(strategy instanceof penambahanStrategy)) {
            throw new IllegalArgumentException("This method is for income transactions only.");
        }
        return new Transaksi(saldoSebelum, jumlah, strategy, tanggal); // Tambahkan tanggal
    }

    // Metode untuk transaksi Pengeluaran
    public Transaksi createTransaction(int saldoSebelum, int jumlah, saldoStrategy strategy, KategoriPengeluaran kategori, LocalDate tanggal) {
        if (!(strategy instanceof penguranganStrategy)) {
            throw new IllegalArgumentException("This method is for expense transactions only.");
        }
        return new Transaksi(saldoSebelum, jumlah, strategy, kategori, tanggal); // Tambahkan tanggal
    }

    public void updateTransaction(Transaksi transaksi, int jumlahBaru, saldoStrategy strategyBaru, KategoriPengeluaran kategoriBaru) {
        transaksi.setJumlah(jumlahBaru);
        transaksi.setStrategy(strategyBaru);

        if (strategyBaru instanceof penguranganStrategy) {
            transaksi.setKategori(kategoriBaru);
        } else {
            transaksi.setKategori(null);
        }
        // Tanggal tidak diubah saat edit, diasumsikan transaksi terjadi pada tanggal yang sama
    }

    public void recalculateTransaction(Transaksi transaksi, int saldoSebelum) {
        transaksi.setAwal(saldoSebelum);
        transaksi.hitungUlang();
    }
}
