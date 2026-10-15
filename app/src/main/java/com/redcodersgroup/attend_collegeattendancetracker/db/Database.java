package com.redcodersgroup.attend_collegeattendancetracker.db;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.redcodersgroup.attend_collegeattendancetracker.App;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.assignment.AssignmentSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.attendance.AttendanceSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidayDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.holiday.HolidaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.marks.MarksSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectAdditionalInfoDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectAdditionalInfoSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.subjects.SubjectSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.DayScheduleSchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDayDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.timeTable.TimeTableDaySchema;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserDao;
import com.redcodersgroup.attend_collegeattendancetracker.db.user.UserSchema;

@androidx.room.Database(entities = {HolidaySchema.class,
        SubjectSchema.class,
        AssignmentSchema.class,
        MarksSchema.class,
        UserSchema.class,
        SubjectAdditionalInfoSchema.class,
        TimeTableDaySchema.class,
        DayScheduleSchema.class,
        AttendanceSchema.class}, version = 10)
public abstract class Database extends RoomDatabase {
    public static final String DB_NAME = "attend_db";
    public static Database instance;

    public static Database getInstance() {
        if (instance == null) {
            instance = Room.databaseBuilder(App.getContext(), Database.class, DB_NAME)
                    .addMigrations(DatabaseMigrations.ALL_MIGRATIONS)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build();
        }
        return instance;
    }

    public abstract HolidayDao getHolidayDao();

    public abstract SubjectDao getSubjectDao();

    public abstract AssignmentDao getAssignmentDao();

    public abstract MarksDao getMarksDao();

    public abstract UserDao getUserDao();

    public abstract SubjectAdditionalInfoDao getSubjectAdditionalInfoDao();

    public abstract TimeTableDayDao getTimeTableDayDao();
    public abstract DayScheduleDao getDayScheduleDao();
    public abstract AttendanceDao getAttendanceDao();
}
