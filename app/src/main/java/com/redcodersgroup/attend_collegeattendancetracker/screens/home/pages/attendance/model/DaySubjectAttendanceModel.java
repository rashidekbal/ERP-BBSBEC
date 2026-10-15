package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

public class DaySubjectAttendanceModel {
    private final int sid;
    private final String subjectName;
    private final boolean isLab;
    private final String colorCode;
    private String status; // "PRESENT", "ABSENT", "CANCELLED", or null
    private final boolean isScheduled;
    private final SubjectSchema subject;

    public DaySubjectAttendanceModel(int sid, String subjectName, boolean isLab, String colorCode,
                                     String status, boolean isScheduled, SubjectSchema subject) {
        this.sid = sid;
        this.subjectName = subjectName;
        this.isLab = isLab;
        this.colorCode = colorCode;
        this.status = status;
        this.isScheduled = isScheduled;
        this.subject = subject;
    }

    public int getSid() {
        return sid;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public boolean isLab() {
        return isLab;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isScheduled() {
        return isScheduled;
    }

    public SubjectSchema getSubject() {
        return subject;
    }

    public String getDisplayName() {
        if (subjectName == null) return "";
        if (isLab && !subjectName.toLowerCase().contains("lab")) {
            return subjectName + " Lab";
        }
        return subjectName;
    }
}
