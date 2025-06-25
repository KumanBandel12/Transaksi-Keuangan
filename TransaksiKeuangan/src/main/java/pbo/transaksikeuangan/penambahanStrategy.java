package pbo.transaksikeuangan;

public class penambahanStrategy implements saldoStrategy{
    @Override
    public void execute(Transaksi transaksi){
        int hasil;
        hasil = transaksi.getAwal() + transaksi.getJumlah();
        transaksi.setAkhir(hasil);
    }

    @Override
    public String getNama(){
        return "Saldo Masuk";
    }
}
