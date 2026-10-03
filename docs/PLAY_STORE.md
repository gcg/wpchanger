# Google Play release plan

Everything that can live in the repo is done. What's left needs your accounts, keys, or judgment.

## ✅ Done in the repo

- [x] **targetSdk 36** (required for new apps since Aug 31, 2026); AGP 8.13 / Gradle 8.13
- [x] **Release signing** from git-ignored `keystore.properties` (or `WPCHANGER_*` env vars for CI)
- [x] `make keystore` (generate upload key) and `make bundle` (signed `.aab`)
- [x] Removed `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: Play only allows it for VoIP, navigation, and similar apps. The battery banner now opens App info instead.
- [x] Removed WorkManager's unused foreground service, so there's no FGS declaration to fill in
- [x] Release build (R8-minified) smoke-tested on an API 37 emulator: folder import, apply now, scheduling, notifications permission, battery banner
- [x] Store listing text + graphics in [`fastlane/metadata/android/en-US/`](../fastlane/metadata/android/en-US/)
- [x] Privacy policy: [`PRIVACY.md`](../PRIVACY.md)
- [x] CI workflow (format check, lint, tests, debug APK artifact)

## 📝 Your checklist

### 0. Quick decisions
- [ ] **Set GitHub's default branch to `main`.** It's currently `feature/claude`, so the repo page shows an old README. Go to Settings → General → Default branch.
- [ ] **App name.** "Wallpaper Changer" is fine for Play but very generic, with many apps of the same name. Rename now if you want something distinctive (`app_name` in `strings.xml` + `title.txt`, max 30 chars).
- [ ] **Package name `com.gcg.wpchanger` is permanent** once uploaded. Keep it unless you object now.
- [ ] *(Optional)* Add a `LICENSE`. Without one, the code is "all rights reserved".

### 1. Developer account (~1–3 days)
- [ ] Sign up at <https://play.google.com/console/signup> and pay the $25 one-time fee
- [ ] Choose **Personal** or **Organization**:
  - Personal (created after Nov 2023): requires a 12-tester, 14-day closed test before production (step 5)
  - Organization: skips the closed test, but needs a D-U-N-S number
- [ ] Complete identity verification and verify your contact email and phone
- [ ] Verify access to an Android device through the Play Console mobile app, if prompted

### 2. Upload key (10 min)
- [ ] `make keystore`, then pick a strong password
- [ ] **Back up `upload-keystore.jks` + password** in your password manager. It's git-ignored, so the repo won't keep it for you.
- [ ] `cp keystore.properties.example keystore.properties` and fill in the passwords
- [ ] `make bundle` → `app/build/outputs/bundle/release/app-release.aab`

> Play App Signing is on by default for new apps: Google holds the real signing key, and yours is only the *upload* key. A lost upload key can be reset through Play support.

### 3. Create the app in Play Console
- [ ] **Create app**: name, default language English (US), App, Free, accept the declarations
- [ ] **Store listing**: paste from `fastlane/metadata/android/en-US/`

  | Field | Source |
  | :--- | :--- |
  | App name | `title.txt` |
  | Short description | `short_description.txt` |
  | Full description | `full_description.txt` |
  | App icon (512×512) | `images/icon.png` |
  | Feature graphic (1024×500) | `images/featureGraphic.png` |
  | Phone screenshots | `images/phoneScreenshots/1–5.png` |

- [ ] Category **Personalization**, plus your contact email (shown publicly)

### 4. App content (Policy → App content)
- [ ] **Privacy policy URL**: `https://github.com/gcg/wpchanger/blob/main/PRIVACY.md` (after this branch is merged)
- [ ] **Ads**: No
- [ ] **App access**: All functionality is available without special access
- [ ] **Content rating** (IARC): category *All other app types*, answer **No** to everything. Expect Everyone / PEGI 3.
- [ ] **Target audience**: 18 and over; "appeals to children": No. This avoids Families policy requirements and doesn't restrict who can install.
- [ ] **Data safety**: "Does your app collect or share any required user data types?" → **No**
  - Justification: no INTERNET permission, and photos are processed only on device, which Google says doesn't need disclosure
  - One gray area: Android Auto Backup includes app *settings* (never photos). Google's Data safety help doesn't cover system backup either way. Answering No is standard, but it's your call.
- [ ] Government app / financial features / health / news: No / None / No / No

### 5. Closed test (personal accounts only, 14+ days)
- [ ] Testing → Closed testing → create a track, upload the `.aab`, and paste `changelogs/1.txt` as release notes
- [ ] Add **at least 12 testers** (email list or Google Group) and send them the opt-in link
- [ ] Testers must **accept and install**, then stay opted in for **14 consecutive days**
- [ ] Ask a few testers for feedback; the production-access form asks about it

### 6. Production
- [ ] Apply for production access (Dashboard), which is usually reviewed within 7 days
- [ ] Production → Create release → reuse the same `.aab` → choose countries → **Send for review**
- [ ] *(Optional)* Start with a staged rollout (e.g. 20%)

### 7. After launch
- [ ] Add a "Get it on Google Play" badge to the README
- [ ] Tag the release: `git tag v1.0.0 && git push --tags`

## 🔁 Shipping updates

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`
2. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (≤500 chars)
3. `make check && make bundle`, then upload to a track in Play Console

Later on, [Gradle Play Publisher](https://github.com/Triple-T/gradle-play-publisher) or `fastlane supply` can push the AAB and this metadata folder automatically.
