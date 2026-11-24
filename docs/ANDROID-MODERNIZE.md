# Android modernize status

Already in place: minSdk 28 / targetSdk 37 / compileSdk 37 (version catalog, androidApp + KMP library convention), enableEdgeToEdge in MainActivity and per theme, Scaffold/safeDrawing/imePadding, no orientation lock or resizeableActivity override, DataStore, Koin, Material3 adaptive.

This pass: 48dp targets and semantics (selected state on step progress, radio role on language rows, link role on wordmark).

Not verified (no builds allowed in this pass): compile of the three touched files, TalkBack/200% font run, targetSdk 37 behaviour on device. Run one combined round.

## Two-pane layouts (adaptive pass)

Rule: a window at least 600dp wide (`TwoPaneMinWidth`; unfolded Fold, tablets, landscape phones, desktop windows) gets two panes; narrower stays single-pane exactly as before. Shared helper: `TwoPane` in `core/designsystem/components/Layout.kt` (weighted Row, divider, each pane its own traversal group, mirrors in RTL). The width is read from the layout constraints (`BoxWithConstraints`), so it follows rotation, fold/unfold and window resize with no extra dependency and works the same on Android, iOS and desktop.

Two-pane now:
- Example list -> detail: list (search, header, selection highlighted) left, detail right, placeholder until an item is picked, no back button in the pane. Selected item is `ExampleState.selectedId` in `SavedStateHandle` (survives rotation, fold, process death); the list scroll state is hoisted above the pane switch. If a compact detail route is open when the window grows, that route renders the two-pane screen with its item selected.
- Settings: one app bar, categories (Language, Theme, run setup again) left, the chosen category right. Selected category is `AboutUiState.category` in the ViewModel (survives rotation and fold; not process death).
- Onboarding welcome step: greeting left, language and theme cards right from 720dp (the Fold inner screen stays one column); the other steps keep the 720dp readable width.

Fold / hinge handling (done): `TwoPane` reads `LocalFoldingState` (designsystem, common code, default "no fold"). The Android host (`MainActivity`) fills it from `androidx.window` `WindowInfoTracker` with the first separating `FoldingFeature` (half-open or occluding hinge). Vertical hinge (book): list left of the hinge, detail right of it, no pane under the hinge, no divider. Horizontal hinge (tabletop): list above, detail below. Flat or folded: the plain 40/60 weighted split. Pane content is `movableContentOf`, so folding keeps scroll and text state. iOS and desktop keep the default (no fold). Screenshot tests: `example_two_pane_fold_hinge`, `example_two_pane_tabletop`.

Detail opened from compact, then widened: the detail route renders the two-pane screen with its item selected. Tapping an item in the compact list also stores it as the list entry's selection, so going back from that route lands on the list with the item selected when the window is wide. Selections made inside the pane of a widened detail route stay on that route's ViewModel.

Process death: `ExampleState.selectedId` and the query use `SavedStateHandle`; the settings category (`AboutViewModel`) now does too. ViewModel tests cover both restores.

Accessibility (done): headings on screen titles, section labels, onboarding and detail titles, dialog title; loading indicator has a description; list selection and settings category are shown without colour alone (bold plus check mark) and exposed as selected state; 48dp minimum for list items, step progress, consent row, wordmark link; option cards and step titles grow with font scale instead of clipping; onboarding buttons stack vertically from font scale 1.5 so they fit at 200%; each pane is a traversal group (finish list, then detail). Decorative icons (beside text) have `contentDescription = null`, every icon-only button (back, feedback menu, About) has a label. Reduced motion: the app has no infinite or Lottie animation of its own; the only motion is Compose animations (crossfade, step transition, colour) and the platform progress indicator, which Compose stops when the system animator duration scale is 0 (Remove animations). Screenshot tests at 200% font scale: `example_font200`, `onboarding_welcome_font200`, `example_two_pane_fold_font200`, `about_font200`.

Architecture: DataStore for settings (theme, onboarding); the app language stays in SharedPreferences on purpose: Android needs it synchronously in `attachBaseContext`, before DI or coroutines exist (documented in `AndroidAppLocale`). State lives in ViewModels, `collectAsStateWithLifecycle` everywhere, Koin, strings in resources (no new strings were added, so no new translations), `allowBackup=false` plus data extraction rules, `enableOnBackInvokedCallback=true` (predictive back), minSdk 28 / targetSdk 37.

Not done / limits:
- Width-based `TwoPane` instead of `ListDetailPaneScaffold` / Navigation 3 `ListDetailSceneStrategy`: the shared code must also run on iOS and desktop, where those artifacts do not apply. The hinge handling above covers the Android-specific part.
- TalkBack, 200% font and real foldable behaviour are verified in Roborazzi shots only; a device run is a separate step.

## Feedback dialog screenshot tests (AppNotIdleException)

Cause (reasoned, not run): the dialog window takes focus, the first text field gets focus, and its cursor blink is an endless animation; the compose test clock counts a running animation as busy, so `waitForIdle()` waits until its 60s limit. The About/onboarding/home shots have no focused field and pass. Fix is in the test only: `dialogShot` pauses the clock (`mainClock.autoAdvance = false`), idles the Robolectric looper and advances the clock by hand before capturing. Verify on the next Mac round.

## Tabletop, cross-screen detail, ViewModel tests (check pass)

- Tabletop posture: already implemented in `TwoPane` (horizontal `FoldingFeature` via `LocalFoldingState`: list above, detail below; screenshot `example_two_pane_tabletop`). The Settings screen uses the same `TwoPane`, so it gets it too. Nothing to add.
- Deep links: the app declares no deep-link intent filters (only MAIN/LAUNCHER). Every way to open an item (list tap, detail route, widening a compact detail) goes through `ExampleRoute.Detail`, which on a wide window renders the two-pane screen with the item selected (`state.selectedId ?: itemId`) and on a compact window the full-screen detail. A future deep link only needs to push `ExampleRoute.Detail(id)`; widening or narrowing keeps the selection (saved state).
- ViewModel tests: `ExampleViewModelTest` (selection + query restore), `AboutViewModelTest` (category restore), `OnboardingViewModelTest` (step restore) cover every ViewModel touched in this modernisation.
