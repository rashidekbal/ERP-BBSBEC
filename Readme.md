# BBSBEC Attend — Student ERP & Academic Companion

**BBSBEC Attend** is the native Android companion app for students of **Baba Banda Singh Bahadur Engineering College (BBSBEC)**. It connects directly with the college ERP to automate attendance tracking, monitor mid-term assessment marks, track fee dues, manage course registrations, access campus webmail, and receive live college broadcasts—with offline-first local caching and home screen widgets.

---

## 🌟 Key Features

### 🏛️ 1. BBSBEC ERP Integration
- **Direct Student Authentication**: Secure session-based authentication with automatic token renewal (`/student/login/`, `/student/profile/`).
- **Smart 4-Hour Synchronization**: Background sync engine refreshes college data every 4 hours on fresh launch, or immediately on demand via pull-to-refresh.
- **Rate-Limit & Debounce Protection**: Rapid swipe debounce prevents excessive API calls and protects server resources.
- **Startup Sync Routing**: Initial data verification runs seamlessly during entry startup before reaching the home dashboard.

### 🔔 2. Campus Notifications & Background Alerts
- **Dedicated Notification Center**: View college circulars and departmental announcements (`/notifications/inbox/`).
- **Category Filter Chips**: Easily switch between **All**, **Academic**, **General**, and **Fees** updates.
- **Read & Unread State Tracking**: Instant visual badges on unread items, with single-tap and bulk "Mark all as read" support.
- **Background Push Service**: Periodic background receiver (`NotificationCheckReceiver`) checks for unread campus broadcasts even when the app is completely closed or killed.
- **Direct Deep-Linking**: Tapping status-bar notifications opens the announcement details page instantly.
- **Adaptive Alarm System**: Graceful fallback between exact alarms (`SCHEDULE_EXACT_ALARM`) and power-saving idle wakeups, preventing unexpected crashes across Android 12 through 15.

### 📊 3. Period-Weighted Attendance Engine
- **ERP Subject & Attendance Sync**: Synchronizes enrolled subjects and verified attendance counts (`/student/fetchsubjects/`, `/student/fetchattendance/`).
- **Intelligent "Safe to Bunk" Math**: Real-time formula calculates exactly how many consecutive lectures can be missed while remaining above the mandatory 75% threshold, or how many must be attended to recover from shortage.
- **Multi-Period Lab Support**: Consecutively scheduled lab periods are weighted proportionally in conducted and attended tallies.
- **Interactive Calendar History**: Backfill and review historical class-by-class attendance with day-level precision.

### 📝 4. Assessments & Mid-Sem Marks
- **Internal Assessment Sync**: Fetches mid-term examination scores and evaluation criteria (`/student/assessments/`).
- **Performance Trends**: Visual tags and progress indicators identify strong subjects and areas requiring academic focus.

### 💳 5. Fees & Payments
- **Official Fee Structure**: View semester fee breakdowns, installments, and paid receipts (`/student/fees/`).
- **Payment Workflow**: Clean interface mirroring the ERP payment process with clear breakdowns and manual checkout flow (no automated financial mutations without student consent).

### 📋 6. Semester Registration
- **Course Enrollment Flow**: View and manage subject registrations matching the official BBSBEC registration portal (`/student/registration/`).
- **Core & Elective Selection**: Browse course codes, credits, and syllabus requirements.

### 📬 7. Campus Webmail
- **Integrated Student Email**: Connect to institutional student webmail without third-party mail clients.
- **Rich HTML Reader**: Built-in full-screen message reader with formatting, attachments, and offline caching.

### 📱 8. Android App Widget & Offline First
- **Home Screen Quick Widget**: Displays overall attendance percentage and safe-to-bunk status directly on the Android home screen (`AttendanceWidgetProvider`).
- **100% Offline-First Architecture**: All ERP data, assignments, timetable schedules, and notifications are stored locally in Room SQLite and disk cache for instant offline access.
- **Dynamic Theming**: Multi-color accent swatch palette with automatic and manual Light/Dark mode.

---

## 🏗️ Architecture & Engineering

The application is engineered strictly following modern native Android best practices:

