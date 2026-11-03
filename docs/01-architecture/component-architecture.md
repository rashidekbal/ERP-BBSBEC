# Component Architecture & Modular Breakdown

## 1. Architectural Pattern: MVVM (Model-View-ViewModel)

The application follows the **Model-View-ViewModel (MVVM)** pattern combined with Android Jetpack Architecture Components (`ViewModel`, `LiveData`, `ViewBinding`, and `Room`).

```
+-----------------------------------------------------------------------------------+
|                                  VIEW LAYER                                       |
|  Activities, Fragments, Custom Views, Dialogs, RecyclerView Adapters              |
|  - ViewBinding provides compile-time safe reference to XML views                  |
|  - Observes LiveData streams emitted by ViewModels                                |
|  - Handles UI transitions, user gestures, and animations                          |
+----------------------------------------+------------------------------------------+
                                         |
                                         | Observes LiveData / Dispatches Actions
                                         v
+-----------------------------------------------------------------------------------+
|                                VIEWMODEL LAYER                                    |
|  HomeViewModel, AttendancePageViewModel, SubjectInfoViewModel, MarksViewModel,    |
|  TaskViewModel, TimeTableViewModel, ProfileViewModel, AnalyticsViewModel         |
|  - Survives configuration changes (e.g. screen rotation, theme switch)            |
|  - Exposes observable LiveData properties to the View layer                       |
|  - Dispatches business computations and calls repository/DB facades              |
+----------------------------------------+------------------------------------------+
                                         |
                                         | Reads & Writes Asynchronously
                                         v
+-----------------------------------------------------------------------------------+
|                                  MODEL LAYER                                      |
|  Database (Room SQLite), NotificationStorage / WebmailStorage (JSON Cache),       |
|  TokenManager (SharedPreferences), ErpSyncManager (Network Sync Facade)           |
+-----------------------------------------------------------------------------------+
```

---

## 2. Core Subsystems & Components

### 2.1 Networking & Sync Subsystem (`com.redcodersgroup...network`)

The networking subsystem orchestrates communication with institutional backend servers and reconciles local storage.

- **`BbsbecApiService`**: The primary Retrofit interface declaring all REST endpoints:
  - Authentication: `webLogin(...)`, `refreshToken(...)`
  - Academics: `getStudentProfile()`, `getClassAttendance(...)`, `getTimetable()`, `getAssessments()`, `getCollegeSession()`, `getFetchSubjects()`, `getEligibleSubjects()`, `getSemesterRegistrations()`
  - Financials: `getFeesHistory()`, `getPayments()`
  - Campus Comms: `getWebmailInbox(...)`, `getNotificationsInbox()`, `getNotificationUnreadCount()`, `markNotificationRead(...)`, `markAllNotificationsRead(...)`
- **`NetworkClient`**: OkHttp and Retrofit factory.
  - Base URLs: `MOBILE_API_HOST` (`https://api.bbsbec.edu.in/`), `WEB_LOGIN_URL` (`https://erp.bbsbec.edu.in/authentication/api/login`).
  - Common Headers: `X-College-Id: 1399`, `Accept: application/json`, `Content-Type: application/json`.
  - Interceptors: `authHeaderInterceptor` injects dynamic `Authorization: Bearer <token>`; `HttpLoggingInterceptor` logs payload bodies in debug builds.
  - Authenticator: `tokenAuthenticator` monitors HTTP 401 responses and triggers `reloginSync()` using saved credentials to seamlessly retrieve new tokens without terminating active sessions.
- **`TokenManager`**: Encapsulates session tokens (`access_token`, `refresh_token`), student identity metadata (`student_roll`, `student_name`, `student_email`), and credentials (`saved_username`, `saved_password`) within private Android `SharedPreferences` (`bbsbec_auth_pref`).
- **`ErpSyncManager`**: Central sync orchestrator.
  - **Launch Sync**: Enforces a 4-hour synchronization interval (`FULL_SYNC_INTERVAL_MS = 14,400,000 ms`). If current time minus last sync is < 4 hours and profile setup is complete, full sync is bypassed.
  - **Manual Sync**: Debounced with a 10-second cooldown (`MANUAL_SYNC_COOLDOWN_MS = 10,000 ms`) to protect ERP servers against aggressive swipe-to-refresh gestures.
  - **Multi-Type Attendance Probing**: Automatically queries enrolled subjects against `lab`, `theory`, and `tutorial` types to correctly ingest classes even when college departments misclassify subject types.

