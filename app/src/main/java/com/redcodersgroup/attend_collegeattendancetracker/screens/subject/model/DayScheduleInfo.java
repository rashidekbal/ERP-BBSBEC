package com.redcodersgroup.attend_collegeattendancetracker.screens.subject.model;

import java.util.ArrayList;
import java.util.List;

public class DayScheduleInfo {
    private final String dayName;
    private final String dateFormatted;
    private final boolean isToday;
    private final boolean isHoliday;
    private final String holidayName;
    private final boolean isWeekend;
    private final boolean isLab;
    private final boolean hasClass;
    private final List<Integer> periodNumbers;
    private final List<String> periodTimeRanges;
    private final String statusTitle;
    private final String statusDetail;

    public DayScheduleInfo(String dayName, String dateFormatted, boolean isToday,
                           boolean isHoliday, String holidayName, boolean isWeekend,
                           boolean isLab, boolean hasClass, List<Integer> periodNumbers,
                           List<String> periodTimeRanges, String statusTitle, String statusDetail) {
        this.dayName = dayName;
        this.dateFormatted = dateFormatted;
        this.isToday = isToday;
        this.isHoliday = isHoliday;
        this.holidayName = holidayName != null ? holidayName : "";
        this.isWeekend = isWeekend;
        this.isLab = isLab;
        this.hasClass = hasClass;
        this.periodNumbers = periodNumbers != null ? periodNumbers : new ArrayList<>();
        this.periodTimeRanges = periodTimeRanges != null ? periodTimeRanges : new ArrayList<>();
        this.statusTitle = statusTitle;
        this.statusDetail = statusDetail;
    }

    public String getDayName() {
        return dayName;
    }

    public String getDateFormatted() {
        return dateFormatted;
    }

    public boolean isToday() {
        return isToday;
    }

    public boolean isHoliday() {
        return isHoliday;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public boolean isWeekend() {
        return isWeekend;
    }

    public boolean isLab() {
        return isLab;
    }

    public boolean isHasClass() {
        return hasClass;
    }

    public List<Integer> getPeriodNumbers() {
        return periodNumbers;
    }

    public List<String> getPeriodTimeRanges() {
        return periodTimeRanges;
    }

    public String getStatusTitle() {
        return statusTitle;
    }

    public String getStatusDetail() {
        return statusDetail;
    }
}
