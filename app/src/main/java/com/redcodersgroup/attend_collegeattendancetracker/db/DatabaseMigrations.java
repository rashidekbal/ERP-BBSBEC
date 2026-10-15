package com.redcodersgroup.attend_collegeattendancetracker.db;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class DatabaseMigrations {

    /**
     * Helper to check whether a specific column exists in a given SQLite table.
     */
    public static boolean doesColumnExist(@NonNull SupportSQLiteDatabase db, @NonNull String tableName, @NonNull String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.query("PRAGMA table_info(`" + tableName + "`)");
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if (columnName.equalsIgnoreCase(cursor.getString(nameIndex))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    /**
     * Helper to add a column safely if it doesn't already exist.
     */
    public static void addColumnIfNotExists(@NonNull SupportSQLiteDatabase db, @NonNull String tableName, @NonNull String columnName, @NonNull String columnDefinition) {
        if (!doesColumnExist(db, tableName, columnName)) {
            db.execSQL("ALTER TABLE `" + tableName + "` ADD COLUMN `" + columnName + "` " + columnDefinition);
        }
    }

    /**
     * Helper to check whether a specific table exists.
     */
    public static boolean doesTableExist(@NonNull SupportSQLiteDatabase db, @NonNull String tableName) {
        Cursor cursor = null;
        try {
            cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new Object[]{tableName});
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Subject additional info table
            database.execSQL("CREATE TABLE IF NOT EXISTS `subjectAdditionalInfo` (" +
                    "`said` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sid` INTEGER NOT NULL, " +
                    "`totalClasses` INTEGER NOT NULL, " +
                    "`totalLabs` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`sid`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_subjectAdditionalInfo_sid` ON `subjectAdditionalInfo` (`sid`)");

            // Timetable day table
            database.execSQL("CREATE TABLE IF NOT EXISTS `timeTable_day` (" +
                    "`did` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`dayName` TEXT)");

            // Day schedule table
            database.execSQL("CREATE TABLE IF NOT EXISTS `daySchedule` (" +
                    "`scheduleId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`did` INTEGER NOT NULL, " +
                    "`p1` INTEGER, `p2` INTEGER, `p3` INTEGER, `p4` INTEGER, `p5` INTEGER, " +
                    "`p6` INTEGER, `p7` INTEGER, `p8` INTEGER, `p9` INTEGER, " +
                    "FOREIGN KEY(`did`) REFERENCES `timeTable_day`(`did`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p1`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p2`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p3`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p4`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p5`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p6`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p7`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p8`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`p9`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daySchedule_did` ON `daySchedule` (`did`)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Attendance table
            database.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (" +
                    "`aid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sid` INTEGER NOT NULL, " +
                    "`date` TEXT, " +
                    "`status` TEXT, " +
                    "`isLab` INTEGER NOT NULL DEFAULT 0, " +
                    "FOREIGN KEY(`sid`) REFERENCES `subject`(`sid`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_sid` ON `attendance` (`sid`)");
            addColumnIfNotExists(database, "attendance", "isLab", "INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addColumnIfNotExists(database, "user", "maxMarks", "INTEGER NOT NULL DEFAULT 24");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addColumnIfNotExists(database, "user", "rollNumber", "TEXT");
            addColumnIfNotExists(database, "user", "phone", "TEXT");
            addColumnIfNotExists(database, "user", "email", "TEXT");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_sid` ON `attendance` (`sid`)");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addColumnIfNotExists(database, "subject", "isLab", "INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // These columns were added to SubjectSchema but never had a migration.
            // Existing subjects should be treated as setup-complete (DEFAULT 1).
            addColumnIfNotExists(database, "subject", "attendanceSetupComplete", "INTEGER NOT NULL DEFAULT 1");
            addColumnIfNotExists(database, "subject", "timeTableSetupComplete", "INTEGER NOT NULL DEFAULT 1");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addColumnIfNotExists(database, "subject", "erpSubjectId", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfNotExists(database, "subject", "subjectCode", "TEXT");
            addColumnIfNotExists(database, "subject", "subjectType", "TEXT");
            addColumnIfNotExists(database, "subject", "credits", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfNotExists(database, "subject", "teacherName", "TEXT");
            addColumnIfNotExists(database, "subject", "roomNo", "TEXT");

            addColumnIfNotExists(database, "attendance", "erpAttendanceId", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfNotExists(database, "attendance", "lectureSessionId", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfNotExists(database, "attendance", "hour", "TEXT");
            addColumnIfNotExists(database, "attendance", "rawMark", "TEXT");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addColumnIfNotExists(database, "user", "universityRollNumber", "TEXT");
            addColumnIfNotExists(database, "user", "classGroup", "TEXT");
            addColumnIfNotExists(database, "user", "course", "TEXT");
            addColumnIfNotExists(database, "user", "erpStudentId", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfNotExists(database, "user", "photoUrl", "TEXT");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            addColumnIfNotExists(database, "user", "fatherName", "TEXT");
            addColumnIfNotExists(database, "user", "motherName", "TEXT");
            addColumnIfNotExists(database, "user", "fatherProfession", "TEXT");
            addColumnIfNotExists(database, "user", "motherProfession", "TEXT");
            addColumnIfNotExists(database, "user", "gender", "TEXT");
            addColumnIfNotExists(database, "user", "dob", "TEXT");
            addColumnIfNotExists(database, "user", "aadhar", "TEXT");
            addColumnIfNotExists(database, "user", "bloodGroup", "TEXT");
            addColumnIfNotExists(database, "user", "category", "TEXT");
            addColumnIfNotExists(database, "user", "status", "TEXT");
            addColumnIfNotExists(database, "user", "parentMobile", "TEXT");
            addColumnIfNotExists(database, "user", "batch", "TEXT");
            addColumnIfNotExists(database, "user", "admissionType", "TEXT");
            addColumnIfNotExists(database, "user", "practicalGroup", "TEXT");
            addColumnIfNotExists(database, "user", "communicationAddress", "TEXT");
            addColumnIfNotExists(database, "user", "permanentAddress", "TEXT");
            addColumnIfNotExists(database, "user", "highSchoolBoard", "TEXT");
            addColumnIfNotExists(database, "user", "highSchoolPassingYear", "TEXT");
            addColumnIfNotExists(database, "user", "highSchoolMarks", "TEXT");
            addColumnIfNotExists(database, "user", "highSchoolMaxMarks", "TEXT");
            addColumnIfNotExists(database, "user", "highSchoolPercentage", "TEXT");
            addColumnIfNotExists(database, "user", "interBoard", "TEXT");
            addColumnIfNotExists(database, "user", "interPassingYear", "TEXT");
            addColumnIfNotExists(database, "user", "interMarks", "TEXT");
            addColumnIfNotExists(database, "user", "interMaxMarks", "TEXT");
            addColumnIfNotExists(database, "user", "interPercentage", "TEXT");
            addColumnIfNotExists(database, "user", "scholarship", "TEXT");
        }
    };

    public static final Migration[] ALL_MIGRATIONS = new Migration[]{
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10
    };
}
