package pbo.transaksikeuangan;

public class penguranganStrategy implements saldoStrategy{
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
