package com.cmptemplate.feature.about.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmptemplate.core.datastore.UserPreferencesRepository
import com.cmptemplate.core.designsystem.i18n.AppLocale
import com.cmptemplate.core.designsystem.i18n.LocaleManager
import com.cmptemplate.core.model.AppInfo
import com.cmptemplate.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The settings groups; on wide windows they are the left-hand list, each with its own content pane. */
enum class SettingsCategory { Language, Theme }

data class AboutUiState(
    val app: AppInfo,
    val locale: AppLocale,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val category: SettingsCategory = SettingsCategory.Language,
)

sealed interface AboutAction {
    data class SetLocale(val locale: AppLocale) : AboutAction
    data class SetTheme(val mode: ThemeMode) : AboutAction
    /** Two-pane: which category the right-hand pane shows. */
    data class SelectCategory(val category: SettingsCategory) : AboutAction
    /** Clears "onboarding done"; the caller then navigates to onboarding. */
    data object RunSetupAgain : AboutAction
}

class AboutViewModel(
    app: AppInfo,
    private val localeManager: LocaleManager,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {
    // Kept in the ViewModel so rotation, folding and window resizes keep the selected category.
    private val category = MutableStateFlow(SettingsCategory.Language)

    val state: StateFlow<AboutUiState> = combine(localeManager.locale, preferences.settings, category) { locale, settings, selected ->
        AboutUiState(app, locale, settings.themeMode, selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AboutUiState(app, localeManager.locale.value))

    fun onAction(action: AboutAction) {
        when (action) {
            is AboutAction.SetLocale -> localeManager.setLocale(action.locale)
            is AboutAction.SelectCategory -> category.value = action.category
            is AboutAction.SetTheme -> viewModelScope.launch { preferences.setThemeMode(action.mode) }
            AboutAction.RunSetupAgain -> viewModelScope.launch { preferences.setOnboardingDone(false) }
        }
    }
}
