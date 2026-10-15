package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model;

public class OverallAnalyticsModel {
    private final int overallPercentage;
    private final int totalAttended;
    private final int totalConducted;
    private final int totalMissed;
    private final int totalCancelled;
    private final int targetCriteria;
    private final int bunkSafeCount;
    private final int classesNeeded;
    private final int currentStreak;
    private final int bestStreak;
    private final int last7DaysAttended;
    private final int last7DaysConducted;
    private final int last7DaysPct;
    private final int last30DaysAttended;
    private final int last30DaysConducted;
    private final int last30DaysPct;
    private final String velocity; // "UP", "DOWN", "STABLE"
    private final int safeSubjectCount;
    private final int warningSubjectCount;
    private final int atRiskSubjectCount;
    private final int totalRemaining;
    private final int maxPossiblePercentage;
    private final boolean targetUnreachable;
    private final String statusChipText;
    private final String headlineText;
    private final String subheadlineText;

    public OverallAnalyticsModel(
            int overallPercentage,
            int totalAttended,
            int totalConducted,
            int totalMissed,
            int totalCancelled,
            int targetCriteria,
            int bunkSafeCount,
            int classesNeeded,
            int currentStreak,
            int bestStreak,
            int last7DaysAttended,
            int last7DaysConducted,
            int last7DaysPct,
            int last30DaysAttended,
            int last30DaysConducted,
            int last30DaysPct,
            String velocity,
            int safeSubjectCount,
            int warningSubjectCount,
            int atRiskSubjectCount,
            int totalRemaining,
            int maxPossiblePercentage,
            boolean targetUnreachable,
            String statusChipText,
            String headlineText,
            String subheadlineText
    ) {
        this.overallPercentage = overallPercentage;
        this.totalAttended = totalAttended;
        this.totalConducted = totalConducted;
        this.totalMissed = totalMissed;
        this.totalCancelled = totalCancelled;
        this.targetCriteria = targetCriteria;
        this.bunkSafeCount = bunkSafeCount;
        this.classesNeeded = classesNeeded;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.last7DaysAttended = last7DaysAttended;
        this.last7DaysConducted = last7DaysConducted;
        this.last7DaysPct = last7DaysPct;
        this.last30DaysAttended = last30DaysAttended;
        this.last30DaysConducted = last30DaysConducted;
        this.last30DaysPct = last30DaysPct;
        this.velocity = velocity;
        this.safeSubjectCount = safeSubjectCount;
        this.warningSubjectCount = warningSubjectCount;
        this.atRiskSubjectCount = atRiskSubjectCount;
        this.totalRemaining = totalRemaining;
        this.maxPossiblePercentage = maxPossiblePercentage;
        this.targetUnreachable = targetUnreachable;
        this.statusChipText = statusChipText;
        this.headlineText = headlineText;
        this.subheadlineText = subheadlineText;
    }

    public int getOverallPercentage() {
        return overallPercentage;
    }

    public int getTotalAttended() {
        return totalAttended;
    }

    public int getTotalConducted() {
        return totalConducted;
    }

    public int getTotalMissed() {
        return totalMissed;
    }

    public int getTotalCancelled() {
        return totalCancelled;
    }

    public int getTargetCriteria() {
        return targetCriteria;
    }

    public int getBunkSafeCount() {
        return bunkSafeCount;
    }

    public int getClassesNeeded() {
        return classesNeeded;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public int getLast7DaysAttended() {
        return last7DaysAttended;
    }

    public int getLast7DaysConducted() {
        return last7DaysConducted;
    }

    public int getLast7DaysPct() {
        return last7DaysPct;
    }

    public int getLast30DaysAttended() {
        return last30DaysAttended;
    }

    public int getLast30DaysConducted() {
        return last30DaysConducted;
    }

    public int getLast30DaysPct() {
        return last30DaysPct;
    }

    public String getVelocity() {
        return velocity;
    }

    public int getSafeSubjectCount() {
        return safeSubjectCount;
    }

    public int getWarningSubjectCount() {
        return warningSubjectCount;
    }

    public int getAtRiskSubjectCount() {
        return atRiskSubjectCount;
    }

    public int getTotalRemaining() {
        return totalRemaining;
    }

    public int getMaxPossiblePercentage() {
        return maxPossiblePercentage;
    }

    public boolean isTargetUnreachable() {
        return targetUnreachable;
    }

    public String getStatusChipText() {
        return statusChipText;
    }

    public String getHeadlineText() {
        return headlineText;
    }

    public String getSubheadlineText() {
        return subheadlineText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverallAnalyticsModel that = (OverallAnalyticsModel) o;
        return overallPercentage == that.overallPercentage &&
                totalAttended == that.totalAttended &&
                totalConducted == that.totalConducted &&
                totalMissed == that.totalMissed &&
                totalCancelled == that.totalCancelled &&
                targetCriteria == that.targetCriteria &&
                bunkSafeCount == that.bunkSafeCount &&
                classesNeeded == that.classesNeeded &&
                currentStreak == that.currentStreak &&
                bestStreak == that.bestStreak &&
                last7DaysAttended == that.last7DaysAttended &&
                last7DaysConducted == that.last7DaysConducted &&
                last7DaysPct == that.last7DaysPct &&
                last30DaysAttended == that.last30DaysAttended &&
                last30DaysConducted == that.last30DaysConducted &&
                last30DaysPct == that.last30DaysPct &&
                safeSubjectCount == that.safeSubjectCount &&
                warningSubjectCount == that.warningSubjectCount &&
                atRiskSubjectCount == that.atRiskSubjectCount &&
                totalRemaining == that.totalRemaining &&
                maxPossiblePercentage == that.maxPossiblePercentage &&
                targetUnreachable == that.targetUnreachable &&
                (statusChipText != null ? statusChipText.equals(that.statusChipText) : that.statusChipText == null) &&
                (headlineText != null ? headlineText.equals(that.headlineText) : that.headlineText == null) &&
                (subheadlineText != null ? subheadlineText.equals(that.subheadlineText) : that.subheadlineText == null) &&
                (velocity != null ? velocity.equals(that.velocity) : that.velocity == null);
    }

    @Override
    public int hashCode() {
        int result = overallPercentage;
        result = 31 * result + totalAttended;
        result = 31 * result + totalConducted;
        result = 31 * result + totalMissed;
        result = 31 * result + totalCancelled;
        result = 31 * result + targetCriteria;
        result = 31 * result + bunkSafeCount;
        result = 31 * result + classesNeeded;
        result = 31 * result + currentStreak;
        result = 31 * result + bestStreak;
        result = 31 * result + last7DaysPct;
        result = 31 * result + last30DaysPct;
        result = 31 * result + totalRemaining;
        result = 31 * result + maxPossiblePercentage;
        return result;
    }
}
