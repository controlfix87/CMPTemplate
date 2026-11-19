# Changelog

## [Unreleased]

### Added
- Android splash on the official SplashScreen API (`core-splashscreen` 1.2.0): `Theme.CMPTemplate.Starting`, placeholder `ic_splash_icon`, light/dark brand colours, held only until the first settings read (`App(onFirstScreenReady)`) (T1078)
- CMPTemplate: Compose Multiplatform (Android, iOS, desktop), Roborazzi screenshots, ControlFix branding, About + reports to reports.controlfix.net with e-mail fallback, optional onboarding, DataStore settings, ViewModel-owned state; repo and folder renamed to CMPTemplate (T0978)

### Fixed
- Brand-coloured outlined buttons, system back in onboarding, RTL/locale screenshots, device tests wait for first screen; validated on s8 and generated projects; reports worker can e-mail tickets (deploy pending owner OK) (T0996)

### Changed
- docs/NEW_PROJECT.md: how to create and ship a new app from the template; AgentsHub skill cmp-project (T1009)
- Docs: CMP-APP-PROMPT.md, the brief for building a CMP app for Android and iOS (architecture, Android CLI, edge-to-edge, rotation, Roborazzi and device tests) (T1006)

