# VISION DIGITAL WALLS 2.0 — Science & Mathematics Edition

Android app (Java / Gradle / Android 8.0+) for **new digital STEM wallpapers**, real live wallpaper animations, and functional digital home widgets.

## What changed
- Old 270 landscape, posters, natural scenes, and clock-theme artwork have been removed from the app's catalogue.
- **240 unique, deterministic digital designs (12 categories × 20 variations)** are drawn offline through Android Canvas.
- Categories: Digital Atom, Mathematical Graphs, Molecular Matrix, DNA Helix, Digital Heart, Kidney & Nephron, Cells & Tissues, Cyber Circuits, Fractal Geometry, Solar System HUD, Digital Botany, Quantum Waves.
- The same 240 original digital scenes can be used as animated live wallpapers. Actual artwork elements animate with time; backgrounds are not a static GIF preview.
- Digital widgets: full and compact layouts with true device-local time and date via TextClock, plus genuine device battery level refreshed on widget updates and charging events.
- Dark navy / neon cyan / purple / gradient palette, offline art, no login, no ads, no tracking.

**Note:** This is a wallpaper + widget Android app, not yet a full Android home launcher replacement. Weather is not included because it would need a live data provider and internet; the app never shows fake weather.

## Install and build
Open `vision-walls/` in Android Studio (JDK 17, Android SDK 35) or run `gradle assembleDebug` from that folder.

A GitHub Actions workflow builds and uploads a test APK artifact after source changes; verify a successful run before downloading. APKs already present in `download/` are old v1.x builds **and do not contain these updates**.

## Privacy
Everything is locally generated and requires no network access. Wallpapers are original procedural designs and not copies of online reference images.

## Validation status
Source updates are committed on branch `vision-walls-digital-2026`. Android compilation and device verification are required before marking this version installable.
