# SAUNE VIBE Android

**Version 1.0.1:** Launcher name changed to **Saune Vibe** and launcher/home branding updated to the supplied neon S-and-headphones icon. App ID is unchanged to preserve app identity. Existing debug builds may have incompatible signing keys, so a prior installation may require uninstalling before installing this version.


Kotlin + Jetpack Compose music discovery application. Open this folder in Android Studio or use GitHub Actions to create a debug-signed installable APK. Android 7.0+.

**Live music requires a YouTube Data API v3 key**, entered on the Settings tab on your phone. YouTube videos play via the official YouTube embedded iframe. The app does not provide MP3 extraction, offline downloads, background-only playback, or music stream caching.

Features: trending, latest music videos, song/artist search, Hindi/English/Punjabi/Bhojpuri categories, moods, on-device favourites and recently played, personal playlists, theme selection, and a local admin page for featured videos, category additions, banner and device usage. Admin is **local to the device and not a remotely authenticated admin**; registered-user dashboard requires a backend.

**Build**: GitHub repository > Actions > SAUNE VIBE Android APK > latest successful run > download artifact ZIP > extract app-debug.apk and install on Android. Use Settings to save your API key. Android may request permission to install files from your browser.

This repository is public. Never commit API secrets or passwords here. API keys stored within phone preferences are not a production-secret management system. Before Play Store release, implement a backend for YouTube API requests, secure authentication, privacy notice and release signing.

**Status:** GitHub Actions build status determines if this source has compiled successfully. Don't treat source code as an APK until that workflow succeeds.
