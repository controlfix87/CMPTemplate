package com.cmptemplate.app.screenshots

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cmptemplate.app.App
import com.cmptemplate.app.androidHostModule
import com.cmptemplate.app.appModules
import com.cmptemplate.core.designsystem.CMPTemplateTheme
import com.cmptemplate.core.datastore.InMemoryUserPreferences
import com.cmptemplate.core.datastore.UserPreferencesRepository
import com.cmptemplate.core.model.ThemeMode
import com.cmptemplate.core.model.UserSettings
import com.cmptemplate.core.designsystem.i18n.AppLocale
import com.cmptemplate.core.designsystem.i18n.AppLocaleProvider
import com.cmptemplate.core.model.AppInfo
import androidx.compose.ui.platform.LocalUriHandler
import com.cmptemplate.feature.about.ui.AboutScreen
import com.cmptemplate.feature.about.ui.AboutSheetContent
import com.cmptemplate.feature.about.ui.FeedbackDialog
import com.cmptemplate.feature.about.ui.FeedbackState
import com.cmptemplate.feature.about.ui.SettingsCategory
import com.cmptemplate.feature.example.ui.ExampleScreen
import com.cmptemplate.feature.example.ui.ExampleState
import com.cmptemplate.core.model.ExampleItem
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.cmptemplate.feature.onboarding.ui.OnboardingScreen // [onboarding]
import com.cmptemplate.feature.onboarding.ui.OnboardingState // [onboarding]
import com.cmptemplate.feature.onboarding.ui.OnboardingStep // [onboarding]
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A phone in portrait and the Galaxy Z Fold's inner screen (GameController sizes). */
private const val PHONE = "w411dp-h891dp-xxhdpi"
private const val FOLD = "w882dp-h736dp-land-xhdpi"
// Fold inner screen (portrait, ~w673dp-h841dp) and a landscape tablet: both are two-pane widths.
private const val FOLD_INNER = "w673dp-h841dp-xhdpi"
private const val TABLET = "w1280dp-h800dp-land-xhdpi"
private const val DIR = "build/outputs/roborazzi"

