package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.home.model;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

public class TodayClassModel {
    private int periodNumber;
    private String timeRange;
    private int sid;
    private boolean isLab;
    private String subjectName;
    private String colorCode;
    private String status; // "PRESENT", "ABSENT", "CANCELLED", or null
    private SubjectSchema subject;

    public TodayClassModel(int periodNumber, String timeRange, int sid, boolean isLab, String subjectName, String colorCode, String status, SubjectSchema subject) {
        this.periodNumber = periodNumber;
        this.timeRange = timeRange;
        this.sid = sid;
        this.isLab = isLab;
        this.subjectName = subjectName;
        this.colorCode = colorCode;
        this.status = status;
        this.subject = subject;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(int periodNumber) {
        this.periodNumber = periodNumber;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SubjectSchema getSubject() {
        return subject;
    }

    public void setSubject(SubjectSchema subject) {
        this.subject = subject;
    }
}
