package dao;

import java.sql.*;
import model.Patient;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class PatientDAO {
    private Connection conn;

    // Constructor
    public PatientDAO(Connection conn) {
        this.conn = conn;
    }

    // Insert
    public boolean insertPatient(Patient patient) throws SQLException {
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
        }
    }

    // Get All Patients for REST API
    public List<Patient> getAllPatients() {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT * FROM Patients";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Patient(
                    rs.getInt("patient_id"),
                    rs.getString("name"),
                    rs.getString("gender"),
                    rs.getString("dob"),
                    rs.getString("phone"),
                    rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // View All (CLI)
    public void viewAll() {
        List<Patient> list = getAllPatients();
        for (Patient rs : list) {
            System.out.printf("ID: %d, Name: %s, Gender: %s, DOB: %s, Phone: %s, Email: %s\n",
                rs.getPatientId(), rs.getName(), rs.getGender(), rs.getDob(), rs.getPhone(), rs.getEmail());
        }
    }

    // Update by ID (REST API)
    public boolean updatePatientById(int id, Patient patient) throws SQLException {
        String sql = "UPDATE Patients SET name = ?, gender = ?, dob = ?, phone = ?, email = ? WHERE patient_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getName());
            ps.setString(2, patient.getGender());
            ps.setString(3, patient.getDob());
            ps.setString(4, patient.getPhone());
            ps.setString(5, patient.getEmail());
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Update (CLI)
    public void updatePatient(Scanner sc) {
        try {
            System.out.print("Enter Patient ID to update: ");
            int id = sc.nextInt(); sc.nextLine();
            System.out.print("Enter new name: ");
            String name = sc.nextLine();
            System.out.print("Enter new phone: ");
            String phone = sc.nextLine();

            // Fetch current to keep others
            Patient existing = null;
            for (Patient p : getAllPatients()) {
                if (p.getPatientId() == id) {
                    existing = p;
                    break;
                }
            }
            if (existing != null) {
                Patient updated = new Patient(id, name, existing.getGender(), existing.getDob(), phone, existing.getEmail());
                if (updatePatientById(id, updated)) {
                    System.out.println("✅ Patient updated successfully.");
                } else {
                    System.out.println("❌ Failed to update patient.");
                }
            } else {
                System.out.println("❌ Patient ID not found.");
            }
        } catch (Exception e) {
            System.out.println("Update error: " + e.getMessage());
        }
    }

    // Delete by ID (REST API)
    public boolean deletePatientById(int id) throws SQLException {
        String sql = "DELETE FROM Patients WHERE patient_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Delete (CLI)
    public void deletePatient(Scanner sc) {
        try {
            System.out.print("Enter Patient ID to delete: ");
            int id = sc.nextInt();
            if (deletePatientById(id)) {
                System.out.println("✅ Patient deleted.");
            } else {
                System.out.println("❌ Patient not found.");
            }
        } catch (Exception e) {
            System.out.println("Delete error: " + e.getMessage());
        }
    }
}
