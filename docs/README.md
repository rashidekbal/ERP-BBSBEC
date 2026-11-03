# BBSBEC Attend — Engineering Documentation Hub

Welcome to the **BBSBEC Attend** "Docs-as-Code" architectural and operational documentation suite. This hub provides authoritative technical specifications, contracts, topologies, playbooks, and Architecture Decision Records (ADRs) to support verified, contract-bound engineering and AI-assisted pair programming.

---

## 🏛️ System Overview

**BBSBEC Attend** is an offline-first, native Android academic companion for students of **Baba Banda Singh Bahadur Engineering College (BBSBEC)**. The application integrates directly with institutional ERP endpoints to provide automated attendance tracking, period-weighted bunk safety calculations, midterm marks monitoring, fee breakdowns, course registration, integrated campus webmail, home-screen widgets, and background broadcast alerts.

```
                      +---------------------------------------+
                      |       BBSBEC Institutional ERP        |
                      |   (erp.bbsbec.edu.in / api.bbsbec...) |
                      +-------------------+-------------------+
                                          |
                                          | HTTPS / REST (X-College-Id: 1399)
                                          v
      +=======================================================================+
      |                           Android Runtime                             |
      |                                                                       |
      |   +---------------------+             +---------------------------+   |
      |   |   NetworkClient     | <=========> |     ErpSyncManager        |   |
      |   | (OkHttp + Retrofit) |             | (4h Interval / Debounce)  |   |
      |   +----------+----------+             +-------------+-------------+   |
      |              |                                      |                 |
      |              | TokenManager                         v                 |
      |              v                            +-------------------+       |
      |    [ SharedPreferences ]                  |     Database      |       |
      |   - Session JWTs                          | (Room SQLite v10) |       |
      |   - User Credentials                      +---------+---------+       |
      |   - Setting Toggles                                 |                 |
      |                                                     |                 |
      |        +--------------------+                       |                 |
      |        |   AlarmManager     |                       v                 |
      |        |  (Exact / Idle)    |             +-------------------+       |
      |        +---------+----------+             |  ViewModels & UI  |       |
      |                  |                        |   (MVVM Layer)    |       |
      |                  v                        +---------+---------+       |
      |      +-----------------------+                      |                 |
      |      | Broadcast Receivers   |                      v                 |
      |      | - NotificationCheck   |            +-------------------+       |
      |      | - DailyAlarm          |            | AppWidgetProvider |       |
      |      | - BootReceiver        |            | (Home Widget)     |       |
      |      +-----------------------+            +-------------------+       |
      +=======================================================================+
```

---

## 📚 Documentation Index

### [00. Master Codebase Inventory](CODEBASE_INDEX.md)
Exhaustive file-by-file inventory indexing all 148 source files, their responsibilities, exported types, upstream imports, and downstream callers.

### [01. Architecture](01-architecture/)
- **[System Overview & Topology](01-architecture/overview.md)**: Product goals, high-level topology, technical constraints, library versions, and platform boundaries.
- **[Component Architecture](01-architecture/component-architecture.md)**: Modular breakdown across networking, persistence, alarms, widgets, ViewModels, and UI layers.
- **[Process Isolation & Security](01-architecture/process-isolation.md)**: Process boundaries, threading models, Android security permissions (exact alarm handling), and private storage isolation.
- **[Data Flows & Sequence Diagrams](01-architecture/data-flows.md)**: Mermaid sequence diagrams documenting cold start routing, full sync, background notifications, safe-to-bunk simulation, and widget interactions.

### [02. Contracts & APIs](02-contracts-and-apis/)
- **[API Contracts](02-contracts-and-apis/api-contracts.md)**: Complete REST API signatures for authentication, attendance, timetable, assessments, webmail, and notifications, including headers, query parameters, request/response JSON schemas, and error contracts.
- **[Realtime & Event Protocols](02-contracts-and-apis/realtime-protocol.md)**: Polling architecture, AlarmManager event cycles, and AppWidget IPC broadcast actions.
- **[Persistence & Storage](02-contracts-and-apis/persistence-and-storage.md)**: Room SQLite database schema (version 10), 9 relational entities, migrations V1–V10, file-backed JSON caches, and export schemas.

### [03. Developer Playbook](03-developer-playbook/)
- **[Local Setup Guide](03-developer-playbook/local-setup.md)**: Prerequisites (JDK 17, Android Studio, SDK 31-37), environment variables, build flags, and OS-specific setup.
- **[Debugging & Inspection](03-developer-playbook/debugging-and-inspection.md)**: Logcat logging filters, OkHttp inspection, Database Inspector walkthrough, and failure simulation recipes.
- **[Build & Packaging](03-developer-playbook/build-and-packaging.md)**: Debug vs. Release builds, Proguard/R8 optimization rules, APK/AAB bundling, and backup extraction policies.

### [04. Change Playbooks](04-change-playbooks/)
- **[How to Add a Feature](04-change-playbooks/how-to-add-a-feature.md)**: Step-by-step recipes for adding new ERP endpoints, creating Room entities/migrations, adding settings, and creating custom UI components.
- **[Troubleshooting & Diagnostics](04-change-playbooks/troubleshooting.md)**: Root-cause analysis and recovery steps for common issues (token refresh loops, migration mismatches, exact alarm exceptions, widget desync).

### [05. Architecture Decision Records (ADRs)](05-adrs/)
- **[ADR-001: Core Network Engine & Token Interception](05-adrs/ADR-001-core-engine-choice.md)**: Selection of Retrofit 2 + OkHttp Authenticator for ERP communication.
- **[ADR-002: Native Java MVVM & Room Persistence](05-adrs/ADR-002-state-and-framework.md)**: Decision to maintain Java 17, Android Architecture Components, ViewBinding, and Room SQLite.

---

## 🛠️ Key Architectural Invariants

When modifying or extending this codebase, the following invariants MUST be preserved:

1. **Direct College Communication Only**: Network calls connect solely to `api.bbsbec.edu.in` and `erp.bbsbec.edu.in`. No third-party analytics, crashlytics, or telemetry endpoints may be introduced.
2. **Offline-First Resilience**: The user interface must never block on live network responses. Data is read immediately from Room SQLite and disk JSON caches, with background reconciliation updating UI via LiveData.
3. **Transparent Token Recovery**: 401 Unauthorized responses must trigger silent re-login through OkHttp `Authenticator` (`NetworkClient.reloginSync()`) without terminating user sessions or corrupting local caches.
4. **Non-Destructive Migrations**: The Room database schema version must increment linearly with explicit `Migration` instances added to `DatabaseMigrations.ALL_MIGRATIONS`. Destructive fallback is permitted only on downgrade.
5. **Exact Alarm Defensive Fallback**: When scheduling alarms with `AlarmManager`, code must check `alarmManager.canScheduleExactAlarms()` on Android 12+ (API 31+) and fall back to `setAndAllowWhileIdle()` to prevent `SecurityException` crashes.
