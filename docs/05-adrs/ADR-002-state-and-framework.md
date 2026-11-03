# ADR-002: Native Java MVVM & Room Persistence Architecture

- **Status**: Accepted
- **Deciders**: Lead Software Architect, Engineering Team
- **Date**: 2026-11-03

---

## 1. Context & Problem Statement

When structuring the core architecture and UI framework for **BBSBEC Attend**, several paradigms were evaluated:
1. Complete rewrite in Kotlin with Jetpack Compose.
2. Cross-platform framework (Flutter or React Native).
3. Legacy Android imperative design (raw `SQLiteOpenHelper`, `findViewById`, `AsyncTask`).
4. Modern Native Java 17 with Android Architecture Components (`ViewModel`, `LiveData`, `ViewBinding`, and `Room`).

The app requires low-latency startup, strict battery conservation, 100% offline-first reliability, background alarm scheduling across Android 12–15, and native home screen widgets.

---

## 2. Decision Drivers

- **Zero-Glitch Performance**: Rapid startup and fluid 60fps list scrolling across budget student smartphones.
- **Deep Android OS Subsystem Integration**: Seamless compatibility with `AppWidgetProvider`, `RemoteViewsService`, `AlarmManager`, and `BroadcastReceiver`.
- **Compile-Time Safety**: Elimination of runtime `NullPointerException`s from view lookups and type errors in database queries.
- **Offline Integrity**: Strict schema versioning, ACID transactions, and non-destructive migrations.
- **Architectural Simplicity (YAGNI)**: Minimal abstraction layers, clean separation of concerns, and direct control over memory and background execution.

---

## 3. Considered Options

### Option A: Cross-Platform (Flutter / React Native)
- **Drawbacks**: Adds 15–30MB of engine overhead; introduces heavy IPC bridges for Android Home Widgets (`AppWidgetProvider`) and background alarm scheduling; high memory usage on entry-level Android devices.

### Option B: Jetpack Compose + Kotlin Multiplatform
- **Drawbacks**: Requires complete rewrite of 148 source files; larger APK footprint from Compose compiler runtimes; complex interop required for legacy XML `RemoteViews` utilized by Android App Widgets.

### Option C: Legacy Android (`SQLiteOpenHelper`, `findViewById`)
- **Drawbacks**: High vulnerability to runtime view-casting crashes; manual SQL string concatenation prone to syntax errors; lack of lifecycle awareness leading to memory leaks and activity orientation crashes.

### Option D: Native Java 17 + MVVM + Room + ViewBinding (Chosen)
- **Approach**: Maintain modern Java 17 codebase utilizing Android Architecture Components (`ViewModel`, `LiveData`), compile-time generated `ViewBinding`, and `Room Database` (SQLite ORM).

---

## 4. Decision Outcome

We chose **Option D: Native Java 17 + MVVM + Room + ViewBinding**.

### Key Architectural Characteristics
1. **ViewBinding**: Replaces `findViewById` with type-safe, null-safe binding classes generated at compile time.
2. **ViewModel & LiveData**: Encapsulates UI state and business logic, surviving configuration changes and orientation pivots without re-fetching network data.
3. **Room Database**: Enforces schema validation at compile time, produces type-safe DAOs, and supports defensive SQLite migrations (`DatabaseMigrations.java`).
4. **Dedicated Thread Execution (`DbHelper`)**: A single-threaded `ExecutorService` serializes database writes, preventing SQLite locking contention.

---

## 5. Consequences & Trade-offs

### Positive Consequences
- **Instantaneous Cold Start**: Startup time remains under 800ms on modern devices.
- **Deterministic Widget Synchronization**: Native `AppWidgetProvider` directly accesses `Database.getInstance()` without cross-runtime serialization bridges.
- **Reliable Offline-First Experience**: Changes reflect immediately in local SQLite, and UI observes LiveData updates reactively.

### Negative / Mitigated Consequences
- **Java Verbosity**: Requires getters, setters, and explicit builders (mitigated by modern Java 17 features like records/lambdas and clear naming conventions).
- **Manual Migration Authoring**: Database schema changes require manual `Migration` definitions (mitigated by defensive migration helpers like `addColumnIfNotExists`).
