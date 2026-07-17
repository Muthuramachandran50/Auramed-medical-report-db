package dao;

import model.MedicalReport;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class MedicalReportDAO {
    private Connection conn;

    public MedicalReportDAO(Connection conn) {
        this.conn = conn;
    }

    // Insert Report
    public boolean insertMedicalReport(MedicalReport report) throws SQLException {
        String sql = "INSERT INTO Medical_Report (patient_id, doctor_id, report_date) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, report.getPatientId());
            stmt.setInt(2, report.getDoctorId());
            stmt.setString(3, report.getReportDate());

            int rows = stmt.executeUpdate();
            return rows > 0;
        }
    }

    // Get All Reports for REST API
    public List<MedicalReport> getAllReports() {
        List<MedicalReport> list = new ArrayList<>();
        String sql = "SELECT * FROM Medical_Report ORDER BY report_id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                MedicalReport r = new MedicalReport(
                    rs.getInt("patient_id"),
                    rs.getInt("doctor_id"),
                    rs.getString("report_date")
                );
                r.setReportId(rs.getInt("report_id"));
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // View All Reports (CLI)
    public void viewAll() {
        List<MedicalReport> list = getAllReports();
        System.out.println("\n--- Medical Reports ---");
        for (MedicalReport r : list) {
            System.out.printf("Report ID: %d | Patient ID: %d | Doctor ID: %d | Date: %s\n",
                    r.getReportId(), r.getPatientId(), r.getDoctorId(), r.getReportDate());
        }
    }

    // Update Report by ID (REST API)
    public boolean updateMedicalReportById(int id, MedicalReport report) throws SQLException {
        String sql = "UPDATE Medical_Report SET patient_id = ?, doctor_id = ?, report_date = ? WHERE report_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, report.getPatientId());
            stmt.setInt(2, report.getDoctorId());
            stmt.setString(3, report.getReportDate());
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // Update Report (CLI)
    public void updateMedicalReport(Scanner sc) {
        try {
            System.out.print("Enter Report ID to update: ");
            int id = sc.nextInt();

            System.out.print("Enter new Patient ID: ");
            int newPatientId = sc.nextInt();

            System.out.print("Enter new Doctor ID: ");
            int newDoctorId = sc.nextInt();

            sc.nextLine();  // consume newline

            System.out.print("Enter new Report Date (YYYY-MM-DD): ");
            String newDate = sc.nextLine();

            MedicalReport report = new MedicalReport(newPatientId, newDoctorId, newDate);
            if (updateMedicalReportById(id, report)) {
                System.out.println("✅ Report updated successfully.");
            } else {
                System.out.println("❌ Report ID not found.");
            }
        } catch (Exception e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    // Delete Report by ID (REST API)
    public boolean deleteMedicalReportById(int id) throws SQLException {
        String sql = "DELETE FROM Medical_Report WHERE report_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // Delete Report (CLI)
    public void deleteMedicalReport(Scanner sc) {
        try {
            System.out.print("Enter Report ID to delete: ");
            int id = sc.nextInt();

            if (deleteMedicalReportById(id)) {
                System.out.println("✅ Report deleted.");
            } else {
                System.out.println("❌ Report ID not found.");
            }
        } catch (Exception e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }
}
