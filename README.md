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
