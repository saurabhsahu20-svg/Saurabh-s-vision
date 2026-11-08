# VISION ONE PDF Scanner v1.1 Beta

Scan Smart. Save Simple.

## New in v1.1
- Document-boundary auto suggestions for bright paper against a darker surface (best effort, manual adjustment available).
- Four draggable crop corners and perspective rectification.
- Crop controls for A4, A5, Letter, Legal, ID card and original document proportions.
- Paper-size selection during PDF export (no stretch, image fitted inside chosen page).
- Original scans backed up while editing for non-destructive re-cropping.
- EXIF image orientation correction for camera and imported photos.
- Camera capture automatically attempts high-confidence crop; questionable cases preserve the full image.
- Scan-to-review navigation fix; tap any page to crop.

## Build
GitHub Actions > VISION ONE APK Build > choose run for v1.1 branch > Artifacts.

This is a **debug-signed beta APK**, and requires real-device scanning/print testing. Since CI uses a temporary debug signing key, earlier betas may have to be uninstalled first. Export personal PDFs before uninstall; uninstalling destroys private app storage.

Automatic corner detection uses a heuristic and may not work with white backgrounds, glossy pages, busy desks or extreme lighting. Manual four-corner crop always remains available. Offline OCR models are included in the base app; ML Kit accuracy can vary.
