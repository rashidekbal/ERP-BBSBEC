package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model;

public class DayOfWeekAnalyticsModel {
    private final String dayLabel; // "Mon", "Tue", etc.
    private final String fullDayName; // "Monday", etc.
    private final int attended;
    private final int conducted;
    private final int percentage;
    private final boolean isBestDay;
    private final boolean isWorstDay;

    public DayOfWeekAnalyticsModel(String dayLabel, String fullDayName, int attended, int conducted, int percentage, boolean isBestDay, boolean isWorstDay) {
        this.dayLabel = dayLabel;
        this.fullDayName = fullDayName;
        this.attended = attended;
        this.conducted = conducted;
        this.percentage = percentage;
        this.isBestDay = isBestDay;
        this.isWorstDay = isWorstDay;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public String getFullDayName() {
        return fullDayName;
    }

    public int getAttended() {
        return attended;
    }

    public int getConducted() {
        return conducted;
    }

    public int getPercentage() {
        return percentage;
    }

    public boolean isBestDay() {
        return isBestDay;
    }

    public boolean isWorstDay() {
        return isWorstDay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DayOfWeekAnalyticsModel that = (DayOfWeekAnalyticsModel) o;
        return attended == that.attended &&
                conducted == that.conducted &&
                percentage == that.percentage &&
                isBestDay == that.isBestDay &&
                isWorstDay == that.isWorstDay &&
                (dayLabel != null ? dayLabel.equals(that.dayLabel) : that.dayLabel == null);
    }

    @Override
    public int hashCode() {
        int result = dayLabel != null ? dayLabel.hashCode() : 0;
        result = 31 * result + attended;
        result = 31 * result + conducted;
        result = 31 * result + percentage;
        result = 31 * result + (isBestDay ? 1 : 0);
        result = 31 * result + (isWorstDay ? 1 : 0);
        return result;
    }
}
