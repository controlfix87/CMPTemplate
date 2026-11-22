# Prompt: build a Compose Multiplatform app for Android and iOS

Copy everything below the line into the brief of whoever builds a new app (or reworks an existing
one). Replace `<AppName>`, `<package>` and the product paragraph. It describes how a ControlFix CMP
app looks when it is done, based on this template (`~/data/develop/kmp/CMPTemplate`) and the lessons
in [INSIGHTS.md](INSIGHTS.md).

---

## The product

`<AppName>` (`<package>`): <one paragraph: what it does, for whom, the main screens>.
It is a Compose Multiplatform app for **Android and iOS** (desktop optional), with one shared UI.
If the core feature cannot exist on iOS (it needs accessibility services, overlays, input
injection), say so before you start: such an app drops the iOS target instead of shipping a shell.

## Start from the template

- Generate, don't copy by hand:
  `python3 scripts/new_project.py --name <AppName> --package <package> --destination ../<AppName>`
  (`--without-onboarding` if the app needs no first-run setup). Then `./scripts/verify.sh`.
- Keep the template's structure, conventions and tests. Replace the `feature:example` sample only
  when the first real feature takes its place, and keep its restoration and testing patterns.
- Read `README.md`, `docs/INSIGHTS.md`, `docs/TESTING.md`, `docs/LOCALIZATION.md`,
  `docs/PERSISTENCE.md` and `~/AgentsHub/android-architecture.md` before writing code.

## Toolchain and versions

- JDK 21, the checked-in Gradle wrapper, versions only in `gradle/libs.versions.toml`.
- Android: compileSdk/targetSdk = the latest stable in the installed SDK (template: 37), minSdk 26.
  AGP 9: do not apply `org.jetbrains.kotlin.android` to the app; set `buildConfig = true` explicitly.
- iOS: Xcode + XcodeGen (`iosApp/project.yml`, `./scripts/ios-project.sh`), Apple Silicon device and
  simulator targets. iOS targets are configured on macOS only; Linux results are no iOS evidence.
- Builds go through `build-queue` / `gw` (Mac lane: `gw --host mac`, it builds the pushed commit).

## The Android CLI (`android`)

Use it before hand-written adb/Gradle work:
- `android skills find <keyword>` before each task and read the match: `edge-to-edge`, `adaptive`,
  `navigation-3`, `testing-setup`, `r8-analyzer`, `android-permissions-security`, `agp-9-upgrade`
  (not for KMP). Don't `skills add` without asking the owner.
- `android docs search <topic>` for the current official text instead of memory.
- `android describe` for module and APK paths, `android analyze <apk|aab>` for size and R8 results.
- `android preview` renders `@Preview`s to PNG without a device.
- On a leased device (`device-queue`, pass the serial): `android install`, `android run`,
  `android screen` (screenshot) and `android layout` (UI tree) to inspect what is on screen.

## Architecture (required)

- Modules: `common:core:{model,common,designsystem,datastore,network}`, `common:feature:*`,
  `common:sharedApp` (navigation, keys, production Koin module list, iOS framework), hosts
  `androidApp`, `iosApp`, optional `desktopApp`. Convention plugins in `build-logic`.
- `core` never depends on `feature`; features never depend on each other (cross-feature navigation
  through callbacks). `commonMain` never imports Android/JVM APIs: platform pieces are
  `expect/actual` or injected interfaces. `scripts/check_project.py` enforces this.
- Every screen: a ViewModel with **one** `StateFlow<UiState>`
  (`stateIn(viewModelScope, WhileSubscribed(5_000), initial)`), actions as functions or a sealed
  `Action`, a stateless `Screen(state, onAction)` and a `Route` that only wires `koinViewModel()`
  and collects state. Screens never inject repositories.
- Navigation results are state, not one-off events. Repositories are the single source of truth,
  main-safe, exposing `Flow` and `suspend`. Dispatchers are injected (`DispatcherProvider`);
  in common code `Dispatchers.IO` needs `import kotlinx.coroutines.IO`.
- Koin: one module per feature (`viewModelOf(::X)`), `single` for repositories, host-only facts
  supplied by the host; a DI unit test resolves every ViewModel from the production module list.
- Settings: Preferences DataStore through a repository, one instance per file (Koin `single`),
  corrupt files fall back to defaults; the app waits for the first read instead of drawing a
  guessed theme or start screen. No SharedPreferences / NSUserDefaults in new code.
- Networking: one injected HTTPS Ktor client (OkHttp / Darwin engines), relative paths, typed
  errors, cancellation rethrown, logging off by default and never with bodies or secrets.

## UI, theme, language

- ControlFix theme from `core:designsystem` (palette, Exo 2 for headings and labels), light, dark
  and "follow device", chosen in onboarding and About. Status/navigation bar icons follow the
  **app's** theme (`SystemBarsEffect`), not the phone's.
- Shared components (`ScreenHeader`, `SectionCard`, `OptionCard`, `StepProgress`, ...) before new
  ones. Outlined buttons use `brandOutlinedButtonColors()`.
- All user-visible text in Compose resources, English, Hebrew (RTL), Russian and French. The app
  language never follows the device; it changes only through `LocaleManager.setLocale`. Edit
  `values-he/` only (`values-iw/` is generated). Use start/end, never left/right.
- `contentDescription` on icons that carry meaning, touch targets >= 48dp, a `testTag` on every
  control a test or automation touches, stable keys in lazy lists, previews for screens.
