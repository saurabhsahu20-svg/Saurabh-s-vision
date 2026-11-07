# SAUNE VIBE Android

**Version 1.0.1:** Launcher name changed to **Saune Vibe** and launcher/home branding updated to the supplied neon S-and-headphones icon. App ID is unchanged to preserve app identity. Existing debug builds may have incompatible signing keys, so a prior installation may require uninstalling before installing this version.


Kotlin + Jetpack Compose music discovery application. Open this folder in Android Studio or use GitHub Actions to create a debug-signed installable APK. Android 7.0+.

**Live music requires a YouTube Data API v3 key**, entered on the Settings tab on your phone. YouTube videos play via the official YouTube embedded iframe. The app does not provide MP3 extraction, offline downloads, background-only playback, or music stream caching.

Features: trending, latest music videos, song/artist search, Hindi/English/Punjabi/Bhojpuri categories, moods, on-device favourites and recently played, personal playlists, theme selection, and a local admin page for featured videos, category additions, banner and device usage. Admin is **local to the device and not a remotely authenticated admin**; registered-user dashboard requires a backend.

**Build**: GitHub repository > Actions > SAUNE VIBE Android APK > latest successful run > download artifact ZIP > extract app-debug.apk and install on Android. Use Settings to save your API key. Android may request permission to install files from your browser.

This repository is public. Never commit API secrets or passwords here. API keys stored within phone preferences are not a production-secret management system. Before Play Store release, implement a backend for YouTube API requests, secure authentication, privacy notice and release signing.

**Status:** GitHub Actions build status determines if this source has compiled successfully. Don't treat source code as an APK until that workflow succeeds.

## Direct APK download from GitHub

After a successful CI build, the workflow publishes the latest debug APK at GitHub Releases tagged **v1.0.1**. Open the Releases page and select **Saune_Vibe_v1.0.1.apk**. If a release isn't yet available, use the successful Actions run artifact.


## Version 1.0.2: Included YouTube songs

The Home and Discover tabs now have 20 curated, verified official YouTube video links across Hindi, English, Punjabi, Bhojpuri, moods and devotional. These are video links and metadata, NOT MP3/music file imports. No API key is necessary to browse these links. Use a YouTube video URL in Discover to open that song. An optional user-supplied YouTube Data API key enables broad live search. Playback may be blocked by rights owners or unavailable in some regions; use the Play in YouTube button in the song screen. Internet is required to play songs.


## 1.0.3 APK integrity build

GitHub Actions checks APK structure, Android package identity and manifest minSdk, APK cryptographic signing (apksigner), alignment (zipalign), SHA256 and app label before publishing. Download the .apk from the GitHub v1.0.3 Release (not the raw GitHub HTML preview). If Android reports "Package appears invalid", delete the incomplete downloaded file and download again in Chrome; confirm the download is an actual .apk, not an HTML or ZIP. If a previous Saune Vibe install has a mismatched signing certificate, back up local favourites/playlists before uninstalling the old app, as uninstall clears local data. This is a debug-signed test build; Play Store requires a stable release key.

## Version 1.0.4: Offline MP3 Music

The **Offline** tab contains a real device-local MP3 player with play/pause, previous/next, seek and stop controls. Import existing MP3 files using the Android file picker, or paste a direct HTTPS link to an MP3 file you have the right to download. Imported and downloaded MP3 audio is saved in the app's private offline storage, playable without internet. Downloads are checked for an MP3 header and restricted to 150 MB. The app does **not** download or extract audio from YouTube, Instagram, or other video services. YouTube songs remain embedded and need an internet connection. Songs in private storage are removed if the app is uninstalled. Android's Storage Access Framework handles selection without storage permissions.

## Saune Vibe v1.1.0: Real source APIs and Media3

The new **Live** tab is the default opening page. It queries actual songs from Audius public read-only API without credentials, optionally Jamendo Music API using a developer client ID, and Wikimedia Commons API for explicitly free-licensed audio. Search shows results from live responses; empty or unavailable sources show a message instead of fake items. Music streams using AndroidX Media3 ExoPlayer, with a MediaSessionService supporting background playback, Bluetooth and system media controls. Track source and license links are exposed; the provider download button appears only when its metadata permits it. Old YouTube discovery, offline MP3 import/download, playlists, favourites, and user settings remain. Jamendo needs a legitimate account-specific client ID. Some tracks can be region restricted or incompatible; a successful APK build does not verify song playback on a phone.

**Not included yet:** rights-cleared commercial Hindi/Bollywood catalog; Musopen, Internet Archive adapter; comprehensive Room/Hilt/WorkManager data migration; in-app equalizer/sleep timer; fully verified device end-to-end testing. Those require additional implementation and/or rights permissions. Never extract MP3 from YouTube.

Sources: https://docs.audius.co/api/ ; https://developer.jamendo.com/v3.0/tracks ; https://www.mediawiki.org/wiki/API:Search ; https://developer.android.com/media/media3/session/background-playback

## Version 1.1.1: Fresh-install compatibility APK

Previous debug APKs used different signing certificates in separate GitHub Actions builds. Android refuses an in-place upgrade when signatures differ. The new **Fresh Install** compatibility package uses a distinct Android application ID (`com.saunevibe.fresh`), while preserving the existing Kotlin namespace, logo, app label, and user-facing music features. It can coexist with the older `com.musicvibe.app` APK without an update-signature collision. **Old app favourites, playlists and offline MP3 files are not automatically migrated** and remain in the old app's storage until that app is uninstalled. Do not uninstall the old app until you are sure you no longer need its data.

GitHub Actions validates the APK zip, v2 signing, package name, app label, SDK compatibility, zip alignment, and attempts an `adb install` + launcher smoke test on an Android 11 emulator before publishing. This verifies the package can be installed and started in one test environment, not compatibility with every phone. To enable safe future in-place upgrades, configure a private, consistent release-signing certificate using GitHub Actions secrets; do NOT commit signing keys to this public repository.

**Install**: Download the `.apk` from GitHub Releases v1.1.1 using Chrome; verify that it is an APK rather than HTML or ZIP. Android 7.0+ required. If installation fails, share the precise error text, Android version, and whether the old app is installed.
