package dao;

import java.sql.*;
import model.Patient;
import java.util.Scanner;

public class PatientDAO {
    private Connection conn;

    // Constructor
    public PatientDAO(Connection conn) {
        this.conn = conn;
    }

    // Insert
    public boolean insertPatient(Patient patient) {
        String sql = "INSERT INTO Patients (patient_id, name, gender, dob, phone, email) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patient.getPatientId());
            stmt.setString(2, patient.getName());
            stmt.setString(3, patient.getGender());
            stmt.setString(4, patient.getDob());
            stmt.setString(5, patient.getPhone());
            stmt.setString(6, patient.getEmail());
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // View All
    public void viewAll() {
        String sql = "SELECT * FROM Patients";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("ID: %d, Name: %s, Gender: %s, DOB: %s, Phone: %s, Email: %s\n",
                    rs.getInt("patient_id"),
                    rs.getString("name"),
                    rs.getString("gender"),
                    rs.getString("dob"),
                    rs.getString("phone"),
                    rs.getString("email"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving patients: " + e.getMessage());
        }
    }

    // Update
    public void updatePatient(Scanner sc) {
        try {
            System.out.print("Enter Patient ID to update: ");
            int id = sc.nextInt(); sc.nextLine();
            System.out.print("Enter new name: ");
            String name = sc.nextLine();
            System.out.print("Enter new phone: ");
            String phone = sc.nextLine();

            String sql = "UPDATE Patients SET name = ?, phone = ? WHERE patient_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setInt(3, id);
                int rows = ps.executeUpdate();
                if (rows > 0)
                    System.out.println("✅ Patient updated successfully.");
                else
                    System.out.println("❌ Patient ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    // Delete
    public void deletePatient(Scanner sc) {
        try {
            System.out.print("Enter Patient ID to delete: ");
            int id = sc.nextInt();

            String sql = "DELETE FROM Patients WHERE patient_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0)
                    System.out.println("✅ Patient deleted.");
                else
                    System.out.println("❌ Patient not found.");
            }
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }
}
