# Creating a new Compose Multiplatform project

How to start a ControlFix app from this template (Android, iOS, desktop) and what to change before
the first release. The lessons behind these rules are in [INSIGHTS.md](INSIGHTS.md).

## 1. Generate

```bash
cd ~/data/develop/kmp/CMPTemplate && git pull --rebase
python3 scripts/new_project.py --name MyApp --package net.controlfix.myapp --destination ../MyApp
# add --without-onboarding when the app needs no first-run setup
```

- `--name`: PascalCase, becomes the root project, theme (`MyAppTheme`), Application class and app name.
- `--package`: lowercase reverse DNS; ControlFix apps use `net.controlfix.<app>` (or the product's own).
  It is also the Android `applicationId`, the reports-service app id and the desktop settings folder.
- The destination must not exist and must be outside the template. Nothing secret or machine-local
  is copied (`local.properties`, keys, build output, `.git`).
- Then: `git init`, first commit, create the GitHub repo `controlfix87/<Name>` (private unless the
  owner says otherwise), push to `main`.

## 2. Make it the product (first commit after generating)

| What | Where |
|---|---|
| App name in every language | `common/core/designsystem/.../composeResources/values*/strings.xml` `app_name` (+ Android `androidApp/src/main/res/values/strings.xml`) |
| About line | `about_body` in all four languages |
| Privacy policy link | `ControlFixLinks.PRIVACY` (`common/feature/about/.../Links.kt`); stores require a real page |
| App mark and launcher icon | Replace `AppMark` (drawn in code) and `androidApp/src/main/res/drawable/ic_launcher_*`; keep the mark inside the launcher safe zone |
| Splash screen | Official SplashScreen API (`core-splashscreen`): replace the placeholder `androidApp/src/main/res/drawable/ic_splash_icon.xml` (mark inside the centre 192dp of 288dp) and, only if the brand differs, `splash_background`/`splash_icon` in `res/values{,-night}/colors.xml`. No splash Activity, no timed delay |
| Wordmark accent | `Wordmark(name, highlight = "Control")`-style highlight of part of the name, if the brand has one |
| Onboarding product step | `OnboardingStep.APP` + `AppStep()`; only what the app cannot work without (permission, account, first choice) |
| Desktop identity | `desktopApp`: `AppInfo` id = the Android package, window title, `packageName`/`packageVersion` |
| iOS identity | `iosApp/project.yml` bundle id = the Android package; display name |
| Signing | `~/.gradle/gradle.properties` on the build machine: `controlfix.signing.<app>=<assets repo>/…/keystore.properties` (default ControlFix key; never copy the password into the repo) |
| Targets | Drop iOS (and/or desktop) when the core feature cannot exist there (GameController, LockdownControl): remove the target from the conventions and the host module, say why in the README |

Keep: the ControlFix palette and Exo 2, the language stack (en/he/ru/fr, never the device
language), About with report/suggest and support e-mail, the reports service app id = package.

## 3. Build features the template's way

- **Module per feature** under `common/feature/<name>` with the `cmptemplate.cmp.feature` convention
  (renamed to your project). Features never depend on each other; shared code goes to `common/core`.
  `scripts/check_project.py` enforces the boundaries and `commonMain` purity.
- **Screen = Root + Screen + ViewModel**: the ViewModel exposes one `StateFlow<UiState>` via
  `stateIn(viewModelScope, WhileSubscribed(5_000), initial)` and takes a sealed `Action`; `Screen`
  is stateless (`state`, `onAction`) and screenshot-testable; `Root` only does `koinViewModel()` +
  `collectAsStateWithLifecycle()`. Screens never inject repositories or managers.
- **State**: typed input and position in `SavedStateHandle`; settings in DataStore
  (`UserPreferencesRepository`, add keys there; renaming a key is a migration); data in a database
  per [PERSISTENCE.md](PERSISTENCE.md). Navigation results are state (e.g. `finished`), not events.
- **Koin**: one module per feature (`viewModelOf(::X)`, `parametersOf` for route arguments),
  platform pieces as `expect fun xModule()`, host facts (`AppInfo`, Android `Context`) from the host.
  Register the module in `appModules` and extend `KoinModulesTest`.
- **Navigation 3**: `@Serializable` keys, registered in `App.kt`'s `SerializersModule`; push guards
  against duplicate destinations; keep the back stack above `AppLocaleProvider`.
- **Strings**: only in `core:designsystem` resources, all four languages, same placeholders
  (checked). Hebrew: edit `values-he` only.
- **UI**: `ScreenHeader` (48dp back), `SectionCard`, `ContentMaxWidth` for wide screens,
  `safeDrawing` insets, a `testTag` on every control, `brandOutlinedButtonColors()` for outlined
  buttons, `primary` for accent text (readable on light and dark).
- **Network**: one `createHttpClient(base)` per service (HTTPS only), errors mapped to typed
  results, cancellation rethrown, `MockEngine` tests.
- **Security**: apply AgentsHub `skills/vibesec/CHECKLIST.md`; no secret in the app (the reports
  endpoint is public by design); redact before anything leaves the device.

## 4. Test (once, at the end of a round)

1. `python3 scripts/check_project.py && python3 -m unittest discover -s scripts/tests`
2. One build, through the queue (Mac lane builds the pushed commit, iOS targets on):
   `gw --host mac --pull 'androidApp/build/outputs/**' --pull 'desktopApp/build/outputs/roborazzi/**' :common:core:model:allTests :common:core:common:allTests testAndroidHostTest desktopTest :androidApp:recordRoborazziDebug :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleDebugAndroidTest :desktopApp:recordRoborazziDesktop :common:sharedApp:linkDebugFrameworkIosSimulatorArm64 --continue`
3. **Look at every screenshot** (`*/build/outputs/roborazzi/`): contrast, RTL, overflow, Fold size.
   Add a shot for every new screen (stateless `Screen` with fixed state; one `setContent` per test).
4. Device run via `device-queue` with the debug + androidTest APKs, then `am instrument`; release
   the device the moment tests end. Device tests wait for the first screen (settings load first)
   and skip onboarding.
5. Record what ran and what did not in `docs/VALIDATION.md`.

## 5. Before the first release

- Real icon, about line, privacy page; signing property set; `:androidApp:assembleRelease` (R8)
  passes and the app starts from the release build.
- A real report from a debug build lands in `~/data/tickets/<package>.debug/` and the ticket
  e-mail arrives (reports service, AgentsHub `tools/reports`).
- iOS (if kept): simulator run on the Mac; desktop: `:desktopApp:packageDistributionForCurrentOS`.
