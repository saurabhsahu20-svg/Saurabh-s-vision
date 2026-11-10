# ✦ VISION WALLS — Wallpapers, Live Themes & Widgets

**Free, ad-free, offline Android wallpaper studio.**

- **240 static wallpaper presets** (24 categories × 10 palettes), rendered at full-HD 1080×1920 when saved or applied.
- **240 animated live wallpaper presets** drawn dynamically by a real Android `WallpaperService`, paused when not visible.
- **240 customizable clock-widget styles**, independently selectable for each widget instance through a standard Android widget configuration screen.
- Simple dark navy UI; live previews; category filters; Save via Android's document picker; one-tap set wallpaper; pinned widget support; privacy-friendly (no network permission, no analytics, no ads).

These are **procedurally generated artistic variations** (not 720 separately hand-illustrated artworks, videos, or widget APKs). Different presets share visual engines; each of the three modes has 240 selectable combinations.

## Free public APK download

[Download VISION WALLS APK directly](https://raw.githubusercontent.com/saurabhsahu20-svg/Saurabh-s-vision/main/vision-walls/download/Vision_Walls_Free_v1.0.apk) — no account or payment needed; Android 8.0+; debug-signed, not yet device-tested.

## Build

1. Open `vision-walls/` in Android Studio (Android SDK API 35, JDK 17).
2. Sync Gradle and build `app` (`gradle assembleDebug`).
3. Install `app/build/outputs/apk/debug/app-debug.apk` on Android 8+.

Or download the `Vision-Walls-Free-APK` build artifact from the repository's **Actions → Build Vision Walls Android APK** run (artifacts may require GitHub login to download). This is a **debug-signed build**, suitable for testing; signing and device testing are recommended before store releases.

## Android implementation

- Native Java, no external runtime libraries.
- `Art.java`: original drawing engine that generates color-rich vector-like pictures and animation frames.
- `VisionLiveService.java`: Android `WallpaperService` engine (~11 FPS, stops when hidden).
- `VisionWidgetProvider.java`: Android home-screen clock/date widget renderer.
- `WidgetConfigActivity.java`: per-widget style picker.
- `MainActivity.java`: accessible categories, previews, saving, wallpaper apply and widget pin request.

## License

MIT License. All code and procedural designs in this project are offered free to use and modify, with attribution. The collection does not require a paid service or an internet connection.
