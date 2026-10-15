package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.model;

public class MarksSummaryModel {
    private final String mid1Avg;
    private final String mid2Avg;
    private final String mid3Avg;
    private final String trendText;
    private final int maxMarks;

    public MarksSummaryModel(String mid1Avg, String mid2Avg, String mid3Avg, String trendText, int maxMarks) {
        this.mid1Avg = mid1Avg;
        this.mid2Avg = mid2Avg;
        this.mid3Avg = mid3Avg;
        this.trendText = trendText;
        this.maxMarks = maxMarks <= 0 ? 24 : maxMarks;
    }

    public String getMid1Avg() {
        return mid1Avg;
    }

    public String getMid2Avg() {
        return mid2Avg;
    }

    public String getMid3Avg() {
        return mid3Avg;
    }

    public String getTrendText() {
        return trendText;
    }

    public int getMaxMarks() {
        return maxMarks;
    }
}
