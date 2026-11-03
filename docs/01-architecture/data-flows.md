# Data Flows & Sequence Diagrams

This document illustrates the end-to-end runtime data flows for all primary user journeys using GitHub-flavored Mermaid sequence diagrams.

---

## 1. Cold Start & Session Routing Flow

Triggered when the student launches the app from the Android home screen launcher.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant OS as Android OS
    participant Entry as EntryActivity
    participant Token as TokenManager
    participant Sync as ErpSyncManager
    participant Home as HomeActivity
    participant Login as LoginActivity

    Student->>OS: Tap App Icon
    OS->>Entry: onCreate()
    Entry->>Token: isLoggedIn()
    alt Not Logged In
        Token-->>Entry: false
        Entry->>Login: startActivity()
        Entry->>Entry: finish()
    else Logged In
        Token-->>Entry: true
        Entry->>Sync: syncOnLaunchIfNeeded(callback)
        alt 4 Hours Not Elapsed (Data Fresh)
            Sync-->>Entry: onSuccess() (Instant skip)
            Entry->>Home: startActivity()
            Entry->>Entry: finish()
        else 4 Hours Elapsed or Setup Incomplete
            Sync->>Sync: syncInternal(false, callback)
            Sync-->>Entry: onSuccess() / onError()
            Note over Entry: Max splash timeout: 2500ms
            Entry->>Home: startActivity()
            Entry->>Entry: finish()
        end
    end
```

---

## 2. Student Authentication & Initial ERP Hydration

Occurs when an unauthenticated student submits their BBSBEC email and password.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Login as LoginActivity
    participant NetClient as NetworkClient
    participant ERP_Auth as BBSBEC ERP (erp.bbsbec...)
    participant Token as TokenManager
    participant Prefs as SharedPrefUtil
    participant Sync as ErpSyncManager
    participant Home as HomeActivity

    Student->>Login: Enter Email, Password & Tap "Sign In"
    Login->>Login: Validate Input Fields
    Login->>NetClient: getAuthService().webLogin(url, WebLoginRequest)
    NetClient->>ERP_Auth: POST /authentication/api/login
    ERP_Auth-->>NetClient: 200 OK (WebLoginResponse: token, refresh, profile)
    NetClient-->>Login: onResponse(WebLoginResponse)
    Login->>Token: saveSession(token, refresh, email, password, name)
    Login->>Prefs: setSetupComplete(true)
    Login->>Sync: syncAll(callback)
    Note over Sync: Executes Full DB Ingestion Pipeline
    Sync-->>Login: onSuccess()
    Login->>Home: startActivity(CLEAR_TASK | NEW_TASK)
    Login->>Login: finish()
```

---

## 3. 4-Hour Background & Pull-to-Refresh Sync Engine

The comprehensive data synchronization pipeline orchestrated by `ErpSyncManager`.

```mermaid
sequenceDiagram
    autonumber
    participant UI as HomeFragment / SwipeRefreshLayout
    participant Sync as ErpSyncManager
    participant DBWorker as DbHelper (SingleThreadExecutor)
    participant API as BbsbecApiService (api.bbsbec...)
    participant Room as Database (Room SQLite)
    participant Disk as NotificationStorage (JSON Disk)

    UI->>Sync: syncImmediately(callback)
    Sync->>Sync: Check isSyncing() & Debounce (10s Cooldown)
    Sync->>DBWorker: execute(performFullSync)
    
    rect rgb(240, 248, 255)
        note over DBWorker,API: Phase 1: Enrolled Subjects
        DBWorker->>API: GET /student/fetchsubjects/
        API-->>DBWorker: 200 OK (EnrolledSubjectsResponse)
        DBWorker->>Room: syncEnrolledSubjectsToDb()
    end

    rect rgb(255, 248, 240)
        note over DBWorker,API: Phase 2: Student Profile
        DBWorker->>API: GET /student/profile/
        API-->>DBWorker: 200 OK (ProfileResponse)
        DBWorker->>Room: syncProfileToDb()
    end

    rect rgb(248, 255, 240)
        note over DBWorker,API: Phase 3: Timetable
        DBWorker->>API: GET /student/timetable/
        API-->>DBWorker: 200 OK (TimetableResponse)
        DBWorker->>Room: syncTimetableToDb()
    end

    rect rgb(255, 240, 245)
        note over DBWorker,API: Phase 4: Class Attendance per Subject
        loop For each enrolled subject
            DBWorker->>API: GET /student/class-attendance/?subject=ID&type=lab/theory
            alt Successful response
                API-->>DBWorker: 200 OK (AttendanceResponse)
                DBWorker->>Room: syncSingleSubjectAttendanceToDb()
            else Type mismatch / 400
                DBWorker->>API: Retry with alternate type (theory/tutorial/lab)
                API-->>DBWorker: 200 OK (AttendanceResponse)
                DBWorker->>Room: syncSingleSubjectAttendanceToDb()
            end
        end
    end

    rect rgb(245, 240, 255)
        note over DBWorker,API: Phase 5: Assessments & Marks
        DBWorker->>API: GET /student/assessments/
        API-->>DBWorker: 200 OK (AssessmentsResponse)
        DBWorker->>Room: syncAssessmentsToDb()
    end

    rect rgb(240, 255, 255)
        note over DBWorker,API: Phase 6: Circulars & Announcements
        DBWorker->>API: GET /notifications/inbox/
        API-->>DBWorker: 200 OK (NotificationsInboxResponse)
        DBWorker->>Disk: saveInbox(data)
    end

    DBWorker->>Sync: Update lastFullSyncTime
    Sync-->>UI: post(callback.onSuccess())
    UI->>UI: setRefreshing(false) & Refresh LiveData
```

