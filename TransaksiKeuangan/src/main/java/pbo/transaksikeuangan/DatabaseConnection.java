package pbo.transaksikeuangan;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // GANTI DENGAN DETAIL DATABASE ANDA
    // Pastikan nama database adalah "saquku"
    private static final String DB_URL = "jdbc:mysql://localhost:3306/saquku";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = ""; // Sesuaikan dengan password database XAMPP/MySQL Anda

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}