package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

public class SubjectAttendanceModel {
    private final SubjectSchema subject;
    private final String displayName;
    private final boolean isLab;
    private final int totalClasses;
    private final int attendedClasses;
    private final int plannedClasses;
    private final int criteria;
    private final int percentage;

    public SubjectAttendanceModel(SubjectSchema subject, String displayName, boolean isLab, int totalClasses, int attendedClasses, int plannedClasses, int criteria) {
        this.subject = subject;
        this.displayName = displayName;
        this.isLab = isLab;
        this.totalClasses = totalClasses;
        this.attendedClasses = attendedClasses;
        this.plannedClasses = plannedClasses;
        this.criteria = criteria;
        if (totalClasses > 0) {
            this.percentage = (int) Math.round(((double) attendedClasses / totalClasses) * 100);
        } else {
            this.percentage = 0;
        }
    }

    public SubjectAttendanceModel(SubjectSchema subject, String displayName, boolean isLab, int totalClasses, int attendedClasses, int criteria) {
        this(subject, displayName, isLab, totalClasses, attendedClasses, totalClasses, criteria);
    }

    public SubjectAttendanceModel(SubjectSchema subject, String displayName, boolean isLab, int totalClasses, int attendedClasses) {
        this(subject, displayName, isLab, totalClasses, attendedClasses, 75);
    }

    public SubjectAttendanceModel(SubjectSchema subject, int totalClasses, int attendedClasses) {
        this(subject, subject != null ? subject.getSubjectName() : "", false, totalClasses, attendedClasses, 75);
    }

    public SubjectSchema getSubject() {
        return subject;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isLab() {
        return isLab;
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public int getAttendedClasses() {
        return attendedClasses;
    }

    public int getCriteria() {
        return criteria;
    }

    public int getPlannedClasses() {
        return plannedClasses;
    }

    public int getPercentage() {
        return percentage;
    }
}
