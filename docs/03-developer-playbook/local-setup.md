# Developer Playbook: Local Setup & Environment

This playbook provides exact instructions for configuring a developer workstation to build, run, and test **BBSBEC Attend**.

---

## 1. System Prerequisites

| Component | Minimum Version | Recommended / Tested Version | Notes |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10/11, macOS Sonoma+, Ubuntu 22.04+ | Windows 11 / macOS | Cross-platform Gradle build |
| **Java Development Kit (JDK)**| JDK 17 | Eclipse Temurin 17 or Android Studio Embedded JDK 17 | Required by AGP 9.4.0 |
| **Android Studio** | Android Studio Iguana (2023.2.1)+ | Android Studio Ladybug (2024.2.1)+ | Needed for AGP 9.x compatibility |
| **Android SDK Platform** | API 31 (minSdk) | API 37 (compileSdk / targetSdk) | Install API 31, 33, 34, and 37 via SDK Manager |
| **Build Tools** | `34.0.0` | `35.0.0` or higher | Managed by Gradle |

---

## 2. Environment Variables

Ensure your shell profile (`~/.bashrc`, `~/.zshrc`, or Windows System Environment Variables) defines the following:

### Windows (PowerShell)
```powershell
[System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Android\Android Studio\jbr', 'User')
[System.Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:LOCALAPPDATA\Android\Sdk", 'User')
```

### macOS / Linux
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

---

## 3. Repository Configuration Files

### 3.1 `local.properties`
Located in the project root (must be ignored in `.gitignore`). Verify that `sdk.dir` matches your local Android SDK location:

```properties
sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
```

### 3.2 `gradle.properties`
Default settings configured for modern Gradle execution:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
```

---

## 4. Build & Verification Commands

Execute the following commands from the repository root:

### Step 1: Clean and download dependencies
```bash
# Windows PowerShell
.\gradlew.bat clean --refresh-dependencies

# macOS / Linux
./gradlew clean --refresh-dependencies
```

### Step 2: Compile Debug APK
```bash
# Windows PowerShell
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
The resulting APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Step 3: Run Unit Tests
```bash
.\gradlew.bat test
```

### Step 4: Install directly to connected device / emulator
Ensure a physical device (with USB Debugging enabled) or Android Emulator running API 31+ is connected via `adb devices`:
```bash
.\gradlew.bat installDebug
```

---

## 5. OS-Specific Nuances & Troubleshooting

### Windows PowerShell Execution Policy
If PowerShell blocks the Gradle script with `File gradlew.bat cannot be loaded because running scripts is disabled`, run:
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

### Line Endings (CRLF vs LF)
Git on Windows may check out `.gradlew` shell scripts with Windows CRLF line breaks, causing failures on POSIX CI runners:
```bash
git config core.autocrlf true
```

### Gradle Daemon Resource Leaks
If building hangs on locked SQLite schemas or build cache:
```bash
.\gradlew.bat --stop
```
