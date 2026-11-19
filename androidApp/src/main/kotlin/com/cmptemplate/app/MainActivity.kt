package com.cmptemplate.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cmptemplate.core.designsystem.i18n.AndroidAppLocale

class MainActivity : ComponentActivity() {

    /** False until [App] draws its first real screen; holds the system splash until then. */
    private var firstScreenReady = false

    /**
     * Forces this Activity's resources onto the chosen language, so `resources.getString`, date and
     * number formatting -- anything reading `context.resources` rather than Compose's
     * `stringResource()` -- agrees with the UI from the very first frame.
     * [CMPTemplateApplication] does the same for the Application context, which is the one Android
     * re-derives the process locale from.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AndroidAppLocale.wrap(newBase))
    }

    /**
     * Every configuration change makes Android re-derive the locale from the device configuration,
     * overriding the in-app choice. Patch the incoming config *before* `super` so the framework
     * propagates the corrected locale to Compose, which reads it via `LocalConfiguration`.
     *
     * This Activity does not declare `android:configChanges`, so normal recreation is preserved (an
     * explicit template rule) and `attachBaseContext` re-applies the locale on the way back up; the
     * patch here covers the changes the platform delivers without a recreation.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        AndroidAppLocale.patch(newConfig, this)
        super.onConfigurationChanged(newConfig)
    }

    /** Repairs the process locale after anything that reset it while this Activity was not resumed. */
    override fun onResume() {
        super.onResume()
        AndroidAppLocale.reassert(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Official SplashScreen API: must run before super.onCreate(); swaps the launch theme
        // (Theme.CMPTemplate.Starting) for postSplashScreenTheme.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash up while the settings (theme, onboarding) are read, so the first frame is
        // the right screen in the right theme, not an empty background. Checked before every frame
        // on the main thread, so a plain field is enough. Never hold it for network or other slow work.
        splashScreen.setKeepOnScreenCondition { !firstScreenReady }
        enableEdgeToEdge()
        setContent { App(onFirstScreenReady = { firstScreenReady = true }) }
    }
}
