// src/main/java/pbo/transaksikeuangan/AnalisisPemasukanManager.java
package pbo.transaksikeuangan;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class AnalisisPemasukanManager {
    private TransaksiManager transaksiManager;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AnalisisPemasukanManager(TransaksiManager transaksiManager) {
        this.transaksiManager = transaksiManager;
    }

    public double getTotalPemasukan(LocalDate mulai, LocalDate akhir) {
        return transaksiManager.getRiwayat().stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .filter(t -> isInDateRange(t, mulai, akhir))
                .mapToDouble(Transaksi::getJumlah)
                .sum();
    }

    public double getRataRataPemasukan(LocalDate mulai, LocalDate akhir) {
        List<Transaksi> pemasukanList = transaksiManager.getRiwayat().stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .filter(t -> isInDateRange(t, mulai, akhir))
                .collect(Collectors.toList());

        if (pemasukanList.isEmpty()) {
            return 0.0;
        }

        return pemasukanList.stream()
                .mapToDouble(Transaksi::getJumlah)
                .average()
                .orElse(0.0);
    }

    public Map<String, Double> getPemasukanPerKategori(LocalDate mulai, LocalDate akhir) {
        Map<String, Double> kategoriMap = new HashMap<>();

        transaksiManager.getRiwayat().stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .filter(t -> isInDateRange(t, mulai, akhir))
                .forEach(t -> {
                    // Pemasukan tidak memiliki kategori spesifik di Transaksi.java, jadi gunakan "Pemasukan Umum"
                    String kategori = "Pemasukan Umum";
                    kategoriMap.merge(kategori, (double) t.getJumlah(), Double::sum);
                });

        return kategoriMap;
    }

    public Map<String, Double> getTrendPemasukan(LocalDate mulai, LocalDate akhir, String periode) {
        Map<String, Double> trendMap = new LinkedHashMap<>();

        List<Transaksi> pemasukanList = transaksiManager.getRiwayat().stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .filter(t -> isInDateRange(t, mulai, akhir))
                .collect(Collectors.toList());

        if (pemasukanList.isEmpty()) {
            return trendMap;
        }

        switch (periode) {
            case "Harian":
                trendMap = groupByDaily(pemasukanList);
                break;
            case "Mingguan":
                trendMap = groupByWeekly(pemasukanList);
                break;
            case "Bulanan":
                trendMap = groupByMonthly(pemasukanList);
                break;
            case "Tahunan":
                trendMap = groupByYearly(pemasukanList);
                break;
        }

        return trendMap;
    }

    public List<String> getDetailPemasukan(LocalDate mulai, LocalDate akhir) {
        return transaksiManager.getRiwayat().stream()
                .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                .filter(t -> isInDateRange(t, mulai, akhir))
                .map(t -> String.format("%s - %s: Rp %,d",
                        t.getTanggal() != null ? t.getTanggal().format(formatter) : "Tanpa Tanggal",
                        "Pemasukan Umum", // Pemasukan tidak punya kategori spesifik
                        t.getJumlah()))
                .collect(Collectors.toList());
    }

    public void exportToExcel(LocalDate mulai, LocalDate akhir) {
        try (FileWriter writer = new FileWriter("analisis_pemasukan.csv")) {
            writer.write("Tanggal,Kategori,Jumlah\n");

            transaksiManager.getRiwayat().stream()
                    .filter(t -> t.getStrategy() instanceof penambahanStrategy)
                    .filter(t -> isInDateRange(t, mulai, akhir))
                    .forEach(t -> {
                        try {
                            writer.write(String.format("%s,%s,%d\n",
                                    t.getTanggal() != null ? t.getTanggal().format(formatter) : "Tanpa Tanggal",
                                    "Pemasukan Umum", // Pemasukan tidak punya kategori spesifik
                                    t.getJumlah()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // New method to export all transactions (income and expense)
    public void exportAllTransactionsToCsv(LocalDate mulai, LocalDate akhir) {
        try (FileWriter writer = new FileWriter("semua_transaksi.csv")) {
            writer.write("Tanggal,Jenis Transaksi,Kategori,Jumlah,Saldo Awal,Saldo Akhir\n");

            transaksiManager.getRiwayat().stream()
                    .filter(t -> isInDateRange(t, mulai, akhir))
                    .forEach(t -> {
                        try {
                            String jenisTransaksi = t.getStrategy().getNama();
                            String kategori = (t.getKategori() != null) ? t.getKategori().toString() : "-";
                            writer.write(String.format("%s,%s,%s,%d,%d,%d\n",
                                    t.getTanggal() != null ? t.getTanggal().format(formatter) : "Tanpa Tanggal",
                                    jenisTransaksi,
                                    kategori,
                                    t.getJumlah(),
                                    t.getAwal(),
                                    t.getAkhir()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper methods
    private boolean isInDateRange(Transaksi transaksi, LocalDate mulai, LocalDate akhir) {
        if (transaksi.getTanggal() == null) {
            return false;
        }
        LocalDate tanggalTransaksi = transaksi.getTanggal();
        return !tanggalTransaksi.isBefore(mulai) && !tanggalTransaksi.isAfter(akhir);
    }

    private Map<String, Double> groupByDaily(List<Transaksi> transaksiList) {
        return transaksiList.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTanggal().format(formatter),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaksi::getJumlah)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Double> groupByWeekly(List<Transaksi> transaksiList) {
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        return transaksiList.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            LocalDate date = t.getTanggal();
                            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
                            int year = date.get(weekFields.weekBasedYear());
                            return String.format("Minggu %d, %d", weekOfYear, year);
                        },
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaksi::getJumlah)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Double> groupByMonthly(List<Transaksi> transaksiList) {
        return transaksiList.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            LocalDate date = t.getTanggal();
                            return date.getMonth().toString() + " " + date.getYear();
                        },
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaksi::getJumlah)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Double> groupByYearly(List<Transaksi> transaksiList) {
        return transaksiList.stream()
                .collect(Collectors.groupingBy(
                        t -> String.valueOf(t.getTanggal().getYear()),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Transaksi::getJumlah)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }
}
