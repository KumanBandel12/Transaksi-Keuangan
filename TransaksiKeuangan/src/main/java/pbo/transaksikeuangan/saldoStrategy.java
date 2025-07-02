package pbo.transaksikeuangan;
import java.io.Serializable; // Pastikan ini ada
interface saldoStrategy extends Serializable { // Pastikan 'extends Serializable' ada
    void execute(Transaksi transaksi);
    String getNama();
}