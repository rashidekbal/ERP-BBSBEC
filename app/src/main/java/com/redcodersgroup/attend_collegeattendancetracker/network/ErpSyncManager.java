package com.redcodersgroup.attend_collegeattendancetracker.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.redcodersgroup.attend_collegeattendancetracker.db.Database;
import com.redcodersgroup.attend_collegeattendancetracker.db.DbHelper;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.AssessmentsResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.AttendanceResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.ProfileResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.TimetableResponse;
import com.redcodersgroup.attend_collegeattendancetracker.utils.DateUtil;
import com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

public class ErpSyncManager {

    private static final String TAG = "ErpSyncManager";

    private static final String[] SUBJECT_COLORS = {
            "#4F46E5", "#06B6D4", "#10B981", "#F59E0B",
            "#EC4899", "#8B5CF6", "#3B82F6", "#14B8A6",
            "#F43F5E", "#6366F1"
    };

    public interface SyncCallback {
        void onSuccess();
        void onError(String message);
    }

    // Cached live data from ERP for immediate UI access
    private static AttendanceResponse.Data cachedAttendanceData;
    private static TimetableResponse.Data cachedTimetableData;

    public static AttendanceResponse.Data getCachedAttendanceData() {
        return cachedAttendanceData;
    }

    public static TimetableResponse.Data getCachedTimetableData() {
        return cachedTimetableData;
    }

