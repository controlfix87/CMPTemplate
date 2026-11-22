package com.cmptemplate.feature.about.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cmptemplate.core.common.DefaultDispatcherProvider
import com.cmptemplate.core.common.DispatcherProvider
import com.cmptemplate.core.model.AppInfo
import com.cmptemplate.feature.about.report.Diagnostics
import com.cmptemplate.feature.about.report.ReportDraft
import com.cmptemplate.feature.about.report.ReportKind
import com.cmptemplate.feature.about.report.ReportResult
import com.cmptemplate.feature.about.report.ReportSender
import com.cmptemplate.feature.about.report.SupportMail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The open feedback popup: a bug report ([bug]) or a suggestion. [crashAt] only for a bug report. */
data class FeedbackOpen(val bug: Boolean, val crashAt: Long? = null)

/** What the snackbar says after Send. */
sealed interface FeedbackNotice {
    data object Sending : FeedbackNotice
    data class Sent(val ticketId: String) : FeedbackNotice
    data object RateLimited : FeedbackNotice
    /** The service could not take it and no mail app opened either. */
    data object NoMailApp : FeedbackNotice
}

data class FeedbackState(
    val open: FeedbackOpen? = null,
    val notice: FeedbackNotice? = null,
    /** A `mailto:` the screen must open now (the service could not be reached); then [FeedbackAction.MailHandled]. */
    val mailFallback: String? = null,
)

sealed interface FeedbackAction {
    /** From the feedback menu: true "Report a bug", false "Suggest an idea". */
    data class Open(val bug: Boolean) : FeedbackAction
    data object Close : FeedbackAction
    data class Send(val draft: ReportDraft) : FeedbackAction
    data object NoticeShown : FeedbackAction
    data class MailHandled(val opened: Boolean) : FeedbackAction
}

/**
 * "Suggest an idea" / "Report a bug": which popup is open (survives rotation and process death; the
 * popup keeps its own fields with rememberSaveable), the send, and its outcome. The popup closes
 * before sending; the result is a snackbar, or the mail app when the service cannot be reached.
 */
class FeedbackViewModel(
    private val sender: ReportSender,
    private val diagnostics: Diagnostics,
    private val app: AppInfo,
    private val saved: SavedStateHandle,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ViewModel() {
    private val openKind = saved.getStateFlow<String?>(KEY_OPEN, null)
    private val crashAt = MutableStateFlow<Long?>(null)
    private val notice = MutableStateFlow<FeedbackNotice?>(null)
    private val mail = MutableStateFlow<String?>(null)

    val state: StateFlow<FeedbackState> = combine(openKind, crashAt, notice, mail) { kind, crash, n, m ->
        val bug = kind == ReportKind.BUG.wire
        FeedbackState(kind?.let { FeedbackOpen(bug, if (bug) crash else null) }, n, m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedbackState())

    init {
        if (openKind.value == ReportKind.BUG.wire) loadCrash()
    }

    fun onAction(action: FeedbackAction) {
        when (action) {
            is FeedbackAction.Open -> {
                saved[KEY_OPEN] = (if (action.bug) ReportKind.BUG else ReportKind.SUGGESTION).wire
                crashAt.value = null
                if (action.bug) loadCrash()
            }
            FeedbackAction.Close -> saved[KEY_OPEN] = null
            is FeedbackAction.Send -> send(action.draft)
            FeedbackAction.NoticeShown -> if (notice.value != FeedbackNotice.Sending) notice.value = null
            is FeedbackAction.MailHandled -> {
                mail.value = null
                if (!action.opened) notice.value = FeedbackNotice.NoMailApp
            }
        }
    }

    /** The crash time is a disk read: off the main thread, and only for a bug report. */
    private fun loadCrash() {
        viewModelScope.launch {
            crashAt.value = withContext(dispatchers.io) { diagnostics.lastCrash()?.atEpochMillis }
        }
    }

    private fun send(input: ReportDraft) {
        if (notice.value == FeedbackNotice.Sending || input.title.isBlank() || input.message.isBlank()) return
        // Logs only ever go with a bug report.
        val draft = input.copy(includeLogs = input.includeLogs && input.kind == ReportKind.BUG)
        saved[KEY_OPEN] = null
        notice.value = FeedbackNotice.Sending
        viewModelScope.launch {
            val result = withContext(dispatchers.io) { sender.send(draft) }
            when (result) {
                is ReportResult.Sent -> notice.value = FeedbackNotice.Sent(result.ticketId)
                ReportResult.RateLimited -> notice.value = FeedbackNotice.RateLimited
                ReportResult.Offline, ReportResult.Rejected -> {
                    notice.value = null
                    mail.value = SupportMail.uri(draft, app, diagnostics.device())
                }
            }
        }
    }

    private companion object {
        const val KEY_OPEN = "feedback_open"
    }
}