### 2.2 Local Persistence Subsystem (`com.redcodersgroup...db`)

Structured data is stored locally in Room SQLite database `attend_db` at **Version 10**.

- **`Database`**: Abstract `RoomDatabase` master singleton configuring:
  - `HolidaySchema` (`holiday` table)
  - `SubjectSchema` (`subject` table)
  - `AssignmentSchema` (`assignment` table)
  - `MarksSchema` (`marks` table)
  - `UserSchema` (`user` table)
  - `SubjectAdditionalInfoSchema` (`subjectAdditionalInfo` table)
  - `TimeTableDaySchema` (`timeTable_day` table)
  - `DayScheduleSchema` (`daySchedule` table)
  - `AttendanceSchema` (`attendance` table)
- **`DatabaseMigrations`**: Contains incremental, backward-compatible SQLite migration scripts (`MIGRATION_1_2` through `MIGRATION_9_10`) utilizing defensive schema checks (`doesColumnExist`, `addColumnIfNotExists`) to prevent duplicate column alteration exceptions.
- **`DbHelper`**: Concurrency facade managing a single-threaded background worker (`Executors.newSingleThreadExecutor()`). All write mutations (adding subjects, cascading deletions, marks updates) are routed through `DbHelper.execute(...)` to guarantee FIFO write ordering and prevent database locking conflicts.

### 2.3 Offline Document Caches (`NotificationStorage` & `WebmailStorage`)

Semi-structured JSON payloads with rich text and HTML bodies are persisted to internal private app storage:

- **`NotificationStorage`**: Serializes `NotificationsInboxResponse.Data` to `notifications_cache.json` under `context.getFilesDir()`. Maintains an in-memory index map (`notificationIndex`) for fast $O(1)$ unread lookups and bulk "mark all as read" operations.
- **`WebmailStorage`**: Serializes `WebmailInboxResponse.WebmailData` to `webmail_inbox_cache.json`. Provides asynchronous background writes with synchronized memory access, tracking read states and sync timestamps (`KEY_LAST_SYNC`).

### 2.4 Alarm & Background Notification Subsystem (`com.redcodersgroup...notifications`)

Responsible for background alerts without relying on external push servers.

- **`NotificationScheduler`**:
  - Schedules morning attendance reminder alarm at 8:00 AM daily (`scheduleDailyReminder`).
  - Schedules periodic announcement polling alarm every 30 minutes (`NOTIF_CHECK_INTERVAL_MS = 1,800,000 ms`).
  - Guards exact alarm scheduling via `canScheduleExactAlarms()` on Android 12+, falling back to `setAndAllowWhileIdle()` if ungranted.
- **`NotificationCheckReceiver`**:
  - Wakes up periodically, calls `api.getNotificationsInbox().execute()`, checks for new unread IDs against `SharedPrefUtil.getNotifiedAnnouncementIds()`, and triggers status bar alerts via `NotificationHelper`.
- **`DailyAlarmReceiver`**:
  - Wakes up at 8:00 AM, inspects `HolidayDao` to verify if today is a holiday. If no holiday is active and attendance reminders are enabled, posts a class attendance reminder.
