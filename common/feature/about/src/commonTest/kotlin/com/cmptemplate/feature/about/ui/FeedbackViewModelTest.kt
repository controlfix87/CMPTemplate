package com.cmptemplate.feature.about.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.cmptemplate.core.common.DispatcherProvider
import com.cmptemplate.core.model.AppInfo
import com.cmptemplate.feature.about.report.CrashRecord
import com.cmptemplate.feature.about.report.DeviceInfo
import com.cmptemplate.feature.about.report.Diagnostics
import com.cmptemplate.feature.about.report.ReportDraft
import com.cmptemplate.feature.about.report.ReportKind
import com.cmptemplate.feature.about.report.ReportResult
import com.cmptemplate.feature.about.report.ReportSender
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSender(var result: ReportResult = ReportResult.Sent("T-1")) : ReportSender {
    val sent = mutableListOf<ReportDraft>()
    override suspend fun send(draft: ReportDraft): ReportResult { sent += draft; return result }
}

private class CrashDiagnostics(private val at: Long?) : Diagnostics {
    var crashReads = 0
    override fun device() = DeviceInfo()
    override suspend fun recentLog(): String? = null
    override fun lastCrash() = at?.let { crashReads++; CrashRecord(it, "trace") }
    override fun clearCrash() = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }
    private val stores = mutableListOf<ViewModelStore>()
    private val app = AppInfo("com.example.app", "Example", "1", "1", "test", "android")

    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { stores.forEach { it.clear() }; stores.clear(); Dispatchers.resetMain() }

    private fun model(
        sender: ReportSender = FakeSender(),
        diagnostics: Diagnostics = CrashDiagnostics(null),
        saved: SavedStateHandle = SavedStateHandle(),
    ): FeedbackViewModel {
        val store = ViewModelStore().also { stores.add(it) }
        return ViewModelProvider.create(store, viewModelFactory {
            initializer { FeedbackViewModel(sender, diagnostics, app, saved, dispatchers) }
        })[FeedbackViewModel::class]
    }

    @Test fun `the menu opens the popup in the picked state, and only a bug report reads the crash`() = runTest {
        val diagnostics = CrashDiagnostics(1234L)
        val vm = model(diagnostics = diagnostics)
        vm.state.test {
            assertNull(expectMostRecentItem().open)
            vm.onAction(FeedbackAction.Open(bug = false))
            assertEquals(FeedbackOpen(bug = false, crashAt = null), expectMostRecentItem().open)
            assertEquals(0, diagnostics.crashReads)
            vm.onAction(FeedbackAction.Open(bug = true))
            assertEquals(FeedbackOpen(bug = true, crashAt = 1234L), expectMostRecentItem().open)
            vm.onAction(FeedbackAction.Close)
            assertNull(expectMostRecentItem().open)
        }
    }

    @Test fun `send closes the popup first, then shows the ticket`() = runTest {
        val sender = FakeSender()
        val vm = model(sender)
        vm.state.test {
            vm.onAction(FeedbackAction.Open(bug = true))
            vm.onAction(FeedbackAction.Send(ReportDraft(ReportKind.BUG, "T", "M", includeLogs = true)))
            val s = expectMostRecentItem()
            assertNull(s.open)
            assertEquals(FeedbackNotice.Sent("T-1"), s.notice)
            vm.onAction(FeedbackAction.NoticeShown)
            assertNull(expectMostRecentItem().notice)
        }
        assertEquals(listOf(ReportDraft(ReportKind.BUG, "T", "M", includeLogs = true)), sender.sent)
    }

    @Test fun `a suggestion never carries logs, and a blank draft is not sent`() = runTest {
        val sender = FakeSender()
        val vm = model(sender)
        vm.onAction(FeedbackAction.Send(ReportDraft(ReportKind.SUGGESTION, "", "M")))
        vm.onAction(FeedbackAction.Send(ReportDraft(ReportKind.SUGGESTION, "T", "M", includeLogs = true)))
        assertEquals(listOf(ReportDraft(ReportKind.SUGGESTION, "T", "M", includeLogs = false)), sender.sent)
    }

    @Test fun `rate limit is a notice`() = runTest {
        val vm = model(FakeSender(ReportResult.RateLimited))
        vm.state.test {
            vm.onAction(FeedbackAction.Send(ReportDraft(ReportKind.BUG, "T", "M")))
            assertEquals(FeedbackNotice.RateLimited, expectMostRecentItem().notice)
        }
    }

    @Test fun `service unreachable opens the mail app, and no mail app is a notice`() = runTest {
        val vm = model(FakeSender(ReportResult.Offline))
        vm.state.test {
            vm.onAction(FeedbackAction.Send(ReportDraft(ReportKind.BUG, "T", "M")))
            val s = expectMostRecentItem()
            assertTrue(s.mailFallback!!.startsWith("mailto:bugreport@controlfix.net"))
            assertNull(s.notice)
            vm.onAction(FeedbackAction.MailHandled(opened = false))
            val after = expectMostRecentItem()
            assertNull(after.mailFallback)
            assertEquals(FeedbackNotice.NoMailApp, after.notice)
        }
    }

    @Test fun `the open popup survives process death`() = runTest {
        val vm = model(diagnostics = CrashDiagnostics(99L), saved = SavedStateHandle(mapOf("feedback_open" to "bug")))
        vm.state.test {
            assertEquals(FeedbackOpen(bug = true, crashAt = 99L), expectMostRecentItem().open)
        }
    }
}
