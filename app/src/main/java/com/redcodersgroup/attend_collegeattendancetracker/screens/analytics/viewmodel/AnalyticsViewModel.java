package com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.redcodersgroup.attend_collegeattendancetracker.constants.AttendanceConstants;
import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.DayOfWeekAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.OverallAnalyticsModel;
import com.redcodersgroup.attend_collegeattendancetracker.screens.analytics.model.SubjectRiskModel;
import com.redcodersgroup.attend_collegeattendancetracker.utils.AttendanceScheduleUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AnalyticsViewModel extends AndroidViewModel {

    private final MediatorLiveData<OverallAnalyticsModel> overallAnalytics = new MediatorLiveData<>();
    private final MediatorLiveData<List<DayOfWeekAnalyticsModel>> weekdayAnalytics = new MediatorLiveData<>();
    private final MediatorLiveData<List<SubjectRiskModel>> subjectRisks = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        setupMediators();
    }

    public LiveData<OverallAnalyticsModel> getOverallAnalytics() {
        return overallAnalytics;
    }

    public LiveData<List<DayOfWeekAnalyticsModel>> getWeekdayAnalytics() {
        return weekdayAnalytics;
    }

    public LiveData<List<SubjectRiskModel>> getSubjectRisks() {
        return subjectRisks;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    private void setupMediators() {
        Database db = Database.getInstance();
        LiveData<List<SubjectSchema>> subjectsLiveData = db.getSubjectDao().getSetupCompletedSubjects();
        LiveData<List<AttendanceSchema>> attendanceLiveData = db.getAttendanceDao().getAllAttendance();
        LiveData<UserSchema> userLiveData = db.getUserDao().getUserLiveData();
        LiveData<List<HolidaySchema>> holidayLiveData = db.getHolidayDao().getHolidays();

        overallAnalytics.addSource(subjectsLiveData, s -> loadAllAnalytics());
        overallAnalytics.addSource(attendanceLiveData, a -> loadAllAnalytics());
        overallAnalytics.addSource(userLiveData, u -> loadAllAnalytics());
        overallAnalytics.addSource(holidayLiveData, h -> loadAllAnalytics());

        loadAllAnalytics();
    }

    public void loadAllAnalytics() {
        isLoading.postValue(true);
        DbHelper.execute(() -> {
            try {
                Database db = Database.getInstance();
                UserSchema user = db.getUserDao().getUser();
                int userCriteria = (user != null && user.getAttendanceCriteria() > 0) ? user.getAttendanceCriteria() : 75;

                List<SubjectSchema> subjects = db.getSubjectDao().getSetupCompletedSubjectsSync();
                List<AttendanceSchema> allRecords = db.getAttendanceDao().getAllAttendanceSync();
                List<HolidaySchema> holidays = db.getHolidayDao().getHolidaysSync();
                List<TimeTableDaySchema> days = db.getTimeTableDayDao().getDays();
                List<DayScheduleSchema> schedules = db.getDayScheduleDao().getAllSchedules();

                if (subjects == null) subjects = new ArrayList<>();
                if (allRecords == null) allRecords = new ArrayList<>();
                if (holidays == null) holidays = new ArrayList<>();
                if (days == null) days = new ArrayList<>();
                if (schedules == null) schedules = new ArrayList<>();

                // 1. Subject Breakdown & Aggregates
                int totalAttended = 0;
                int totalConducted = 0;
                int totalMissed = 0;
                int totalCancelled = 0;
                int totalRemaining = 0;
                int safeSubCount = 0;
                int warningSubCount = 0;
                int atRiskSubCount = 0;
                boolean hasSemEnd = (user != null && user.getSemEndDate() > 0);

                List<SubjectRiskModel> riskList = new ArrayList<>();

                for (SubjectSchema subj : subjects) {
                    boolean isLab = subj.isLab();
                    List<AttendanceSchema> subjRecords = db.getAttendanceDao().getAttendanceBySidAndLabSync(subj.getSid(), isLab);
                    AttendanceScheduleUtil.AttendanceStats stats = AttendanceScheduleUtil.calculateSubjectAttendanceStats(
                            user, subj, isLab, subjRecords, holidays, days, schedules);
                    int subjRemaining = AttendanceScheduleUtil.calculateRemainingScheduledClasses(
                            user, subj, isLab, holidays, days, schedules);

                    int attended = stats.getAttended();
                    int missed = stats.getMissed();
                    int cancelled = stats.getCancelled();
                    int conducted = stats.getConducted();
                    int pct = stats.getPercentage();
                    int criteria = (subj.getAttendanceCriteria() > 0) ? subj.getAttendanceCriteria() : userCriteria;

                    if (subjRemaining <= 0 && !hasSemEnd) {
                        var addInfo = db.getSubjectAdditionalInfoDao().getSubjectAdditionalInfoBySid(subj.getSid());
                        if (addInfo != null) {
                            int planned = isLab ? addInfo.getTotalLabs() : addInfo.getTotalClasses();
                            if (planned > conducted) {
                                subjRemaining = planned - conducted;
                            }
                        }
                    }

                    totalAttended += attended;
                    totalConducted += conducted;
                    totalMissed += missed;
                    totalCancelled += cancelled;
                    totalRemaining += subjRemaining;

                    double threshold = criteria / 100.0;
                    SubjectRiskModel.RiskCategory category;
                    String marginText;

                    int subjFinalConducted = conducted + subjRemaining;
                    int subjMaxPct = subjFinalConducted > 0
                            ? (int) Math.round(((attended + subjRemaining) * 100.0) / subjFinalConducted)
                            : pct;

                    if (conducted == 0) {
                        category = SubjectRiskModel.RiskCategory.SAFE;
                        marginText = (hasSemEnd && subjRemaining > 0)
                                ? (subjRemaining + (subjRemaining == 1 ? " class left" : " classes left") + " in semester")
                                : "No classes recorded yet";
                    } else if (pct < criteria) {
                        category = SubjectRiskModel.RiskCategory.AT_RISK;
                        atRiskSubCount++;
                        int needed = (int) Math.ceil((threshold * conducted - attended) / (1.0 - threshold));
                        if (needed <= 0) needed = 1;
                        if (hasSemEnd && (needed > subjRemaining || subjMaxPct < criteria)) {
                            marginText = (subjRemaining > 0)
                                    ? ("Attend all " + subjRemaining + " left · Max possible: " + subjMaxPct + "%")
                                    : ("No classes left · Max possible: " + subjMaxPct + "%");
                        } else if (hasSemEnd) {
                            marginText = "Attend next " + needed + (needed == 1 ? " class" : " classes") + " · " + subjRemaining + " left (Max " + subjMaxPct + "%)";
                        } else {
                            marginText = "Attend next " + needed + (needed == 1 ? " class" : " classes") + " to hit " + criteria + "%";
                        }
                    } else if (pct <= criteria + 5) {
                        category = SubjectRiskModel.RiskCategory.WARNING;
                        warningSubCount++;
                        int safe = (int) Math.floor((attended - threshold * conducted) / threshold);
                        if (hasSemEnd && subjRemaining > 0 && safe > subjRemaining) safe = subjRemaining;
                        marginText = (safe > 0) ? ("Tight margin: Can skip " + safe + (safe == 1 ? " class" : " classes")) : "On track (at criteria threshold)";
                    } else {
                        category = SubjectRiskModel.RiskCategory.SAFE;
                        safeSubCount++;
                        int safe = (int) Math.floor((attended - threshold * conducted) / threshold);
                        if (hasSemEnd && subjRemaining > 0 && safe > subjRemaining) safe = subjRemaining;
                        marginText = (safe > 0) ? ("Can safely skip " + safe + (safe == 1 ? " class" : " classes")) : "Well on track (" + pct + "%)";
                    }

                    riskList.add(new SubjectRiskModel(
                            subj, isLab, attended, conducted, pct, criteria, category, marginText
                    ));
                }

                // Sort risk list: AT_RISK first, then WARNING, then SAFE
                Collections.sort(riskList, (a, b) -> {
                    int catCmp = a.getRiskCategory().compareTo(b.getRiskCategory());
                    if (catCmp != 0) return catCmp;
                    return Integer.compare(a.getPercentage(), b.getPercentage());
                });
                subjectRisks.postValue(riskList);

                // Overall Hero Risk Summary
                AttendanceScheduleUtil.HeroRiskSummary heroSummary = AttendanceScheduleUtil.calculateHeroRiskSummary(
                        user, subjects.size(), totalAttended, totalConducted, totalRemaining);
                int overallPct = heroSummary.getOverallPercentage();

                // 2. Streaks Calculation
                int[] streaks = calculateStreaks(allRecords);
                int currentStreak = streaks[0];
                int bestStreak = streaks[1];

                // 3. Time Windows: Last 7 Days vs Last 30 Days
                int[] last7 = calculateWindowStats(allRecords, 7, days, schedules);
                int[] last30 = calculateWindowStats(allRecords, 30, days, schedules);

                int last7Attended = last7[0];
                int last7Conducted = last7[1];
                int last7Pct = last7Conducted > 0 ? (int) Math.round((last7Attended * 100.0) / last7Conducted) : overallPct;

                int last30Attended = last30[0];
                int last30Conducted = last30[1];
                int last30Pct = last30Conducted > 0 ? (int) Math.round((last30Attended * 100.0) / last30Conducted) : overallPct;

                String velocity = "STABLE";
                if (last7Conducted > 0 && last30Conducted > 0) {
                    if (last7Pct > last30Pct + 2) {
                        velocity = "UP";
                    } else if (last7Pct < last30Pct - 2) {
                        velocity = "DOWN";
                    }
                }

                OverallAnalyticsModel overallModel = new OverallAnalyticsModel(
                        overallPct,
                        totalAttended,
                        totalConducted,
                        totalMissed,
                        totalCancelled,
                        userCriteria,
                        heroSummary.getBunkSafeCount(),
                        heroSummary.getClassesNeeded(),
                        currentStreak,
                        bestStreak,
                        last7Attended,
                        last7Conducted,
                        last7Pct,
                        last30Attended,
                        last30Conducted,
                        last30Pct,
                        velocity,
                        safeSubCount,
                        warningSubCount,
                        atRiskSubCount,
                        heroSummary.getTotalRemaining(),
                        heroSummary.getMaxPossiblePercentage(),
                        heroSummary.isTargetUnreachable(),
                        heroSummary.getStatusChipText(),
                        heroSummary.getHeadlineText(),
                        heroSummary.getSubheadlineText()
                );
                overallAnalytics.postValue(overallModel);

                // 4. Weekday Distribution
                List<DayOfWeekAnalyticsModel> weekdayList = calculateWeekdayDistribution(allRecords, days, schedules);
                weekdayAnalytics.postValue(weekdayList);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    private int[] calculateWindowStats(
            List<AttendanceSchema> records,
            int daysBack,
            List<TimeTableDaySchema> days,
            List<DayScheduleSchema> schedules
    ) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endOfToday = cal.getTimeInMillis();

        // Beginning of cutoff window (e.g. 7 days back = 6 days before today at 00:00:00)
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, -(daysBack - 1));
        long cutoffTime = cal.getTimeInMillis();

        int attended = 0;
        int conducted = 0;

        for (AttendanceSchema r : records) {
            if (r == null || r.getDate() == null) continue;
            long dateMs = DateUtil.getDateMs(r.getDate());
            if (dateMs >= cutoffTime && dateMs <= endOfToday) {
                int periods = AttendanceScheduleUtil.getScheduledPeriodsOnDate(r.getDate(), r.getSid(), days, schedules);
                if (AttendanceConstants.isPresent(r.getStatus())) {
                    attended += periods;
                    conducted += periods;
                } else if (AttendanceConstants.isAbsent(r.getStatus())) {
                    conducted += periods;
                }
            }
        }

        return new int[]{attended, conducted};
    }

    private int[] calculateStreaks(List<AttendanceSchema> records) {
        if (records == null || records.isEmpty()) return new int[]{0, 0};

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        // Map date -> whether all recorded subjects on that date were attended
        Map<String, Boolean> dateHasAbsence = new HashMap<>();
        Map<String, Boolean> dateHasPresent = new HashMap<>();
        Set<Date> validDates = new HashSet<>();

        for (AttendanceSchema r : records) {
            if (r == null || r.getDate() == null) continue;
            long ms = DateUtil.getDateMs(r.getDate());
            if (ms > 0) {
                Date d = new Date(ms);
                validDates.add(d);
                String dateKey = r.getDate();
                if (AttendanceConstants.isAbsent(r.getStatus())) {
                    dateHasAbsence.put(dateKey, true);
                }
                if (AttendanceConstants.isPresent(r.getStatus())) {
                    dateHasPresent.put(dateKey, true);
                }
            }
        }

        List<Date> sortedDates = new ArrayList<>(validDates);
        Collections.sort(sortedDates);

        int bestStreak = 0;
        int runningStreak = 0;

        for (Date d : sortedDates) {
            String key = sdf.format(d);
            boolean present = Boolean.TRUE.equals(dateHasPresent.get(key));
            boolean absent = Boolean.TRUE.equals(dateHasAbsence.get(key));

            if (present && !absent) {
                runningStreak++;
                if (runningStreak > bestStreak) {
                    bestStreak = runningStreak;
                }
            } else if (absent) {
                runningStreak = 0;
            }
        }

        // Current streak backwards from latest date
        int currentStreak = 0;
        for (int i = sortedDates.size() - 1; i >= 0; i--) {
            Date d = sortedDates.get(i);
            String key = sdf.format(d);
            boolean present = Boolean.TRUE.equals(dateHasPresent.get(key));
            boolean absent = Boolean.TRUE.equals(dateHasAbsence.get(key));

            if (present && !absent) {
                currentStreak++;
            } else {
                break;
            }
        }

        return new int[]{currentStreak, bestStreak};
    }

    private List<DayOfWeekAnalyticsModel> calculateWeekdayDistribution(
            List<AttendanceSchema> records,
            List<TimeTableDaySchema> configuredDays,
            List<DayScheduleSchema> schedules
    ) {
        // Collect active working day names from configured timetable days
        Set<String> activeDayNames = new HashSet<>();
        if (configuredDays != null && !configuredDays.isEmpty()) {
            for (TimeTableDaySchema d : configuredDays) {
                if (d.getDayName() != null) {
                    activeDayNames.add(d.getDayName().trim().toLowerCase(Locale.US));
                }
            }
        }

        String[] allLabels = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        String[] allFullNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        int[] allDayConstants = {Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};

        // Strictly use 5 weekdays: Mon-Fri
        List<Integer> activeIndices = new ArrayList<>();
        for (int i = 0; i < allFullNames.length; i++) {
            activeIndices.add(i);
        }

        int numActive = activeIndices.size();
        int[] attendedPerDay = new int[numActive];
        int[] conductedPerDay = new int[numActive];

        Calendar cal = Calendar.getInstance();

        if (records != null) {
            for (AttendanceSchema r : records) {
                if (r == null || r.getDate() == null) continue;
                long ms = DateUtil.getDateMs(r.getDate());
                if (ms > 0) {
                    cal.setTimeInMillis(ms);
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    for (int j = 0; j < numActive; j++) {
                        int originalIdx = activeIndices.get(j);
                        if (allDayConstants[originalIdx] == dow) {
                            int periods = AttendanceScheduleUtil.getScheduledPeriodsOnDate(r.getDate(), r.getSid(), configuredDays, schedules);
                            if (AttendanceConstants.isPresent(r.getStatus())) {
                                attendedPerDay[j] += periods;
                                conductedPerDay[j] += periods;
                            } else if (AttendanceConstants.isAbsent(r.getStatus())) {
                                conductedPerDay[j] += periods;
                            }
                            break;
                        }
                    }
                }
            }
        }

        // Find best and worst among active working days with conducted classes
        int maxPct = -1;
        int minPct = 101;
        int bestIdx = -1;
        int worstIdx = -1;

        int[] pcts = new int[numActive];
        for (int j = 0; j < numActive; j++) {
            if (conductedPerDay[j] > 0) {
                pcts[j] = (int) Math.round((attendedPerDay[j] * 100.0) / conductedPerDay[j]);
                if (pcts[j] > maxPct) {
                    maxPct = pcts[j];
                    bestIdx = j;
                }
                if (pcts[j] < minPct) {
                    minPct = pcts[j];
                    worstIdx = j;
                }
            } else {
                pcts[j] = 0;
            }
        }

        List<DayOfWeekAnalyticsModel> result = new ArrayList<>();
        for (int j = 0; j < numActive; j++) {
            int originalIdx = activeIndices.get(j);
            result.add(new DayOfWeekAnalyticsModel(
                    allLabels[originalIdx],
                    allFullNames[originalIdx],
                    attendedPerDay[j],
                    conductedPerDay[j],
                    pcts[j],
                    (j == bestIdx && conductedPerDay[j] > 0),
                    (j == worstIdx && conductedPerDay[j] > 0 && pcts[j] < 75)
            ));
        }

        return result;
    }
}
