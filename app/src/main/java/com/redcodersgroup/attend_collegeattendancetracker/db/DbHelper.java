package com.redcodersgroup.attend_collegeattendancetracker.db;

import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbHelper {
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    public static void addHoliday(HolidaySchema holidaySchema){
        executorService.execute(()-> Database.getInstance().getHolidayDao().addHoliday(holidaySchema));
    }
    public static void updateHoliday(HolidaySchema holidaySchema){
        executorService.execute(()-> Database.getInstance().getHolidayDao().updateHoliday(holidaySchema));
    }
    public static void removeHoliday(int hid){
        executorService.execute(()-> Database.getInstance().getHolidayDao().deleteHoliday(hid));
    }
    public static void addSubject(SubjectSchema subjectSchema){
        executorService.execute(()-> Database.getInstance().getSubjectDao().addSubject(subjectSchema));
    }
    public static void removeSubject(int sid) {
        removeSubject(sid, null);
    }

    public static void removeSubject(int sid, Runnable onComplete) {
        executorService.execute(() -> {
            Database db = Database.getInstance();
            db.getSubjectDao().deleteSubject(sid);
            db.getSubjectAdditionalInfoDao().deleteBySid(sid);
            db.getMarksDao().deleteMarks(sid);
            db.getAssignmentDao().deleteBySid(sid);
            db.getAttendanceDao().deleteBySid(sid);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public static void addAssignment(AssignmentSchema assignmentSchema) {
        executorService.execute(() -> Database.getInstance().getAssignmentDao().addAssignment(assignmentSchema));
    }
    public static void updateAssignment(AssignmentSchema assignmentSchema) {
        executorService.execute(() -> Database.getInstance().getAssignmentDao().updateAssignment(assignmentSchema));
    }
    public static void setAssignmentSubmitted(int aid, boolean submitted) {
        executorService.execute(() -> Database.getInstance().getAssignmentDao().setSubmitted(aid, submitted));
    }
    public static void removeAssignment(int aid) {
        executorService.execute(() -> Database.getInstance().getAssignmentDao().deleteAssignment(aid));
    }
    public static void addMarks(MarksSchema marksSchema){
        executorService.execute(()-> Database.getInstance().getMarksDao().addMarks(marksSchema));
    }
    public static void clearMidMarks(int sid, int midNumber) {
        clearMidMarks(sid, midNumber, null);
    }
    public static void clearMidMarks(int sid, int midNumber, Runnable onComplete) {
        executorService.execute(() -> {
            com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksDao dao = Database.getInstance().getMarksDao();
            MarksSchema schema = dao.getMarks(sid);
            if (schema != null) {
                if (midNumber == 1) {
                    schema.setMid1(-1);
                } else if (midNumber == 2) {
                    schema.setMid2(-1);
                } else if (midNumber == 3) {
                    schema.setMid3(-1);
                }

                if (schema.getMid1() < 0 && schema.getMid2() < 0 && schema.getMid3() < 0) {
                    dao.deleteMarks(sid);
                } else {
                    dao.addMarks(schema);
                }
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
    public static void clearAllMarksForSubject(int sid, Runnable onComplete) {
        executorService.execute(() -> {
            Database.getInstance().getMarksDao().deleteMarks(sid);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
    public static void updateUser(com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema userSchema) {
        executorService.execute(() -> Database.getInstance().getUserDao().updateUser(userSchema));
    }

    public static void updateSubjectName(int sid, String subjectName) {
        updateSubjectName(sid, subjectName, null);
    }

    public static void updateSubjectName(int sid, String subjectName, Runnable onComplete) {
        executorService.execute(() -> {
            Database db = Database.getInstance();
            db.getSubjectDao().updateSubjectName(sid, subjectName);
            db.getMarksDao().updateSubjectName(sid, subjectName);
            db.getAssignmentDao().updateSubjectName(sid, subjectName);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
