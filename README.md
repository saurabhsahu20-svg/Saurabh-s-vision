# VISION ONE — Scan & PDF Lite v2.0 Beta
**Scan Smart. Save Simple.**

One-purpose native Android scanner. No login, no ads, no OCR, no unrelated PDF utility tools.

## What it does
- Scan paper using CameraX and save multiple high-quality document photos.
- Automatically detect document edges offline with OpenCV; if unreliable, keep the original image.
- Correct scan geometry using a 4-corner manual crop editor.
- Rotate pages, adjust color/grayscale/B&W filters in crop editor.
- Import several photos from phone storage without storage permission.
- Rearrange, remove, or add scanned pages.
- Convert scanned pages to a standard PDF with original proportions, A4, A5, Letter, Legal or ID card page size.
- Store created PDF privately in the app and optionally export a copy to any phone folder using Android's Save Document picker.
- Share PDFs with other apps using Android FileProvider.

## Privacy
Camera permission only. Offline. No uploads, account or broad storage access.

## Install
Download artifact from GitHub Actions workflow on the `vision-one-scan-pdf-lite` branch. Unzip to extract the .apk.
This is a **debug-signed beta**. Older independently built debug versions can have different signing keys. Before uninstalling any old version, export all saved PDFs, since uninstall deletes private app storage.
Actual Android-phone camera, document detection and printer testing is still necessary.
