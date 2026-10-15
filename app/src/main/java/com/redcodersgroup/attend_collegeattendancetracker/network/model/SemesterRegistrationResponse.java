package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class SemesterRegistrationResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<RegistrationItem> data = new ArrayList<>();

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public List<RegistrationItem> getData() { return data != null ? data : new ArrayList<>(); }

    public static class RegistrationItem {
        @SerializedName("id")
        private int id;

        @SerializedName("session")
        private SessionItem session;

        @SerializedName("semester")
        private int semester;

        @SerializedName("status")
        private String status;

        @SerializedName("applied_date")
        private String appliedDate;

        @SerializedName("remarks")
        private String remarks;

        @SerializedName("selected_subjects")
        private List<RegisteredSubject> selectedSubjects = new ArrayList<>();

        public int getId() { return id; }
        public SessionItem getSession() { return session; }
        public int getSemester() { return semester; }
        public String getStatus() { return status != null ? status : "APPROVED"; }
        public String getAppliedDate() { return appliedDate != null ? appliedDate : ""; }
        public String getRemarks() { return remarks != null ? remarks : ""; }
        public List<RegisteredSubject> getSelectedSubjects() { return selectedSubjects != null ? selectedSubjects : new ArrayList<>(); }
    }

    public static class SessionItem {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("session")
        private String session;

        public int getId() { return id; }
        public String getName() {
            if (name != null && !name.isEmpty()) return name;
            if (session != null && !session.isEmpty()) return session;
            return "Current Session";
        }
    }

    public static class RegisteredSubject {
        @SerializedName("id")
        private int id;

        @SerializedName(value = "subject_code", alternate = {"code"})
        private String subjectCode;

        @SerializedName(value = "subject_name", alternate = {"course_title", "title", "name"})
        private String subjectName;

        @SerializedName("credits")
        private int credits;

        public RegisteredSubject() {}

        public RegisteredSubject(int id, String subjectCode, String subjectName, int credits) {
            this.id = id;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.credits = credits;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getSubjectCode() {
            if (subjectCode != null && !subjectCode.trim().isEmpty() && !subjectCode.equalsIgnoreCase("SUB")) {
                return subjectCode;
            }
            if (subjectName != null && subjectName.contains("(") && subjectName.contains(")")) {
                try {
                    int start = subjectName.indexOf("(") + 1;
                    int end = subjectName.indexOf(")");
                    if (end > start) {
                        return subjectName.substring(start, end).trim();
                    }
                } catch (Exception ignored) {}
            }
            return "COURSE";
        }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

        public String getSubjectName() {
            if (subjectName != null && !subjectName.isEmpty()) return subjectName;
            return "Subject #" + id;
        }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

        public int getCredits() { return credits > 0 ? credits : 3; }
        public void setCredits(int credits) { this.credits = credits; }
    }
}
