package model;

public class Test {
    private int testId;
    private String testName;

    public Test(String testName) {
        this.testName = testName;
    }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }
    public String getTestName() { return testName; }
}
