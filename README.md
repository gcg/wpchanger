# 🖼️ Wallpaper Changer (Android)

> A modern, lean Android application written in **Kotlin** and **Jetpack Compose (Material 3)** that automatically shuffles and updates your device wallpaper at customizable timer intervals from a curated pool of photos or full albums.

---

## ✨ Features

- **Standard System Photo Selector**: Pick multiple individual photos using Android's native Photo Picker (`ActivityResultContracts.PickMultipleVisualMedia`).
- **Album / Folder Selector**: Pick entire photo albums or directory trees using the Storage Access Framework (`ActivityResultContracts.OpenDocumentTree`).
- **Customizable Timer Intervals**:
  - `30 minutes`
  - `1 hour`
  - `3 hours`
  - `6 hours`
  - `Daily` (24 hours)
- **Target Screen Selector**: Apply wallpapers to **Home Screen & Lock Screen**, **Home Screen Only**, or **Lock Screen Only**.
- **Smart Randomizer**: Selects a random image from your wallpaper pool while guaranteeing you never get the exact same image twice in a row when multiple photos are available.
- **Reliable Background Execution**: Powered by AndroidX **WorkManager** (`PeriodicWorkRequest`) and survives device restarts (`RECEIVE_BOOT_COMPLETED`).
- **Safe & Optimized Image Handling**: Copies selected photos into private internal storage (`filesDir/wallpapers/`) to avoid permission expiration, with memory-safe downsampling and EXIF rotation correction.
- **Instant Shuffle**: "Change Wallpaper Now" button for immediate manual changes and visual previews.
- **Modern Material 3 UI**: Full Edge-to-Edge support, dynamic color theming (Material You), thumbnail previews, active badge indicators, and confirmation dialogs.

---

## 🛠️ Tech Stack & Requirements

- **Language**: Kotlin 2.0+
- **JDK**: Java 21
- **Target Android Versions**: Android 14 (API 34) & Android 15 (API 35)
- **UI Framework**: Jetpack Compose + Material 3 (Compose BOM `2024.09.02`)
- **Background Tasks**: AndroidX WorkManager KTX (`2.9.1`)
- **Persistence**: AndroidX DataStore Preferences (`1.1.1`)
- **Image Loading**: Coil Compose (`2.7.0`)
- **Code Quality & Linting**: Spotless (`6.25.0`) with `ktlint` + Android Lint

---

## 🚀 Make Commands

A comprehensive [`Makefile`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/Makefile) is provided for rapid development workflows:

| Command | Description |
| :--- | :--- |
| `make help` | Displays list of all available make targets |
| `make open` | Opens the project directly in Android Studio |
| `make run` | Detects/starts an Android emulator, builds the APK, installs it, and launches the app |
| `make build` | Compiles and builds the debug APK (`./gradlew assembleDebug`) |
| `make build-release` | Compiles and builds the release APK (`./gradlew assembleRelease`) |
| `make install` | Installs the debug APK onto a connected device or emulator (`./gradlew installDebug`) |
| `make test` | Runs the unit test suite (`./gradlew test`) |
| `make format` | Formats all Kotlin code and Gradle scripts with Spotless (`./gradlew spotlessApply`) |
| `make lint` | Runs Spotless format checks and Android Lint (`./gradlew spotlessCheck lintDebug`) |
| `make check` | Runs formatter check, linter, and unit tests (Best practices CI check) |
| `make clean` | Cleans build artifacts and caches (`./gradlew clean`) |

---

## 📂 Project Structure & File Directory

Below is the complete list of all project files, detailing their contents and purpose:

### Root Files & Build Configuration
- [`Makefile`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/Makefile): Automates common tasks such as `make open` (launch in Android Studio), `make run` (boot emulator & launch app), `make format`, `make lint`, `make test`, and `make build`.
- [`AGENTS.md`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/AGENTS.md): Developer and AI agent rules specifying code conventions, formatting standards, and mandatory verification steps.
- [`README.md`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/README.md): Primary project documentation, architecture overview, and file directory guide.
- [`.gitignore`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/.gitignore): Excludes build artifacts, caches, IDE files, and local SDK properties from version control.
- [`settings.gradle.kts`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/settings.gradle.kts): Declares plugin repositories, dependency repositories, project root name, and subproject modules (`:app`).
- [`build.gradle.kts`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/build.gradle.kts): Root Gradle configuration declaring Android Application, Kotlin Android, Kotlin Compose, and Spotless plugins.
- [`gradle.properties`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/gradle.properties): JVM memory parameters, AndroidX flags, and Kotlin compilation settings.
- [`local.properties`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/local.properties): Specifies the local machine's Android SDK installation path (`sdk.dir`).
- [`gradle/libs.versions.toml`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/gradle/libs.versions.toml): Gradle Version Catalog defining versions, libraries, and plugins across the project.
- [`gradlew`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/gradlew) / [`gradlew.bat`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/gradlew.bat): Unix and Windows Gradle Wrapper scripts for consistent, reproducible builds without requiring a pre-installed Gradle.
- [`gradle/wrapper/gradle-wrapper.properties`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/gradle/wrapper/gradle-wrapper.properties): Specifies Gradle 8.7 distribution download URL and checksum.

---

### App Module (`app/`)

- [`app/build.gradle.kts`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/build.gradle.kts): App module build script defining `minSdk = 34`, `targetSdk = 35`, Kotlin 21 target, Jetpack Compose configuration, Spotless rules with `ktlint`, and project dependencies.
- [`app/proguard-rules.pro`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/proguard-rules.pro): ProGuard / R8 code shrinking rules for release builds.

