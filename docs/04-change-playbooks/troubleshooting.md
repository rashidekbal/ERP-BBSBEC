# Change Playbook: Troubleshooting & Diagnostics

This playbook provides root-cause analysis, log signatures, and step-by-step remediation procedures for common failure modes.

---

## 1. HTTP 401 Unauthorized / Token Expiry Loop

### Symptoms
- Pull-to-refresh fails with `HTTP 401 Unauthorized`.
- User is repeatedly prompted to log in or data fails to refresh.

### Root Cause
1. Student changed their BBSBEC ERP password via the web portal, rendering cached credentials invalid.
2. The server invalidated all active JWT tokens due to a semester rollout.
3. Network connection interrupted during synchronous re-login in `NetworkClient.reloginSync()`.

### Diagnostics
Check Logcat for:
```
NetworkClient: Synchronous re-login triggered
NetworkClient: Re-login failed: Invalid credentials
```

### Remediation
1. Verify if `TokenManager.hasSavedCredentials()` returns `true`.
2. If the user changed their ERP password, invoke `TokenManager.clearSession()` and route them to `LoginActivity` to re-enter credentials:
   ```java
   TokenManager.clearSession();
   Intent intent = new Intent(context, LoginActivity.class);
   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
   context.startActivity(intent);
   ```

---

## 2. HTTP 400 Bad Request on Attendance Ingestion

### Symptoms
- Specific subjects fail to load attendance records while others succeed.
- Logcat shows: `Attendance for ID XXX returned: 400`.

### Root Cause
BBSBEC ERP enforces strict query validation on class type (`?subject=101&type=theory`). If a course classified as "Lab" is queried with `type=theory`, the backend returns HTTP 400 Bad Request.

### Diagnostic & Architecture Solution
`ErpSyncManager.java` contains an automatic fallback cascade:
```java
List<String> typesToTry = new ArrayList<>();
if ("lab".equalsIgnoreCase(qType)) {
    typesToTry.add("lab");
    typesToTry.add("theory");
    typesToTry.add("tutorial");
} else { ... }
```
If a subject still fails with 400 across all 3 types:
1. Verify the exact subject ID in `/student/fetchsubjects/`.
2. Inspect whether the subject is an external project or seminar with no scheduled class sessions.

---

## 3. SQLite `IllegalStateException` / Room Migration Mismatch

### Symptoms
Application crashes immediately upon startup with:
```
java.lang.IllegalStateException: Room cannot verify the data integrity. 
Looks like you've changed the schema but forgot to update the version number.
```
or:
```
java.lang.IllegalStateException: A migration from X to Y was required but not found.
```

### Root Cause
A column was added, removed, or modified in a `*Schema.java` entity class without:
1. Incrementing the `version` attribute in `Database.java`.
2. Providing an explicit `Migration` object in `DatabaseMigrations.java`.

### Remediation
1. Inspect the mismatch table in the crash stack trace.
2. Add an explicit migration in `DatabaseMigrations.java`:
   ```java
   public static final Migration MIGRATION_CURRENT_NEXT = new Migration(CURRENT, NEXT) {
       @Override
       public void migrate(@NonNull SupportSQLiteDatabase db) {
           addColumnIfNotExists(db, "tableName", "columnName", "TEXT");
       }
   };
   ```
3. Add the migration to `DatabaseMigrations.ALL_MIGRATIONS`.
4. Increment `version = NEXT` in `Database.java`.
5. Recompile and install.

---

## 4. `SecurityException: Caller not allowed to schedule exact alarms`

### Symptoms
Crash on Android 12+ (API 31+) when entering background or scheduling reminders:
```
java.lang.SecurityException: Caller com.redcodersgroup.erp not allowed to schedule exact alarms
```

### Root Cause
On Android 12+, calling `alarmManager.setExactAndAllowWhileIdle()` requires the system permission `android.permission.SCHEDULE_EXACT_ALARM`. If the user revoked this permission in system settings, direct calls throw `SecurityException`.

### Remediation
Ensure calls route through `NotificationScheduler.java`:
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (alarmManager.canScheduleExactAlarms()) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    } else {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
    }
}
```
Always wrap alarm scheduling blocks in defensive `try-catch (SecurityException ignored)` guards.

---

## 5. Home Screen AppWidget RemoteViews Desynchronization

### Symptoms
The home screen widget displays outdated attendance counts or "No classes today" even after logging attendance inside the app.

### Root Cause
1. Android OS cached the widget's `RemoteViews` to preserve battery.
2. The collection view data in `AttendanceWidgetService` was not invalidated.

### Remediation
Whenever an attendance or subject mutation completes, trigger the two-step invalidation:
```java
AppWidgetManager manager = AppWidgetManager.getInstance(context);
ComponentName provider = new ComponentName(context, AttendanceWidgetProvider.class);
int[] ids = manager.getAppWidgetIds(provider);

// 1. Invalidate collection views
manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_classes_list);

// 2. Force complete widget re-render
for (int id : ids) {
    AttendanceWidgetProvider.updateAppWidget(context, manager, id);
}
```

---

## 6. Pull-to-Refresh Debounce Throttling

### Symptoms
Student pulls down to refresh and receives toast:
`"Please wait 9s before refreshing again."`

### Root Cause
In `ErpSyncManager.java`, `MANUAL_SYNC_COOLDOWN_MS` is set to 10,000 ms (10 seconds) to prevent aggressive swiping from causing denial-of-service on BBSBEC servers.

### Resolution
This is expected defensive behavior. Do not remove this throttle. If rapid testing is required in local debug builds, set `lastManualSyncTime = 0L` in test hooks.
