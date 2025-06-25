package pbo.transaksikeuangan;

interface saldoStrategy {
    void execute(Transaksi transaksi);
    String getNama();
}