    public static void syncAll(SyncCallback callback) {
        DbHelper.execute(() -> {
            boolean anySuccess = false;
            StringBuilder errorLog = new StringBuilder();
            BbsbecApiService api = NetworkClient.getApiService();
            Map<Integer, String> subjectMap = new LinkedHashMap<>();
            Map<Integer, String> subjectTypeMap = new HashMap<>();

            // 1. Sync Enrolled Subjects (/student/fetchsubjects/)
            try {
                Response<com.redcodersgroup.attend_collegeattendancetracker.network.model.EnrolledSubjectsResponse> subResp = api.getFetchSubjects().execute();
                if (subResp.isSuccessful() && subResp.body() != null && subResp.body().getData() != null) {
                    syncEnrolledSubjectsToDb(subResp.body().getData());
                    for (var s : subResp.body().getData()) {
                        subjectMap.put(s.getId(), s.getSubjectName());
                        subjectTypeMap.put(s.getId(), s.getAttendanceQueryType());
                    }
                    anySuccess = true;
                    Log.d(TAG, "Enrolled subjects synced successfully: " + subjectMap.size() + " subjects");
                }
            } catch (Exception e) {
                Log.d(TAG, "Enrolled subjects error: " + e.getMessage());
            }

            // 2. Sync Profile (/student/profile/)
            try {
                Response<ProfileResponse> profileResp = api.getStudentProfile().execute();
                if (profileResp.isSuccessful() && profileResp.body() != null && profileResp.body().getData() != null) {
                    ProfileResponse.Data pData = profileResp.body().getData();
                    syncProfileToDb(pData);
                    for (int subId : pData.getSelectedSubjects()) {
                        if (!subjectMap.containsKey(subId)) {
                            subjectMap.put(subId, "Subject " + subId);
                        }
                    }
                    anySuccess = true;
                    Log.d(TAG, "Profile synced successfully: " + pData.getFullName());
                } else {
                    Log.w(TAG, "Profile sync response: code=" + profileResp.code() + ", msg=" + profileResp.message());
                    errorLog.append("Profile HTTP ").append(profileResp.code()).append("; ");
                }
            } catch (Exception e) {
                Log.e(TAG, "Profile sync error", e);
                errorLog.append("Profile error: ").append(e.getMessage()).append("; ");
            }

            // 3. Sync Timetable (/student/timetable/)
            try {
                Response<TimetableResponse> ttResp = api.getTimetable().execute();
                if (ttResp.isSuccessful() && ttResp.body() != null && ttResp.body().getData() != null) {
                    cachedTimetableData = ttResp.body().getData();
                    syncTimetableToDb(cachedTimetableData);
                    anySuccess = true;
                    Log.d(TAG, "Timetable synced successfully");
                } else {
                    Log.w(TAG, "Timetable sync response: code=" + ttResp.code() + ", msg=" + ttResp.message());
                    errorLog.append("Timetable HTTP ").append(ttResp.code()).append("; ");
                }
            } catch (Exception e) {
                Log.e(TAG, "Timetable sync error", e);
                errorLog.append("Timetable error: ").append(e.getMessage()).append("; ");
            }

            // 4. Sync Attendance (/student/class-attendance/?subject=<id>&type=<theory|lab|tutorial>)
            try {
                boolean attendanceSynced = false;
                List<AttendanceResponse.SubjectItem> accumulatedSubjects = new ArrayList<>();
                Log.d(TAG, "Querying attendance for " + subjectMap.size() + " enrolled subjects...");

                for (Map.Entry<Integer, String> entry : subjectMap.entrySet()) {
                    int subId = entry.getKey();
                    String fallbackName = entry.getValue();
                    String qType = subjectTypeMap.get(subId);
                    if (qType == null) {
                        qType = (fallbackName != null && fallbackName.toLowerCase().contains("lab")) ? "lab" : "theory";
                    }

                    List<String> typesToTry = new ArrayList<>();
                    if ("lab".equalsIgnoreCase(qType)) {
                        typesToTry.add("lab");
                        typesToTry.add("theory");
                        typesToTry.add("tutorial");
                    } else if ("tutorial".equalsIgnoreCase(qType)) {
                        typesToTry.add("tutorial");
                        typesToTry.add("theory");
                        typesToTry.add("lab");
                    } else {
                        typesToTry.add("theory");
                        typesToTry.add("lab");
                        typesToTry.add("tutorial");
                    }

                    for (String tryType : typesToTry) {
                        try {
                            Response<AttendanceResponse> subAttResp = api.getClassAttendance(subId, tryType).execute();
                            if (subAttResp.isSuccessful() && subAttResp.body() != null && subAttResp.body().getData() != null) {
                                List<AttendanceResponse.SubjectItem> items = subAttResp.body().getData().getSubjects();
                                for (AttendanceResponse.SubjectItem item : items) {
                                    if (item.getSubjectName() == null || item.getSubjectName().isEmpty()) {
                                        item.setSubjectName(fallbackName);
                                    }
                                    accumulatedSubjects.add(item);
                                }
                                syncSingleSubjectAttendanceToDb(subId, fallbackName, subAttResp.body());
                                attendanceSynced = true;
                                anySuccess = true;
                                Log.d(TAG, "Attendance for " + fallbackName + " (ID: " + subId + ", type=" + tryType + ") synced");
                                break;
                            } else if (subAttResp.code() != 400) {
                                // If not 400 (e.g. 404 or other), don't keep retrying types
                                Log.d(TAG, "Attendance for ID " + subId + " (" + fallbackName + ", type=" + tryType + ") returned: " + subAttResp.code());
                                break;
                            }
                        } catch (Exception subEx) {
                            Log.w(TAG, "Error fetching attendance for subject " + subId + ": " + subEx.getMessage());
                            break;
                        }
                    }
                }

                if (attendanceSynced) {
                    AttendanceResponse.Data accData = new AttendanceResponse.Data();
                    accData.setSubjects(accumulatedSubjects);
                    cachedAttendanceData = accData;
                }
            } catch (Exception e) {
                Log.e(TAG, "Attendance sync error", e);
                errorLog.append("Attendance error: ").append(e.getMessage()).append("; ");
            }

            // 5. Sync Assessments / Marks (/student/assessments/)
            try {
                Response<AssessmentsResponse> marksResp = api.getAssessments().execute();
                if (marksResp.isSuccessful() && marksResp.body() != null && marksResp.body().getData() != null) {
                    syncAssessmentsToDb(marksResp.body().getData(), subjectMap);
                    anySuccess = true;
                    Log.d(TAG, "Assessments synced successfully");
                } else {
                    Log.w(TAG, "Assessments sync response: code=" + marksResp.code() + ", msg=" + marksResp.message());
                    errorLog.append("Assessments HTTP ").append(marksResp.code()).append("; ");
                }
            } catch (Exception e) {
                Log.e(TAG, "Assessments sync error", e);
                errorLog.append("Assessments error: ").append(e.getMessage()).append("; ");
            }

            // 6. Sync College Session (/student/collegesession/)
            try {
                Response<com.redcodersgroup.attend_collegeattendancetracker.network.model.CollegeSessionResponse> sessResp = api.getCollegeSession().execute();
                if (sessResp.isSuccessful() && sessResp.body() != null && sessResp.body().getData() != null) {
                    syncSessionToDb(sessResp.body().getData());
                    anySuccess = true;
                    Log.d(TAG, "College session synced successfully");
                }
            } catch (Exception e) {
                Log.d(TAG, "College session endpoint not available or error: " + e.getMessage());
            }

            // Mark setup as complete
            SharedPrefUtil.setSetupComplete(true);
            SharedPrefUtil.setProfileComplete(true);

            if (anySuccess) {
                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
            } else {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(
                        errorLog.length() > 0 ? errorLog.toString() : "Sync failed"
                ));
            }
        });
    }

    private static void syncProfileToDb(ProfileResponse.Data data) {
        Database db = Database.getInstance();
        UserSchema existing = db.getUserDao().getUser();
        int uid = existing != null ? existing.getUid() : 0;
        int criteria = existing != null && existing.getAttendanceCriteria() > 0 ? existing.getAttendanceCriteria() : 75;

        String name = data.getFullName();
        String branch = data.getBranch() != null ? data.getBranch() : "";
        String course = data.getCourse() != null && !data.getCourse().isEmpty() ? data.getCourse() : branch;
        int semester = data.getSemester() > 0 ? data.getSemester() : 1;
        String roll = data.getRollNo() != null && !data.getRollNo().isEmpty() ? data.getRollNo() : TokenManager.getStudentRoll();
        String phone = data.getMobileNo() != null ? data.getMobileNo() : "";
        String email = data.getEmail() != null && !data.getEmail().isEmpty() ? data.getEmail() : TokenManager.getStudentEmail();

        if (roll != null && !roll.isEmpty()) {
            TokenManager.saveStudentRoll(roll);
        }

        UserSchema user = new UserSchema(
                uid,
                name,
                "Baba Banda Singh Bahadur Engineering College",
                course,
                branch,
                semester,
                0, // Independent of start/end dates
                0,
                5,
                0,
                criteria,
                "#4F46E5",
                24,
                roll,
                phone,
                email
        );

        user.setUniversityRollNumber(data.getUniversityRollNo());
        user.setClassGroup(data.getClassGroup());
        user.setCourse(data.getCourse());
        user.setErpStudentId(data.getId());
        user.setPhotoUrl(data.getPhoto());

        // Extended Profile Data
        user.setFatherName(data.getFatherName());
        user.setMotherName(data.getMotherName());
        user.setFatherProfession(data.getFatherProfession());
        user.setMotherProfession(data.getMotherProfession());
        user.setGender(data.getGender());
        user.setDob(data.getDob());
        user.setAadhar(data.getAadhar());
        user.setBloodGroup(data.getBloodGroup());
        user.setCategory(data.getCategory());
        user.setStatus(data.getStatus());
        user.setParentMobile(data.getParentMobile());
        user.setBatch(data.getBatch());
        user.setAdmissionType(data.getAdmissionType());
        user.setPracticalGroup(data.getPracticalGroup());
        user.setCommunicationAddress(data.getCommunicationAddress());
        user.setPermanentAddress(data.getPermanentAddress());
        user.setHighSchoolBoard(data.getHighSchoolBoard());
        user.setHighSchoolPassingYear(data.getHighSchoolPassingYear());
        user.setHighSchoolMarks(data.getHighSchoolMarks());
        user.setHighSchoolMaxMarks(data.getHighSchoolMaxMarks());
        user.setHighSchoolPercentage(data.getHighSchoolPercentage());
        user.setInterBoard(data.getInterBoard());
        user.setInterPassingYear(data.getInterPassingYear());
        user.setInterMarks(data.getInterMarks());
        user.setInterMaxMarks(data.getInterMaxMarks());
        user.setInterPercentage(data.getInterPercentage());
        user.setScholarship(data.getScholarship());

        if (existing == null) {
            db.getUserDao().addUser(user);
        } else {
            db.getUserDao().updateUser(user);
        }
    }

    private static void syncAttendanceToDb(AttendanceResponse.Data data) {
        if (data == null || data.getSubjects() == null) return;
        Database db = Database.getInstance();

        int colorIdx = 0;
        for (AttendanceResponse.SubjectItem item : data.getSubjects()) {
            String name = item.getSubjectName() != null ? item.getSubjectName().trim() : "Subject";
            SubjectSchema existing = db.getSubjectDao().getSubjectByName(name);
            int sid;

            if (existing == null) {
                String color = SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.length];
                colorIdx++;
                SubjectSchema newSubject = new SubjectSchema(
                        name,
                        false,
                        item.getTotalLectures() > 0 ? Math.min(6, item.getTotalLectures()) : 4,
                        0,
                        75,
                        color,
                        true,
                        true
                );
                db.getSubjectDao().addSubject(newSubject);
                SubjectSchema created = db.getSubjectDao().getSubjectByName(name);
                sid = created != null ? created.getSid() : 0;
            } else {
                sid = existing.getSid();
                if (!existing.isAttendanceSetUpComplete() || !existing.isTimeTableSetUpComplete()) {
                    existing.setAttendanceSetUpComplete(true);
                    existing.setTimeTableSetUpComplete(true);
                    db.getSubjectDao().updateSubject(existing);
                }
            }

            if (sid <= 0) continue;

            // Sync attendance history records
            if (item.getRecords() != null) {
                for (AttendanceResponse.RecordItem rec : item.getRecords()) {
                    String date = rec.getDate();
                    if (date == null || date.isEmpty()) continue;
                    long dateMs = DateUtil.getDateMs(date);
                    if (dateMs > 0) {
                        date = DateUtil.getDateString(dateMs);
                    }

                    String mark = rec.getMark() != null ? rec.getMark().trim().toUpperCase() : "P";
                    String status;
                    if ("P".equals(mark) || "PRESENT".equals(mark)) {
                        status = "PRESENT";
                    } else if ("AB".equals(mark) || "ABSENT".equals(mark)) {
                        status = "ABSENT";
                    } else if ("DL".equals(mark) || "DUTY".equals(mark) || "DUTY LEAVE".equals(mark)) {
                        status = "PRESENT"; // Duty leaves count towards attendance
                    } else {
                        status = "PRESENT";
                    }

                    AttendanceSchema existingRec = db.getAttendanceDao().getAttendanceRecord(sid, date, false);
                    if (existingRec == null) {
                        db.getAttendanceDao().addAttendance(new AttendanceSchema(sid, date, status, false));
                    } else if (!status.equalsIgnoreCase(existingRec.getStatus())) {
                        db.getAttendanceDao().updateStatus(existingRec.getAid(), status);
                    }
                }
            }
        }
    }

    private static void syncSingleSubjectAttendanceToDb(int subjectId, String fallbackName, AttendanceResponse resp) {
        if (resp == null || resp.getData() == null) return;
        Database db = Database.getInstance();
        AttendanceResponse.Data data = resp.getData();
        List<AttendanceResponse.SubjectItem> subjects = data.getSubjects();
        if (subjects == null || subjects.isEmpty()) return;

        for (AttendanceResponse.SubjectItem item : subjects) {
            String name = item.getSubjectName();
            if (name == null || name.trim().isEmpty()) {
                name = fallbackName;
            }
            if (name == null || name.trim().isEmpty()) continue;

            SubjectSchema existing = null;
            if (subjectId > 0) {
                existing = db.getSubjectDao().getSubjectByErpId(subjectId);
            }
            if (existing == null) {
                existing = db.getSubjectDao().getSubjectByName(name.trim());
            }

            boolean isLab = existing != null ? existing.isLab() : name.toLowerCase().contains("lab");
            int sid;

            if (existing == null) {
                SubjectSchema newSub = new SubjectSchema(
                        name.trim(),
                        isLab,
                        isLab ? 2 : 4,
                        0,
                        75,
                        "#4F46E5",
                        true,
                        true
                );
                newSub.setErpSubjectId(subjectId);
                newSub.setSubjectType(isLab ? "practical" : "theory");
                if (item.getTeacherName() != null && !item.getTeacherName().trim().isEmpty()) {
                    newSub.setTeacherName(item.getTeacherName().trim());
                }
                db.getSubjectDao().addSubject(newSub);
                SubjectSchema created = db.getSubjectDao().getSubjectByName(name.trim());
                sid = created != null ? created.getSid() : 0;
            } else {
                sid = existing.getSid();
                if (subjectId > 0 && existing.getErpSubjectId() == 0) {
                    existing.setErpSubjectId(subjectId);
                }
                if (!existing.isAttendanceSetUpComplete() || !existing.isTimeTableSetUpComplete()) {
                    existing.setAttendanceSetUpComplete(true);
                    existing.setTimeTableSetUpComplete(true);
                }
                if (item.getTeacherName() != null && !item.getTeacherName().trim().isEmpty()) {
                    existing.setTeacherName(item.getTeacherName().trim());
                }
                db.getSubjectDao().updateSubject(existing);
            }

            if (sid <= 0) continue;

            // Sync records: wipe existing attendance for this sid and insert fresh ERP marks
            if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                db.getAttendanceDao().deleteBySid(sid);
                for (AttendanceResponse.RecordItem rec : item.getRecords()) {
                    String date = rec.getDate();
                    if (date == null || date.isEmpty()) continue;
                    long dateMs = DateUtil.getDateMs(date);
                    if (dateMs > 0) {
                        date = DateUtil.getDateString(dateMs);
                    }
                    String mark = rec.getMark() != null ? rec.getMark().trim().toUpperCase() : "P";
                    String status;
                    if ("P".equals(mark) || "PRESENT".equals(mark)) {
                        status = "PRESENT";
                    } else if ("AB".equals(mark) || "ABSENT".equals(mark)) {
                        status = "ABSENT";
                    } else if ("DL".equals(mark) || "DUTY".equals(mark) || "DUTY LEAVE".equals(mark)) {
                        status = "PRESENT";
                    } else {
                        status = "PRESENT";
                    }

                    AttendanceSchema att = new AttendanceSchema(sid, date, status, isLab);
                    att.setErpAttendanceId(rec.getAttendanceId());
                    att.setLectureSessionId(rec.getLectureSessionId());
                    att.setHour(rec.getHour());
                    att.setRawMark(mark);
                    db.getAttendanceDao().addAttendance(att);
                }
            }
        }
    }

    private static void syncTimetableToDb(TimetableResponse.Data data) {
        if (data == null || data.getDays() == null) return;
        Database db = Database.getInstance();

        // Ensure days exist
        String[] standardDays = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        Map<String, Integer> dayNameToIdMap = new HashMap<>();

        for (String dayName : standardDays) {
            TimeTableDaySchema daySchema = db.getTimeTableDayDao().getDayByName(dayName);
            if (daySchema == null) {
                db.getTimeTableDayDao().insert(new TimeTableDaySchema(dayName));
                daySchema = db.getTimeTableDayDao().getDayByName(dayName);
            }
            if (daySchema != null) {
                dayNameToIdMap.put(dayName, daySchema.getDid());
            }
        }

        // Map periods into DaySchedule
        int colorIdx = 0;
        for (Map.Entry<String, List<TimetableResponse.PeriodItem>> entry : data.getDays().entrySet()) {
            String day = entry.getKey();
            Integer did = dayNameToIdMap.get(day);
            if (did == null) continue;

            List<TimetableResponse.PeriodItem> periods = entry.getValue();
            if (periods == null || periods.isEmpty()) continue;

            Integer[] periodSids = new Integer[10]; // 1-indexed up to 9 periods
            for (TimetableResponse.PeriodItem p : periods) {
                int pNum = p.getPeriod();
                if (pNum >= 1 && pNum <= 9) {
                    String subName = p.getSubjectName() != null ? p.getSubjectName().trim() : "";
                    if (subName.isEmpty()) continue;

                    SubjectSchema subject = db.getSubjectDao().getSubjectByName(subName);
                    if (subject == null) {
                        // Auto-create subject
                        SubjectSchema newSub = new SubjectSchema(
                                subName,
                                p.getType() != null && p.getType().equalsIgnoreCase("Lab"),
                                4,
                                0,
                                75,
                                SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.length],
                                true,
                                true
                        );
                        colorIdx++;
                        db.getSubjectDao().addSubject(newSub);
                        subject = db.getSubjectDao().getSubjectByName(subName);
                    }
                    if (subject != null) {
                        periodSids[pNum] = subject.getSid();
                        boolean updated = false;
                        if (p.getTeacherName() != null && !p.getTeacherName().trim().isEmpty()
                                && (subject.getTeacherName() == null || subject.getTeacherName().trim().isEmpty())) {
                            subject.setTeacherName(p.getTeacherName().trim());
                            updated = true;
                        }
                        if (p.getRoomNo() != null && !p.getRoomNo().trim().isEmpty()
                                && (subject.getRoomNo() == null || subject.getRoomNo().trim().isEmpty())) {
                            subject.setRoomNo(p.getRoomNo().trim());
                            updated = true;
                        }
                        if (p.getSubjectCode() != null && !p.getSubjectCode().trim().isEmpty()
                                && (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty())) {
                            subject.setSubjectCode(p.getSubjectCode().trim());
                            updated = true;
                        }
                        if (updated) {
                            db.getSubjectDao().updateSubject(subject);
                        }
                    }
                }
            }

            DayScheduleSchema schedule = db.getDayScheduleDao().getScheduleByDid(did);
            if (schedule == null) {
                DayScheduleSchema newSchedule = new DayScheduleSchema(
                        did,
                        periodSids[1], periodSids[2], periodSids[3],
                        periodSids[4], periodSids[5], periodSids[6],
                        periodSids[7], periodSids[8], periodSids[9]
                );
                db.getDayScheduleDao().insert(newSchedule);
            } else {
                schedule.setP1(periodSids[1]);
                schedule.setP2(periodSids[2]);
                schedule.setP3(periodSids[3]);
                schedule.setP4(periodSids[4]);
                schedule.setP5(periodSids[5]);
                schedule.setP6(periodSids[6]);
                schedule.setP7(periodSids[7]);
                schedule.setP8(periodSids[8]);
                schedule.setP9(periodSids[9]);
                db.getDayScheduleDao().update(schedule);
            }
        }
    }

    private static void syncAssessmentsToDb(List<AssessmentsResponse.AssessmentItem> list, Map<Integer, String> subjectMap) {
        if (list == null || list.isEmpty()) return;
        Database db = Database.getInstance();

        for (AssessmentsResponse.AssessmentItem item : list) {
            String subName = item.getSubjectName() != null ? item.getSubjectName().trim() : "";
            if (subName.isEmpty() && item.getSubjectCode() != null) {
                try {
                    int subId = Integer.parseInt(item.getSubjectCode());
                    if (subjectMap != null && subjectMap.containsKey(subId)) {
                        subName = subjectMap.get(subId);
                    }
                } catch (Exception ignored) {}
            }

            SubjectSchema subject = null;
            if (subName != null && !subName.isEmpty()) {
                subject = db.getSubjectDao().getSubjectByName(subName);
            }
            if (subject == null) continue;

            int sid = subject.getSid();
            MarksSchema existingMarks = db.getMarksDao().getMarks(sid);
            double m1 = item.getMst1Marks();
            double m2 = item.getMst2Marks();
            double m3 = item.getMst3Marks();

            if (existingMarks == null) {
                db.getMarksDao().addMarks(new MarksSchema(sid, subject.getSubjectName(), m1, m2, m3));
            } else {
                existingMarks.setMid1(m1);
                existingMarks.setMid2(m2);
                existingMarks.setMid3(m3);
                db.getMarksDao().addMarks(existingMarks);
            }
        }
    }

    private static void syncSessionToDb(com.redcodersgroup.attend_collegeattendancetracker.network.model.CollegeSessionResponse.Data session) {
        if (session == null) return;
        Database db = Database.getInstance();
        UserSchema user = db.getUserDao().getUser();
        if (user != null) {
            long start = parseIsoDate(session.getStartDate());
            long end = parseIsoDate(session.getEndDate());
            if (start > 0) user.setSemStartDate(start);
            if (end > 0) user.setSemEndDate(end);
            db.getUserDao().updateUser(user);
        }
    }

    private static void syncEnrolledSubjectsToDb(List<com.redcodersgroup.attend_collegeattendancetracker.network.model.EnrolledSubjectsResponse.SubjectItem> list) {
        if (list == null || list.isEmpty()) return;
        Database db = Database.getInstance();
        int colorIdx = 0;
        for (com.redcodersgroup.attend_collegeattendancetracker.network.model.EnrolledSubjectsResponse.SubjectItem item : list) {
            String name = item.getSubjectName() != null ? item.getSubjectName().trim() : "";
            if (name.isEmpty()) continue;
            SubjectSchema existing = null;
            if (item.getId() > 0) {
                existing = db.getSubjectDao().getSubjectByErpId(item.getId());
            }
            if (existing == null) {
                existing = db.getSubjectDao().getSubjectByName(name);
            }

            if (existing == null) {
                SubjectSchema newSub = new SubjectSchema(
                        name,
                        item.isLab(),
                        item.getCredits() > 0 ? item.getCredits() : 4,
                        0,
                        75,
                        SUBJECT_COLORS[colorIdx % SUBJECT_COLORS.length],
                        true,
                        true
                );
                newSub.setErpSubjectId(item.getId());
                newSub.setSubjectCode(item.getSubjectCode());
                newSub.setSubjectType(item.getType());
                newSub.setCredits(item.getCredits());
                colorIdx++;
                db.getSubjectDao().addSubject(newSub);
            } else {
                existing.setErpSubjectId(item.getId());
                existing.setSubjectCode(item.getSubjectCode());
                existing.setSubjectType(item.getType());
                existing.setCredits(item.getCredits());
                existing.setAttendanceSetUpComplete(true);
                existing.setTimeTableSetUpComplete(true);
                db.getSubjectDao().updateSubject(existing);
            }
        }
    }

    private static long parseIsoDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 0;
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date d = sdf.parse(dateStr.trim());
            return d != null ? d.getTime() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