#### Android Manifest & Resources
- [`app/src/main/AndroidManifest.xml`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/AndroidManifest.xml): Declares application permissions (`SET_WALLPAPER`, `RECEIVE_BOOT_COMPLETED`), custom `Application` class, and `MainActivity` launcher filter.
- [`app/src/main/res/values/strings.xml`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/res/values/strings.xml): App string resources (`app_name`, `app_tagline`).
- [`app/src/main/res/values/themes.xml`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/res/values/themes.xml): Base XML theme definition delegating to Compose.
- [`app/src/main/res/values/colors.xml`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/res/values/colors.xml): Color resources for adaptive launcher icon background.
- [`app/src/main/res/drawable/ic_launcher_foreground.xml`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/res/drawable/ic_launcher_foreground.xml): Vector drawable for the app launcher icon foreground.
- [`app/src/main/res/mipmap-*/`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml): Adaptive launcher icons and square/round mipmap definitions.

#### Application & Presentation Layer (`com.gcg.wpchanger`)
- [`WPChangerApp.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/WPChangerApp.kt): `Application` subclass that checks user preferences on boot/startup and ensures periodic wallpaper rotation is active.
- [`MainActivity.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/MainActivity.kt): Single Activity with `enableEdgeToEdge()`, registering `ActivityResultContracts.PickMultipleVisualMedia` and `ActivityResultContracts.OpenDocumentTree` photo launchers, and rendering `HomeScreen`.

#### UI Components & Theming (`com.gcg.wpchanger.ui`)
- [`HomeScreen.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/ui/HomeScreen.kt): Main Jetpack Compose screen featuring:
  - Header with live status pill (Active / Paused).
  - Hero Control Card with auto-rotation switch and "Change Wallpaper Now" shuffle button.
  - Timer Interval selector chips (`30 min`, `1 hour`, `3 hours`, `6 hours`, `Daily`).
  - Target Screen selector chips (`Home & Lock`, `Home Screen`, `Lock Screen`).
  - Wallpaper Pool header with image count and "Clear All" confirmation dialog.
  - Empty state illustration when pool is empty.
  - Adaptive 3-column image grid with delete buttons, active badges, and full-screen preview dialog.
- [`WallpaperViewModel.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/ui/WallpaperViewModel.kt): Android ViewModel combining DataStore settings and repository state into a reactive `StateFlow<WallpaperUiState>`. Exposes functions for importing photos/folders, deleting items, toggling rotation, and instant wallpaper changes.
- [`ui/theme/Theme.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/ui/theme/Theme.kt): Material 3 Theme composable with Dynamic Color support for Android 14/15.
- [`ui/theme/Color.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/ui/theme/Color.kt): Light & Dark color schemes and brand accents.
- [`ui/theme/Type.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/ui/theme/Type.kt): Material 3 Typography definitions.

#### Data & Storage Layer (`com.gcg.wpchanger.data`)
- [`TimerInterval.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/data/TimerInterval.kt): Enum representing supported intervals (`MINUTES_30`, `HOURS_1`, `HOURS_3`, `HOURS_6`, `DAILY`) with labels, durations, and `TimeUnit`s.
- [`WallpaperTarget.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/data/WallpaperTarget.kt): Enum representing target screens (`BOTH`, `HOME`, `LOCK`) mapping directly to `WallpaperManager` flags.
- [`WallpaperItem.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/data/WallpaperItem.kt): Data model representing an imported wallpaper image (id, file, name, sizeBytes, addedTimestamp, uri).
- [`WallpaperPreferences.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/data/WallpaperPreferences.kt): DataStore repository managing persistent user preferences (`IS_ACTIVE`, `INTERVAL`, `TARGET`, `LAST_CHANGED_TIMESTAMP`, `LAST_WALLPAPER_ID`, `LAST_WALLPAPER_NAME`).
- [`WallpaperRepository.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/data/WallpaperRepository.kt): Manages the internal `filesDir/wallpapers/` directory, handles single/multi-URI imports, scans and imports folder trees, handles deletions, and implements smart random photo selection.
- [`WallpaperManagerHelper.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/data/WallpaperManagerHelper.kt): Utility for decoding images with memory-safe `inSampleSize`, correcting EXIF orientation, and calling `WallpaperManager.getInstance(context).setBitmap()`.

#### Background Workers (`com.gcg.wpchanger.worker`)
- [`WallpaperChangeWorker.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/worker/WallpaperChangeWorker.kt): AndroidX `CoroutineWorker` that reads repository and preferences, selects the next random wallpaper, applies it to the system/lock screen, and updates rotation history.
- [`WorkManagerScheduler.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/main/java/com/gcg/wpchanger/worker/WorkManagerScheduler.kt): Helper object for scheduling `PeriodicWorkRequest`, cancelling unique work, and triggering immediate one-time work for testing.

#### Unit Tests (`app/src/test/java/com/gcg/wpchanger`)
- [`TimerIntervalTest.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/test/java/com/gcg/wpchanger/TimerIntervalTest.kt): Unit tests verifying interval durations, time units, and string parsing/fallback logic.
- [`WallpaperTargetTest.kt`](file:///Users/gcg/Work/src/github.com/gcg/wpchanger/app/src/test/java/com/gcg/wpchanger/WallpaperTargetTest.kt): Unit tests verifying target enum flags and name resolution.

---

## 🧪 Quality Verification

Run all checks in a single step:
```bash
make check
```
This formats code, runs Android Lint, and executes all unit tests to guarantee clean, robust code at all times.
