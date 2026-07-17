package model;

public class MedicalReport {
    private int reportId;
    private int patientId;
    private int doctorId;
    private String reportDate;

    public MedicalReport(int patientId, int doctorId, String reportDate) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.reportDate = reportDate;
    }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public int getPatientId() { return patientId; }
    public int getDoctorId() { return doctorId; }
    public String getReportDate() { return reportDate; }
}
