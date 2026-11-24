package com.cmptemplate.feature.onboarding.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmptemplate.core.model.ThemeMode
import com.cmptemplate.core.designsystem.components.AppIcons
import com.cmptemplate.core.designsystem.components.AppMark
import com.cmptemplate.core.designsystem.components.LanguageChoices
import com.cmptemplate.core.designsystem.components.StepProgress
import com.cmptemplate.core.designsystem.components.brandOutlinedButtonColors
import com.cmptemplate.core.designsystem.components.ThemeChoices
import com.cmptemplate.core.designsystem.generated.resources.Res
import com.cmptemplate.core.designsystem.generated.resources.app_name
import com.cmptemplate.core.designsystem.generated.resources.onb_app_body
import com.cmptemplate.core.designsystem.generated.resources.onb_app_title
import com.cmptemplate.core.designsystem.generated.resources.onb_back
import com.cmptemplate.core.designsystem.generated.resources.onb_done_body
import com.cmptemplate.core.designsystem.generated.resources.onb_done_title
import com.cmptemplate.core.designsystem.generated.resources.onb_next
import com.cmptemplate.core.designsystem.generated.resources.onb_skip
import com.cmptemplate.core.designsystem.generated.resources.onb_start
import com.cmptemplate.core.designsystem.generated.resources.onb_step_app
import com.cmptemplate.core.designsystem.generated.resources.onb_step_done
import com.cmptemplate.core.designsystem.generated.resources.onb_step_of
import com.cmptemplate.core.designsystem.generated.resources.onb_step_welcome
import com.cmptemplate.core.designsystem.generated.resources.onb_welcome_body
import com.cmptemplate.core.designsystem.generated.resources.onb_welcome_title
import com.cmptemplate.core.designsystem.i18n.AppLocale
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingRoot(onDone: () -> Unit, viewModel: OnboardingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onDone() }
    OnboardingScreen(state, viewModel::onAction)
}

/**
 * First run (LockdownControl / GameController pattern): numbered, titled step bar; language and
 * theme first, as cards; Back, Skip (every step but the last) and Next; the last step says where
 * to run setup again.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OnboardingScreen(state: OnboardingState, onAction: (OnboardingAction) -> Unit) {
    // System back walks back through the steps (LockdownControl); on the first step it is not taken.
    BackHandler(enabled = state.index > 0) { onAction(OnboardingAction.Back) }
    val titles = OnboardingStep.entries.map {
        stringResource(
            when (it) {
                OnboardingStep.WELCOME -> Res.string.onb_step_welcome
                OnboardingStep.APP -> Res.string.onb_step_app
                OnboardingStep.DONE -> Res.string.onb_step_done
            },
        )
    }
    Box(
        Modifier.fillMaxSize().testTag("onboarding").background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = OnboardingMaxWidth).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                stringResource(Res.string.onb_step_of, state.index + 1, state.count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StepProgress(titles, state.index, onStep = { onAction(OnboardingAction.GoTo(it)) }, Modifier.padding(top = 4.dp))
            AnimatedContent(targetState = state.step, modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter, label = "onboarding") { step ->
                Column(
                    Modifier.fillMaxSize().then(if (step == OnboardingStep.WELCOME) Modifier else Modifier.widthIn(max = 720.dp)).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep(
                            state.locale, state.theme,
                            onLocale = { onAction(OnboardingAction.SetLocale(it)) },
                            onTheme = { onAction(OnboardingAction.SetTheme(it)) },
                        )
                        OnboardingStep.APP -> AppStep()
                        OnboardingStep.DONE -> DoneStep()
                    }
                }
            }
            val back: @Composable () -> Unit = {
                if (state.index > 0) {
                    OutlinedButton(onClick = { onAction(OnboardingAction.Back) }, Modifier.testTag("onboarding_back"), colors = brandOutlinedButtonColors()) {
                        Text(stringResource(Res.string.onb_back))
                    }
                }
            }
            val skip: @Composable () -> Unit = {
                TextButton(onClick = { onAction(OnboardingAction.Skip) }, Modifier.testTag("onboarding_skip")) {
                    Text(stringResource(Res.string.onb_skip))
                }
            }
            val forward: @Composable () -> Unit = {
                if (!state.isLast) {
                    Button(onClick = { onAction(OnboardingAction.Next) }, Modifier.testTag("onboarding_next")) {
                        Text(stringResource(Res.string.onb_next))
                    }
                } else {
                    Button(onClick = { onAction(OnboardingAction.Finish) }, Modifier.testTag("onboarding_finish")) {
                        Text(stringResource(Res.string.onb_start))
                    }
                }
            }
            if (LocalDensity.current.fontScale >= LargeFontScale) {
                // Large text: the three buttons would not fit one row; stack them, main action first.
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    forward()
                    if (!state.isLast) skip()
                    back()
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    back()
                    Spacer(Modifier.weight(1f))
                    if (!state.isLast) {
                        skip()
                        Spacer(Modifier.width(8.dp))
                    }
                    forward()
                }
            }
        }
    }
}

/** From this font scale the navigation buttons stack vertically. */
private const val LargeFontScale = 1.5f

/** Wide windows put the greeting on the left and the language/theme cards on the right. */
@Composable
private fun WelcomeStep(locale: AppLocale, theme: ThemeMode, onLocale: (AppLocale) -> Unit, onTheme: (ThemeMode) -> Unit) {
    val intro: @Composable ColumnScope.() -> Unit = {
        AppMark(Modifier.align(Alignment.CenterHorizontally), size = 88.dp)
        Text(
            stringResource(Res.string.onb_welcome_title, stringResource(Res.string.app_name)),
            Modifier.fillMaxWidth().semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(Res.string.onb_welcome_body),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    val choices: @Composable ColumnScope.() -> Unit = {
        LanguageChoices(locale, onLocale)
        ThemeChoices(theme, onTheme)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= WideWelcomeMinWidth) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = intro)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), content = choices)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                intro()
                choices()
            }
        }
    }
}

/** Below this the two columns would squeeze the language cards (a Fold inner screen stays single column). */
private val WideWelcomeMinWidth = 720.dp

/** Wider than the shared readable width so the two-column welcome has room on tablets. */
private val OnboardingMaxWidth = 960.dp

/** Template step: replace with the product's own (keep the title + body + controls shape). */
@Composable
private fun AppStep() {
    Text(stringResource(Res.string.onb_app_title), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
    Text(stringResource(Res.string.onb_app_body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ColumnScope.DoneStep() {
    Spacer(Modifier.height(24.dp))
    Icon(
        AppIcons.Check,
        contentDescription = null,
        modifier = Modifier.size(88.dp).align(Alignment.CenterHorizontally),
        tint = MaterialTheme.colorScheme.primary,
    )
    Text(
        stringResource(Res.string.onb_done_title),
        Modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        stringResource(Res.string.onb_done_body),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
