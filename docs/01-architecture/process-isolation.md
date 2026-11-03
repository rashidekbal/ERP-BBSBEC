# Process Isolation, Privilege Levels & Security Model

## 1. Operating System Process Architecture

**BBSBEC Attend** runs as a single Linux user process on Android identified by package namespace `com.redcodersgroup.erp`. The operating system enforces kernel-level sandbox isolation through Android's application sandbox model (UID/GID isolation).

```
+===================================================================================+
|               Linux Kernel User Space (UID: u0_aXXX, GID: u0_aXXX)                |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                 Android Application Process: com.redcodersgroup.erp          |  |
|  |                                                                             |  |
|  |   [ Main Thread (UI Looper) ]                                               |  |
|  |   - Activity / Fragment ViewBinding Rendering                               |  |
|  |   - Touch Events & Layout Measuring                                         |  |
|  |   - LiveData Observer Callbacks                                             |  |
|  |                                                                             |  |
|  |   [ DbHelper Worker Pool (SingleThreadExecutor) ]                            |  |
|  |   - FIFO Serialized Room Database Mutations (attend_db)                     |  |
|  |   - Database Cascading Deletions & Record Computations                      |  |
|  |                                                                             |  |
|  |   [ OkHttp Dispatcher Thread Pool ]                                         |  |
|  |   - TLS 1.3 Handshake & Network HTTP/2 Framing                              |  |
|  |   - Asynchronous Retrofit Callbacks                                         |  |
|  |   - Synchronous Relogin Interception during 401 recovery                    |  |
|  |                                                                             |  |
|  |   [ Short-Lived Broadcast Receiver Windows ]                                |  |
|  |   - NotificationCheckReceiver (runs <= 10s budget before termination)       |  |
|  |   - DailyAlarmReceiver (8:00 AM wakeup)                                     |  |
|  |   - BootReceiver (re-arms alarms after reboot)                              |  |
|  +-----------------------------------------------------------------------------+  |
+===================================================================================+
```

---

## 2. Threading Boundaries & Concurrency Model

Android strictly forbids performing synchronous network I/O or heavy SQLite queries on the Main UI Thread (`NetworkOnMainThreadException` and ANR watchdog triggers). The codebase enforces rigid threading boundaries:

| Thread / Executor | Component Responsibility | Mechanism | Fallback / Safety Mechanism |
| :--- | :--- | :--- | :--- |
| **Main UI Thread (`Looper.getMainLooper()`)** | View drawing, layout inflation, animations, click dispatch. | Android UI Event Loop | Never touches Room or OkHttp synchronously. |
| **DB SingleThreadExecutor (`DbHelper`)** | Write operations to SQLite (`insert`, `update`, `deleteSubject`, `addMarks`). | `Executors.newSingleThreadExecutor()` | Guarantees sequential write consistency, eliminating SQLite `database is locked` race conditions. |
| **OkHttp Network Thread Pool** | Executing HTTP network calls and receiving JSON responses. | OkHttp `Dispatcher` thread pool | Managed automatically by Retrofit `.enqueue()` or explicit `DbHelper.execute(...)` for batch sync. |
| **Main-Thread Posting** | Dispatching completion callbacks from background threads to UI. | `new Handler(Looper.getMainLooper()).post(...)` | Ensures UI updates always occur on the main looper thread. |

---

## 3. Privilege Levels & Android Permissions

The application requests only the minimum set of permissions necessary to execute academic tracking. No intrusive permissions (such as `CAMERA`, `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, or `RECORD_AUDIO`) are requested.

### Declared Permissions Analysis

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

| Permission | Protection Level | Purpose & Usage Boundary | Modern OS Nuance (Android 12 to 15) |
| :--- | :--- | :--- | :--- |
| **`INTERNET`** | Normal | Allows Retrofit/OkHttp to query college ERP endpoints over HTTPS. | Mandatory for ERP synchronization. Encrypted via TLS 1.3. |
| **`POST_NOTIFICATIONS`** | Dangerous (API 33+) | Permits posting status-bar alerts for circulars and daily reminders. | Dynamically prompted on Android 13+; gracefully muted if denied. |
| **`SCHEDULE_EXACT_ALARM`**| Special (API 31+) | Permits exact timing for the 8:00 AM attendance alarm. | Requires user permission toggle on modern devices. **Defensive fallback**: app checks `canScheduleExactAlarms()` and degrades to `setAndAllowWhileIdle()` to avoid fatal `SecurityException`. |
| **`RECEIVE_BOOT_COMPLETED`**| Normal | Wakes up `BootReceiver` upon device startup to rearm scheduled alarms. | Handled via static manifest receiver; immediately reschedules alarms and exits. |

---

## 4. Component Visibility & Export Security

Android components (`<activity>`, `<receiver>`, `<service>`) declared in `AndroidManifest.xml` follow strict isolation principles:

### Activity Export Audit
- **Entry Points**: 
  - `EntryActivity`: `android:exported="true"` with `<category android:name="android.intent.category.LAUNCHER" />`. This is the sole external launch target.
  - `HomeActivity`: `android:exported="true"`. Accessible for internal navigation or launcher shortcut restoration.
- **Internal Screens (`android:exported="false"`)**:
  - `LoginActivity`, `OnboardingActivity`, `SubjectInfoActivity`, `AdditionalSubjectInfoActivity`, `TimeTableActivity`, `ProfileActivity`, `AnalyticsActivity`, `SemesterRegistrationActivity`, `EditRegistrationActivity`, `FeesPaymentActivity`, `SubmitPaymentActivity`, `WebmailActivity`, `WebmailDetailActivity`, `NotificationsActivity`, `HolidayActivity`, `SettingActivity`, `TermsActivity`, `PrivacyPolicyActivity`.
  - **Security Guarantee**: Third-party malicious apps cannot bypass login or invoke private activities directly via explicit intents.

### Receiver & Service Export Audit
- **`AttendanceWidgetProvider`**: `android:exported="true"` (Mandatory for `AppWidgetManager` system IPC). Explicit intent filters restrict handling strictly to widget actions (`APPWIDGET_UPDATE`, `ACTION_MARK_ATTENDANCE`, `ACTION_MARK_ALL_PRESENT`, `ACTION_REFRESH_WIDGET`).
- **`BootReceiver`**: `android:exported="true"` with system actions (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`).
- **`DailyAlarmReceiver` & `NotificationCheckReceiver`**: `android:exported="false"`. Cannot be triggered or snooped by unauthorized external applications.
- **`AttendanceWidgetService`**: `android:exported="false"`, protected by system permission `android.permission.BIND_REMOTEVIEWS`.

---

## 5. Local Storage Security & Data Isolation

1. **Private File Sandbox**:
   - SQLite database `attend_db` is stored under `/data/data/com.redcodersgroup.erp/databases/`, strictly protected by Linux file permissions (`-rw-rw----`).
   - JSON caches (`notifications_cache.json`, `webmail_inbox_cache.json`) reside in `/data/data/com.redcodersgroup.erp/files/`.
2. **Credential Storage**:
   - Student session JWTs (`access_token`, `refresh_token`) and saved login credentials (`saved_username`, `saved_password`) are saved in `bbsbec_auth_pref.xml` via `Context.MODE_PRIVATE`.
   - Access is restricted exclusively to the app UID.
3. **Backup Rules Configuration**:
   - `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` prevent sensitive credentials from leaking into unencrypted adb backup streams unless explicitly permitted.
4. **Zero Third-Party Telemetry**:
   - Zero foreign SDKs are bundled. No third-party network libraries or tracking scripts have access to student data or device telemetry.
