package dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/medical_report_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "131205";

    public static Connection getConnection() throws Exception {
        String url = System.getenv("DB_URL");
        if (url == null) url = DEFAULT_URL;

        String user = System.getenv("DB_USER");
        if (user == null) user = DEFAULT_USER;

        String password = System.getenv("DB_PASSWORD");
        if (password == null) password = DEFAULT_PASSWORD;

        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }
}
