# Change Playbook: How to Add a Feature

This playbook provides verified, step-by-step recipes for extending **BBSBEC Attend**. Follow each recipe strictly to ensure contract integrity and regression-free development.

---

## Recipe 1: Adding a New ERP API Endpoint

**Scenario**: You need to ingest a new endpoint from the BBSBEC ERP (e.g. `GET /student/hostel-allotment/`).

### Step 1: Create the Network Model (DTO)
In `com.redcodersgroup.attend_collegeattendancetracker.network.model`, create `HostelAllotmentResponse.java`:
```java
package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.annotations.SerializedName;

public class HostelAllotmentResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private Data data;

    public int getStatus() { return status; }
    public Data getData() { return data; }

    public static class Data {
        @SerializedName("hostel_name")
        private String hostelName;

        @SerializedName("room_number")
        private String roomNumber;

        public String getHostelName() { return hostelName; }
        public String getRoomNumber() { return roomNumber; }
    }
}
```

### Step 2: Declare the Endpoint in Retrofit Service
In `BbsbecApiService.java`, add the call signature:
```java
@GET("student/hostel-allotment/")
Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.HostelAllotmentResponse> getHostelAllotment();
```

### Step 3: Wire into `ErpSyncManager` (or Screen Controller)
If this data should be refreshed during the 4-hour sync cycle, add a phase in `ErpSyncManager.java` inside `performFullSync(...)`:
```java
try {
    Response<HostelAllotmentResponse> resp = api.getHostelAllotment().execute();
    if (resp.isSuccessful() && resp.body() != null && resp.body().getData() != null) {
        // Persist to Room or SharedPreferences
        Log.d(TAG, "Hostel allotment synced");
    }
} catch (Exception e) {
    Log.w(TAG, "Hostel allotment sync skipped: " + e.getMessage());
}
```

### Step 4: Verification
1. Run `./gradlew assembleDebug`.
2. Trigger sync in app and verify Logcat output: `adb logcat -s ErpSyncManager:D`.

---

## Recipe 2: Adding a New Field to Room SQLite Database

**Scenario**: You need to add a new column `mentorName` to the `subject` table.

### Step 1: Update the Entity Schema
In `SubjectSchema.java`:
1. Add the `@ColumnInfo` field:
   ```java
   @ColumnInfo(name = "mentorName")
   private String mentorName;
   ```
2. Add getter and setter:
   ```java
   public String getMentorName() { return mentorName; }
   public void setMentorName(String mentorName) { this.mentorName = mentorName; }
   ```

### Step 2: Define the Migration Script
In `DatabaseMigrations.java`:
1. Define `MIGRATION_10_11`:
   ```java
   public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
       @Override
       public void migrate(@NonNull SupportSQLiteDatabase database) {
           addColumnIfNotExists(database, "subject", "mentorName", "TEXT");
       }
   };
   ```
2. Add `MIGRATION_10_11` to `ALL_MIGRATIONS` array:
   ```java
   public static final Migration[] ALL_MIGRATIONS = new Migration[]{
       ...,
       MIGRATION_9_10,
       MIGRATION_10_11
   };
   ```

### Step 3: Increment Database Version
In `Database.java`:
Change the version from `10` to `11`:
```java
@androidx.room.Database(entities = { ... }, version = 11)
public abstract class Database extends RoomDatabase { ... }
```

### Step 4: Verification
1. Install an existing app build (version 10).
2. Install the new build without uninstalling.
3. Verify that Room runs the migration cleanly without throwing `IllegalStateException: A migration from 10 to 11 was required but not found`.

---

## Recipe 3: Adding a New User Setting Toggle

**Scenario**: Adding a setting to toggle "Vibrate on Announcements".

### Step 1: Add the Preference Key
In `com.redcodersgroup.attend_collegeattendancetracker.constants.SharedPrefKeys`:
```java
public enum SharedPrefKeys {
    ...,
    VIBRATE_ON_ANNOUNCEMENT
}
```

### Step 2: Add Accessors in `SharedPrefUtil`
In `com.redcodersgroup.attend_collegeattendancetracker.utils.SharedPrefUtil`:
```java
public static boolean isVibrateOnAnnouncementEnabled() {
    return SharedPref.getSharedPrefs().getBoolean(
        SharedPrefKeys.VIBRATE_ON_ANNOUNCEMENT.toString(), 
        true
    );
}

public static void setVibrateOnAnnouncementEnabled(boolean enabled) {
    SharedPref.getSharedPrefs().edit()
        .putBoolean(SharedPrefKeys.VIBRATE_ON_ANNOUNCEMENT.toString(), enabled)
        .apply();
}
```

### Step 3: Bind to Setting Activity Layout
In `screens/setting/SettingActivity.java`:
1. Initialize the switch state:
   ```java
   binding.vibrateSwitch.setChecked(SharedPrefUtil.isVibrateOnAnnouncementEnabled());
   ```
2. Listen for change events:
   ```java
   binding.vibrateSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
       SharedPrefUtil.setVibrateOnAnnouncementEnabled(isChecked);
   });
   ```

### Step 4: Verification
Toggle switch in Settings, restart the app, and verify that the switch retains its position.

---

## Recipe 4: Updating Safe-to-Bunk Mathematical Thresholds

**Scenario**: Adjusting attendance margin calculations for a departmental requirement.

### Source File: `app/src/main/java/com/redcodersgroup/attend_collegeattendancetracker/utils/AttendanceScheduleUtil.java`
Inspect method:
```java
public static HeroRiskSummary calculateHeroRiskSummary(
    int totalAttended, int totalConducted, int totalRemaining, int criteria, boolean hasSemEnd
)
```

1. Shortage calculation formula:
   $$\text{needed} = \left\lceil \frac{\text{criteria} \times \text{conducted} - 100 \times \text{attended}}{100 - \text{criteria}} \right\rceil$$
2. Safe bunk calculation formula:
   $$\text{bunkSafe} = \left\lfloor \frac{100 \times \text{attended} - \text{criteria} \times \text{conducted}}{\text{criteria}} \right\rfloor$$
3. Update unit tests in `app/src/test/` to verify boundary edge cases (0 conducted classes, 100% attendance, exact 75% boundary).
