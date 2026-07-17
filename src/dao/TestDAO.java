package dao;

import model.Test;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

public class TestDAO {
    private Connection conn;

    public TestDAO(Connection conn) {
        this.conn = conn;
    }

    // Insert Test
    public boolean insertTest(Test test) throws SQLException {
        String sql = "INSERT INTO Test (test_name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, test.getTestName());
            stmt.executeUpdate();
            System.out.println("✅ Test inserted successfully.");
            return true;
        }
    }

    // Get All Tests for REST API
    public List<Test> getAllTests() {
        List<Test> list = new ArrayList<>();
        String sql = "SELECT * FROM Test ORDER BY test_id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Test t = new Test(rs.getString("test_name"));
                t.setTestId(rs.getInt("test_id"));
                list.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // View All Tests (CLI)
    public void viewAll() {
        List<Test> list = getAllTests();
        System.out.println("\n--- Test Records ---");
        for (Test t : list) {
            System.out.printf("ID: %d | Test Name: %s\n", t.getTestId(), t.getTestName());
        }
    }

    // Update Test by ID (REST API)
    public boolean updateTestById(int id, String newName) throws SQLException {
        String sql = "UPDATE Test SET test_name = ? WHERE test_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Update Test (CLI)
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

            if (updateTestById(id, newName)) {
                System.out.println("✅ Test updated successfully.");
            } else {
                System.out.println("❌ Test ID not found.");
            }
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            sc.nextLine(); // clear scanner buffer
        }
    }

    // Delete Test by ID (REST API)
    public boolean deleteTestById(int id) throws SQLException {
        String sql = "DELETE FROM Test WHERE test_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Delete Test (CLI)
    public void deleteTest(Scanner sc) {
        try {
            System.out.print("Enter Test ID to delete: ");
            int id = sc.nextInt();

            if (deleteTestById(id)) {
                System.out.println("✅ Test deleted.");
            } else {
                System.out.println("❌ Test not found.");
            }
        } catch (Exception e) {
            System.out.println("❌ Invalid input.");
            sc.nextLine(); // Clear scanner buffer
        }
    }
}
