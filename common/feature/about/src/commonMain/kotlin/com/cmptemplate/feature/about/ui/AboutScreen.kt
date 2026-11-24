package com.cmptemplate.feature.about.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.cmptemplate.core.designsystem.components.TwoPane
import com.cmptemplate.core.designsystem.components.TwoPaneMinWidth
import com.cmptemplate.core.designsystem.generated.resources.language
import com.cmptemplate.core.designsystem.generated.resources.theme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmptemplate.core.designsystem.components.AppIcons
import com.cmptemplate.core.designsystem.components.ContentMaxWidth
import com.cmptemplate.core.designsystem.components.LanguageChoices
import com.cmptemplate.core.designsystem.components.ScreenHeader
import com.cmptemplate.core.designsystem.components.SectionCard
import com.cmptemplate.core.designsystem.components.ThemeChoices
import com.cmptemplate.core.designsystem.components.brandOutlinedButtonColors
import com.cmptemplate.core.designsystem.generated.resources.Res
import com.cmptemplate.core.designsystem.generated.resources.about_run_setup
import com.cmptemplate.core.designsystem.generated.resources.about_settings
import com.cmptemplate.core.designsystem.generated.resources.about_title
import com.cmptemplate.core.designsystem.generated.resources.common_back
import com.cmptemplate.core.designsystem.generated.resources.feedback_no_mail_app
import com.cmptemplate.core.designsystem.generated.resources.feedback_rate_limited
import com.cmptemplate.core.designsystem.generated.resources.feedback_sending
import com.cmptemplate.core.designsystem.generated.resources.feedback_sent
import com.cmptemplate.core.designsystem.generated.resources.feedback_sent_id
import com.cmptemplate.core.designsystem.i18n.AppLocale
import com.cmptemplate.core.model.AppInfo
import com.cmptemplate.core.model.ThemeMode
import com.cmptemplate.feature.about.ControlFixLinks
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Wires the ViewModels; [AboutScreen] itself is state in, callbacks out (and screenshot-testable). */
@Composable
fun AboutRoot(
    onBack: (() -> Unit)?,
    onRunSetupAgain: (() -> Unit)?,
    viewModel: AboutViewModel = koinViewModel(),
    feedbackViewModel: FeedbackViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedback by feedbackViewModel.state.collectAsStateWithLifecycle()
    AboutScreen(
        app = state.app,
        locale = state.locale,
        theme = state.theme,
        category = state.category,
        onCategory = { viewModel.onAction(AboutAction.SelectCategory(it)) },
        feedback = feedback,
        onLocale = { viewModel.onAction(AboutAction.SetLocale(it)) },
        onTheme = { viewModel.onAction(AboutAction.SetTheme(it)) },
        onFeedback = feedbackViewModel::onAction,
        onBack = onBack,
        onRunSetupAgain = onRunSetupAgain?.let { navigate -> { viewModel.onAction(AboutAction.RunSetupAgain); navigate() } },
    )
}

/**
 * Settings: language, theme and (with onboarding) run setup again. Its action bar holds the
 * feedback menu ("Suggest an idea" / "Report a bug", one popup in two states) and the "i" that
 * opens About as a bottom sheet. Send closes the popup; the outcome is a snackbar here, or the
 * mail app when the reports service cannot be reached.
 */
