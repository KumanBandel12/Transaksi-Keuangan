package pbo.transaksikeuangan;

import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransaksiManager {
    private final TransactionWriter writer;
    private final TransactionHistory history;
    private final BalanceManager balanceManager;
    private final int saldoAwal;
    private String dataFile;

    public TransaksiManager(int saldoAwal, String username) {
        this.saldoAwal = saldoAwal;
        this.writer = new TransactionWriter();
        this.history = new TransactionHistory();
        this.balanceManager = BalanceManager.getInstance();
        this.dataFile = "transaksi_" + username + ".dat";
        loadRiwayat();
        this.balanceManager.setSaldo(getSaldoTerakhir());
        System.out.println("TransaksiManager created for user " + username + " with initial balance: " + saldoAwal);
    }


    public void tambahTransaksi(int jumlah, saldoStrategy strategy, KategoriPengeluaran kategori, LocalDate tanggal) {
        int saldoSebelum = getSaldoTerakhir();
        Transaksi transaksi;
        if (strategy instanceof penambahanStrategy) {
            transaksi = writer.createTransaction(saldoSebelum, jumlah, strategy, tanggal);
        } else {
            transaksi = writer.createTransaction(saldoSebelum, jumlah, strategy, kategori, tanggal);
        }
        history.addTransaction(transaksi);
        balanceManager.setSaldo(transaksi.getAkhir());
        saveRiwayat();
    }

    public void editTransaksi(int index, Transaksi transaksiBaru) {
        if (index < 0 || index >= history.getTransactionCount()) return;
        List<Transaksi> riwayat = history.getAllTransactions();
        riwayat.set(index, transaksiBaru);
        history.setAllTransactions(riwayat);
        recalculateFromIndex(0); // Hitung ulang dari awal untuk memastikan semua saldo setelahnya benar
        saveRiwayat();
        balanceManager.setSaldo(getSaldoTerakhir());
    }

    public void hapusTransaksi(int index) {
        if (index < 0 || index >= history.getTransactionCount()) return;
        history.removeTransaction(index);
        recalculateFromIndex(index);
        saveRiwayat();
        balanceManager.setSaldo(getSaldoTerakhir());
    }

    private void recalculateFromIndex(int startIndex) {
        double saldoSebelum = (startIndex == 0) ? saldoAwal : history.getTransaction(startIndex - 1).getAkhir();
        for (int i = startIndex; i < history.getTransactionCount(); i++) {
            Transaksi transaksi = history.getTransaction(i);
            transaksi.setAwal((int) saldoSebelum);
            transaksi.hitungUlang();
            saldoSebelum = transaksi.getAkhir();
        }
    }

    // Get current balance from transaction history
    private int getCurrentBalance() {
        return getSaldoTerakhir();
    }

    public int getSaldoTerakhir() {
        Transaksi lastTransaction = history.getLastTransaction();
        return (lastTransaction != null) ? lastTransaction.getAkhir() : saldoAwal;
    }

    // Get current balance (public method)
    public int getSaldoSekarang() {
        return getCurrentBalance();
    }

    public void tampilRiwayat() {
        history.displayHistory();
        System.out.println("Saldo saat ini = " + getSaldoSekarang());
        System.out.println();
    }

    public List<Transaksi> getRiwayat() {
        return history.getAllTransactions();
    }

    public BalanceManager getBalanceManager() {
        return balanceManager;
    }

    @SuppressWarnings("unchecked")
    private void loadRiwayat() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            List<Transaksi> loadedRiwayat = (ArrayList<Transaksi>) ois.readObject();
            history.setAllTransactions(loadedRiwayat);
            System.out.println("Loaded " + loadedRiwayat.size() + " transactions from " + dataFile);
        } catch (FileNotFoundException e) {
            System.out.println("No transaction data found for " + dataFile + ". Starting fresh.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void saveRiwayat() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(history.getAllTransactions());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearAllTransactions() {
        history.clearHistory();
        // Perbaikan 5: Konversi int ke double saat memanggil resetSaldo
        balanceManager.resetSaldo((double) saldoAwal);
        saveRiwayat();
    }

    // Method untuk mendapatkan semua transaksi pada tanggal tertentu
    public List<Transaksi> getTransactionsByDate(LocalDate date) {
        return getRiwayat().stream()
                .filter(t -> t.getTanggal() != null && t.getTanggal().equals(date))
                .collect(Collectors.toList());
    }

    // Method untuk mendapatkan semua transaksi dalam bulan dan tahun tertentu
    public List<Transaksi> getTransactionsByMonth(YearMonth month) {
        return getRiwayat().stream()
                .filter(t -> {
                    LocalDate tDate = t.getTanggal();
                    return tDate != null && tDate.getYear() == month.getYear() && tDate.getMonth() == month.getMonth();
                })
                .collect(Collectors.toList());
    }
}