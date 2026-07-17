package dao;

import model.Test;

import java.sql.*;
import java.util.Scanner;

public class TestDAO {
    private Connection conn;

    public TestDAO(Connection conn) {
        this.conn = conn;
    }

    // Insert Test
 // TestDAO.java
    public boolean insertTest(Test test) {
        String sql = "INSERT INTO Test (test_name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, test.getTestName());
            stmt.executeUpdate();
            System.out.println("✅ Test inserted successfully.");
            return true;
        } catch (SQLException e) {
            System.out.println("❌ Failed to insert test: " + e.getMessage());
            return false;
        }
    }

    // View All Tests
    public void viewAll() {
        String sql = "SELECT * FROM Test";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- Test Records ---");
            while (rs.next()) {
                int id = rs.getInt("test_id");
                String name = rs.getString("test_name");
                System.out.printf("ID: %d | Test Name: %s\n", id, name);
            }
        } catch (SQLException e) {
            System.out.println("View error: " + e.getMessage());
        }
    }

 // Update Test
    public void updateTest(Scanner sc) {
        try {
            System.out.print("Enter Test ID to update: ");
            int id = sc.nextInt();
            sc.nextLine(); // consume leftover newline

            System.out.print("Enter new Test Name: ");
            String newName = sc.nextLine();

            // Validate the new name before updating
            if (!newName.matches("^[a-zA-Z0-9 ]+$")) {
                System.out.println("❌ Invalid test name! Only alphanumeric characters and spaces allowed.");
                return;
            }

            String sql = "UPDATE Test SET test_name = ? WHERE test_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newName);
                ps.setInt(2, id);
                int rows = ps.executeUpdate();
                if (rows > 0)
                    System.out.println("✅ Test updated successfully.");
                else
                    System.out.println("❌ Test ID not found.");
            }
        } catch (SQLException e) {
            System.out.println("Update error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            sc.nextLine(); // clear scanner buffer
        }
    }

    public void deleteTest(Scanner sc) {
        try {
            System.out.print("Enter Test ID to delete: ");
            int id = sc.nextInt();

            String sql = "DELETE FROM Test WHERE test_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0)
                    System.out.println("✅ Test deleted.");
                else
                    System.out.println("❌ Test not found.");
            }
        } catch (SQLException e) {
            System.out.println("Delete error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            sc.nextLine(); // Clear scanner buffer
        }
    }

}
