package pbo.transaksikeuangan;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

        // Perbaikan 1: Pastikan saldo di BalanceManager disinkronkan dengan saldo akhir setelah memuat riwayat.
        // Jika tidak ada riwayat, gunakan saldoAwal.
        this.balanceManager.setSaldo((double) getSaldoSekarang());

        System.out.println("TransaksiManager created for user " + username + " with initial balance: " + saldoAwal);
    }

    public void tambahTransaksi(int jumlah, saldoStrategy strategy) {
        if (!(strategy instanceof penambahanStrategy)) {
            throw new IllegalArgumentException("Invalid strategy for income transaction.");
        }
        int saldoSebelum = getCurrentBalance();
        Transaksi transaksi = writer.createTransaction(saldoSebelum, jumlah, strategy, LocalDate.now());
        history.addTransaction(transaksi);
        // Perbaikan 2: Konversi int ke double saat memanggil setSaldo
        balanceManager.setSaldo((double) transaksi.getAkhir());
        saveRiwayat();
        System.out.println("TransaksiManager - Income transaction added: " + jumlah + ", New balance: " + transaksi.getAkhir());
    }

    public void tambahTransaksi(int jumlah, saldoStrategy strategy, KategoriPengeluaran kategori) {
        if (!(strategy instanceof penguranganStrategy)) {
            throw new IllegalArgumentException("Invalid strategy for expense transaction.");
        }
        int saldoSebelum = getCurrentBalance();
        Transaksi transaksi = writer.createTransaction(saldoSebelum, jumlah, strategy, kategori, LocalDate.now());
        history.addTransaction(transaksi);
        // Perbaikan 3: Konversi int ke double saat memanggil setSaldo
        balanceManager.setSaldo((double) transaksi.getAkhir());
        saveRiwayat();
        System.out.println("TransaksiManager - Expense transaction added: " + jumlah + " (" + kategori + "), New balance: " + transaksi.getAkhir());
    }

    public void editTransaksi(int index, int jumlahBaru, saldoStrategy strategyBaru, KategoriPengeluaran kategoriBaru) {
        if (index < 0 || index >= history.getTransactionCount()) return;

        Transaksi transaksi = history.getTransaction(index);
        if (transaksi == null) return;

        writer.updateTransaction(transaksi, jumlahBaru, strategyBaru, kategoriBaru);
        recalculateFromIndex(index);
        saveRiwayat();
    }

    public void hapusTransaksi(int index) {
        if (index < 0 || index >= history.getTransactionCount()) return;

        history.removeTransaction(index);
        recalculateFromIndex(index);
        saveRiwayat();
    }

    private void recalculateFromIndex(int startIndex) {
        // Recalculate all transactions from startIndex
        int saldoSebelum = (startIndex == 0) ? saldoAwal : history.getTransaction(startIndex - 1).getAkhir();

        for (int i = startIndex; i < history.getTransactionCount(); i++) {
            Transaksi transaksi = history.getTransaction(i);
            transaksi.setAwal(saldoSebelum);
            transaksi.hitungUlang();
            saldoSebelum = transaksi.getAkhir();
        }

        // Update balance manager with the final balance
        int finalBalance = getCurrentBalance();
        // Perbaikan 4: Konversi int ke double saat memanggil setSaldo
        balanceManager.setSaldo((double) finalBalance);
    }

    // Get current balance from transaction history
    private int getCurrentBalance() {
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
            history.clearHistory();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error loading transaction data. Starting fresh.");
            history.clearHistory();
        }
    }

    public void saveRiwayat() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(history.getAllTransactions());
            System.out.println("Saved " + history.getTransactionCount() + " transactions to " + dataFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error saving transaction data.");
        }
    }

    public void clearAllTransactions() {
        history.clearHistory();
        // Perbaikan 5: Konversi int ke double saat memanggil resetSaldo
        balanceManager.resetSaldo((double) saldoAwal);
        saveRiwayat();
    }
}