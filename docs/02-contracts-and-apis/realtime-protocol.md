# Realtime Protocol, Event Streams & Message Delivery

## 1. Architectural Reality Assessment

The **BBSBEC Institutional ERP** is a traditional Python/Django enterprise system exposed over HTTPS. It **does not** provide WebSocket connections, Server-Sent Events (SSE), or Webhook callbacks, nor does the application bundle Firebase Cloud Messaging (FCM) or foreign push relays (in adherence to the zero-external-telemetry privacy mandate).

To provide real-time updates and proactive notifications to students, the application employs a **Synthetic Real-Time Delivery Model** combining:
1. Low-power, idle-tolerant background polling (`AlarmManager`).
2. Inter-Process Communication (IPC) via Android `BroadcastReceiver` pipelines.
3. Intra-process reactive state streaming (`LiveData`).

```
+-----------------------------------------------------------------------------------+
|                           REAL-TIME & EVENT PIPELINES                             |
|                                                                                   |
|  [ Event 1: Background Announcement Polling (Every 30 Mins) ]                    |
|  AlarmManager (RTC_WAKEUP) ===> NotificationCheckReceiver ===> Retrofit GET ===>  |
|  NotificationStorage (Disk Cache) ===> NotificationHelper (Status Bar Alert)      |
|                                                                                   |
|  [ Event 2: Morning Academic Schedule Alert (8:00 AM Daily) ]                     |
|  AlarmManager (RTC_WAKEUP) ===> DailyAlarmReceiver ===> HolidayDao Check ===>     |
|  Attendance Reminder Alert                                                        |
|                                                                                   |
|  [ Event 3: AppWidget IPC Broadcasts ]                                            |
|  Home Widget RemoteViews ===> PendingIntent Broadcast ===>                       |
|  AttendanceWidgetProvider ===> DbHelper Worker ===> Room Update ===>              |
|  AppWidgetManager.notifyAppWidgetViewDataChanged()                                |
|                                                                                   |
|  [ Event 4: Intra-Process State Updates ]                                         |
|  DbHelper / ErpSyncManager ===> MutableLiveData.postValue() ===>                  |
|  ViewBinding UI Rerender (Home, Attendance, Marks, Tasks)                         |
+-----------------------------------------------------------------------------------+
```

---

## 2. Background Polling Protocol

### 2.1 Announcement Check Protocol
- **Trigger**: `AlarmManager.setAndAllowWhileIdle()` configured for `triggerAt = System.currentTimeMillis() + 1800000L` (30 minutes).
- **Target Receiver**: `NotificationCheckReceiver.class`.
- **Request Intent**:
  - Request Code: `NOTIF_CHECK_REQUEST_CODE` (`5002`)
  - Flags: `PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`
- **Execution Payload**:
  1. Synchronous HTTP execution: `BbsbecApiService.getNotificationsInbox().execute()`.
  2. Cache serialization: `NotificationStorage.getInstance(context).saveInbox(data)`.
  3. De-duplication check: Compares item ID against `SharedPrefUtil.getNotifiedAnnouncementIds()`.
  4. Status Bar Alert: Dispatches Android notification to channel `campus_alerts_channel` for unread items.
  5. Chain Reschedule: Rearms the alarm for another 30-minute interval before exiting.

### 2.2 Morning Daily Reminder Protocol
- **Trigger**: Daily at 8:00:00 AM local time.
- **Target Receiver**: `DailyAlarmReceiver.class`.
- **Request Intent**:
  - Request Code: `ALARM_REQUEST_CODE` (`5001`)
  - Flags: `PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE`
- **Execution Payload**:
  1. Checks if `SharedPrefUtil.isAttendanceReminderEnabled()` is true.
  2. Verifies current date against `HolidayDao.getHolidayList()`.
  3. If a college holiday is active, posts a "Campus Holiday: [Holiday Name]" notice and exits.
  4. If regular academic day, builds and fires notification: *"Remember to log your class attendance for today."*
  5. Advances calendar by 1 day and reschedules for tomorrow 8:00 AM.

### 2.3 Reboot Recovery Protocol
- **Trigger**: System broadcasts:
  - `android.intent.action.BOOT_COMPLETED`
  - `android.intent.action.MY_PACKAGE_REPLACED`
  - `android.intent.action.QUICKBOOT_POWERON`
- **Target Receiver**: `BootReceiver.class` (`android:exported="true"`).
- **Action**: Instantly re-arms both `scheduleDailyReminder(context)` and `scheduleNotificationCheck(context)` to ensure continuous operation across device restarts.

---

## 3. AppWidget IPC Action Protocol

The Android home screen widget communicates with the main application sandbox through broadcast intents carrying explicit actions and typed extras:

### Actions Specification

| Intent Action | Extras Contract | Processing Logic |
| :--- | :--- | :--- |
| `ACTION_MARK_ATTENDANCE` | `extra_sid` (int)<br>`extra_is_lab` (bool)<br>`extra_target_status` (string)<br>`extra_current_status` (string) | Asynchronously updates attendance table in Room for the given subject ID on today's date. Toggles between "present", "absent", and cleared states. |
| `ACTION_MARK_ALL_PRESENT` | *None* | Iterates through all subjects scheduled in today's timetable (`DayScheduleSchema`) and inserts "present" records into `attendance`. |
| `ACTION_REFRESH_WIDGET` | *None* | Queries latest attendance tallies from `AttendanceDao`, recalculates safe-to-bunk metrics, and triggers `appWidgetManager.updateAppWidget()`. |

---

## 4. Intra-Process Reactive State Streams

Data binding inside the user interface uses **Lifecycle-aware LiveData**:

- **`HomeViewModel`**:
  - `overallPercentage`: `LiveData<Integer>`
  - `bunkStatus`: `LiveData<String>`
  - `todayClasses`: `LiveData<List<TodayClassModel>>`
  - `isSyncing`: `LiveData<Boolean>`
- **`SubjectInfoViewModel`**:
  - `attendanceStats`: `LiveData<AttendanceScheduleUtil.AttendanceStats>`
  - `bunkSafeStatus`: `LiveData<String>`
  - `calendarDays`: `LiveData<List<CalendarDayModel>>`
  - `attendanceHistory`: `LiveData<List<AttendanceSchema>>`
- **`MarksViewModel`**:
  - `marksSummary`: `LiveData<List<MarksSummaryModel>>`
  - `highestMarkSubject`: `LiveData<String>`
  - `lowestMarkSubject`: `LiveData<String>`

When asynchronous background workers complete database writes, calling `.postValue()` immediately notifies active UI observers on the Main Looper, ensuring 60fps glitch-free transitions.
