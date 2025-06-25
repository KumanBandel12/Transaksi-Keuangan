// src/main/java/pbo/transaksikeuangan/Transaksi.java
package pbo.transaksikeuangan;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Transaksi implements Serializable {
    private static final long serialVersionUID = 1L;
    private saldoStrategy strategy;
    private KategoriPengeluaran kategori;
    private int awal;
    private int jumlah;
    private int akhir;
    private LocalDate tanggal;

    // Constructor for income (penambahanStrategy)
    public Transaksi(int awal, int jumlah, saldoStrategy strategy, LocalDate tanggal){
        if(!(strategy instanceof penambahanStrategy)){
            throw new IllegalArgumentException("This constructor is only for income transactions (penambahanStrategy).");
        }

        this.strategy = strategy;
        this.awal = awal;
        this.jumlah = jumlah;
        this.kategori = null; // Income transactions don't have a specific category
        this.tanggal = tanggal;
        strategy.execute(this);
    }

    // Constructor for expense (penguranganStrategy)
    public Transaksi(int awal, int jumlah, saldoStrategy strategy, KategoriPengeluaran kategori, LocalDate tanggal){
        if (!(strategy instanceof penguranganStrategy)){
            throw new IllegalArgumentException("This constructor is only for expense transactions (penguranganStrategy).");
        }

        this.strategy = strategy;
        this.awal = awal;
        this.jumlah = jumlah;
        this.tanggal = tanggal;

        if (kategori == null){
            throw new IllegalArgumentException("Expense category cannot be null for expense transactions.");
        }
        this.kategori = kategori;
        strategy.execute(this);
    }

    // Setters
    public void setAwal(int awal){
        this.awal = awal;
    }
    public void setJumlah(int jumlah){
        this.jumlah = jumlah;
    }
    public void setAkhir(int akhir){
        this.akhir = akhir;
    }
    public void setStrategy(saldoStrategy strategy){
        this.strategy = strategy;
    }
    public void setKategori(KategoriPengeluaran kategori){
        this.kategori = kategori;
    }
    public void setTanggal(LocalDate tanggal) {
        this.tanggal = tanggal;
    }

    // Getters
    public int getAwal(){
        return this.awal;
    }
    public int getJumlah(){
        return this.jumlah;
    }
    public int getAkhir(){
        return this.akhir;
    }
    public saldoStrategy getStrategy(){
        return strategy;
    }
    public KategoriPengeluaran getKategori(){
        return kategori;
    }
    public LocalDate getTanggal() {
        return tanggal;
    }

    public void hitungUlang(){
        strategy.execute(this);
    }

    @Override
    public String toString(){
        String jenis = (kategori != null) ? kategori.toString() : "Pemasukan Umum"; // For display
        String operator = (strategy instanceof penambahanStrategy) ? "+" : "-";
        String tanggalStr = (tanggal != null) ? tanggal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Tanpa Tanggal";
        return String.format("[%s - %s] Saldo Awal: Rp %,d %s Rp %,d = Saldo Akhir: Rp %,d",
                tanggalStr, strategy.getNama(), awal, operator, jumlah, akhir);
    }

    // A shorter string representation for history list
    public String toShortString() {
        String jenis = (kategori != null) ? kategori.toString() : "Pemasukan Umum";
        String operator = (strategy instanceof penambahanStrategy) ? "+" : "-";
        return String.format("%s %s Rp %,d (Saldo: Rp %,d)",
                jenis, operator, jumlah, akhir);
    }
}
