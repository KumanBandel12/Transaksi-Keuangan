package pbo.transaksikeuangan;

public class SharedDataManager {
    private static SharedDataManager instance;
    private static TransaksiManager transaksiManager;
    private static BalanceManager balanceManager;
    private static String currentUsername;

    private SharedDataManager() {
        // Privat untuk singleton
    }

    /**
     * Initialize for a specific user after login.
     */
    public static void initializeForUser(String username) {
        if (instance == null) {
            instance = new SharedDataManager();
        }
        if (!username.equals(currentUsername)) {
            currentUsername = username;

            // ===== PERBAIKAN ADA DI SINI =====
            // Panggil konstruktor dengan dua argumen (saldoAwal dan username)
            transaksiManager = new TransaksiManager(currentUsername);
        }
        balanceManager = BalanceManager.getInstance();
    }

    public static SharedDataManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SharedDataManager not initialized for a user. Call initializeForUser() after successful login.");
        }
        return instance;
    }

    public TransaksiManager getTransaksiManager() {
        if (transaksiManager == null) {
            throw new IllegalStateException("TransaksiManager not initialized. Ensure user is logged in via SharedDataManager.initializeForUser().");
        }
        return transaksiManager;
    }

    public BalanceManager getBalanceManager() {
        if (balanceManager == null) {
            throw new IllegalStateException("BalanceManager not initialized. Ensure user is logged in via SharedDataManager.initializeForUser().");
        }
        return balanceManager;
    }

    public void resetData() {
        if (transaksiManager != null) {
            // Asumsi TransaksiManager memiliki method ini
            // Jika tidak, Anda perlu menambahkannya untuk menghapus data dari file/db
            // transaksiManager.clearAllTransactions();
        }
    }

    public static void resetInstance() {
        instance = null;
        transaksiManager = null;
        currentUsername = null;
        // Reset saldo di BalanceManager saat logout
        BalanceManager.getInstance().resetSaldo(0.0);
    }
}