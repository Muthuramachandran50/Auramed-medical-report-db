package model;

public class Patient {
    private int patientId;
    private String name;
    private String gender;
    private String dob;
    private String phone;
    private String email;

    public Patient(int patientId, String name, String gender, String dob, String phone, String email) {
        this.patientId = patientId;
        this.name = name;
        this.gender = gender;
        this.dob = dob;
        this.phone = phone;
        this.email = email;
    }

    public int getPatientId() { return patientId; }
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getDob() { return dob; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
}
