# Persistence, Database Schemas & Storage Architecture

Data persistence in **BBSBEC Attend** is structured into three distinct tiers:
1. **Relational Storage**: Room SQLite Database (`attend_db`, Version 10).
2. **Document Disk Caches**: File-backed JSON stores under internal app storage (`context.getFilesDir()`).
3. **Key-Value Store**: Private Android `SharedPreferences` files.

---

## 1. Room Relational Database Schema (`attend_db`)

The database is built on Android Room at **Version 10**.

### 1.1 `subject` Table (`SubjectSchema`)
Represents an academic course or laboratory module.

| Column Name | SQLite Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `sid` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT NOT NULL` | Internal subject identifier. |
| `subjectName` | `TEXT` | `NOT NULL` | Course title (e.g. "Network Security"). |
| `isLab` | `INTEGER` | `NOT NULL DEFAULT 0` | Flag: 1 if practical/lab, 0 if theory. |
| `classesPerWeek` | `INTEGER` | `NOT NULL DEFAULT 0` | Scheduled weekly lectures. |
| `labsPerWeek` | `INTEGER` | `NOT NULL DEFAULT 0` | Scheduled weekly lab periods. |
| `attendance_criteria` | `INTEGER` | `NOT NULL DEFAULT 75` | Required target threshold (default: 75%). |
| `uniqueColorCode` | `TEXT` | `DEFAULT '#4F46E5'` | Hex color accent for UI badges. |
| `timeTableSetupComplete` | `INTEGER` | `NOT NULL DEFAULT 1` | Flag indicating timetable slots are mapped. |
| `attendanceSetupComplete`| `INTEGER` | `NOT NULL DEFAULT 1` | Flag indicating attendance baseline is set. |
| `erpSubjectId` | `INTEGER` | `NOT NULL DEFAULT 0` | Official BBSBEC ERP subject ID. |
| `subjectCode` | `TEXT` | `NULLABLE` | Course catalog code (e.g. "BTCS-701-18"). |
| `subjectType` | `TEXT` | `NULLABLE` | Type descriptor ("Theory", "Lab", "Tutorial"). |
| `credits` | `INTEGER` | `NOT NULL DEFAULT 0` | Academic credit hours. |
| `teacherName` | `TEXT` | `NULLABLE` | Faculty instructor name. |
| `roomNo` | `TEXT` | `NULLABLE` | Classroom/Lab room allocation. |

- **Indices**: `CREATE UNIQUE INDEX index_subject_sid ON subject(sid)`

---

### 1.2 `attendance` Table (`AttendanceSchema`)
Individual attendance punch records.

| Column Name | SQLite Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `aid` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT NOT NULL` | Unique record ID. |
| `sid` | `INTEGER` | `NOT NULL, FK -> subject(sid) ON DELETE CASCADE` | Enrolled subject reference. |
| `date` | `TEXT` | `NOT NULL` | Date string formatted as `yyyy-MM-dd` or `dd/MM/yyyy`. |
| `status` | `TEXT` | `NOT NULL` | "present", "absent", or "cancelled". |
| `isLab` | `INTEGER` | `NOT NULL DEFAULT 0` | Flag indicating if this was a lab block. |
| `erpAttendanceId` | `INTEGER` | `NOT NULL DEFAULT 0` | Upstream ERP attendance record ID. |
| `lectureSessionId`| `INTEGER` | `NOT NULL DEFAULT 0` | Upstream ERP lecture session identifier. |
| `hour` | `TEXT` | `NULLABLE` | Period label (e.g. "Period 2 (09:50 - 10:40)"). |
| `rawMark` | `TEXT` | `NULLABLE` | Raw status character from ERP ("P", "A", "DL", "ML"). |

- **Indices**: `CREATE INDEX index_attendance_sid ON attendance(sid)`
- **Foreign Keys**: `FOREIGN KEY(sid) REFERENCES subject(sid) ON DELETE CASCADE`

---

### 1.3 `user` Table (`UserSchema`)
Student academic identity and profile dossier.

