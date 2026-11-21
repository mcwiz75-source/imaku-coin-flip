# SPORTYBET RED BLACK ANALYZER

Native Android starter app for recording Red/Black outcomes and reviewing historical counts.

## Included in this build
- Dark Android dashboard in Swahili.
- Add RED or BLACK outcomes with one tap.
- Local on-device history using SharedPreferences.
- Total counts, percentages, current streak, and last 30-round sample summary.
- Undo last entry and clear history with confirmation.
- No SportyBet login, password, betting, deposit, or automatic bet placement.

## Important limitations
This first Android build does not yet read the SportyBet app live, use Android screen-capture permission, or display a floating overlay. The trend display describes recorded past outcomes only and is not a reliable prediction of the next round.

## Build APK
GitHub Actions builds the debug APK when files under `RedBlack_Analyzer_Android/` change. Open the repository's Actions tab, open the latest successful run, and download the artifact named `SPORTYBET-RED-BLACK-ANALYZER-APK`.
