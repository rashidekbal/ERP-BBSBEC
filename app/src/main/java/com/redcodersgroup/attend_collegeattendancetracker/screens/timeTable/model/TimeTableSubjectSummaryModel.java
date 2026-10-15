package com.redcodersgroup.attend_collegeattendancetracker.screens.timeTable.model;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

public class TimeTableSubjectSummaryModel {
    private final SubjectSchema subject;
    private final int scheduledSlotsCount;

    public TimeTableSubjectSummaryModel(SubjectSchema subject, int scheduledSlotsCount) {
        this.subject = subject;
        this.scheduledSlotsCount = scheduledSlotsCount;
    }

    public SubjectSchema getSubject() {
        return subject;
    }

    public int getScheduledSlotsCount() {
        return scheduledSlotsCount;
    }
}