@Composable
fun AboutScreen(
    app: AppInfo,
    locale: AppLocale,
    theme: ThemeMode,
    feedback: FeedbackState,
    onLocale: (AppLocale) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onFeedback: (FeedbackAction) -> Unit,
    onBack: (() -> Unit)?,
    onRunSetupAgain: (() -> Unit)?,
    uriHandler: UriHandler = LocalUriHandler.current,
    category: SettingsCategory = SettingsCategory.Language,
    onCategory: (SettingsCategory) -> Unit = {},
) {
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val currentOnFeedback by rememberUpdatedState(onFeedback)

    // Snackbar per outcome. "Sending..." stays until the result replaces it (this effect restarts).
    LaunchedEffect(feedback.notice) {
        val notice = feedback.notice ?: return@LaunchedEffect
        when (notice) {
            FeedbackNotice.Sending -> snackbar.showSnackbar(getString(Res.string.feedback_sending), duration = SnackbarDuration.Indefinite)
            is FeedbackNotice.Sent -> snackbar.showSnackbar(
                if (notice.ticketId.isBlank()) getString(Res.string.feedback_sent) else getString(Res.string.feedback_sent_id, notice.ticketId),
            )
            FeedbackNotice.RateLimited -> snackbar.showSnackbar(getString(Res.string.feedback_rate_limited))
            FeedbackNotice.NoMailApp -> snackbar.showSnackbar(getString(Res.string.feedback_no_mail_app, ControlFixLinks.BUG_REPORT_EMAIL))
        }
        currentOnFeedback(FeedbackAction.NoticeShown)
    }
    // The service could not be reached: hand the text to the mail app instead.
    LaunchedEffect(feedback.mailFallback) {
        val uri = feedback.mailFallback ?: return@LaunchedEffect
        currentOnFeedback(FeedbackAction.MailHandled(uriHandler.openSafely(uri)))
    }

    Box(Modifier.fillMaxSize().testTag("about_screen").background(MaterialTheme.colorScheme.background)) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            if (maxWidth >= TwoPaneMinWidth) {
                // Wide window: one app bar on top, categories on the left, the chosen one on the right.
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingsHeader(onBack, onFeedback, onAbout = { showAbout = true }, Modifier.fillMaxWidth())
                    TwoPane(
                        first = {
                            Column(
                                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CategoryRow(category == SettingsCategory.Language, AppIcons.Language, stringResource(Res.string.language), "category_language") {
                                    onCategory(SettingsCategory.Language)
                                }
                                CategoryRow(category == SettingsCategory.Theme, AppIcons.ThemeAuto, stringResource(Res.string.theme), "category_theme") {
                                    onCategory(SettingsCategory.Theme)
                                }
                                if (onRunSetupAgain != null) {
                                    OutlinedButton(onClick = onRunSetupAgain, Modifier.testTag("about_run_setup"), colors = brandOutlinedButtonColors()) {
                                        Text(stringResource(Res.string.about_run_setup))
                                    }
                                }
                            }
                        },
                        second = {
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp)) {
                                SectionCard {
                                    when (category) {
                                        SettingsCategory.Language -> LanguageChoices(locale, onLocale)
                                        SettingsCategory.Theme -> ThemeChoices(theme, onTheme)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        firstWeight = 0.35f,
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SettingsHeader(onBack, onFeedback, onAbout = { showAbout = true }, Modifier.fillMaxWidth().widthIn(max = ContentMaxWidth))

                    SectionCard {
                        LanguageChoices(locale, onLocale)
                        ThemeChoices(theme, onTheme)
                        if (onRunSetupAgain != null) {
                            OutlinedButton(onClick = onRunSetupAgain, Modifier.testTag("about_run_setup"), colors = brandOutlinedButtonColors()) {
                                Text(stringResource(Res.string.about_run_setup))
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
    }

    if (showAbout) AboutSheet(app, uriHandler) { showAbout = false }
    feedback.open?.let { open ->
        FeedbackDialog(
            bug = open.bug,
            crashAt = open.crashAt,
            onSend = { onFeedback(FeedbackAction.Send(it)) },
            onDismiss = { onFeedback(FeedbackAction.Close) },
        )
    }
}

/** Opens a link; false when nothing on the device can (no browser / no mail app). */
internal fun UriHandler.openSafely(uri: String): Boolean = try {
    openUri(uri)
    true
} catch (e: Exception) {
    false
}

/** Local "yyyy-MM-dd HH:mm"; numbers only, so no per-locale month names to translate. */
internal fun formatTime(epochMillis: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.two() = toString().padStart(2, '0')
    return "${t.year}-${t.monthNumber.two()}-${t.dayOfMonth.two()} ${t.hour.two()}:${t.minute.two()}"
}

/** Back, title, feedback menu and the "i" that opens About: one action bar for both layouts. */
@Composable
private fun SettingsHeader(onBack: (() -> Unit)?, onFeedback: (FeedbackAction) -> Unit, onAbout: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ScreenHeader(stringResource(Res.string.about_settings), onBack, stringResource(Res.string.common_back), Modifier.weight(1f))
        FeedbackMenu(onPick = { onFeedback(FeedbackAction.Open(it)) })
        IconButton(onClick = onAbout, modifier = Modifier.testTag("about_open")) {
            Icon(AppIcons.Info, contentDescription = stringResource(Res.string.about_title), tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** A selectable settings category (left pane on wide windows); at least 48dp tall. */
@Composable
private fun CategoryRow(selected: Boolean, icon: ImageVector, label: String, tag: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, Modifier.size(24.dp), tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.titleMedium, color = if (selected) colors.onSecondaryContainer else colors.onSurface)
    }
}
