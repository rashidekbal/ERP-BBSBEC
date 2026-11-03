# System Overview & Architecture

## 1. Product Goals & Purpose

**BBSBEC Attend** is the production native Android academic companion for students enrolled at **Baba Banda Singh Bahadur Engineering College (BBSBEC), Fatehgarh Sahib, Punjab**. 

The application is engineered to eliminate the manual, error-prone tracking of university attendance, course requirements, and academic schedules by interfacing directly with the college ERP platform. Key goals include:

1. **Automated Attendance Monitoring**: Synchronize attendance logs directly from official ERP servers, accounting for distinct course delivery formats (Theory, Lab, and Tutorial).
2. **Defensive Attendance Forecasting ("Safe-to-Bunk" Math)**: Calculate the exact number of classes a student can safely miss while maintaining the institution's mandatory 75% attendance threshold, or the exact count required to recover from a shortage.
3. **Period-Weighted Labs**: Automatically detect consecutive multi-period lab blocks and weight conducted and attended metrics accurately.
4. **Resilient Offline-First UX**: Retain local databases and file-based JSON caches so all historical data, schedules, marks, and webmail messages remain instantaneously accessible during network disconnects or ERP server downtime.
5. **Proactive Background Notifications**: Notify students of newly published administrative circulars, daily lecture routines, and holiday cancellations without relying on heavy third-party push services.
6. **Home Screen Widget**: Provide instantaneous glancable attendance summaries and 1-tap logging via Android App Widgets.

---

## 2. System Topology

The system comprises three physical and logical environments: the Android client runtime, institutional ERP web/API services, and client-side operating system subsystems.

```
+-----------------------------------------------------------------------------------------+
|                                    BBSBEC ERP SERVERS                                   |
|                                                                                         |
|   +------------------------------------+    +---------------------------------------+   |
|   |  https://erp.bbsbec.edu.in/        |    |  https://api.bbsbec.edu.in/           |   |
|   |  - Web Login Service               |    |  - Student Profile & Session          |   |
|   |  - Manual Fee Payment Gateways     |    |  - Class Attendance & Timetable       |   |
|   |  - Course Registration Portal      |    |  - Mid-sem Assessments                |   |
|   |                                    |    |  - Webmail Inbox & Notifications      |   |
|   +------------------------------------+    +---------------------------------------+   |
+--------------------------------------------+--------------------------------------------+
                                             ^
                                             | HTTPS / TLS 1.3
                                             | REST JSON (Headers: X-College-Id: 1399)
                                             v
+-----------------------------------------------------------------------------------------+
|                                 ANDROID CLIENT RUNTIME                                  |
|                             Package: com.redcodersgroup.erp                             |
|                                                                                         |
|   +------------------------------------+    +---------------------------------------+   |
|   | Networking & Sync Subsystem        |    | Persistence Subsystem                 |   |
|   | - NetworkClient (OkHttp + Retrofit)|    | - Database (Room SQLite, Version 10)  |   |
|   | - TokenManager (Private SharedPreferences) - NotificationStorage (JSON disk)    |   |
|   | - ErpSyncManager (4-Hour Sync)     |    | - WebmailStorage (JSON disk)          |   |
|   +------------------------------------+    +---------------------------------------+   |
|                      |                                          |                       |
|                      +--------------------+---------------------+                       |
|                                           v                                             |
|   +---------------------------------------------------------------------------------+   |
|   | Presentation Subsystem (MVVM)                                                   |   |
|   | - EntryActivity / LoginActivity / OnboardingActivity                            |   |
|   | - HomeActivity (Home, Attendance, Tasks, Marks ViewModels)                      |   |
|   | - SubjectInfoActivity, TimeTableActivity, AnalyticsActivity                     |   |
|   | - NotificationsActivity, WebmailActivity, FeesPaymentActivity                   |   |
|   +---------------------------------------+-----------------------------------------+   |
+-------------------------------------------|---------------------------------------------+
                                            |
                                            v
+-----------------------------------------------------------------------------------------+
|                               ANDROID OS PLATFORM SERVICES                              |
|                                                                                         |
|   - AlarmManager: Periodic wakeup alarms for notification polling (30m) & morning alerts|
|   - AppWidgetManager: Renders RemoteViews on launcher for quick widget access           |
|   - NotificationManager: Handles notification channels and status bar circular alerts  |
|   - Broadcast Receivers: Receives BOOT_COMPLETED to rearm alarms on device reboot       |
+-----------------------------------------------------------------------------------------+
```

