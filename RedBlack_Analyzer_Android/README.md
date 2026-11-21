# SPORTYBET RED BLACK ANALYZER

Native Android app for recording RED/BLACK outcomes and reviewing historical statistics.

## Features in this build
- Swahili dark dashboard with RED/BLACK counts, percentages, streak and recent history.
- Local on-device storage.
- Manual result entry, undo and clear history.
- User-started Android MediaProjection screen capture.
- Bundled Google ML Kit Latin text recognition scans visible screen text for exact RED/BLACK words.
- Floating overlay shows OCR status; tap it or use the persistent notification action to stop capture.
- OCR saves a detected color after the recognized screen text is stable across frames and changes.

## First-time setup on the phone
1. Install and open the APK.
2. Tap **ANZA KUSOMA SKRINI**.
3. Allow **Display over other apps** for this app when Android asks, then return and tap Start again.
4. Android shows its own screen-sharing confirmation. Choose **Start now**.
5. Open SportyBet and keep its result/history area visible. The floating OCR status bubble appears.
6. Tap the floating bubble or the notification's **SIMAMISHA** action to stop capture.

Android requires explicit user consent for each screen-capture session. The app does not silently start capture.

## OCR limitations and safe use
- OCR can misread text, buttons, or other page labels. Check saved history against the game.
- It recognizes visible text labels RED and BLACK; it does not reliably interpret colored circles or graphics.
- It may read other RED/BLACK labels on the page. Keep the game history/results area visible and verify entries.
- It does not log into SportyBet, read passwords, bypass app protections, place bets, or guarantee future outcomes.
- Recorded frequencies and streaks describe past observations only; they are not reliable predictions.

## Build APK
GitHub Actions builds the debug APK when files under `RedBlack_Analyzer_Android/` change. Open the repository's Actions tab, open the latest successful run, and download `SPORTYBET-RED-BLACK-ANALYZER-APK`.
