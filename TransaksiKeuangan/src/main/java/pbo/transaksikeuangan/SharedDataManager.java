package pbo.transaksikeuangan;

public class SharedDataManager {
    private static SharedDataManager instance;
    private static TransaksiManager transaksiManager;
    private static BalanceManager balanceManager;
    private static String currentUsername; // To manage user-specific data files

    private SharedDataManager() {
        // balanceManager is now initialized directly within initializeForUser
        // to ensure it's always associated with a logged-in user's data.
    }

    // Initialize for a specific user after login
    public static void initializeForUser(String username) {
        if (instance == null) {
            instance = new SharedDataManager();
        }
        // Always reset BalanceManager before initializing TransaksiManager for a new user
        // This ensures a clean state for the balance associated with the new user.
        BalanceManager.getInstance().resetSaldo(0.0); // Reset existing singleton BalanceManager

        if (!username.equals(currentUsername)) { // Only re-initialize if user changes
            currentUsername = username;
            // TransaksiManager is now responsible for initializing BalanceManager's saldo
            // based on loaded transaction history or initial 0.
            transaksiManager = new TransaksiManager(0, currentUsername); // Initial balance 0, user-specific file
        }
        balanceManager = BalanceManager.getInstance(); // Get the instance after TransaksiManager might have updated it
    }

    public static SharedDataManager getInstance() {
        if (instance == null) {
            // This case should ideally not happen if initializeForUser is called after login
            // For safety, we can return a default uninitialized instance or throw an error.
            // For an application flow where login is mandatory, throwing an error is safer.
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
            transaksiManager.clearAllTransactions(); // Clear all transactions and reset balance
        }
        // BalanceManager's state is reset by TransaksiManager.clearAllTransactions()
    }

    // Method to reset the singleton instance (e.g., on logout)
    public static void resetInstance() {
        instance = null;
        if (transaksiManager != null) {
            // Optionally, save current state before nulling if needed for next login.
            // For now, we assume save is handled during normal operations.
            transaksiManager = null;
        }
        currentUsername = null;
        // BalanceManager is a separate singleton, its state is managed by TransaksiManager
        // or explicitly reset if needed. Ensure balance is reset to 0 for a clean start.
        BalanceManager.getInstance().resetSaldo(0.0); // Ensure balance is reset
    }
}