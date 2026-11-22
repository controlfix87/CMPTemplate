package com.cmptemplate.feature.about.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.cmptemplate.feature.about.ui.AboutRoot
import kotlinx.serialization.Serializable

sealed interface AboutRoute : NavKey {
    /** The Settings screen; its action bar holds the feedback menu and About ("i"). */
    @Serializable data object About : AboutRoute
}

/**
 * Settings, with "Suggest an idea" / "Report a bug" (one popup) and the About sheet in its action
 * bar. [onRunSetupAgain] null hides "Run setup again" (projects generated without onboarding).
 */
fun EntryProviderScope<NavKey>.aboutEntries(
    onBack: () -> Unit,
    onRunSetupAgain: (() -> Unit)?,
) {
    entry<AboutRoute.About> { AboutRoot(onBack, onRunSetupAgain) }
}
