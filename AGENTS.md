# Guidelines for AI Agents & Developers

This document defines the quality standards, architectural conventions, and mandatory workflows for developing and maintaining the **Wallpaper Changer** codebase.

---

## 1. Quality & Verification Rules

After making **any** changes to the codebase, you **must** run the quality verification suite:

```bash
make check
```

This runs:
1. `make format` (`./gradlew spotlessApply`): Formats all Kotlin files and Gradle scripts using Spotless + ktlint.
2. `make lint` (`./gradlew spotlessCheck lintDebug`): Validates code style and executes Android Lint checks.
3. `make test` (`./gradlew test`): Executes unit tests for both debug and release variants.

If only formatting or linting is needed:
- Format code: `make format`
- Check lint & style: `make lint`
- Run unit tests: `make test`
- Build debug APK: `make build`

---

## 2. Architecture & Design Principles

- **Target SDK**: Android 16 (`targetSdk = 36`, `compileSdk = 36`), the minimum Google Play accepts for new apps and updates since Aug 31, 2026.
- **Minimum SDK**: Android 14 (`minSdk = 34`). Only the last 2 major Android releases are supported.
- **UI Framework**: Modern Jetpack Compose with Material 3 and dynamic color theming.
  - Composable functions must be decorated with `@Composable`.
  - Prefer `collectAsStateWithLifecycle()` when observing Kotlin `StateFlow` from Compose UI.
  - Keep Composables stateless where appropriate; hoist state into `WallpaperViewModel`.
- **Background Tasks**:
  - Always use AndroidX `WorkManager` (`CoroutineWorker`, `PeriodicWorkRequestBuilder`).
  - Never use raw background `Thread.sleep()` or foreground services unless strictly necessary.
- **Storage & Permissions**:
  - Store user wallpaper photos inside app-private internal storage (`context.filesDir/wallpapers/`) to ensure background reliability without URI permission expiration.
  - Store application settings in AndroidX `DataStore<Preferences>`.
- **Google Play**:
  - Store listing text and graphics live in `fastlane/metadata/android/en-US/`; release steps are in `docs/PLAY_STORE.md`.
  - Never commit `keystore.properties` or `*.jks` files.
  - Don't add permissions Play restricts (e.g. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) or `INTERNET` without updating `PRIVACY.md` and the Data safety answers.
- **Lean Dependencies**:
  - Keep dependencies lean and standard. Avoid introducing heavy third-party libraries when AndroidX Jetpack libraries suffice.
  - Manage all versions centrally in `gradle/libs.versions.toml`.

---

## 3. Standard Makefile Commands

| Command | Action |
| :--- | :--- |
| `make open` | Opens the project in Android Studio |
| `make run` | Starts the emulator (if none running), builds, installs, and launches the app |
| `make build` | Builds the debug APK |
| `make build-release` | Builds the release APK |
| `make bundle` | Builds the signed release App Bundle (`.aab`) for Google Play |
| `make keystore` | Generates the Play upload key (`upload-keystore.jks`, git-ignored) |
| `make install` | Installs the debug APK onto the active device or emulator |
| `make test` | Runs the unit test suite |
| `make format` | Formats all source code with Spotless (ktlint) |
| `make lint` | Validates formatting rules and runs Android Lint |
| `make check` | Runs format + lint + unit tests in one command |
| `make clean` | Cleans build artifacts and caches |
