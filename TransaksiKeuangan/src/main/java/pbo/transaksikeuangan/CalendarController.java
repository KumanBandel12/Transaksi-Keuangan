package pbo.transaksikeuangan;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarController {

    @FXML private GridPane calendarGrid;
    @FXML private Label monthYearLabel;

    // --- Tombol Filter ---
    @FXML private Button btnPengeluaran;
    @FXML private Button btnPenghasilan;
    @FXML private Button btnTotal;

    // --- Label Ringkasan ---
    @FXML private Label summaryTotalLabel;
    @FXML private Label summaryPemasukanLabel;
    @FXML private Label summaryPengeluaranLabel;

    // --- Data and State ---
    private YearMonth currentYearMonth;
    private TransaksiManager transaksiManager;
    private Map<LocalDate, List<Transaksi>> transactionsByDate;
    private String activeFilterOnDay = "Total"; // Filter untuk angka di dalam tanggal
    private LocalDate selectedDate;

    @FXML
    public void initialize() {
        transaksiManager = SharedDataManager.getInstance().getTransaksiManager();
        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now(); // Default tanggal terpilih adalah hari ini
        groupTransactionsByDate();
        populateCalendar();
        updateBottomSummary(selectedDate); // Tampilkan ringkasan untuk hari ini
        updateFilterButtonStyles();
    }

    private void groupTransactionsByDate() {
        transactionsByDate = new HashMap<>();
        List<Transaksi> allTransactions = transaksiManager.getRiwayat();
        for (Transaksi t : allTransactions) {
            LocalDate date = t.getTanggal();
            transactionsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(t);
        }
    }

    private void populateCalendar() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM uuuu")));
        LocalDate calendarDate = currentYearMonth.atDay(1);
        int firstDayOfWeek = calendarDate.getDayOfWeek().getValue() % 7; // 0=Min, 1=Sen,...

        for (int day = 1; day <= currentYearMonth.lengthOfMonth(); day++) {
            LocalDate currentDate = currentYearMonth.atDay(day);
            int col = currentDate.getDayOfWeek().getValue() % 7;
            int row = (day + firstDayOfWeek -1) / 7;

            StackPane dayCell = createDayCell(currentDate);
            calendarGrid.add(dayCell, col, row);
        }
    }

    private StackPane createDayCell(LocalDate date) {
        StackPane dayCell = new StackPane();
        dayCell.setPrefSize(50, 50);

        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setTextFill( (date.getDayOfWeek().getValue() >= 6) ? Color.web("#E91E63") : Color.BLACK );

        dayCell.getChildren().add(dayNumber);

        // Tambahkan event handler klik
        dayCell.setOnMouseClicked(event -> {
            this.selectedDate = date;
            updateBottomSummary(date);
            populateCalendar(); // Gambar ulang untuk menyorot tanggal
        });

        // Beri style berbeda jika tanggal dipilih
        if (date.equals(selectedDate)) {
            Circle selectionCircle = new Circle(20, Color.web("#F48FB1"));
            dayCell.getChildren().add(0, selectionCircle); // Tambahkan di belakang angka
            dayNumber.setTextFill(Color.WHITE);
        }

        // Tampilkan ringkasan pada tanggal jika ada transaksi
        if (transactionsByDate.containsKey(date)) {
            Label summaryLabel = createDaySummaryLabel(transactionsByDate.get(date));
            dayCell.getChildren().add(summaryLabel);
            StackPane.setAlignment(summaryLabel, Pos.BOTTOM_CENTER);
        }

        return dayCell;
    }

    private Label createDaySummaryLabel(List<Transaksi> dailyTransactions) {
        double value = 0;
        switch (activeFilterOnDay) {
            case "Pengeluaran":
                value = dailyTransactions.stream().filter(t -> t.getStrategy() instanceof penguranganStrategy).mapToDouble(Transaksi::getJumlah).sum();
                break;
            case "Pemasukan":
                value = dailyTransactions.stream().filter(t -> t.getStrategy() instanceof penambahanStrategy).mapToDouble(Transaksi::getJumlah).sum();
                break;
            case "Total":
                double income = dailyTransactions.stream().filter(t -> t.getStrategy() instanceof penambahanStrategy).mapToDouble(Transaksi::getJumlah).sum();
                double expense = dailyTransactions.stream().filter(t -> t.getStrategy() instanceof penguranganStrategy).mapToDouble(Transaksi::getJumlah).sum();
                value = income - expense;
                break;
        }

        if (value == 0) return new Label("");

        // Format ke "rb" atau "jt"
        String formattedValue;
        if (Math.abs(value) >= 1_000_000) {
            formattedValue = String.format("%.1f jt", value / 1_000_000.0);
        } else {
            formattedValue = String.format("%.0f rb", value / 1_000.0);
        }

        Label summary = new Label(formattedValue);
        summary.setFont(Font.font(10));
        summary.setTextFill(Color.WHITE);
        return summary;
    }

    private void updateBottomSummary(LocalDate date) {
        if (date == null) return;

        double dailyIncome = 0;
        double dailyExpense = 0;

        if (transactionsByDate.containsKey(date)) {
            List<Transaksi> dailyTx = transactionsByDate.get(date);
            dailyIncome = dailyTx.stream().filter(t -> t.getStrategy() instanceof penambahanStrategy).mapToDouble(Transaksi::getJumlah).sum();
            dailyExpense = dailyTx.stream().filter(t -> t.getStrategy() instanceof penguranganStrategy).mapToDouble(Transaksi::getJumlah).sum();
        }

        summaryPemasukanLabel.setText(String.format("Rp%,.0f", dailyIncome));
        summaryPengeluaranLabel.setText(String.format("Rp%,.0f", dailyExpense));
        summaryTotalLabel.setText(String.format("Rp%,.0f", dailyIncome - dailyExpense));
    }

    private void updateFilterButtonStyles() {
        btnPengeluaran.getStyleClass().remove("selected");
        btnPenghasilan.getStyleClass().remove("selected");
        btnTotal.getStyleClass().remove("selected");

        switch(activeFilterOnDay) {
            case "Pengeluaran": btnPengeluaran.getStyleClass().add("selected"); break;
            case "Pemasukan": btnPenghasilan.getStyleClass().add("selected"); break;
            case "Total": btnTotal.getStyleClass().add("selected"); break;
        }
    }

    // --- Action Handlers ---
    @FXML void previousMonth(ActionEvent event) { currentYearMonth = currentYearMonth.minusMonths(1); populateCalendar(); }
    @FXML void nextMonth(ActionEvent event) { currentYearMonth = currentYearMonth.plusMonths(1); populateCalendar(); }

    @FXML void filterPengeluaran(ActionEvent event) { this.activeFilterOnDay = "Pengeluaran"; updateFilterButtonStyles(); populateCalendar(); }
    @FXML void filterPemasukan(ActionEvent event) { this.activeFilterOnDay = "Pemasukan"; updateFilterButtonStyles(); populateCalendar(); }
    @FXML void filterTotal(ActionEvent event) { this.activeFilterOnDay = "Total"; updateFilterButtonStyles(); populateCalendar(); }

    @FXML void backToMainMenu(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pbo/transaksikeuangan/MainMenu.fxml"));
        Parent root = loader.load();
        MenuController menuController = loader.getController();
        if (menuController != null) {
            menuController.refreshData();
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}