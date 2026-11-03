# Developer Playbook: Build, Packaging & Distribution

This playbook defines build configurations, Proguard/R8 optimization rules, APK/AAB artifact generation, and release packaging procedures.

---

## 1. Build Types & Variants

Configured in `app/build.gradle`:

```groovy
buildTypes {
    release {
        minifyEnabled false // Set to true when preparing production store builds
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Build Matrix

| Variant | Minification (R8) | Debuggable | Logging Level | Output Location |
| :--- | :--- | :--- | :--- | :--- |
| **Debug** | Disabled (`minifyEnabled false`) | `true` | Full HTTP bodies (`HttpLoggingInterceptor.Level.BODY`) | `app/build/outputs/apk/debug/app-debug.apk` |
| **Release** | Configurable (`minifyEnabled true/false`) | `false` | Muted / Production | `app/build/outputs/apk/release/app-release-unsigned.apk` |

---

## 2. Resource Packaging Exclusions

To eliminate build collision warnings and reduce APK overhead, `app/build.gradle` excludes duplicate license and metadata descriptors:

```groovy
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "META-INF/DEPENDENCIES"
        excludes += "META-INF/INDEX.LIST"
        excludes += "META-INF/LICENSE"
        excludes += "META-INF/LICENSE.txt"
        excludes += "META-INF/license.txt"
        excludes += "META-INF/NOTICE"
        excludes += "META-INF/NOTICE.txt"
        excludes += "META-INF/notice.txt"
    }
}
```

---

## 3. R8 / ProGuard Optimization Rules

When enabling R8 minification (`minifyEnabled true`) for release builds, ensure the following keep rules are added to `app/proguard-rules.pro` to prevent reflection stripping:

```proguard
# 1. Retrofit 2 & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# 2. Gson Serialization & DTO Models
# Prevents stripping of @SerializedName fields
-keep class com.redcodersgroup.attend_collegeattendancetracker.network.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 3. Room Database & SQLite Entities
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.redcodersgroup.attend_collegeattendancetracker.db.** { *; }

# 4. ViewBinding
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
    public static * bind(android.view.View);
}
```

---

## 4. Release Artifact Generation

### 4.1 Assemble Release APK (Direct Sideloading / Testing)
```bash
# Windows
.\gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```
Artifact generated: `app/build/outputs/apk/release/app-release.apk`

### 4.2 Bundle Release App Bundle (AAB for Google Play Distribution)
```bash
# Windows
.\gradlew.bat bundleRelease

# macOS / Linux
./gradlew bundleRelease
```
Artifact generated: `app/build/outputs/bundle/release/app-release.aab`

---

## 5. Keystore Signing Configuration

For official production builds, configure signing in `app/build.gradle` using environment variables or a local `keystore.properties` (never commit keys to git):

```groovy
signingConfigs {
    release {
        storeFile file(System.getenv("KEYSTORE_PATH") ?: "keystore.jks")
        storePassword System.getenv("KEYSTORE_PASSWORD")
        keyAlias System.getenv("KEY_ALIAS")
        keyPassword System.getenv("KEY_PASSWORD")
    }
}
buildTypes {
    release {
        signingConfig signingConfigs.release
        ...
    }
}
```

---

## 6. Backup & Data Extraction Rules

The application explicitly configures Android 12+ backup extraction manifests:
- **`app/src/main/res/xml/data_extraction_rules.xml`**: Controls automatic cloud backups to Google Drive.
- **`app/src/main/res/xml/backup_rules.xml`**: Controls device-to-device transfers (`adb backup`).
- **Policy**: `bbsbec_auth_pref.xml` is excluded from automatic cloud backups to prevent persisting student session tokens across untrusted restore environments.
