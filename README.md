# VISION ONE 1.2 Beta — Actual Document Auto Edge Detection

*Scan Smart. Save Simple.*

## Improved scanning
- OpenCV contour detection using Canny edges and multiple threshold passes to identify the outer quadrilateral of a document regardless of its preset page size.
- Perspective rectification with four-corner crop for notes, bills, ID cards, letters and examination papers.
- Best-effort automatic crop after capturing; scans remain intact if no reliable page boundary is detected.
- Manual adjustment with four draggable corners; retest with AUTO EDGES button on the crop screen.
- **Original proportions** is the new default for PDF, allowing smaller receipts, books and certificates to keep their captured proportions; A4, A5, Legal, Letter and ID card remain selectable.
- No uploads or login; document edge detection runs locally on-device.
- Multi-page scanning, image import, PDF export, OCR and local PDF operations from earlier versions remain.

## Beta limitations
Detection can be less accurate on a document with a background of similar color, heavy shadows, folding, glare, or partially missing page edges. Review the crop preview before saving. **Original proportions** preserves shape, but photographs cannot tell the exact physical millimetre dimensions of a paper without a reference object or a known preset size.
A successful GitHub compilation does not equal device-tested quality. Android phone scanning, crop accuracy and installation compatibility must be tested. Debug builds signed by a different key may require uninstalling the earlier beta after exporting any important PDFs.

## APK
In GitHub Actions select a successful run on branch `vision-one-scanner-v1-2-autoedge` and download the `VISION-ONE-v1.2-AutoEdge-APK` artifact.
