package pbo.transaksikeuangan;

import java.io.Serializable; // Tambahkan import ini

public class penguranganStrategy implements saldoStrategy, Serializable { // Tambahkan ', Serializable'
    @Override
    public void execute(Transaksi transaksi){
        int hasil;
        hasil = transaksi.getAwal() - transaksi.getJumlah();
        transaksi.setAkhir(hasil);
    }

    @Override
    public String getNama(){
        return "Saldo Keluar";
    }
}