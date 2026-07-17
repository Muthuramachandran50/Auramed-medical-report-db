package dao;

import model.ReportDetail;

import java.sql.*;
import java.util.Scanner;
import java.util.InputMismatchException;


public class ReportDetailDAO {
    private Connection conn;

    public ReportDetailDAO(Connection conn) {
        this.conn = conn;
    }

    // INSERT
    public boolean insertReportDetail(ReportDetail detail) {
        String sql = "INSERT INTO Report_Details (report_id, test_id, result, diagnosis) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, detail.getReportId());
            stmt.setInt(2, detail.getTestId());
            stmt.setString(3, detail.getResult());
            stmt.setString(4, detail.getDiagnosis());
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Insert error: " + e.getMessage());
            return false;
        }
    }

    // VIEW ALL
    public void viewAll() {
        String sql = "SELECT * FROM Report_Details";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("ReportID: %d, TestID: %d, Result: %s, Diagnosis: %s\n",
                        rs.getInt("report_id"),
                        rs.getInt("test_id"),
                        rs.getString("result"),
                        rs.getString("diagnosis"));
            }
        } catch (SQLException e) {
            System.out.println("View error: " + e.getMessage());
        }
    }

    // UPDATE
    public void updateReportDetail(Scanner sc) {
        try {
            System.out.print("Enter Report ID to update: ");
            int reportId = sc.nextInt();
            System.out.print("Enter Test ID: ");
            int testId = sc.nextInt();
            sc.nextLine(); // Consume newline

            System.out.print("Enter new Result: ");
            String result = sc.nextLine().trim();
            if (result.isEmpty()) {
                System.out.println("❌ Result cannot be empty.");
                return;
            }

            System.out.print("Enter new Diagnosis: ");
            String diagnosis = sc.nextLine().trim();
            if (diagnosis.isEmpty()) {
                System.out.println("❌ Diagnosis cannot be empty.");
                return;
            }

            String sql = "UPDATE Report_Details SET result = ?, diagnosis = ? WHERE report_id = ? AND test_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, result);
                stmt.setString(2, diagnosis);
                stmt.setInt(3, reportId);
                stmt.setInt(4, testId);

                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "✅ Report detail updated." : "❌ Report detail not found.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Update error: " + e.getMessage());
        }
    }


    // DELETE
    public void deleteReportDetail(Scanner sc) {
        try {
            System.out.print("Enter Report ID to delete: ");
            int reportId = sc.nextInt();

            System.out.print("Enter Test ID: ");
            int testId = sc.nextInt();

            String sql = "DELETE FROM Report_Details WHERE report_id = ? AND test_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reportId);
                stmt.setInt(2, testId);

                int rows = stmt.executeUpdate();
                System.out.println(rows > 0 ? "✅ Report detail deleted." : "❌ Report detail not found.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Delete error: " + e.getMessage());
        } catch (InputMismatchException e) {
            System.out.println("❌ Invalid input! Must be numeric.");
            sc.nextLine(); // clear invalid input
        }
    }

}
