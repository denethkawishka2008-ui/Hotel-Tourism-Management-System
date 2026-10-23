package Hospitality_and_tourism_system;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/hospitality_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "deneth";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}