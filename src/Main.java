// Main.java
import dao.*;
import model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            String url = "jdbc:mysql://localhost:3306/medical_report_db";
            String user = "root";
            String password = "131205";
            Connection conn = DriverManager.getConnection(url, user, password);

            PatientDAO patientDAO = new PatientDAO(conn);
            DoctorDAO doctorDAO = new DoctorDAO(conn);
            TestDAO testDAO = new TestDAO(conn);
            MedicalReportDAO reportDAO = new MedicalReportDAO(conn);
            ReportDetailDAO detailDAO = new ReportDetailDAO(conn);

            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.println("\n=== Operation Menu ===");
                System.out.println("1. Insert");
                System.out.println("2. View");
                System.out.println("3. Update");
                System.out.println("4. Delete");
                System.out.println("0. Exit");
                System.out.print("Select an operation: ");
                int operation = sc.nextInt(); sc.nextLine();

                if (operation == 0) break;

                System.out.println("\n--- Select Table ---");
                System.out.println("1. Patient");
                System.out.println("2. Doctor");
                System.out.println("3. Test");
                System.out.println("4. Medical Report");
                System.out.println("5. Report Detail");
                System.out.print("Select a table: ");
                int table = sc.nextInt(); sc.nextLine();

                switch (operation) {
                    case 1: // Insert
                        switch (table) {
                            case 1: insertPatient(sc, patientDAO); break;
                            case 2: insertDoctor(sc, doctorDAO); break;
                            case 3: insertTest(sc, testDAO); break;
                            case 4: insertReport(sc, reportDAO); break;
                            case 5: insertReportDetail(sc, detailDAO); break;
                            default: System.out.println("Invalid table.");
                        }
                        break;

                    case 2: // View
                        switch (table) {
                            case 1: patientDAO.viewAll(); break;
                            case 2: doctorDAO.viewAll(); break;
                            case 3: testDAO.viewAll(); break;
                            case 4: reportDAO.viewAll(); break;
                            case 5: detailDAO.viewAll(); break;
                            default: System.out.println("Invalid table.");
                        }
                        break;

                    case 3: // Update
                        switch (table) {
                            case 1: patientDAO.updatePatient(sc); break;
                            case 2: doctorDAO.updateDoctor(sc); break;
                            case 3: testDAO.updateTest(sc); break;
                            case 4: reportDAO.updateMedicalReport(sc); break;
                            case 5: detailDAO.updateReportDetail(sc); break;
                            default: System.out.println("Invalid table.");
                        }
                        break;

                    case 4: // Delete
                        switch (table) {
                            case 1: patientDAO.deletePatient(sc); break;
                            case 2: doctorDAO.deleteDoctor(sc); break;
                            case 3: testDAO.deleteTest(sc); break;
                            case 4: reportDAO.deleteMedicalReport(sc); break;
                            case 5: detailDAO.deleteReportDetail(sc); break;
                            default: System.out.println("Invalid table.");
                        }
                        break;

                    default:
                        System.out.println("Invalid operation.");
                }
            }

            sc.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertPatient(Scanner sc, PatientDAO dao) {
        try {
            System.out.print("Enter Patient ID: ");
            int pid = sc.nextInt(); sc.nextLine();
            System.out.print("Enter Name: ");
            String pname = sc.nextLine();
            if (!pname.matches("^[A-Za-z ]+$")) {
                System.out.println("❌ Invalid name! Only letters and spaces allowed.");
                return;
            }
            System.out.print("Enter Gender (Male/Female/Other): ");
            String gender = sc.nextLine().toUpperCase();
            if (!Arrays.asList("MALE", "FEMALE", "OTHER").contains(gender)) {
                System.out.println("❌ Invalid gender!");
                return;
            }
            System.out.print("Enter DOB (YYYY-MM-DD): ");
            String dob = sc.nextLine();
            try {
                LocalDate enteredDob = LocalDate.parse(dob);
                if (enteredDob.isAfter(LocalDate.now())) {
                    System.out.println("❌ DOB cannot be in the future.");
                    return;
                }
            } catch (DateTimeParseException e) {
                System.out.println("❌ Invalid date format.");
                return;
            }
            System.out.print("Enter Phone: ");
            String phone = sc.nextLine();
            if (!phone.matches("^\\d{10,15}$")) {
                System.out.println("❌ Invalid phone.");
                return;
            }
            System.out.print("Enter Email: ");
            String email = sc.nextLine();
            if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
                System.out.println("❌ Invalid email format.");
                return;
            }
            dao.insertPatient(new Patient(pid, pname, gender, dob, phone, email));
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    public static void insertDoctor(Scanner sc, DoctorDAO dao) throws Exception {
        System.out.print("Enter Doctor Name: ");
        String name = sc.nextLine();
        if (!name.matches("^[a-zA-Z ]+$")) {
            System.out.println("❌ Invalid name! Only letters and spaces allowed.");
            return;
        }

        System.out.print("Enter Specialization: ");
        String spec = sc.nextLine();
        if (!spec.matches("^[a-zA-Z ]+$")) {
            System.out.println("❌ Invalid specialization! Only letters and spaces allowed.");
            return;
        }

        System.out.print("Enter Phone: ");
        String phone = sc.nextLine();
        if (!phone.matches("^\\d{10,15}$")) {
            System.out.println("❌ Invalid phone! Must be 10 to 15 digits.");
            return;
        }

        boolean success = dao.insertDoctor(new Doctor(name, spec, phone));
        if (success) {
            System.out.println("✅ Doctor inserted successfully.");
        } else {
            System.out.println("❌ Failed to insert doctor.");
        }
    }

    public static void insertTest(Scanner sc, TestDAO dao) throws Exception {
        System.out.print("Enter Test Name: ");
        String tname = sc.nextLine();

        if (!tname.matches("^[a-zA-Z0-9 ]+$")) {
            System.out.println("❌ Invalid test name! Only letters, numbers, and spaces allowed.");
            return;
        }

        dao.insertTest(new Test(tname));
    }


    public static void insertReport(Scanner sc, MedicalReportDAO dao) throws Exception {
        System.out.print("Enter Patient ID: ");
        int pid = sc.nextInt();

        System.out.print("Enter Doctor ID: ");
        int did = sc.nextInt();

        sc.nextLine(); // consume newline
        System.out.print("Enter Report Date (YYYY-MM-DD): ");
        String rdate = sc.nextLine();

        try {
            LocalDate reportDate = LocalDate.parse(rdate);
            if (reportDate.isAfter(LocalDate.now())) {
                System.out.println("❌ Report date cannot be in the future.");
                return;
            }
        } catch (DateTimeParseException e) {
            System.out.println("❌ Invalid date format.");
            return;
        }

        boolean success = dao.insertMedicalReport(new MedicalReport(pid, did, rdate));
        if (success)
            System.out.println("✅ Report inserted successfully.");
        else
            System.out.println("❌ Failed to insert report.");
    }


    public static void insertReportDetail(Scanner sc, ReportDetailDAO dao) {
        try {
            System.out.print("Enter Report ID: ");
            int repId = sc.nextInt();

            System.out.print("Enter Test ID: ");
            int testId = sc.nextInt();
            sc.nextLine(); // Consume newline

            System.out.print("Enter Result: ");
            String result = sc.nextLine().trim();
            if (result.isEmpty()) {
                System.out.println("❌ Result cannot be empty.");
                return;
            }

            System.out.print("Enter Diagnosis: ");
            String diag = sc.nextLine().trim();
            if (diag.isEmpty()) {
                System.out.println("❌ Diagnosis cannot be empty.");
                return;
            }

            boolean success = dao.insertReportDetail(new ReportDetail(repId, testId, result, diag));
            if (success)
                System.out.println("✅ Report detail inserted.");
            else
                System.out.println("❌ Failed to insert report detail.");
        } catch (Exception e) {
            System.out.println("❌ Input error: " + e.getMessage());
        }
    }
}