---

## 4. Safe-to-Bunk Attendance Simulation Journey

How `SubjectInfoViewModel` and `AttendanceScheduleUtil` compute real-time margins and forecast consequences when the student experiments with what-if scenarios.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant View as SubjectInfoActivity
    participant VM as SubjectInfoViewModel
    participant Util as AttendanceScheduleUtil
    participant DB as AttendanceDao / SubjectDao

    Student->>View: Open Subject Detail Screen
    View->>VM: init(subjectId)
    VM->>DB: Query subject & attendance records
    DB-->>VM: SubjectSchema, List<AttendanceSchema>
    VM->>Util: calculateOverallAttendance(...)
    Util-->>VM: stats (attended, conducted, pct)
    VM->>Util: calculateHeroRiskSummary(...)
    Util-->>VM: HeroRiskSummary (needed, bunkSafe, status)
    VM-->>View: LiveData update (bunkSafeStatus, overallStats)
    
    Student->>View: Tap "+ Bunk" Simulation Button
    View->>View: Increment simBunk counter
    View->>View: Calculate projected: (attended) / (conducted + simAttend + simBunk)
    View->>View: Re-evaluate bunk margin vs criteria (75%)
    View->>View: Update projection UI badge & change indicator
```

---

## 5. Periodic Background Announcement Alert Flow

Executes automatically in the background even if the application process has been terminated.

```mermaid
sequenceDiagram
    autonumber
    participant Alarm as AlarmManager (System)
    participant Receiver as NotificationCheckReceiver
    participant API as BbsbecApiService
    participant Storage as NotificationStorage
    participant Prefs as SharedPrefUtil
    participant NotifHelper as NotificationHelper
    participant Status as Android Status Bar

    Alarm->>Receiver: onReceive(context, intent)
    Note over Receiver: Wakes up via RTC_WAKEUP every 30 mins
    Receiver->>API: getNotificationsInbox().execute()
    API-->>Receiver: 200 OK (NotificationsInboxResponse)
    Receiver->>Storage: saveInbox(data)
    Receiver->>Prefs: getNotifiedAnnouncementIds()
    loop For each item in results
        alt item.isRead() == false AND id not in notifiedIds
            Receiver->>NotifHelper: showCampusAlertNotification(item)
            NotifHelper->>Status: post(NOTIFICATION_ID_CAMPUS_BASE + id)
            Receiver->>Prefs: markAnnouncementAsNotified(id)
        end
    end
    Receiver->>Alarm: Reschedule next check in 30 minutes
```

---

## 6. Home Screen App Widget Action & RemoteViews Flow

Executes when the student taps an action button directly on the Android home screen launcher widget.

```mermaid
sequenceDiagram
    autonumber
    actor Student
    participant Launcher as Android Home Launcher
    participant Provider as AttendanceWidgetProvider
    participant DBWorker as DbHelper
    participant Room as AttendanceDao
    participant AppWidgetMgr as AppWidgetManager

    Student->>Launcher: Tap "Present" on Today's Class item
    Launcher->>Provider: onReceive(ACTION_MARK_ATTENDANCE, extra_sid, extra_is_lab)
    Provider->>DBWorker: execute(toggleAttendance)
    DBWorker->>Room: insert(AttendanceSchema: date=today, status="present")
    DBWorker->>Provider: Handler.post(updateWidget)
    Provider->>AppWidgetMgr: notifyAppWidgetViewDataChanged()
    Provider->>Provider: updateAppWidget(context, appWidgetManager, id)
    Provider->>Launcher: Update RemoteViews (Percentage, Schedule, Bunk-safe badge)
```
