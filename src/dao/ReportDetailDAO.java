package dao;

import model.ReportDetail;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.InputMismatchException;

public class ReportDetailDAO {
    private Connection conn;

    public ReportDetailDAO(Connection conn) {
        this.conn = conn;
    }

    // INSERT
    public boolean insertReportDetail(ReportDetail detail) throws SQLException {
        String sql = "INSERT INTO Report_Details (report_id, test_id, result, diagnosis) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, detail.getReportId());
            stmt.setInt(2, detail.getTestId());
            stmt.setString(3, detail.getResult());
            stmt.setString(4, detail.getDiagnosis());
            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    // Get All Report Details for REST API
    public List<ReportDetail> getAllReportDetails() {
        List<ReportDetail> list = new ArrayList<>();
        String sql = "SELECT * FROM Report_Details ORDER BY detail_id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ReportDetail detail = new ReportDetail(
                    rs.getInt("detail_id"),
                    rs.getInt("report_id"),
                    rs.getInt("test_id"),
                    rs.getString("result"),
                    rs.getString("diagnosis")
                );
                list.add(detail);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // VIEW ALL (CLI)
    public void viewAll() {
        List<ReportDetail> list = getAllReportDetails();
        for (ReportDetail rs : list) {
            System.out.printf("ReportID: %d, TestID: %d, Result: %s, Diagnosis: %s\n",
                    rs.getReportId(), rs.getTestId(), rs.getResult(), rs.getDiagnosis());
        }
    }

    // UPDATE by IDs (REST API)
    public boolean updateReportDetailByIds(int reportId, int testId, String result, String diagnosis) throws SQLException {
        String sql = "UPDATE Report_Details SET result = ?, diagnosis = ? WHERE report_id = ? AND test_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result);
            stmt.setString(2, diagnosis);
            stmt.setInt(3, reportId);
            stmt.setInt(4, testId);
            return stmt.executeUpdate() > 0;
        }
    }

    // UPDATE (CLI)
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

            if (updateReportDetailByIds(reportId, testId, result, diagnosis)) {
                System.out.println("✅ Report detail updated.");
            } else {
                System.out.println("❌ Report detail not found.");
            }
        } catch (Exception e) {
            System.out.println("❌ Update error: " + e.getMessage());
        }
    }

    // DELETE by IDs (REST API)
    public boolean deleteReportDetailByIds(int reportId, int testId) throws SQLException {
        String sql = "DELETE FROM Report_Details WHERE report_id = ? AND test_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reportId);
            stmt.setInt(2, testId);
            return stmt.executeUpdate() > 0;
        }
    }

    // DELETE (CLI)
    public void deleteReportDetail(Scanner sc) {
        try {
            System.out.print("Enter Report ID to delete: ");
            int reportId = sc.nextInt();

            System.out.print("Enter Test ID: ");
            int testId = sc.nextInt();

            if (deleteReportDetailByIds(reportId, testId)) {
                System.out.println("✅ Report detail deleted.");
            } else {
                System.out.println("❌ Report detail not found.");
            }
        } catch (Exception e) {
            System.out.println("❌ Delete error: " + e.getMessage());
            if (sc.hasNext()) sc.nextLine(); // clear buffer
        }
    }
}
