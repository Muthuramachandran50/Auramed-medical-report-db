package model;

public class ReportDetail {
    private int detailId;
    private int reportId;
    private int testId;
    private String result;
    private String diagnosis;

    public ReportDetail(int detailId, int reportId, int testId, String result, String diagnosis) {
        this.detailId = detailId;
        this.reportId = reportId;
        this.testId = testId;
        this.result = result;
        this.diagnosis = diagnosis;
    }

    public ReportDetail(int reportId, int testId, String result, String diagnosis) {
        this.reportId = reportId;
        this.testId = testId;
        this.result = result;
        this.diagnosis = diagnosis;
    }

    // Getters and Setters

    public int getDetailId() {
        return detailId;
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getTestId() {
        return testId;
    }

    public void setTestId(int testId) {
        this.testId = testId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }
}
