package com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.home.pages.marks.model.MarksSummaryModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarksViewModel extends AndroidViewModel {
    private final MediatorLiveData<MarksSummaryModel> marksSummary = new MediatorLiveData<>();
    private final LiveData<List<MarksSchema>> marksLiveData;

    public MarksViewModel(@NonNull Application application) {
        super(application);
        DbHelper.execute(() -> Database.getInstance().getMarksDao().deleteLabMarks());
        marksLiveData = Database.getInstance().getMarksDao().getMarks();
        marksSummary.addSource(marksLiveData, this::calculateSummary);
    }

    public LiveData<List<MarksSchema>> getMarks() {
        return marksLiveData;
    }

    public LiveData<MarksSummaryModel> getMarksSummary() {
        return marksSummary;
    }

    private void calculateSummary(List<MarksSchema> marksList) {
        DbHelper.execute(() -> {
            UserSchema user = Database.getInstance().getUserDao().getUser();
            int maxMarks = (user != null && user.getMaxMarks() > 0) ? user.getMaxMarks() : 24;

            if (marksList == null || marksList.isEmpty()) {
                marksSummary.postValue(new MarksSummaryModel("----", "----", "----",
                        "No marks data available yet. Tap '+ Add marks' to record scores and see your performance trends.", maxMarks));
                return;
            }

            double sum1 = 0, sum2 = 0, sum3 = 0;
            int count1 = 0, count2 = 0, count3 = 0;

            for (MarksSchema mark : marksList) {
                if (mark.getMid1() >= 0) {
                    sum1 += mark.getMid1();
                    count1++;
                }
                if (mark.getMid2() >= 0) {
                    sum2 += mark.getMid2();
                    count2++;
                }
                if (mark.getMid3() >= 0) {
                    sum3 += mark.getMid3();
                    count3++;
                }
            }

            String mid1Avg = count1 > 0 ? formatAvg((sum1 / (count1 * maxMarks)) * 100.0) : "----";
            String mid2Avg = count2 > 0 ? formatAvg((sum2 / (count2 * maxMarks)) * 100.0) : "----";
            String mid3Avg = count3 > 0 ? formatAvg((sum3 / (count3 * maxMarks)) * 100.0) : "----";

            String trend = generateTrendAnalysis(marksList, maxMarks);

            marksSummary.postValue(new MarksSummaryModel(mid1Avg, mid2Avg, mid3Avg, trend, maxMarks));
        });
    }

    private String formatAvg(double avg) {
        if (avg == (long) avg) {
            return String.format(Locale.US, "%d%%", (long) avg);
        } else {
            return String.format(Locale.US, "%.1f%%", avg);
        }
    }

    private String generateTrendAnalysis(List<MarksSchema> marksList, int maxMarks) {
        List<MarksSchema> validMarks = new ArrayList<>();
        for (MarksSchema m : marksList) {
            if (m.getMid1() >= 0 || m.getMid2() >= 0 || m.getMid3() >= 0) {
                validMarks.add(m);
            }
        }

        if (validMarks.isEmpty()) {
            return "No marks data available yet. Tap '+ Add marks' to record scores and see your performance trends.";
        }

        // Check availability of consecutive comparisons
        int count1And2 = 0;
        int count2And3 = 0;
        int count1And3 = 0;

        for (MarksSchema m : validMarks) {
            if (m.getMid1() >= 0 && m.getMid2() >= 0) count1And2++;
            if (m.getMid2() >= 0 && m.getMid3() >= 0) count2And3++;
            if (m.getMid1() >= 0 && m.getMid3() >= 0) count1And3++;
        }

        String fromExam = null;
        String toExam = null;

        if (count2And3 > 0) {
            fromExam = "Mid 2";
            toExam = "Mid 3";
        } else if (count1And2 > 0) {
            fromExam = "Mid 1";
            toExam = "Mid 2";
        } else if (count1And3 > 0) {
            fromExam = "Mid 1";
            toExam = "Mid 3";
        }

        if (fromExam != null && toExam != null) {
            MarksSchema mostImproved = null;
            double maxGainPct = -Double.MAX_VALUE;

            MarksSchema mostDeclined = null;
            double maxDropPct = 0;

            MarksSchema lowestSubject = null;
            double lowestScore = Double.MAX_VALUE;

            int totalCompared = 0;
            int improvedCount = 0;

            for (MarksSchema m : validMarks) {
                double prevScore = getExamScore(m, fromExam);
                double currScore = getExamScore(m, toExam);

                if (prevScore >= 0 && currScore >= 0) {
                    totalCompared++;
                    double diff = currScore - prevScore;
                    double gainPct = (diff / maxMarks) * 100.0;

                    if (gainPct > maxGainPct) {
                        maxGainPct = gainPct;
                        mostImproved = m;
                    }
                    if (gainPct < maxDropPct) {
                        maxDropPct = gainPct;
                        mostDeclined = m;
                    }
                    if (gainPct > 0) {
                        improvedCount++;
                    }
                }

                if (currScore >= 0 && currScore < lowestScore) {
                    lowestScore = currScore;
                    lowestSubject = m;
                }
            }

            StringBuilder sb = new StringBuilder();

            if (mostImproved != null && maxGainPct > 0) {
                sb.append(mostImproved.getSubjectName())
                        .append(" improved the most between ")
                        .append(fromExam)
                        .append(" and ")
                        .append(toExam)
                        .append(" (+")
                        .append(formatScore(maxGainPct))
                        .append("%).");
            } else if (improvedCount == 0 && totalCompared > 0) {
                sb.append("Performance dipped between ")
                        .append(fromExam)
                        .append(" and ")
                        .append(toExam)
                        .append(".");
            } else {
                sb.append("Performance remained consistent between ")
                        .append(fromExam)
                        .append(" and ")
                        .append(toExam)
                        .append(".");
            }

            if (lowestSubject != null && (mostImproved == null || !lowestSubject.getSubjectName().equals(mostImproved.getSubjectName()))) {
                double lowestPct = (lowestScore / maxMarks) * 100.0;
                sb.append(" ")
                        .append(lowestSubject.getSubjectName())
                        .append(" still trails (")
                        .append(formatScore(lowestScore))
                        .append("/")
                        .append(maxMarks)
                        .append(", ")
                        .append(formatScore(lowestPct))
                        .append("%) — flagged as at-risk.");
            } else if (mostDeclined != null && maxDropPct < 0) {
                sb.append(" ")
                        .append(mostDeclined.getSubjectName())
                        .append(" dropped by ")
                        .append(formatScore(Math.abs(maxDropPct)))
                        .append("% — flagged for review.");
            }

            return sb.toString();
        }

        // Single Exam Insight
        String singleExam = "Mid 1";
        MarksSchema topSubject = null;
        double topScore = -1;
        MarksSchema lowestSubject = null;
        double lowestScore = Double.MAX_VALUE;

        for (MarksSchema m : validMarks) {
            double score = m.getMid1() >= 0 ? m.getMid1() : (m.getMid2() >= 0 ? m.getMid2() : m.getMid3());
            if (m.getMid1() < 0) {
                singleExam = m.getMid2() >= 0 ? "Mid 2" : "Mid 3";
            }
            if (score >= 0) {
                if (score > topScore) {
                    topScore = score;
                    topSubject = m;
                }
                if (score < lowestScore) {
                    lowestScore = score;
                    lowestSubject = m;
                }
            }
        }

        if (topSubject != null && lowestSubject != null && !topSubject.getSubjectName().equals(lowestSubject.getSubjectName())) {
            double topPct = (topScore / maxMarks) * 100.0;
            double lowestPct = (lowestScore / maxMarks) * 100.0;
            return topSubject.getSubjectName() + " leads with " + formatScore(topScore) + "/" + maxMarks + " (" + formatScore(topPct) + "%) in " + singleExam +
                    ". " + lowestSubject.getSubjectName() + " trails at " + formatScore(lowestScore) + "/" + maxMarks + " (" + formatScore(lowestPct) + "%) — flagged for extra focus.";
        } else if (topSubject != null) {
            double topPct = (topScore / maxMarks) * 100.0;
            return topSubject.getSubjectName() + " recorded " + formatScore(topScore) + "/" + maxMarks + " (" + formatScore(topPct) + "%) in " + singleExam +
                    ". Add more subject marks or next mid term to see dynamic comparative trends.";
        }

        return "No marks data available yet. Tap '+ Add marks' to record scores and see your performance trends.";
    }

    private double getExamScore(MarksSchema m, String exam) {
        if ("Mid 1".equals(exam)) return m.getMid1();
        if ("Mid 2".equals(exam)) return m.getMid2();
        if ("Mid 3".equals(exam)) return m.getMid3();
        return -1;
    }

    private String formatScore(double score) {
        if (score == (long) score) {
            return String.valueOf((long) score);
        } else {
            return String.format(Locale.US, "%.1f", score);
        }
    }
}