---

## 3. Technology Stack & Technical Constraints

### Runtime & Toolchain
- **Language**: Java 17 (`sourceCompatibility` & `targetCompatibility` set to Java 11 bytecode compatibility).
- **Android Gradle Plugin (AGP)**: `9.4.0`
- **Gradle**: Modern Gradle wrapper supporting AGP 9.x.
- **Minimum SDK (`minSdk`)**: `31` (Android 12 — Snow Cone).
- **Target SDK (`targetSdk`)**: `37` (Android 15 / future API target).
- **Compile SDK (`compileSdk`)**: `37` (minor API level `0`).
- **Application ID**: `com.redcodersgroup.erp`
- **Namespace**: `com.redcodersgroup.attend_collegeattendancetracker`

### Primary Dependencies & Libraries

| Category | Library | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Architecture** | `androidx.fragment:fragment` | `1.9.0` | Navigation & fragment lifecycle control |
| **Architecture** | `androidx.activity:activity` | `1.13.0` | Activity contracts & back-press handling |
| **UI Components** | `com.google.android.material:material` | `1.14.0` | Material 3 components, BottomSheets, and chips |
| **UI Layouts** | `androidx.constraintlayout:constraintlayout` | `2.2.1` | Constraint-based responsive layouts |
| **Navigation** | `androidx.navigation:navigation-fragment-ktx` | `2.9.8` | Navigation graph controllers |
| **Local DB** | `androidx.room:room-runtime-android` | `2.8.4` | SQLite ORM abstraction |
| **Local DB Compiler**| `androidx.room:room-compiler` | `2.8.4` | Annotation processor for schema generation |
| **Networking** | `com.squareup.retrofit2:retrofit` | `2.11.0` | Declarative REST client |
| **Networking** | `com.squareup.retrofit2:converter-gson` | `2.11.0` | JSON serialization bridge |
| **HTTP Engine** | `com.squareup.okhttp3:okhttp` | `4.12.0` | HTTP/2 transport engine with interceptors |
| **HTTP Logging**| `com.squareup.okhttp3:logging-interceptor` | `4.12.0` | Debug logging for network traffic |
| **Serialization**| `com.google.code.gson:gson` | `2.11.0` | Flexible JSON parsing with alternate keys |
| **UI Controls** | `androidx.swiperefreshlayout:swiperefreshlayout` | `1.1.0` | Pull-to-refresh container |

---

## 4. Key Architectural Constraints & Invariants

1. **Zero External Tracking or Cloud Analytics**: To protect student privacy and comply with strict data hygiene, the app does not incorporate Firebase Analytics, Facebook SDK, Crashlytics, or Google AdMob. All network transactions occur strictly between the user's mobile client and the college servers.
2. **Offline-First Non-Blocking UI**: UI components never await network responses before rendering. They observe cached Room SQLite models or disk JSON structures. Network synchronization occurs asynchronously in the background.
3. **Hardened Alarm Scheduling (Android 12–15 compatibility)**: Starting in Android 12 (`minSdk 31`), `SCHEDULE_EXACT_ALARM` requires explicit user permission or app categorization. The scheduler explicitly checks `canScheduleExactAlarms()` and degrades safely to `setAndAllowWhileIdle()` to prevent fatal `SecurityException` runtime crashes.
4. **Multi-Type Attendance Parsing**: College subjects may be marked as Theory, Practical/Lab, or Tutorial on the ERP backend. Because query endpoints accept subject type parameters (`?type=theory|lab|tutorial`), the synchronization manager must execute fallback cascades if a subject's default classification returns empty or non-200.
