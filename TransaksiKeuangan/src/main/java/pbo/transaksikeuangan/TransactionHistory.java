package pbo.transaksikeuangan;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistory implements Serializable {
    private static final long serialVersionUID = 1L; // Add serialVersionUID
    private final List<Transaksi> riwayat = new ArrayList<>();

    public void addTransaction(Transaksi transaksi) {
        riwayat.add(transaksi);
    }

    public void removeTransaction(int index) {
        if (index >= 0 && index < riwayat.size()) {
            riwayat.remove(index);
        }
    }

    public Transaksi getTransaction(int index) {
        if (index >= 0 && index < riwayat.size()) {
            return riwayat.get(index);
        }
        return null;
    }

    public List<Transaksi> getAllTransactions() {
        return new ArrayList<>(riwayat); // Return copy to prevent external modification
    }

    public void setAllTransactions(List<Transaksi> newRiwayat) {
        this.riwayat.clear();
        this.riwayat.addAll(newRiwayat);
    }

    public int getTransactionCount() {
        return riwayat.size();
    }

    public boolean isEmpty() {
        return riwayat.isEmpty();
    }

    public Transaksi getLastTransaction() {
        return isEmpty() ? null : riwayat.get(riwayat.size() - 1);
    }

    public void displayHistory() {
        System.out.println("========== Riwayat Transaksi ==========");
        for (int i = 0; i < riwayat.size(); i++) {
            System.out.println((i + 1) + ". " + riwayat.get(i));
        }
        System.out.println();
    }

    public void clearHistory() {
        riwayat.clear();
    }
}
