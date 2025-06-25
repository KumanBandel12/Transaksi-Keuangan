package pbo.transaksikeuangan;

import java.util.ArrayList;
import java.util.List;

public class BalanceManager {
    // Singleton pattern
    private static BalanceManager instance;

    // Current balance
    private double saldo = 0.0;

    // Observer pattern
    private final List<saldoObserver> observers = new ArrayList<>();

    // Private constructor for singleton
    private BalanceManager() {}

    // Get singleton instance
    public static BalanceManager getInstance() {
        if (instance == null) {
            instance = new BalanceManager();
        }
        return instance;
    }

    // Get current balance
    public double getSaldo() {
        return saldo;
    }

    // Set balance and notify observers
    public void setSaldo(double newSaldo) { // Changed to public for TransaksiManager to update it
        double oldSaldo = this.saldo;
        this.saldo = newSaldo;

        System.out.println("BalanceManager - Saldo changed: " + oldSaldo + " -> " + newSaldo);
        notifyObservers();
    }

    // Sync method to ensure balance is correctly synchronized
    public void syncWithTransactionBalance(double transactionBalance) {
        setSaldo(transactionBalance);
    }

    // Observer pattern methods
    public void addObserver(saldoObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            System.out.println("BalanceManager - Observer added: " + observer.getClass().getSimpleName());
        }
    }

    public void removeObserver(saldoObserver observer) {
        observers.remove(observer);
        System.out.println("BalanceManager - Observer removed: " + observer.getClass().getSimpleName());
    }

    private void notifyObservers() {
        System.out.println("BalanceManager - Notifying " + observers.size() + " observers");
        for (saldoObserver observer : observers) {
            try {
                observer.onSaldoChanged(saldo);
            } catch (Exception e) {
                System.err.println("Error notifying observer: " + e.getMessage());
            }
        }
    }

    // Reset balance
    public void resetSaldo(double initialBalance) {
        setSaldo(initialBalance);
    }

    // Get observer count for debugging
    public int getObserverCount() {
        return observers.size();
    }
}
