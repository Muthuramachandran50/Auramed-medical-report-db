package dao;

import model.Doctor;
import java.sql.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DoctorDAO {
    private Connection conn;

    public DoctorDAO(Connection conn) {
        this.conn = conn;
    }

    public boolean insertDoctor(Doctor doctor) throws SQLException {
        String sql = "INSERT INTO Doctors (name, specialization, phone) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, doctor.getName());
            stmt.setString(2, doctor.getSpecialization());
            stmt.setString(3, doctor.getPhone());
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Doctor> getAllDoctors() throws Exception {
        List<Doctor> list = new ArrayList<>();
        String sql = "SELECT * FROM Doctors";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Doctor d = new Doctor(
                    rs.getString("name"),
                    rs.getString("specialization"),
                    rs.getString("phone")
                );
                d.setDoctorId(rs.getInt("doctor_id"));
                list.add(d);
            }
        }
        return list;
    }

    public void viewAll() throws Exception {
        List<Doctor> list = getAllDoctors();
        for (Doctor d : list) {
            System.out.printf("ID: %d, Name: %s, Specialization: %s, Phone: %s%n",
                d.getDoctorId(), d.getName(), d.getSpecialization(), d.getPhone());
        }
    }

 // Update doctor by asking input from user
    public void updateDoctor(Scanner sc) {
        try {
            System.out.print("Enter Doctor ID to update: ");
            int id = sc.nextInt(); sc.nextLine();

            System.out.print("Enter new name: ");
            String name = sc.nextLine();
            if (!name.matches("^[a-zA-Z ]+$")) {
                System.out.println("❌ Invalid name.");
                return;
            }

            System.out.print("Enter new specialization: ");
            String spec = sc.nextLine();
            if (!spec.matches("^[a-zA-Z ]+$")) {
                System.out.println("❌ Invalid specialization.");
                return;
            }

            System.out.print("Enter new phone: ");
            String phone = sc.nextLine();
            if (!phone.matches("^\\d{10,15}$")) {
                System.out.println("❌ Invalid phone.");
                return;
            }

            Doctor doctor = new Doctor(name, spec, phone);
            boolean success = updateDoctorById(id, doctor);
            System.out.println(success ? "✅ Doctor updated successfully." : "❌ Failed to update doctor.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Core update logic
    public boolean updateDoctorById(int id, Doctor doctor) throws SQLException {
        String sql = "UPDATE Doctors SET name = ?, specialization = ?, phone = ? WHERE doctor_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, doctor.getName());
            stmt.setString(2, doctor.getSpecialization());
            stmt.setString(3, doctor.getPhone());
            stmt.setInt(4, id);
            return stmt.executeUpdate() > 0;
        }
    }

    // Delete doctor by asking user
    public void deleteDoctor(Scanner sc) {
        try {
            System.out.print("Enter Doctor ID to delete: ");
            int id = sc.nextInt(); sc.nextLine();

            boolean success = deleteDoctorById(id);
            System.out.println(success ? "✅ Doctor deleted successfully." : "❌ Failed to delete doctor.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Core delete logic
    public boolean deleteDoctorById(int id) throws SQLException {
        String sql = "DELETE FROM Doctors WHERE doctor_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

}
