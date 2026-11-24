package com.cmptemplate.feature.about.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.cmptemplate.core.datastore.InMemoryUserPreferences
import com.cmptemplate.core.designsystem.i18n.LocaleManager
import com.cmptemplate.core.designsystem.i18n.LocaleStore
import com.cmptemplate.core.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class MemoryStore : LocaleStore {
    var code: String? = null
    override fun read() = code
    override fun write(code: String) { this.code = code }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AboutViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()
    private val app = AppInfo("com.example.app", "Example", "1", "1", "test", "android")

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun tearDown() { stores.forEach { it.clear() }; stores.clear(); Dispatchers.resetMain() }

    private fun model(saved: SavedStateHandle = SavedStateHandle()): AboutViewModel {
        val store = ViewModelStore().also { stores.add(it) }
        val vm = viewModelFactory {
            initializer { AboutViewModel(app, LocaleManager(MemoryStore()), InMemoryUserPreferences(), saved) }
        }
        return ViewModelProvider.create(store, vm)[AboutViewModel::class]
    }

    @Test fun `category defaults to language and follows selection`() = runTest {
        val vm = model()
        vm.state.test {
            assertEquals(SettingsCategory.Language, expectMostRecentItem().category)
            vm.onAction(AboutAction.SelectCategory(SettingsCategory.Theme))
            assertEquals(SettingsCategory.Theme, expectMostRecentItem().category)
        }
    }

    @Test fun `selected category is restored from saved state`() = runTest {
        val saved = SavedStateHandle()
        model(saved).onAction(AboutAction.SelectCategory(SettingsCategory.Theme))
        model(SavedStateHandle(mapOf("settings_category" to saved.get<String>("settings_category")))).state.test {
            assertEquals(SettingsCategory.Theme, expectMostRecentItem().category)
        }
    }
}
