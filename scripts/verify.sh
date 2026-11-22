#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/check_project.py
python3 -m unittest discover -s scripts/tests -v
# Use the machine's serialized build queue when available; portable CI uses the wrapper.
if command -v gw >/dev/null 2>&1; then
    gradle_runner=(gw)
elif [[ -x "$HOME/AgentsHub/bin/gw" ]]; then
    gradle_runner=("$HOME/AgentsHub/bin/gw")
else
    gradle_runner=(./gradlew)
fi
# Pure common tests, KMP Android host tests, desktop (JVM) tests incl. desktop screenshots, Android
# unit + Robolectric screenshot tests, lint, and app/test APK compilation. Screenshot tests only
# draw here; record/compare with :androidApp:recordRoborazziDebug / verifyRoborazziDebug and
# :desktopApp:recordRoborazziDesktop / verifyRoborazziDesktop (docs/TESTING.md).
"${gradle_runner[@]}" :common:core:model:allTests :common:core:common:allTests testAndroidHostTest desktopTest \
  :androidApp:testDebugUnitTest :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest \
  :desktopApp:compileKotlinDesktop "$@"