- Settings (language, theme, run setup again) with the feedback menu (Suggest an idea / Report a
  bug, one two-state popup, log consent off) and the About sheet in its action bar stay (AgentsHub
  `feedback-report` skill); set `ControlFixLinks.PRIVACY` and
  `about_body` before release. Optional onboarding: language and theme first, step bar, Skip on
  every step but the last, the step survives rotation, language change and process death.

## Edge-to-edge (Android) and safe areas (iOS)

- `enableEdgeToEdge()` before `setContent`; `android:windowSoftInputMode="adjustResize"`;
  predictive back stays on (default from targetSdk 36; never set
  `enableOnBackInvokedCallback="false"`), back handled with `BackHandler` from `ui-backhandler`,
  no `onBackPressed`.
- Each screen's `Scaffold` owns the `safeDrawing` insets. Pass its padding to the content
  (`contentPadding` for lists so they scroll behind the bars) and `consumeWindowInsets` it; apply
  IME padding after that. Use **one** inset method per edge: no `safeDrawingPadding()` inside a
  Scaffold that already padded, no double padding with nested Scaffolds.
- Material 3 bars (`TopAppBar`, `NavigationBar`, `NavigationRail`, `ModalBottomSheet`) handle
  their own insets: don't pad their parent. Backgrounds draw behind the system bars; text and
  controls never do. Check the camera cutout on both landscape sides and the IME over forms.
- iOS: the SwiftUI host is `ComposeRoot().ignoresSafeArea()` and Compose handles safe areas with
  the same `WindowInsets` code. Info.plist has `CADisableMinimumFrameDurationOnPhone`.

## Rotation, resizing and adaptive layout

- No orientation lock, no `resizeableActivity="false"`, no blanket `configChanges`: the Activity
  recreates normally (Android 17 ignores large-screen orientation locks anyway). iOS supports all
  phone and tablet orientations.
- State that matters survives rotation, recreation and process death: the Navigation 3 back stack
  with explicit key serializers, `SavedStateHandle` for drafts, queries and steps (scalar ids, not
  whole models), `rememberSaveable` for UI state, saveable lazy-list state. Nothing user-visible
  only in `remember`.
- Layouts adapt to the window, not the device: compact = bottom `NavigationBar`, medium/expanded
  (landscape phone, tablet, unfolded foldable) = `NavigationRail` or list-detail panes
  (Navigation 3 scenes). Readable width on wide windows (`widthIn(max = 720.dp)`), forms and lists
  scroll in short windows (landscape phone, split screen). Follow the `adaptive` skill.

## Testing infrastructure (required)

Tests assert observable behaviour; no empty tests. Write them with the feature, run them once at
the end (one build round, then one device round).

| Layer | Where | What |
|---|---|---|
| Common unit | `commonTest`, kotlin-test | Result helpers, mappers, pure logic |
| ViewModel | `testAndroidHostTest` / `desktopTest`, fakes + `kotlinx-coroutines-test` | Initial load once, resubscription, single-flight retry, saved-state restore |
| Data | same | DataStore defaults/write/corrupt file, migrations, Ktor `MockEngine`: statuses, malformed JSON, cancellation |
| DI | `:androidApp:testDebugUnitTest` | Production Koin module list resolves every ViewModel |
| Screenshots | Roborazzi + Robolectric: `:androidApp:recordRoborazziDebug` / `verifyRoborazziDebug`; desktop twin `:desktopApp:recordRoborazziDesktop` | Every screen at phone portrait, phone landscape and Fold sizes, light and dark, Hebrew RTL, dialogs and error/empty states. Plain `Application`, Koin started/stopped per test, one `setContent` per test. **Look at the PNGs after every UI change** |
| Device | `:androidApp:connectedDebugAndroidTest` (Compose UI test rule) | Skip onboarding and wait for the first screen; real rotation (restore `UNSPECIFIED` in cleanup) vs. Activity recreation; every screen in portrait, landscape and reverse landscape with text and controls inside the safe-drawing rect and no two of them overlapping; state kept after each rotation |
| Manual | `docs/TESTING.md` matrix | Process death (`am kill`, not force-stop), API 26/35/37, gesture and 3-button nav, predictive back, split screen, fold/unfold, IME, 200% font, TalkBack, locale stability |
| Release | `:androidApp:assembleRelease` | R8 + resource shrinking pass, `android analyze` on the APK |
| iOS | Mac lane | `iosSimulatorArm64Test`, framework link, XcodeGen + Xcode build, run in the simulator in portrait and landscape |

`./scripts/verify.sh` is the local gate (static checks, generator tests, all host tests, lint,
app and test APK compilation, desktop compile). Record exact commands and test counts from the
XML in `docs/VALIDATION.md`; never report an unavailable task as green.

## Security, release, branding

- Apply `~/AgentsHub/skills/vibesec/CHECKLIST.md` while writing code. No secrets in the repo or the
  app; release signing from `keystore.properties` in the private assets repo via the Gradle
  property `controlfix.signing.<app>`. Explicit exported components, `PendingIntent` mutability
  flags, no cleartext traffic, no PII in logs.
- Reports and crashes go to the shared reports service (`feature:about`), redacted on the device,
  with the e-mail fallback. Ads, analytics, Crashlytics, billing, auth and databases are added only
  when the product needs them (see INSIGHTS "Not applied").
- Replace `AppMark`, the launcher icon, the splash icon (`ic_splash_icon`) and `about_body` with the product's own.

## Done means

- All of the above holds on Android and iOS; `verify.sh`, release build, Roborazzi screenshots
  (looked at), device tests and the iOS lane passed, with results in `docs/VALIDATION.md`.
- README updated (what the app does, module map, how to run), CHANGELOG `[Unreleased]` entry,
  everything committed and pushed.
