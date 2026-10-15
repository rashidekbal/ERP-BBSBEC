package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.attendance.model;

public class CalendarDayModel {
    private int dayNumber;
    private int month;
    private int year;
    private long dateMs;
    private String dateString;
    private boolean isCurrentMonth;
    private boolean isToday;
    private boolean isHoliday;
    private String holidayName;
    private boolean isWeekend;
    private boolean isFuture;
    private boolean hasAttendance;
    private int presentCount;
    private int absentCount;
    private int cancelledCount;

    public CalendarDayModel() {
    }

    public CalendarDayModel(int dayNumber, int month, int year, long dateMs, String dateString,
                            boolean isCurrentMonth, boolean isToday, boolean isHoliday,
                            String holidayName, boolean isWeekend, boolean hasAttendance,
                            int presentCount, int absentCount) {
        this(dayNumber, month, year, dateMs, dateString, isCurrentMonth, isToday, isHoliday, holidayName, isWeekend, hasAttendance, presentCount, absentCount, 0);
    }

    public CalendarDayModel(int dayNumber, int month, int year, long dateMs, String dateString,
                            boolean isCurrentMonth, boolean isToday, boolean isHoliday,
                            String holidayName, boolean isWeekend, boolean hasAttendance,
                            int presentCount, int absentCount, int cancelledCount) {
        this.dayNumber = dayNumber;
        this.month = month;
        this.year = year;
        this.dateMs = dateMs;
        this.dateString = dateString;
        this.isCurrentMonth = isCurrentMonth;
        this.isToday = isToday;
        this.isHoliday = isHoliday;
        this.holidayName = holidayName;
        this.isWeekend = isWeekend;
        this.hasAttendance = hasAttendance;
        this.presentCount = presentCount;
        this.absentCount = absentCount;
        this.cancelledCount = cancelledCount;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getDateMs() {
        return dateMs;
    }

    public void setDateMs(long dateMs) {
        this.dateMs = dateMs;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public boolean isCurrentMonth() {
        return isCurrentMonth;
    }

    public void setCurrentMonth(boolean currentMonth) {
        isCurrentMonth = currentMonth;
    }

    public boolean isToday() {
        return isToday;
    }

    public void setToday(boolean isToday) {
        this.isToday = isToday;
    }

    public boolean isHoliday() {
        return isHoliday;
    }

    public void setHoliday(boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }

    public boolean isWeekend() {
        return isWeekend;
    }

    public void setWeekend(boolean isWeekend) {
        this.isWeekend = isWeekend;
    }

    public boolean hasAttendance() {
        return hasAttendance;
    }

    public void setHasAttendance(boolean hasAttendance) {
        this.hasAttendance = hasAttendance;
    }

    public int getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(int presentCount) {
        this.presentCount = presentCount;
    }

    public int getAbsentCount() {
        return absentCount;
    }

    public void setAbsentCount(int absentCount) {
        this.absentCount = absentCount;
    }

    public boolean isFuture() {
        return isFuture;
    }

    public void setFuture(boolean future) {
        isFuture = future;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(int cancelledCount) {
        this.cancelledCount = cancelledCount;
    }
}
