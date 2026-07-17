import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dao.*;
import model.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServer {
    private static final int PORT = 8081;

    public static void main(String[] args) {
        try {
            // Test DB connectivity first
            try (Connection testConn = DBConnection.getConnection()) {
                System.out.println("✅ Database connection verified successfully!");
            } catch (Exception e) {
                System.err.println("❌ Database connection failed: " + e.getMessage());
                System.err.println("Please ensure MySQL is running and the database 'medical_report_db' exists.");
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            // Serve static files
            server.createContext("/", new StaticFileHandler());
            
            // REST API Handlers
            server.createContext("/api/patients", new PatientApiHandler());
            server.createContext("/api/doctors", new DoctorApiHandler());
            server.createContext("/api/tests", new TestApiHandler());
            server.createContext("/api/reports", new ReportApiHandler());
            server.createContext("/api/details", new DetailApiHandler());
            
            // Join Query Handlers
            server.createContext("/api/query/patient-report", new PatientReportQueryHandler());
            server.createContext("/api/query/test-counts", new TestCountsQueryHandler());

            server.setExecutor(null); // default executor
            System.out.println("🚀 Web Server started at http://localhost:" + PORT + "/");
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- HELPER UTILITIES ---

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        // Allow CORS for local development comfort
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void handleOptions(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(204, -1);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8.name());
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name()) : "";
                map.put(key, val);
            } catch (UnsupportedEncodingException e) {
                // skip
            }
        }
        return map;
    }

    private static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([\\d.-]+|true|false|null))");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (value != null && !value.equals("null")) {
                map.put(key, value);
            }
        }
        return map;
    }

    // --- STATIC FILE HANDLER ---

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html";
            else if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".js")) contentType = "application/javascript";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";

            byte[] fileBytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, fileBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }
    }

    // --- REST API HANDLERS ---

    // 1. Patient Handler
    static class PatientApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                PatientDAO dao = new PatientDAO(conn);

                if (method.equalsIgnoreCase("GET")) {
                    List<Patient> list = dao.getAllPatients();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Patient p = list.get(i);
                        json.append(String.format("{\"patient_id\":%d,\"name\":\"%s\",\"gender\":\"%s\",\"dob\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\"}",
                                p.getPatientId(), escape(p.getName()), escape(p.getGender()), escape(p.getDob()), escape(p.getPhone()), escape(p.getEmail())));
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    int pid = Integer.parseInt(data.get("patient_id"));
                    String name = data.get("name");
                    String gender = data.get("gender");
                    String dob = data.get("dob");
                    String phone = data.get("phone");
                    String email = data.get("email");

                    Patient p = new Patient(pid, name, gender, dob, phone, email);
                    boolean success = dao.insertPatient(p);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Patient inserted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to insert patient. Check triggers/validations.\"}");
                    }

                } else if (method.equalsIgnoreCase("PUT")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    String name = data.get("name");
                    String gender = data.get("gender");
                    String dob = data.get("dob");
                    String phone = data.get("phone");
                    String email = data.get("email");

                    Patient p = new Patient(id, name, gender, dob, phone, email);
                    boolean success = dao.updatePatientById(id, p);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Patient updated successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to update patient.\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean success = dao.deletePatientById(id);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Patient deleted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to delete patient. Ensure no dependent reports exist.\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    // 2. Doctor Handler
    static class DoctorApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                DoctorDAO dao = new DoctorDAO(conn);

                if (method.equalsIgnoreCase("GET")) {
                    List<Doctor> list = dao.getAllDoctors();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Doctor d = list.get(i);
                        json.append(String.format("{\"doctor_id\":%d,\"name\":\"%s\",\"specialization\":\"%s\",\"phone\":\"%s\"}",
                                d.getDoctorId(), escape(d.getName()), escape(d.getSpecialization()), escape(d.getPhone())));
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    String name = data.get("name");
                    String spec = data.get("specialization");
                    String phone = data.get("phone");

                    Doctor d = new Doctor(name, spec, phone);
                    boolean success = dao.insertDoctor(d);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Doctor inserted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to insert doctor.\"}");
                    }

                } else if (method.equalsIgnoreCase("PUT")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    String name = data.get("name");
                    String spec = data.get("specialization");
                    String phone = data.get("phone");

                    Doctor d = new Doctor(name, spec, phone);
                    boolean success = dao.updateDoctorById(id, d);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Doctor updated successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to update doctor.\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean success = dao.deleteDoctorById(id);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Doctor deleted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to delete doctor. Ensure no dependent reports exist.\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    // 3. Test Handler
    static class TestApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                TestDAO dao = new TestDAO(conn);

                if (method.equalsIgnoreCase("GET")) {
                    List<Test> list = dao.getAllTests();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Test t = list.get(i);
                        json.append(String.format("{\"test_id\":%d,\"test_name\":\"%s\"}",
                                t.getTestId(), escape(t.getTestName())));
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    String testName = data.get("test_name");

                    Test t = new Test(testName);
                    boolean success = dao.insertTest(t);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Test inserted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to insert test.\"}");
                    }

                } else if (method.equalsIgnoreCase("PUT")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    String testName = data.get("test_name");

                    boolean success = dao.updateTestById(id, testName);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Test updated successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to update test.\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean success = dao.deleteTestById(id);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Test deleted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to delete test. Check references in details.\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    // 4. Medical Report Handler
    static class ReportApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                MedicalReportDAO dao = new MedicalReportDAO(conn);

                if (method.equalsIgnoreCase("GET")) {
                    List<MedicalReport> list = dao.getAllReports();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        MedicalReport r = list.get(i);
                        json.append(String.format("{\"report_id\":%d,\"patient_id\":%d,\"doctor_id\":%d,\"report_date\":\"%s\"}",
                                r.getReportId(), r.getPatientId(), r.getDoctorId(), escape(r.getReportDate())));
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    int pid = Integer.parseInt(data.get("patient_id"));
                    int did = Integer.parseInt(data.get("doctor_id"));
                    String rdate = data.get("report_date");

                    MedicalReport r = new MedicalReport(pid, did, rdate);
                    boolean success = dao.insertMedicalReport(r);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Medical report inserted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to insert report. Check if IDs exist or date is future.\"}");
                    }

                } else if (method.equalsIgnoreCase("PUT")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    int pid = Integer.parseInt(data.get("patient_id"));
                    int did = Integer.parseInt(data.get("doctor_id"));
                    String rdate = data.get("report_date");

                    MedicalReport r = new MedicalReport(pid, did, rdate);
                    boolean success = dao.updateMedicalReportById(id, r);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Medical report updated successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to update report.\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean success = dao.deleteMedicalReportById(id);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Report deleted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to delete report. Clear details first.\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    // 5. Report Details Handler
    static class DetailApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                ReportDetailDAO dao = new ReportDetailDAO(conn);

                if (method.equalsIgnoreCase("GET")) {
                    List<ReportDetail> list = dao.getAllReportDetails();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        ReportDetail rd = list.get(i);
                        json.append(String.format("{\"detail_id\":%d,\"report_id\":%d,\"test_id\":%d,\"result\":\"%s\",\"diagnosis\":\"%s\"}",
                                rd.getDetailId(), rd.getReportId(), rd.getTestId(), escape(rd.getResult()), escape(rd.getDiagnosis())));
                        if (i < list.size() - 1) json.append(",");
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());

                } else if (method.equalsIgnoreCase("POST")) {
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    int rid = Integer.parseInt(data.get("report_id"));
                    int tid = Integer.parseInt(data.get("test_id"));
                    String result = data.get("result");
                    String diagnosis = data.get("diagnosis");

                    ReportDetail rd = new ReportDetail(rid, tid, result, diagnosis);
                    boolean success = dao.insertReportDetail(rd);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Report details inserted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to insert details. Ensure IDs exist and fields are not empty.\"}");
                    }

                } else if (method.equalsIgnoreCase("PUT")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int reportId = Integer.parseInt(query.get("report_id"));
                    int testId = Integer.parseInt(query.get("test_id"));
                    Map<String, String> data = parseJson(readRequestBody(exchange));
                    String result = data.get("result");
                    String diagnosis = data.get("diagnosis");

                    boolean success = dao.updateReportDetailByIds(reportId, testId, result, diagnosis);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Report details updated successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to update details.\"}");
                    }

                } else if (method.equalsIgnoreCase("DELETE")) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int reportId = Integer.parseInt(query.get("report_id"));
                    int testId = Integer.parseInt(query.get("test_id"));

                    boolean success = dao.deleteReportDetailByIds(reportId, testId);
                    if (success) {
                        sendResponse(exchange, 200, "application/json", "{\"success\":true,\"message\":\"Report details deleted successfully.\"}");
                    } else {
                        sendResponse(exchange, 400, "application/json", "{\"success\":false,\"message\":\"Failed to delete details.\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    // --- CUSTOM QUERY HANDLERS ---

    // Query 1: Get tests, results, diagnosis for a specific patient name
    static class PatientReportQueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            if (!method.equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "application/json", "{\"message\":\"Method Not Allowed\"}");
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String name = query.get("name");
            if (name == null || name.trim().isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"message\":\"Patient name query parameter is required\"}");
                return;
            }

            String sql = "SELECT P.name AS patient_name, T.test_name, RD.result, RD.diagnosis, MR.report_date " +
                         "FROM Patients P " +
                         "JOIN Medical_Report MR ON P.patient_id = MR.patient_id " +
                         "JOIN Report_Details RD ON MR.report_id = RD.report_id " +
                         "JOIN Test T ON RD.test_id = T.test_id " +
                         "WHERE P.name LIKE ? OR P.name = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + name + "%");
                stmt.setString(2, name);

                try (ResultSet rs = stmt.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"patient_name\":\"%s\",\"test_name\":\"%s\",\"result\":\"%s\",\"diagnosis\":\"%s\",\"report_date\":\"%s\"}",
                                escape(rs.getString("patient_name")),
                                escape(rs.getString("test_name")),
                                escape(rs.getString("result")),
                                escape(rs.getString("diagnosis")),
                                escape(rs.getString("report_date"))));
                        first = false;
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    // Query 2: Get the count of tests taken by each patient
    static class TestCountsQueryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            if (method.equalsIgnoreCase("OPTIONS")) {
                handleOptions(exchange);
                return;
            }

            if (!method.equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "application/json", "{\"message\":\"Method Not Allowed\"}");
                return;
            }

            String sql = "SELECT P.name AS patient_name, COUNT(RD.test_id) AS number_of_tests " +
                         "FROM Patients P " +
                         "JOIN Medical_Report MR ON P.patient_id = MR.patient_id " +
                         "JOIN Report_Details RD ON MR.report_id = RD.report_id " +
                         "GROUP BY P.name " +
                         "ORDER BY number_of_tests DESC";

            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append(String.format("{\"patient_name\":\"%s\",\"number_of_tests\":%d}",
                            escape(rs.getString("patient_name")),
                            rs.getInt("number_of_tests")));
                    first = false;
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }
}
