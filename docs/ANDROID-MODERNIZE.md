# Android modernize status

Already in place: minSdk 28 / targetSdk 37 / compileSdk 37 (version catalog, androidApp + KMP library convention), enableEdgeToEdge in MainActivity and per theme, Scaffold/safeDrawing/imePadding, no orientation lock or resizeableActivity override, DataStore, Koin, Material3 adaptive.

This pass: 48dp targets and semantics (selected state on step progress, radio role on language rows, link role on wordmark).

Not verified (no builds allowed in this pass): compile of the three touched files, TalkBack/200% font run, targetSdk 37 behaviour on device. Run one combined round.
