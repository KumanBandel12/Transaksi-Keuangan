package pbo.transaksikeuangan;

public class SharedDataManager {
    private static SharedDataManager instance;
    private static TransaksiManager transaksiManager;
    private static BalanceManager balanceManager;
    private static String currentUsername; // To manage user-specific data files

    private SharedDataManager() {
        balanceManager = BalanceManager.getInstance();
    }

    // Initialize for a specific user after login
    public static void initializeForUser(String username) {
        if (instance == null) {
            instance = new SharedDataManager();
        }
        if (!username.equals(currentUsername)) { // Only re-initialize if user changes
            currentUsername = username;
            transaksiManager = new TransaksiManager(0, currentUsername); // Initial balance 0, user-specific file
            // BalanceManager is already a singleton, its state will be updated by TransaksiManager
        }
    }

    public static SharedDataManager getInstance() {
        if (instance == null) {
            // This case should ideally not happen if initializeForUser is called after login
            // For safety, we can initialize with a default user or throw an error
            throw new IllegalStateException("SharedDataManager not initialized for a user. Call initializeForUser() first.");
        }
        return instance;
    }

    public TransaksiManager getTransaksiManager() {
        if (transaksiManager == null) {
            // Fallback if not initialized via initializeForUser, though it's better to ensure it is.
            // This might happen if MainApp directly loads MainMenu without login.
            // For robustness, we can initialize with a default user or throw an error.
            // For now, let's assume it's always initialized after login.
            throw new IllegalStateException("TransaksiManager not initialized. Ensure user is logged in.");
        }
        return transaksiManager;
    }

    public BalanceManager getBalanceManager() {
        return balanceManager;
    }

    public void resetData() {
        if (transaksiManager != null) {
            transaksiManager.clearAllTransactions(); // Clear all transactions and reset balance
        }
        // BalanceManager's state is reset by TransaksiManager.clearAllTransactions()
    }

    // Method to reset the singleton instance (e.g., on logout)
    public static void resetInstance() {
        instance = null;
        transaksiManager = null;
        currentUsername = null;
        // BalanceManager is a separate singleton, its state is managed by TransaksiManager
        // or explicitly reset if needed.
        BalanceManager.getInstance().resetSaldo(0); // Ensure balance is reset
    }
}
