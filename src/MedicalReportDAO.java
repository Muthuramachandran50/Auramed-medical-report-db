package dao;

import model.MedicalReport;

import java.sql.*;
import java.util.Scanner;

public class MedicalReportDAO {
    private Connection conn;

    public MedicalReportDAO(Connection conn) {
        this.conn = conn;
    }

    // Insert Report
    public boolean insertMedicalReport(MedicalReport report) {
        String sql = "INSERT INTO Medical_Report (patient_id, doctor_id, report_date) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, report.getPatientId());
            stmt.setInt(2, report.getDoctorId());
            stmt.setString(3, report.getReportDate());

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Insert error: " + e.getMessage());
            return false;
        }
    }

    // View All Reports
    public void viewAll() {
        String sql = "SELECT * FROM Medical_Report";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Medical Reports ---");
            while (rs.next()) {
                int reportId = rs.getInt("report_id");
                int patientId = rs.getInt("patient_id");
                int doctorId = rs.getInt("doctor_id");
                String date = rs.getString("report_date");

                System.out.printf("Report ID: %d | Patient ID: %d | Doctor ID: %d | Date: %s\n",
                        reportId, patientId, doctorId, date);
            }
        } catch (SQLException e) {
            System.out.println("View error: " + e.getMessage());
        }
    }

    // Update Report
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

            String sql = "UPDATE Medical_Report SET patient_id = ?, doctor_id = ?, report_date = ? WHERE report_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, newPatientId);
                stmt.setInt(2, newDoctorId);
                stmt.setString(3, newDate);
                stmt.setInt(4, id);

                int rows = stmt.executeUpdate();
                if (rows > 0)
                    System.out.println("✅ Report updated successfully.");
                else
                    System.out.println("❌ Report ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    // Delete Report
    public void deleteMedicalReport(Scanner sc) {
        try {
            System.out.print("Enter Report ID to delete: ");
            int id = sc.nextInt();

            String sql = "DELETE FROM Medical_Report WHERE report_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                int rows = stmt.executeUpdate();
                if (rows > 0)
                    System.out.println("✅ Report deleted.");
                else
                    System.out.println("❌ Report ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }
}
