package pbo.transaksikeuangan;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class TransaksiController implements saldoObserver {
    @FXML private ComboBox<String> jenisCombo;
    @FXML private ComboBox<KategoriPengeluaran> kategoriCombo;
    @FXML private Label displayLabel;

    @FXML private Stage stage;
    @FXML private Scene scene;
    @FXML private Parent root;

    private TransaksiManager manager;
    private BalanceManager balanceManager;
    private SharedDataManager sharedDataManager;
    private StringBuilder currentInput = new StringBuilder("0");
    private String currentOperator = "";
    private double firstOperand = 0;
    private boolean isNewInput = true;

    public void setSharedDataManager(SharedDataManager sharedDataManager) {
        this.sharedDataManager = sharedDataManager;
        this.manager = sharedDataManager.getTransaksiManager();
        this.balanceManager = sharedDataManager.getBalanceManager();

        // Register as observer
        balanceManager.addObserver(this);

        System.out.println("TransaksiController - SharedDataManager set, current saldo: " + balanceManager.getSaldo());
    }

    @FXML
    public void initialize() {
        // Jika SharedDataManager belum di-set (misalnya jika langsung ke TransaksiView tanpa login),
        // gunakan default instance. Ini penting untuk pengujian atau alur alternatif.
        if (sharedDataManager == null) {
            sharedDataManager = SharedDataManager.getInstance();
            manager = sharedDataManager.getTransaksiManager();
            balanceManager = sharedDataManager.getBalanceManager();
            balanceManager.addObserver(this);
        }

        // Setup combo boxes
        jenisCombo.getItems().addAll("Pemasukan", "Pengeluaran");
        kategoriCombo.getItems().addAll(KategoriPengeluaran.values());

        // Setup event listeners
        jenisCombo.setOnAction(e -> handleJenisChange());

        // Initialize display
        displayLabel.setText("0");

        System.out.println("TransaksiController initialized");
    }

    @FXML
    public void numberPressed(ActionEvent event) {
        Button button = (Button) event.getSource();
        String number = button.getText();

        if (isNewInput) {
            if ("00".equals(number)) {
                currentInput = new StringBuilder("0");
            } else if (".".equals(number)) {
                currentInput = new StringBuilder("0.");
            } else {
                currentInput = new StringBuilder(number);
            }
            isNewInput = false;
        } else {
            if ("00".equals(number)) {
                currentInput.append("00");
            } else if (".".equals(number) && !currentInput.toString().contains(".")) {
                currentInput.append(".");
            } else if (!".".equals(number)) {
                if ("0".equals(currentInput.toString())) {
                    currentInput = new StringBuilder(number);
                } else {
                    currentInput.append(number);
                }
            }
        }

        updateDisplay();
    }

    @FXML
    public void operatorPressed(ActionEvent event) {
        Button button = (Button) event.getSource();
        String operator = button.getText();

        if (!currentOperator.isEmpty()) {
            calculateResult();
        } else {
            firstOperand = Double.parseDouble(currentInput.toString());
        }

        currentOperator = operator;
        isNewInput = true;
    }

    @FXML
    public void clearCalculator() {
        currentInput = new StringBuilder("0");
        currentOperator = "";
        firstOperand = 0;
        isNewInput = true;
        updateDisplay();
    }

    @FXML
    public void backspaceCalculator() {
        if (currentInput.length() > 1) {
            currentInput.deleteCharAt(currentInput.length() - 1);
        } else {
            currentInput = new StringBuilder("0");
        }
        updateDisplay();
    }

    private void calculateResult() {
        double secondOperand = Double.parseDouble(currentInput.toString());
        double result = firstOperand;

        switch (currentOperator) {
            case "+":
                result = firstOperand + secondOperand;
                break;
            case "-":
                result = firstOperand - secondOperand;
                break;
            case "×":
                result = firstOperand * secondOperand;
                break;
            case "÷":
                if (secondOperand != 0) {
                    result = firstOperand / secondOperand;
                } else {
                    showAlert("Error", "Tidak dapat membagi dengan nol");
                    return;
                }
                break;
        }

        currentInput = new StringBuilder(String.valueOf(result));
        currentOperator = "";
        firstOperand = result;
        updateDisplay();
    }

    private void updateDisplay() {
        String displayText = currentInput.toString();

        // Format number with thousand separators
        try {
            double value = Double.parseDouble(displayText);
            // Remove decimal if it's a whole number
            if (value == (long) value) {
                displayLabel.setText(String.format("%,d", (long) value));
            } else {
                displayLabel.setText(String.format("%,.2f", value)); // Keep 2 decimal places for non-integers
            }
        } catch (NumberFormatException e) {
            displayLabel.setText(displayText);
        }
    }

    @FXML
    public void handleTambah() {
        try {
            // Validasi input
            if (jenisCombo.getValue() == null) {
                showAlert("Input Salah", "Pilih jenis transaksi");
                return;
            }

            double jumlahDouble = Double.parseDouble(currentInput.toString());
            int jumlah = (int) jumlahDouble; // Convert to int for Transaksi

            if (jumlah <= 0) {
                showAlert("Input Salah", "Jumlah harus lebih dari 0");
                return;
            }

            String jenis = jenisCombo.getValue();

            // Simpan saldo sebelum transaksi untuk perbandingan
            double saldoSebelum = balanceManager.getSaldo();

            if ("Pemasukan".equals(jenis)) {
                manager.tambahTransaksi(jumlah, new penambahanStrategy());

                showSuccessAlert("Transaksi Berhasil",
                        String.format("✅ Pemasukan Rp %,d berhasil ditambahkan!\n\n" +
                                        "Saldo sebelum: Rp %,.0f\n" +
                                        "Saldo sekarang: Rp %,.0f",
                                jumlah, saldoSebelum, balanceManager.getSaldo()));

            } else if ("Pengeluaran".equals(jenis)) {
                KategoriPengeluaran kategori = kategoriCombo.getValue();
                if (kategori == null) {
                    showAlert("Input Salah", "Pilih kategori pengeluaran");
                    return;
                }

                // Cek saldo mencukupi
                if (saldoSebelum < jumlah) {
                    showAlert("Saldo Tidak Mencukupi",
                            String.format("❌ Saldo tidak mencukupi!\n\n" +
                                            "Saldo saat ini: Rp %,.0f\n" +
                                            "Jumlah pengeluaran: Rp %,d\n" +
                                            "Kekurangan: Rp %,.0f",
                                    saldoSebelum, jumlah, jumlah - saldoSebelum));
                    return;
                }

                manager.tambahTransaksi(jumlah, new penguranganStrategy(), kategori);

                showSuccessAlert("Transaksi Berhasil",
                        String.format("✅ Pengeluaran Rp %,d berhasil ditambahkan!\n" +
                                        "Kategori: %s\n\n" +
                                        "Saldo sebelum: Rp %,.0f\n" +
                                        "Saldo sekarang: Rp %,.0f",
                                jumlah, kategori.toString(), saldoSebelum, balanceManager.getSaldo()));
            }

            // Reset form setelah transaksi berhasil
            resetForm();

            System.out.println("TransaksiController - Transaksi berhasil: " + jenis + " Rp " + jumlah);

        } catch (NumberFormatException e) {
            showAlert("Input Salah", "Masukkan jumlah yang valid");
        } catch (Exception e) {
            showAlert("Error", "Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetForm() {
        // Reset calculator
        clearCalculator();

        // Reset combo boxes
        jenisCombo.setValue(null);
        kategoriCombo.setValue(null);
        kategoriCombo.setDisable(false); // Ensure it's enabled for next selection
    }

    private void handleJenisChange() {
        String selectedJenis = jenisCombo.getValue();
        if ("Pemasukan".equals(selectedJenis)) {
            kategoriCombo.setDisable(true);
            kategoriCombo.setValue(null);
        } else if ("Pengeluaran".equals(selectedJenis)) {
            kategoriCombo.setDisable(false);
        }
    }

    @Override
    public void onSaldoChanged(double saldoBaru) {
        System.out.println("TransaksiController - Saldo changed to: " + saldoBaru);
        // No direct UI update needed here as the main menu will refresh
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void switchToMainMenu(ActionEvent event) throws IOException {
        try {
            System.out.println("TransaksiController - Switching to MainMenu...");

            // Unregister observer when leaving
            if (balanceManager != null) {
                balanceManager.removeObserver(this);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
            root = loader.load();

            // Get the MenuController and refresh data
            MenuController menuController = loader.getController();
            if (menuController != null) {
                menuController.refreshData(); // Explicitly refresh data
            }

            stage = (Stage)((Node) event.getSource()).getScene().getWindow();
            scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Menu Utama - Sistem Keuangan");
            stage.show();

        } catch (IOException e) {
            showAlert("Error", "Gagal kembali ke menu utama: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