| Column Name | SQLite Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `uid` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT NOT NULL` | Internal user ID. |
| `name` | `TEXT` | `NOT NULL` | Student full name. |
| `collegeName` | `TEXT` | `NOT NULL` | College name ("BBSBEC"). |
| `course` | `TEXT` | `NULLABLE` | Academic degree ("B.Tech"). |
| `branch` | `TEXT` | `NULLABLE` | Department branch ("CSE"). |
| `semester` | `INTEGER` | `NOT NULL DEFAULT 1` | Current semester number. |
| `attendanceCriteria` | `INTEGER` | `NOT NULL DEFAULT 75` | Target threshold for all subjects. |
| `maxMarks` | `INTEGER` | `NOT NULL DEFAULT 24` | Maximum marks per MST examination. |
| `rollNumber` | `TEXT` | `NULLABLE` | College roll number. |
| `universityRollNumber`| `TEXT` | `NULLABLE` | PTU University registration number. |
| `classGroup` | `TEXT` | `NULLABLE` | Section group (e.g. "CSE-A"). |
| `erpStudentId` | `INTEGER` | `NOT NULL DEFAULT 0` | ERP student database ID. |
| `photoUrl` | `TEXT` | `NULLABLE` | Profile photo URL. |
| `fatherName`, `motherName` | `TEXT` | `NULLABLE` | Parent names. |
| `fatherProfession`, `motherProfession` | `TEXT` | `NULLABLE` | Parent occupations. |
| `gender`, `dob`, `aadhar` | `TEXT` | `NULLABLE` | Personal demographic records. |
| `bloodGroup`, `category` | `TEXT` | `NULLABLE` | Medical and reservation category. |
| `highSchoolBoard`, `highSchoolPercentage` | `TEXT` | `NULLABLE` | 10th matriculation details. |
| `interBoard`, `interPercentage` | `TEXT` | `NULLABLE` | 12th intermediate details. |

---

### 1.4 Timetable Tables (`timeTable_day` & `daySchedule`)
Defines the weekly 9-period master schedule:
- **`timeTable_day` (`TimeTableDaySchema`)**: `did` (PK), `dayName` (TEXT: "Monday", "Tuesday", etc.).
- **`daySchedule` (`DayScheduleSchema`)**: `scheduleId` (PK), `did` (FK -> `timeTable_day`), `p1` through `p9` (FK -> `subject(sid) ON DELETE CASCADE`).
  - Represents the subject assigned to Period 1 through Period 9 on that weekday.

---

### 1.5 Academic Supporting Tables
- **`marks` (`MarksSchema`)**: Stores internal assessment scores (`mid1`, `mid2`, `mid3`, `assignmentMarks`, `totalMarks`) keyed to `sid` (FK -> `subject(sid)`).
- **`assignment` (`AssignmentSchema`)**: Academic tasks with `aid` (PK), `sid` (FK), `title`, `description`, `dueDateMs`, `isSubmitted`.
- **`holiday` (`HolidaySchema`)**: Calendar holidays with `hid` (PK), `title`, `dateMs`, `holidayDate` string.
- **`subjectAdditionalInfo` (`SubjectAdditionalInfoSchema`)**: Enforces `totalClasses` and `totalLabs` targets per subject.

---

## 2. Room Database Migration History (V1 to V10)

All migrations are maintained in `DatabaseMigrations.java` and applied sequentially:

```
[V1: Initial]
   │
   ▼ MIGRATION_1_2: Added subjectAdditionalInfo, timeTable_day, daySchedule tables
[V2]
   │
   ▼ MIGRATION_2_3: Added attendance table with cascade foreign key and isLab column
[V3]
   │
   ▼ MIGRATION_3_4: Added maxMarks (DEFAULT 24) to user table
[V4]
   │
   ▼ MIGRATION_4_5: Added rollNumber, phone, email to user; added index on attendance.sid
[V5]
   │
   ▼ MIGRATION_5_6: Added isLab to subject table
[V6]
   │
   ▼ MIGRATION_6_7: Added attendanceSetupComplete, timeTableSetupComplete to subject
[V7]
   │
   ▼ MIGRATION_7_8: Added ERP fields to subject (erpSubjectId, subjectCode, credits, etc.)
   │                and attendance (erpAttendanceId, lectureSessionId, hour, rawMark)
[V8]
   │
   ▼ MIGRATION_8_9: Added universityRollNumber, classGroup, course, erpStudentId, photoUrl to user
[V9]
   │
   ▼ MIGRATION_9_10: Added extended dossier (fatherName, motherName, dob, aadhar, 10th/12th academic stats)
[V10: Current Production]
```

---

## 3. File-Based Document Storage

Located in internal app sandbox `/data/data/com.redcodersgroup.erp/files/`:

1. **`notifications_cache.json`**:
   - Managed by `NotificationStorage.java`.
   - Serializes `NotificationsInboxResponse.Data` (JSON object containing `count`, `results` array of announcements).
   - Provides offline reading of all circulars without hitting the network.
2. **`webmail_inbox_cache.json`**:
   - Managed by `WebmailStorage.java`.
   - Serializes `WebmailInboxResponse.WebmailData` (JSON object containing `email_address`, `results` array of mail items with full HTML bodies).
   - Enables instant mailbox browsing when offline.

---

## 4. SharedPreferences Storage

1. **`bbsbec_auth_pref.xml`**:
   - `access_token` (String): Active JWT Bearer token.
   - `refresh_token` (String): Refresh token for re-authentication.
   - `saved_username`, `saved_password` (String): Credentials for automated background re-login.
   - `student_name`, `student_email`, `student_roll` (String): Cached student profile identity.
   - `is_logged_in` (Boolean): Global auth flag.
2. **`attend_pref.xml`** (`ATTEND_SHARED_PREF`):
   - `IS_SETUP_COMPLETE` (Boolean)
   - `IS_PROFILE_COMPLETE` (Boolean)
   - `LAST_FULL_SYNC_TIMESTAMP` (Long): Timestamp of last successful 4-hour sync.
   - `PERIOD_LENGTH` (Int, default: 50 minutes)
   - `ACCENT_COLOR` (String, default: `#F5A623`)
   - `ATTENDANCE_REMINDERS`, `ASSIGNMENT_REMINDERS`, `HOLIDAY_ALERTS` (Boolean toggles)
   - `DARK_MODE` (Boolean)

---

## 5. Backup & Export Schema (`attend_backup_*.json`)

Generated by `ExportService` and restored by `ImportService`:

```json
{
  "version": 1,
  "app": "BBSBEC Attend",
  "exportDate": "2026-11-03 19:30:00",
  "data": {
    "user": { ... },
    "subjects": [ ... ],
    "subjectAdditionalInfo": [ ... ],
    "timeTableDays": [ ... ],
    "daySchedules": [ ... ],
    "attendance": [ ... ],
    "marks": [ ... ],
    "assignments": [ ... ],
    "holidays": [ ... ],
    "preferences": { ... }
  }
}
```
Written to the device's public `Download/` directory via `MediaStore.Downloads` on Android 10+ (API 29+), allowing students to safeguard their historical attendance data across device changes.
