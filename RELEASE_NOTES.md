# Release v2.5.1 — BBSBEC Attend & ERP Companion

**Tag**: `v2.5.1`  
**Target**: `main`  
**Build**: `versionCode 14` | `versionName 2.5.1`

---

## 🚀 What's New in v2.5.1

### 🔔 Campus Notifications & Alerts System
- **Official ERP Notification Inbox**: Brand-new dedicated notification screen (`NotificationsActivity`) fetching campus broadcasts directly from `/notifications/inbox/`.
- **Category Filter Chips**: Effortlessly switch between `All`, `Academic`, `General`, and `Fees` announcements.
- **Offline Cache & Instant Load**: Local JSON disk storage (`notifications_cache.json`) ensures announcements load instantly, even without an active internet connection.
- **Live Unread Badges**: Real-time unread counter on the Home screen bell icon and navigation sidebar.
- **Background Notification Polling**: Periodic checks even when the app is completely closed or killed (`NotificationCheckReceiver`), posting high-priority status-bar alerts that deep-link directly into the notification screen.
- **Adaptive Alarm & Power Management**: Safe exact-alarm permission handling for Android 12 to 15 (`SCHEDULE_EXACT_ALARM`) with zero crash risks and dedicated guidance in Settings.

---

### 🔄 Intelligent ERP Data Sync & Swipe-to-Refresh
- **4-Hour Smart Resync Interval**: Checks on fresh app launch whether 4 hours have elapsed before performing a full background synchronization.
- **Instant Manual Refresh**: Manual pull-to-refresh on dynamic screens immediately queries live ERP servers.
- **Rapid-Refresh Debouncing**: Prevents server overload and rate-limiting by enforcing a smooth cooldown window on consecutive pull-to-refresh gestures.
- **Startup Optimization**: Moved initial synchronization trigger to `EntryActivity` so data is freshly prepared before arriving at the `HomeActivity`.

---

### 💳 Fees & Payment Management
- **Matching ERP Layout**: Fee structures, installment details, and transaction histories matching the official BBSBEC student portal.
- **Manual Payment Flow**: Added payment confirmation modal and manual checkout flow (no automated API mutations without student interaction).

---

### 📝 Semester Registration Flow
- **Subject Registration**: Complete registration views with subject code, credits, department, and semester criteria.
- **Edit & Add Flow**: Interactive selection for regular and elective subjects mirroring the college ERP flow.

---

### 📬 Campus Webmail Integration
- **Webmail Inbox**: View college student webmail messages.
- **Rich HTML Reader**: Built-in full-screen message reader with formatting and local offline caching.

---

### 🎨 UI & UX Improvements
- **Theme Accents & Dark Mode**: Refined contrast, dynamic color palettes, and typography matching Material 3 guidelines.
- **Widget Sync**: Attendance quick-widgets automatically refresh when timetable or attendance data changes.

---

## 📦 APK Artifacts
- Release APK available in `app/release/app-release.apk`.
