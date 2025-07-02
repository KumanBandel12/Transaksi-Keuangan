package pbo.transaksikeuangan;

import java.util.ArrayList;
import java.util.List;

public class saldoManager {
    // singleton
    private  static saldoManager instance;

    // nilai awal saldo
    private double saldo = 0.0;

    private final List<saldoObserver> observers = new ArrayList<>();

    // Private constructor (untuk singleton)
    private saldoManager() {}

    // Mendapatkan instance singleton
    public static saldoManager getInstance() {
        if (instance == null) {
            instance = new saldoManager();
        }
        return instance;
    }

    // Getter saldo
    public double getSaldo() {
        return saldo;
    }

    // Setter saldo (ubah akses menjadi public agar bisa diatur dari luar)
    public void setSaldo(double newSaldo) { // <-- Ubah dari private menjadi public
        this.saldo = newSaldo;
        notifyObservers(); // Beritahu semua observer
    }

    // Tambah saldo (ini bisa dihapus jika semua perubahan saldo diatur via TransaksiManager)
    // public void tambahSaldo(double jumlah) {
    //     setSaldo(this.saldo + jumlah);
    // }

    // Kurang saldo (ini bisa dihapus jika semua perubahan saldo diatur via TransaksiManager)
    // public void kurangSaldo(double jumlah) {
    //     setSaldo(this.saldo - jumlah);
    // }

    // Tambah observer
    public void addObserver(saldoObserver observer) {
        observers.add(observer);
    }

    // Notifikasi ke semua observer
    private void notifyObservers() {
        for (saldoObserver observer : observers) {
            observer.onSaldoChanged(saldo);
        }
    }
}
    