- **`BootReceiver`**:
  - Listens for `android.intent.action.BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, and `QUICKBOOT_POWERON` to rearm cancelled alarms following device restart.
- **`NotificationHelper`**:
  - Configures four high-priority Notification Channels:
    1. `attendance_channel` ("Attendance Reminders")
    2. `assignment_channel` ("Assignment Reminders")
    3. `holiday_channel` ("Holiday Alerts")
    4. `campus_alerts_channel` ("Campus Announcements")

### 2.5 Home Screen App Widget Subsystem (`com.redcodersgroup...widget`)

- **`AttendanceWidgetProvider`**: An Android `AppWidgetProvider` rendering a responsive `RemoteViews` layout (`R.layout.widget_attendance`).
  - Displays current date, overall attendance percentage, safe-to-bunk badge, and today's period schedule.
  - Receives custom action broadcasts:
    - `ACTION_MARK_ATTENDANCE`: Toggles attendance for a specific subject ID.
    - `ACTION_MARK_ALL_PRESENT`: Marks all scheduled classes for today as attended.
    - `ACTION_REFRESH_WIDGET`: Invalidates widget data and forces a complete RemoteViews re-render.
- **`AttendanceWidgetService`**: A `RemoteViewsService` providing a collection adapter factory (`AttendanceRemoteViewsFactory`) to bind today's timetable period items inside widget list views.

---

## 3. Screen Controllers & ViewModels Breakdown

| Screen / Feature | Primary Activity / Fragment | ViewModel | Primary Responsibilities |
| :--- | :--- | :--- | :--- |
| **Startup Routing** | `EntryActivity` | *None* | Verifies login state; triggers 4-hour background sync or navigates directly to `HomeActivity` or `LoginActivity`. |
| **Authentication** | `LoginActivity` | *None* | Validates student credentials against `/authentication/api/login`, saves session via `TokenManager`, triggers initial ERP hydration. |
| **Dashboard Container** | `HomeActivity` | *None* | Hosts `ViewPager2` managing the 4 core tabs (Home, Attendance, Tasks, Marks) and side navigation drawer. |
| **Home Dashboard Tab** | `HomeFragment` | `HomeViewModel` | Displays overall attendance card, quick stats, today's schedule list (`TodayClassesAdapter`), and quick actions. |
| **Attendance Tab** | `AttendanceFragment` | `AttendancePageViewModel` | Shows calendar view, list of enrolled subjects (`AttendanceSubjectListAdapter`), safe-to-bunk counters, and day-level dialogs. |
| **Marks Tab** | `MarksFragment` | `MarksViewModel` | Displays mid-sem evaluation summaries, subject breakdown, and multi-term comparison graphs (`MultiTermGraphView`). |
| **Tasks Tab** | `TaskFragment` | `TaskViewModel` | Manages assignments, project deadlines, submission status checkboxes, and pending task counts. |
| **Subject Detail** | `SubjectInfoActivity` | `SubjectInfoViewModel` | Deep dive into subject metrics, attendance calendar history, and real-time safe-to-bunk interactive slider/simulator. |
| **Timetable Master** | `TimeTableActivity` | `TimeTableViewModel` | Weekly 9-period master schedule editor and display (Monday to Friday/Saturday) with period assignment tools. |
| **Notifications Center**| `NotificationsActivity` | *None* | Filterable circulars list (All, Academic, General, Fees), unread count badges, and offline storage synchronization. |
| **Campus Webmail** | `WebmailActivity`, `WebmailDetailActivity` | *None* | Institutional webmail inbox browser with pagination, full HTML viewer, and attachment indicator. |
| **Fees & Payments** | `FeesPaymentActivity`, `SubmitPaymentActivity` | *None* | Official semester fee breakdown, installment due dates, paid receipts, and manual payment verification interface. |
| **Course Registration** | `SemesterRegistrationActivity`, `EditRegistrationActivity` | *None* | Displays registered courses, syllabus credit totals, elective selection, and approval status. |
| **Analytics** | `AnalyticsActivity` | `AnalyticsViewModel` | Attendance distribution by day of week (`WeekdayDistributionView`), risk analysis, and subject-level shortage projections. |
| **Profile** | `ProfileActivity` | `ProfileViewModel` | Extended student dossier (Roll number, University registration, Class group, Father/Mother details, Board percentages). |
| **Settings** | `SettingActivity` | *None* | Theme switching (Dark/Light mode, multi-color accents), reminder toggles, and database Export/Import backup operations. |
