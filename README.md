<p align="center">
  <img src="fastlane/metadata/android/en-US/images/featureGraphic.png" alt="Wallpaper Changer" width="720">
</p>

<p align="center">
  <b>Your photos. A fresh wallpaper, on schedule.</b><br>
  A small, private, battery-friendly Android app that rotates your own photos as your wallpaper.
</p>

<p align="center">
  <a href="https://github.com/gcg/wpchanger/actions/workflows/ci.yml"><img src="https://github.com/gcg/wpchanger/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <img src="https://img.shields.io/badge/Android-14%2B-3DDC84?logo=android&logoColor=white" alt="Android 14+">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin + Compose">
  <img src="https://img.shields.io/badge/internet%20permission-none-success" alt="No internet permission">
</p>

---

## Screenshots

| Set it & forget it | Your photo pool | Preview | On your home screen |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/main.png" width="200"> | <img src="docs/screenshots/pool.png" width="200"> | <img src="docs/screenshots/preview.png" width="200"> | <img src="docs/screenshots/home.png" width="200"> |

With Material You, the app re-themes itself to match the wallpaper it just set:

| New wallpaper | App in dark mode |
| :---: | :---: |
| <img src="docs/screenshots/home-2.png" width="200"> | <img src="docs/screenshots/dark.png" width="200"> |

## Features

- 🖼️ **Photos or whole folders**: system photo picker, or import an entire album
- ⏱️ **Intervals**: 30 min · 1 h · 3 h · 6 h · daily
- 📱 **Targets**: home screen, lock screen, or both
- 🔀 **Shuffle bag**: every photo gets a turn before any repeats
- 🔋 **Battery-friendly**: pauses in Battery Saver and on low battery, resumes on its own
- 🔔 **Optional notification** with a preview of the new wallpaper
- 🔒 **Private**: no internet permission, no accounts, no ads, no analytics

## Example setups

| Goal | Interval | Target |
| :--- | :--- | :--- |
| A new holiday photo every morning | Daily | Home & Lock |
| Keep the lock screen surprising, the home screen calm | 1 hour | Lock Screen |
| Slow-cycle a seasonal art folder | 6 hours | Home Screen |
| Get through a big album quickly | 30 min | Home & Lock |

## How it works

```mermaid
flowchart LR
    A[Photo picker / folder] -->|copy + de-dupe| B[(App-private storage)]
    C[WorkManager periodic job] --> D{Battery Saver<br>or low battery?}
    D -- yes --> E[Skip, retry next cycle]
    D -- no --> F[Shuffle bag picks next photo]
    B --> F
    F --> G[Decode, fix EXIF, downsample]
    G --> H[WallpaperManager: home / lock]
```

Photos are copied into `filesDir/wallpapers/` so rotation never breaks when the original moves or a URI permission expires. Settings live in DataStore.

## Build & run

Requires JDK 21 and the Android SDK.

```bash
make run-device   # build + install on a USB/wireless-connected phone
make run          # same, on an emulator (boots one if needed)
make check        # format + lint + unit tests (run before every PR)
make bundle       # signed release .aab for Google Play
make help         # everything else
```

## Project layout

```
app/src/main/java/com/gcg/wpchanger/
├── data/     # repository, DataStore prefs, shuffle bag, WallpaperManager helper
├── ui/       # Compose screen + ViewModel, Material 3 theme
└── worker/   # WorkManager job, scheduler, notifications
fastlane/metadata/android/   # Play Store listing text + graphics
```

## Tech

Kotlin · Jetpack Compose (Material 3) · WorkManager · DataStore · Coil · targetSdk 36, minSdk 34

## Privacy

Nothing leaves your phone. See the [privacy policy](PRIVACY.md).

## Publishing

Release steps live in [docs/PLAY_STORE.md](docs/PLAY_STORE.md).
