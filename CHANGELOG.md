# Changelog

- Android modernisation finished: fold hinge/tabletop-aware two-pane (androidx.window), headings, large-font fixes (option cards, onboarding buttons), settings category saved across process death, predictive back enabled, screenshot tests for hinge and 200% font scale.

## [Unreleased]

### Added
- Two-pane example/settings/onboarding, tests added, dialog shot fix; unbuilt (T1234)
- Two-pane layouts from 600dp (foldable inner screen, tablet, landscape): Example list+detail with persisted selection, settings categories+content, two-column onboarding welcome; Roborazzi shots for Fold inner and tablet; feedback dialog shot made idle-safe (T1234)
- Merged audit/architecture-infrastructure into main (a152322; stricter boundary checks, verify.sh via gw, iOS CI path, audit docs; AGENTS.md left out); deleted dev + audit branches (local and remote); check_project + 15 script tests pass (T1136)
- About/reports per feedback-report skill: Settings screen with feedback menu + About sheet, two-state popup (consent off, reply e-mail), snackbars, mail fallback; tests pass (about 15x2, Koin 3); verified on s25 light+dark (fixed black action icons in dark); pushed 648563e (T1130)
- Official SplashScreen API (core-splashscreen 1.2.0), placeholder ic_splash_icon, light/dark brand colours, held until first settings read; verified on s25 (API 37, light+dark) and s8 (API 28); pushed 042d58d. Host note: ~/.gradle aapt2 override points to missing ~/bin/aapt2 (T1078)
- Android splash on the official SplashScreen API (`core-splashscreen` 1.2.0): `Theme.CMPTemplate.Starting`, placeholder `ic_splash_icon`, light/dark brand colours, held only until the first settings read (`App(onFirstScreenReady)`) (T1078)
- CMPTemplate: Compose Multiplatform (Android, iOS, desktop), Roborazzi screenshots, ControlFix branding, About + reports to reports.controlfix.net with e-mail fallback, optional onboarding, DataStore settings, ViewModel-owned state; repo and folder renamed to CMPTemplate (T0978)

### Fixed
- Brand-coloured outlined buttons, system back in onboarding, RTL/locale screenshots, device tests wait for first screen; validated on s8 and generated projects; reports worker can e-mail tickets (deploy pending owner OK) (T0996)

### Changed
- Modernisation finished, recordRoborazzi green (T1244)
- SDK/edge-to-edge already compliant; a11y semantics fixes pushed; unverified, awaiting combined round (T1218)
- minSdk 28 (Android 9) instead of 26; CI device matrix API 28/35/37 (T1177)
- Merged audit/architecture-infrastructure: layer import and nested-module boundary checks in check_project.py, verify.sh runs Gradle through the build queue when present, iOS CI derived-data path, audit docs (not copied into generated projects); AGENTS.md left out (T1136)
- About and reports follow the feedback-report skill: About route is Settings with a feedback menu (Suggest an idea / Report a bug, one two-state popup, log consent off by default, reply e-mail, snackbar results, mail fallback) and the About bottom sheet; report screen and crash kind removed (T1130)
- docs/NEW_PROJECT.md: how to create and ship a new app from the template; AgentsHub skill cmp-project (T1009)
- Docs: CMP-APP-PROMPT.md, the brief for building a CMP app for Android and iOS (architecture, Android CLI, edge-to-edge, rotation, Roborazzi and device tests) (T1006)

