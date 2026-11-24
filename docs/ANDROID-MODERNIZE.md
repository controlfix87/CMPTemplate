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

Not done / limits:
- Uses a width-based `TwoPane` instead of `ListDetailPaneScaffold` / Navigation 3 `ListDetailSceneStrategy`: the shared code must also run on iOS and desktop, and no extra artifact was added without a build. Swapping to the adaptive Scene strategy later only touches `ExampleScreen`.
- Hinge/posture (tabletop/book) is not handled: no WindowManager fold feature in common code; the 40/60 split is a plain weight, so on a Fold the hinge sits in the pane divider region but is not detected.
- Back from the compact-opened detail route after it was widened returns to the list entry, whose own ViewModel keeps its own selection (the route's item is not copied back).
- Screenshot tests added (Fold inner w673dp-h841dp, tablet w1280dp-h800dp land); not yet recorded or reviewed.

## Feedback dialog screenshot tests (AppNotIdleException)

Cause (reasoned, not run): the dialog window takes focus, the first text field gets focus, and its cursor blink is an endless animation; the compose test clock counts a running animation as busy, so `waitForIdle()` waits until its 60s limit. The About/onboarding/home shots have no focused field and pass. Fix is in the test only: `dialogShot` pauses the clock (`mainClock.autoAdvance = false`), idles the Robolectric looper and advances the clock by hand before capturing. Verify on the next Mac round.