/**
 * Shared screens drawn on the JVM, no phone needed: `./gradlew :androidApp:recordRoborazziDebug`
 * writes them to androidApp/build/outputs/roborazzi/, `verifyRoborazziDebug` compares against the
 * last recording. Screens are stateless (state in, callbacks out), so each shot sets its state
 * directly. GameController's screenshot tests found light-on-light text, overlapping buttons and
 * landscape overflow that no unit test saw: look at the images after every UI change.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// A plain Application: the real one starts Koin itself, and Robolectric makes one per test.
@Config(sdk = [36], qualifiers = PHONE, application = Application::class)
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = AppInfo("com.cmptemplate.app", "CMPTemplate", "1.0.0", "1", "android/debug", "android")

    @Before fun koin() {
        startKoin {
            val context = ApplicationProvider.getApplicationContext<Application>()
            androidContext(context)
            // In-memory settings, onboarding already done: Home is the start screen.
            modules(appModules + androidHostModule(context) + module {
                single<UserPreferencesRepository> { InMemoryUserPreferences(UserSettings(onboardingDone = true)) }
            })
        }
    }

    @After fun stop() {
        stopKoin()
        // AppLocaleProvider sets the process default; keep the next test on English.
        java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    }

    private fun shot(name: String, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent { CMPTemplateTheme(darkTheme = dark) { content() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$DIR/$name.png")
    }

    @Test fun home() {
        compose.setContent { App() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("$DIR/home_phone.png")
    }

    // region onboarding
    @Test fun onboardingSteps() {
        // setContent may run once per test: switch the step through state instead.
        var step by mutableStateOf(OnboardingStep.WELCOME)
        compose.setContent {
            CMPTemplateTheme(darkTheme = true) { OnboardingScreen(OnboardingState(step, AppLocale.English, ThemeMode.SYSTEM)) {} }
        }
        for (s in OnboardingStep.entries) {
            compose.runOnIdle { step = s }
            compose.waitForIdle()
            compose.onRoot().captureRoboImage("$DIR/onboarding_${s.ordinal + 1}_${s.name.lowercase()}.png")
        }
    }

    @Test fun onboardingHebrewLight() = shot("onboarding_welcome_he_light", dark = false) {
        // Through AppLocaleProvider, as in the app: Hebrew strings AND right-to-left layout.
        AppLocaleProvider(AppLocale.Hebrew) {
            OnboardingScreen(OnboardingState(OnboardingStep.WELCOME, AppLocale.Hebrew, ThemeMode.LIGHT)) {}
        }
    }

    @Test @Config(qualifiers = FOLD) fun onboardingFold() = shot("onboarding_welcome_fold") {
        OnboardingScreen(OnboardingState(OnboardingStep.WELCOME, AppLocale.English, ThemeMode.DARK)) {}
    }
    // endregion onboarding

    @Test fun about() = shot("about_dark") { AboutContent() }

    @Test fun aboutLight() = shot("about_light", dark = false) { AboutContent() }

    @Test @Config(qualifiers = FOLD) fun aboutFold() = shot("about_fold") { AboutContent() }

    @Test fun aboutSheet() = shot("about_sheet_dark") { AboutSheetContent(app, LocalUriHandler.current) }

    @Test fun aboutSheetLight() = shot("about_sheet_light", dark = false) { AboutSheetContent(app, LocalUriHandler.current) }

    // The feedback popup is a dialog window: capture the whole screen, not the compose root.
    @Test fun feedbackSuggestion() = dialogShot("feedback_suggestion_dark") {
        FeedbackDialog(bug = false, crashAt = null, onSend = {}, onDismiss = {})
    }

    @Test fun feedbackBugLight() = dialogShot("feedback_bug_light", dark = false) {
        FeedbackDialog(bug = true, crashAt = 1_790_000_000_000L, onSend = {}, onDismiss = {})
    }

    @Test fun feedbackBugHebrew() = dialogShot("feedback_bug_he_dark") {
        AppLocaleProvider(AppLocale.Hebrew) { FeedbackDialog(bug = true, crashAt = null, onSend = {}, onDismiss = {}) }
    }

    // Why not waitForIdle(): the dialog's window takes focus, which focuses its first text field,
    // and a focused field runs an endless cursor-blink animation. The compose test clock treats a
    // running animation as "busy", so waitForIdle() timed out ("Compose did not get idle"). With
    // the clock paused we step it by hand: enough frames to compose and lay out the dialog window.
    @OptIn(ExperimentalRoborazziApi::class)
    private fun dialogShot(name: String, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        // Activity.setContent: the rule's setContent would wait for idle, which the cursor blink prevents.
        compose.activity.setContent { CMPTemplateTheme(darkTheme = dark) { content() } }
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeBy(1_000)
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage("$DIR/$name.png")
    }

    // region two-pane (Example list + detail, settings categories, onboarding welcome)
    private val items = (1..8).map { ExampleItem("$it", "Sample $it") }

    @Test @Config(qualifiers = FOLD_INNER) fun exampleTwoPaneFold() = shot("example_two_pane_fold") {
        ExampleScreen(ExampleState(items = items, isLoading = false, selectedId = "2"), onAction = {}, onDetail = {})
    }

    @Test @Config(qualifiers = TABLET) fun exampleTwoPaneTablet() = shot("example_two_pane_tablet") {
        ExampleScreen(ExampleState(items = items, isLoading = false, selectedId = "3"), onAction = {}, onDetail = {})
    }

    @Test @Config(qualifiers = TABLET) fun exampleTwoPaneTabletNoSelection() = shot("example_two_pane_tablet_empty", dark = false) {
        ExampleScreen(ExampleState(items = items, isLoading = false), onAction = {}, onDetail = {})
    }

    @Test @Config(qualifiers = FOLD_INNER) fun aboutTwoPaneFold() = shot("about_two_pane_fold") { AboutContent() }

    @Test @Config(qualifiers = TABLET) fun aboutTwoPaneTablet() = shot("about_two_pane_tablet") { AboutContent(SettingsCategory.Theme) }

    @Test @Config(qualifiers = TABLET) fun onboardingTablet() = shot("onboarding_welcome_tablet") {
        OnboardingScreen(OnboardingState(OnboardingStep.WELCOME, AppLocale.English, ThemeMode.DARK)) {}
    }
    // endregion two-pane

    @Composable
    private fun AboutContent(category: SettingsCategory = SettingsCategory.Language) = AboutScreen(
        category = category,
        app = app, locale = AppLocale.English, theme = ThemeMode.SYSTEM, feedback = FeedbackState(),
        onLocale = {}, onTheme = {}, onFeedback = {}, onBack = {},
        onRunSetupAgain = {}, // [onboarding]
        // onRunSetupAgain = null, // without onboarding
    )
}
