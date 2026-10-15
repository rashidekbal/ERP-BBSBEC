package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.model;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

public class TimeTablePeriodModel {
    private final int periodNumber;
    private final int did;
    private final String dayName;
    private final String timeRange;
    private final Integer sid;
    private final SubjectSchema subject;

    public TimeTablePeriodModel(int periodNumber, int did, String dayName, String timeRange, Integer sid, SubjectSchema subject) {
        this.periodNumber = periodNumber;
        this.did = did;
        this.dayName = dayName;
        this.timeRange = timeRange;
        this.sid = sid;
        this.subject = subject;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public int getDid() {
        return did;
    }

    public String getDayName() {
        return dayName;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public Integer getSid() {
        return sid;
    }

    public SubjectSchema getSubject() {
        return subject;
    }

    public boolean isAssigned() {
        return sid != null && subject != null;
    }
}