- **Language**: Modern Java 17 (lambdas, switch expressions, method references).
- **Target SDK**: Android 14+ / 15 (minSdk 31, targetSdk 37).
- **Architecture**: MVVM (Model-View-ViewModel) with Android Architecture Components (`ViewModel`, `LiveData`).
- **Networking**: Retrofit 2 + OkHttp 3 with cookie persistence, custom logging interceptors, and Gson serialization.
- **Local Persistence**: **Room Database** (SQLite) for structured relational data (`Attendance`, `Marks`, `Timetable`, `Assignments`, `Holidays`), complemented by JSON disk storage for offline notification inboxes.
- **Threading**: Centralized `DbHelper` facade with dedicated `ExecutorService` background workers and main-thread `Handler` posting.
- **View Layer**: XML Layouts with ViewBinding enabled across all activities, fragments, custom views, and adapters.
- **Background Scheduling**: `AlarmManager` with `setAndAllowWhileIdle` and `BroadcastReceiver` pipelines for reliable reminders and background announcement checking.

---

## 📂 Project Structure

```
app/src/main/java/com/redcodersgroup/attend_collegeattendancetracker/
├── api/ / network/           # Retrofit services, ERP endpoints, TokenManager, and SyncManager
│   ├── BbsbecApiService.java # ERP API contracts (auth, profile, subjects, attendance, fees, notifications)
│   ├── ErpSyncManager.java   # Intelligent background sync engine (4-hour interval & swipe-to-refresh)
│   └── NetworkClient.java    # OkHttp / Retrofit client configuration
├── db/                       # Room Database, DAOs, and Entity schemas
│   ├── attendance/           # Attendance records and schedule logs
│   ├── marks/                # Mid-sem and assessment schemas
│   ├── assignment/           # Academic assignments and tasks
│   ├── holiday/              # Holiday calendar entries
│   ├── timeTable/            # Daily period slots and schedule mapping
│   └── user/                 # Cached student profile details
├── notifications/            # Alarm and Notification services
│   ├── NotificationHelper.java        # Notification channels and status bar alert builders
│   ├── NotificationScheduler.java     # AlarmManager scheduling and exact-alarm permission helpers
│   ├── NotificationCheckReceiver.java # Background receiver for closed-app notification polling
│   ├── DailyAlarmReceiver.java        # Morning reminder and holiday alert receiver
│   └── BootReceiver.java              # Re-arms background alarms on device reboot
├── screens/                  # Activities, Fragments, ViewHolders, and Adapters
│   ├── auth/                 # ERP Student Login
│   ├── home/                 # Main navigation container (Home, Attendance, Tasks, Marks)
│   ├── notifications/        # NotificationsActivity, NotificationAdapter, and NotificationStorage
│   ├── webmail/              # WebmailInboxActivity and WebmailDetailActivity
│   ├── fees/                 # FeesActivity and payment flows
│   ├── registration/         # SubjectRegistrationActivity
│   ├── profile/              # Student profile and academic criteria
│   ├── setting/              # Theme switcher, reminder toggles, and data export/import
│   ├── subject/              # Subject details, safe-to-bunk math, and attendance timeline
│   └── timetable/            # Weekly master timetable scheduler
└── widget/                   # Android Home Screen Widget (AttendanceWidgetProvider)
```

---

## ⚙️ Getting Started

### Prerequisites
- **Android Studio**: Ladybug / Iguana or later
- **JDK**: Java 17
- **Android SDK**: API 31 (minimum) up to API 37
- A device or emulator running **Android 12 (API 31)** or higher

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/rashidekbal/ERP-BBSBEC.git
   ```
2. Open the project in Android Studio.
3. Allow Gradle to download dependencies and sync.
4. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
5. Install and launch on your connected device:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔒 Privacy & Security
- **No Third-Party Analytics**: Zero third-party telemetry, trackers, or advertising SDKs.
- **Direct College Communication**: Network traffic is strictly exchanged between the client app and official college ERP servers over HTTPS.
- **Local Credential Storage**: Authentication sessions and encrypted tokens are stored in private Android `SharedPreferences`.

---

## 📄 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
