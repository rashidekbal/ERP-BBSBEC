# Developer Playbook: Debugging, Inspection & Failure Simulation

This playbook describes how to inspect runtime state, monitor network and database transactions, and simulate edge failure modes during AI-assisted pair programming.

---

## 1. Logcat Logging Filters & Tags

The application uses standard Android logging (`android.util.Log`) with targeted subsystem tags. In Android Studio Logcat or terminal ADB, filter using:

```bash
adb logcat -v time -s ErpSyncManager:V NetworkClient:V NotificationStorage:V WebmailStorage:V AttendanceScheduleUtil:V
```

### Key Subsystem Tags

| Log Tag | Emitted Events & Inspection Target |
| :--- | :--- |
| `ErpSyncManager` | Launch sync evaluation, 4-hour countdown, 10s swipe cooldown throttles, multi-type attendance fallback retries (`lab` -> `theory` -> `tutorial`), sync completion statistics. |
| `NetworkClient` | OkHttp HTTP/2 connection pooling, request headers (`Authorization: Bearer`), 401 interception, `reloginSync()` execution. |
| `NotificationStorage` | Persistence of circulars to `notifications_cache.json`, JSON deserialization, unread counter recalculations. |
| `WebmailStorage` | Asynchronous disk writing of `webmail_inbox_cache.json`, read status mutation, paging offsets. |
| `AttendanceScheduleUtil` | Multi-period lab weighting, safe-to-bunk formula evaluation, shortage recovery margins. |

---

## 2. Network Payload Inspection (OkHttp)

In `NetworkClient.java`, the logging interceptor is set to `HttpLoggingInterceptor.Level.BODY`:

```java
HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
logging.setLevel(HttpLoggingInterceptor.Level.BODY);
```

When running in debug mode, all requests and responses—including headers, status codes, query strings, and complete JSON bodies—are emitted to Logcat under the tag `okhttp.OkHttpClient`.

To view raw network payloads:
```bash
adb logcat -s okhttp.OkHttpClient:D
```

---

## 3. Database Inspection via Android Studio Database Inspector

While the app is running on an emulator or physical device (debuggable build):
1. In Android Studio, open **View > Tool Windows > App Inspection**.
2. Select the **Database Inspector** tab.
3. Select the active process `com.redcodersgroup.erp`.
4. Double-click `attend_db` to inspect the 9 live SQLite tables.

### Useful SQL Inspection Queries

**Check recorded attendance counts for today:**
```sql
SELECT s.subjectName, a.status, a.hour, a.rawMark, a.isLab 
FROM attendance a 
JOIN subject s ON a.sid = s.sid 
WHERE a.date = date('now') 
ORDER BY a.aid DESC;
```

**Verify subject metadata and ERP mappings:**
```sql
SELECT sid, subjectName, subjectCode, erpSubjectId, isLab, attendance_criteria 
FROM subject;
```

**Check timetable periods assigned for Monday:**
```sql
SELECT d.dayName, s1.subjectName AS P1, s2.subjectName AS P2, s3.subjectName AS P3 
FROM daySchedule ds
JOIN timeTable_day d ON ds.did = d.did
LEFT JOIN subject s1 ON ds.p1 = s1.sid
LEFT JOIN subject s2 ON ds.p2 = s2.sid
LEFT JOIN subject s3 ON ds.p3 = s3.sid
WHERE d.dayName = 'Monday';
```

---

## 4. Setting Strategic Breakpoints

When stepping through code during pair programming, set breakpoints at the following architectural gateways:

1. **Token Refresh Gateway (`NetworkClient.java:55`)**:
   - Inside `tokenAuthenticator.authenticate(Route route, Response response)`.
   - Inspect whether `reloginSync()` succeeds and returns a renewed Bearer token.
2. **Attendance Fallback Engine (`ErpSyncManager.java:235`)**:
   - Inside the loop over `typesToTry` (`lab`, `theory`, `tutorial`).
   - Inspect response status code (e.g. if ERP returns 400 for wrong course type).
3. **Safe-to-Bunk Math Engine (`AttendanceScheduleUtil.java:451` & `AttendanceScheduleUtil.java:483`)**:
   - Inspect `totalAttended`, `totalConducted`, `criteria`, and the resulting `bunkSafe` or `needed` counts.

---

## 5. Simulating Failure Modes & Edge Cases

### 5.1 Expired Session / Tampered Token
**Objective**: Verify that the app recovers seamlessly without logging the student out.
1. In Android Studio **Device File Explorer**, open:
   `/data/data/com.redcodersgroup.erp/shared_prefs/bbsbec_auth_pref.xml`.
2. Edit `access_token` to an invalid string: `INVALID_TOKEN_FOR_TESTING`.
3. Perform a pull-to-refresh on the dashboard.
4. **Expected Behavior**: OkHttp interceptor catches HTTP 401, invokes `reloginSync()` using saved username/password, receives a new token, updates `bbsbec_auth_pref`, and successfully finishes sync.

### 5.2 Network Disconnection / Offline Mode
**Objective**: Ensure zero crashes, zero blank screens, and instant cached rendering.
1. Put device into Airplane Mode.
2. Force stop and restart the app.
3. **Expected Behavior**: Dashboard opens instantly rendering Room database attendance stats. Pull-to-refresh fails gracefully with a non-blocking toast message without deleting cached data.

### 5.3 Rapid Pull-to-Refresh (Debounce Throttle)
**Objective**: Prevent rapid swipe gestures from flooding college ERP servers.
1. Swipe down on `HomeFragment` repeatedly within 3 seconds.
2. **Expected Behavior**: `ErpSyncManager` throttles the request:
   `"Please wait 8s before refreshing again."`

### 5.4 Exact Alarm Permission Revocation (Android 12+)
**Objective**: Verify defensive fallback to `setAndAllowWhileIdle()` without a `SecurityException`.
1. Go to Android **Settings > Apps > BBSBEC Attend > Alarms & reminders**.
2. Toggle "Allow setting alarms" to **Disabled**.
3. Force stop the app and launch.
4. **Expected Behavior**: `NotificationScheduler.scheduleDailyReminder()` detects that `canScheduleExactAlarms()` is false, invokes `setAndAllowWhileIdle()`, and runs without crashing.
