package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class EligibleSubjectsResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("data")
    private List<EligibleSubject> data = new ArrayList<>();

    public int getStatus() { return status; }
    public List<EligibleSubject> getData() { return data != null ? data : new ArrayList<>(); }

    public static class EligibleSubject {
        @SerializedName("id")
        private int id;

        @SerializedName("subject_code")
        private String subjectCode;

        @SerializedName("subject_name")
        private String subjectName;

        @SerializedName("name")
        private String name;

        @SerializedName("subject_type")
        private String subjectType;

        @SerializedName("credits")
        private int credits;

        @SerializedName("is_elective")
        private boolean isElective;

        public int getId() { return id; }
        public String getSubjectCode() { return subjectCode != null ? subjectCode : ""; }
        public String getSubjectName() {
            if (subjectName != null && !subjectName.isEmpty()) return subjectName;
            if (name != null && !name.isEmpty()) return name;
            return "Subject #" + id;
        }
        public String getSubjectType() { return subjectType != null ? subjectType : "THEORY"; }
        public int getCredits() { return credits > 0 ? credits : 4; }
        public boolean isElective() { return isElective; }
    }
}
