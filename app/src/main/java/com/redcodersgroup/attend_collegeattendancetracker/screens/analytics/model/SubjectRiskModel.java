package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model;

import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

public class SubjectRiskModel {
    public enum RiskCategory {
        SAFE,       // Percentage > Criteria + 5%
        WARNING,    // Criteria <= Percentage <= Criteria + 5%
        AT_RISK     // Percentage < Criteria
    }

    private final SubjectSchema subject;
    private final boolean isLab;
    private final int attended;
    private final int conducted;
    private final int percentage;
    private final int criteria;
    private final RiskCategory riskCategory;
    private final String marginText; // e.g. "Safe to bunk 3" or "Attend next 2"

    public SubjectRiskModel(
            SubjectSchema subject,
            boolean isLab,
            int attended,
            int conducted,
            int percentage,
            int criteria,
            RiskCategory riskCategory,
            String marginText
    ) {
        this.subject = subject;
        this.isLab = isLab;
        this.attended = attended;
        this.conducted = conducted;
        this.percentage = percentage;
        this.criteria = criteria;
        this.riskCategory = riskCategory;
        this.marginText = marginText;
    }

    public SubjectSchema getSubject() {
        return subject;
    }

    public boolean isLab() {
        return isLab;
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

    public int getCriteria() {
        return criteria;
    }

    public RiskCategory getRiskCategory() {
        return riskCategory;
    }

    public String getMarginText() {
        return marginText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubjectRiskModel that = (SubjectRiskModel) o;
        if (isLab != that.isLab) return false;
        if (attended != that.attended) return false;
        if (conducted != that.conducted) return false;
        if (percentage != that.percentage) return false;
        if (criteria != that.criteria) return false;
        if (riskCategory != that.riskCategory) return false;
        if (subject != null ? (that.subject == null || subject.getSid() != that.subject.getSid()) : that.subject != null) return false;
        return marginText != null ? marginText.equals(that.marginText) : that.marginText == null;
    }

    @Override
    public int hashCode() {
        int result = subject != null ? subject.getSid() : 0;
        result = 31 * result + (isLab ? 1 : 0);
        result = 31 * result + attended;
        result = 31 * result + conducted;
        result = 31 * result + percentage;
        result = 31 * result + criteria;
        result = 31 * result + (riskCategory != null ? riskCategory.hashCode() : 0);
        result = 31 * result + (marginText != null ? marginText.hashCode() : 0);
        return result;
    }
}
