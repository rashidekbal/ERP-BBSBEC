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

        @SerializedName("subject_code")
        private String subjectCode;

        @SerializedName("subject_name")
        private String subjectName;

        @SerializedName("name")
        private String name;

        @SerializedName("credits")
        private int credits;

        public int getId() { return id; }
        public String getSubjectCode() { return subjectCode != null ? subjectCode : ""; }
        public String getSubjectName() {
            if (subjectName != null && !subjectName.isEmpty()) return subjectName;
            if (name != null && !name.isEmpty()) return name;
            return "Subject #" + id;
        }
        public int getCredits() { return credits > 0 ? credits : 4; }
    }
}
