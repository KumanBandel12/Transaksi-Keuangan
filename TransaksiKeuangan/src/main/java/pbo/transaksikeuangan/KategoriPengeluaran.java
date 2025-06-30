package pbo.transaksikeuangan;

public enum KategoriPengeluaran {
    PEMBELIAN,
    TABUNGAN,
    PENARIKAN,
    LAINNYA;

    @Override
    public String toString(){
        switch (this) {
            case PEMBELIAN:
                return "Pembelian";
            case TABUNGAN:
                return "Tabungan";
            case PENARIKAN:
                return "Penarikan";
            case LAINNYA:
                return "Lainnya";
            default:
                return name();
        }
    }
}
