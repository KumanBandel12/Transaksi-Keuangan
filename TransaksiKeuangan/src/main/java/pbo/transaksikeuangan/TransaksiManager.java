package pbo.transaksikeuangan;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TransaksiManager {
    private final BalanceManager balanceManager;
    private final int userId;

    public TransaksiManager(String username) {
        this.userId = getUserIdByUsername(username);
        this.balanceManager = BalanceManager.getInstance();
        // Saat aplikasi dimulai, set saldo dari data terakhir di database
        this.balanceManager.setSaldo(getSaldoTerakhirFromDB());
    }

    // --- INTERAKSI DATABASE ---

    public void tambahTransaksi(int jumlah, saldoStrategy strategy, KategoriPengeluaran kategori, LocalDate tanggal) {
        double saldoSebelum = getSaldoTerakhirFromDB();
        double saldoSetelah = (strategy instanceof penambahanStrategy) ? saldoSebelum + jumlah : saldoSebelum - jumlah;
        String jenis = (strategy instanceof penambahanStrategy) ? "Pemasukan" : "Pengeluaran";
        Integer kategoriId = getKategoriId(kategori);

        String sql = "INSERT INTO transaksi (user_id, kategori_id, tanggal, jenis_transaksi, jumlah, saldo_awal, saldo_akhir) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.userId);
            if (kategoriId != null) pstmt.setInt(2, kategoriId); else pstmt.setNull(2, Types.INTEGER);
            pstmt.setDate(3, Date.valueOf(tanggal));
            pstmt.setString(4, jenis);
            pstmt.setDouble(5, jumlah);
            pstmt.setDouble(6, saldoSebelum);
            pstmt.setDouble(7, saldoSetelah);
            pstmt.executeUpdate();

            // Setelah menambah, langsung hitung ulang semua saldo untuk konsistensi
            recalculateAllBalances();
            balanceManager.setSaldo(getSaldoTerakhirFromDB());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void editTransaksi(int transaksiId, Transaksi transaksiBaru) {
        String sql = "UPDATE transaksi SET tanggal = ?, jenis_transaksi = ?, kategori_id = ?, jumlah = ? WHERE transaksi_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String jenis = (transaksiBaru.getStrategy() instanceof penambahanStrategy) ? "Pemasukan" : "Pengeluaran";
            Integer kategoriId = getKategoriId(transaksiBaru.getKategori());
            pstmt.setDate(1, Date.valueOf(transaksiBaru.getTanggal()));
            pstmt.setString(2, jenis);
            if (kategoriId != null) pstmt.setInt(3, kategoriId); else pstmt.setNull(3, Types.INTEGER);
            pstmt.setDouble(4, transaksiBaru.getJumlah());
            pstmt.setInt(5, transaksiId);
            pstmt.executeUpdate();

            recalculateAllBalances();
            balanceManager.setSaldo(getSaldoTerakhirFromDB());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void hapusTransaksi(int transaksiId) {
        String sql = "DELETE FROM transaksi WHERE transaksi_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, transaksiId);
            pstmt.executeUpdate();
            recalculateAllBalances();
            balanceManager.setSaldo(getSaldoTerakhirFromDB());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void recalculateAllBalances() {
        List<Transaksi> allUserTransactions = getRiwayat();
        double lastBalance = 0;
        for (Transaksi tx : allUserTransactions) {
            tx.setAwal((int) lastBalance);
            tx.hitungUlang();
            lastBalance = tx.getAkhir();

            String updateSql = "UPDATE transaksi SET saldo_awal = ?, saldo_akhir = ? WHERE transaksi_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setDouble(1, tx.getAwal());
                pstmt.setDouble(2, tx.getAkhir());
                pstmt.setInt(3, tx.getTransaksiId());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Transaksi> getRiwayat() {
        List<Transaksi> riwayat = new ArrayList<>();
        String sql = "SELECT t.*, k.nama_kategori FROM transaksi t LEFT JOIN kategori k ON t.kategori_id = k.kategori_id WHERE t.user_id = ? ORDER BY t.transaksi_id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                saldoStrategy strategy = rs.getString("jenis_transaksi").equals("Pemasukan") ? new penambahanStrategy() : new penguranganStrategy();
                KategoriPengeluaran kategori = getKategoriByName(rs.getString("nama_kategori"));
                Transaksi t = new Transaksi(rs.getInt("saldo_awal"), rs.getInt("jumlah"), strategy, kategori, rs.getDate("tanggal").toLocalDate());
                t.setTransaksiId(rs.getInt("transaksi_id"));
                t.setAkhir(rs.getInt("saldo_akhir"));
                riwayat.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return riwayat;
    }

    public double getSaldoTerakhirFromDB() {
        String sql = "SELECT saldo_akhir FROM transaksi WHERE user_id = ? ORDER BY transaksi_id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("saldo_akhir");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private int getUserIdByUsername(String username) {
        String sql = "SELECT users_id FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("users_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Integer getKategoriId(KategoriPengeluaran kategori) {
        if (kategori == null) return null;
        String sql = "SELECT kategori_id FROM kategori WHERE nama_kategori = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, kategori.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("kategori_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private KategoriPengeluaran getKategoriByName(String namaKategori) {
        if (namaKategori == null) return null;
        for (KategoriPengeluaran k : KategoriPengeluaran.values()) {
            if (k.toString().equals(namaKategori)) {
                return k;
            }
        }
        return null;
    }

    public List<Transaksi> getTransactionsByDate(LocalDate date) {
        return getRiwayat().stream()
                .filter(t -> t.getTanggal() != null && t.getTanggal().equals(date))
                .collect(Collectors.toList());
    }

    public List<Transaksi> getTransactionsByMonth(YearMonth month) {
        return getRiwayat().stream()
                .filter(t -> {
                    LocalDate tDate = t.getTanggal();
                    return tDate != null && tDate.getYear() == month.getYear() && tDate.getMonth() == month.getMonth();
                })
                .collect(Collectors.toList());
    }

    public void clearAllTransactions() {
        String sql = "DELETE FROM transaksi WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, this.userId);
            pstmt.executeUpdate();
            balanceManager.setSaldo(0.0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public BalanceManager getBalanceManager() {
        if (balanceManager == null) {
            throw new IllegalStateException("BalanceManager not initialized. Ensure user is logged in via SharedDataManager.initializeForUser().");
        }
        return balanceManager;
    }

}