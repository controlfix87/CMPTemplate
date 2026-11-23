package com.cmptemplate.feature.about.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cmptemplate.core.designsystem.components.AppIcons
import com.cmptemplate.core.designsystem.components.AppMark
import com.cmptemplate.core.designsystem.components.Wordmark
import com.cmptemplate.core.designsystem.generated.resources.Res
import com.cmptemplate.core.designsystem.generated.resources.about_by
import com.cmptemplate.core.designsystem.generated.resources.about_body
import com.cmptemplate.core.designsystem.generated.resources.about_privacy
import com.cmptemplate.core.designsystem.generated.resources.about_version
import com.cmptemplate.core.designsystem.generated.resources.app_name
import com.cmptemplate.core.designsystem.generated.resources.feedback_bug_field_message
import com.cmptemplate.core.designsystem.generated.resources.feedback_bug_field_title
import com.cmptemplate.core.designsystem.generated.resources.feedback_bug_hint
import com.cmptemplate.core.designsystem.generated.resources.feedback_bug_intro
import com.cmptemplate.core.designsystem.generated.resources.feedback_bug_title
import com.cmptemplate.core.designsystem.generated.resources.feedback_cancel
import com.cmptemplate.core.designsystem.generated.resources.feedback_crash_recorded
import com.cmptemplate.core.designsystem.generated.resources.feedback_entry
import com.cmptemplate.core.designsystem.generated.resources.feedback_field_contact
import com.cmptemplate.core.designsystem.generated.resources.feedback_include_logs
import com.cmptemplate.core.designsystem.generated.resources.feedback_logs_hint
import com.cmptemplate.core.designsystem.generated.resources.feedback_privacy
import com.cmptemplate.core.designsystem.generated.resources.feedback_send_bug
import com.cmptemplate.core.designsystem.generated.resources.feedback_send_suggestion
import com.cmptemplate.core.designsystem.generated.resources.feedback_suggest_field_message
import com.cmptemplate.core.designsystem.generated.resources.feedback_suggest_field_title
import com.cmptemplate.core.designsystem.generated.resources.feedback_suggest_hint
import com.cmptemplate.core.designsystem.generated.resources.feedback_suggest_intro
import com.cmptemplate.core.designsystem.generated.resources.feedback_suggest_title
import com.cmptemplate.core.model.AppInfo
import com.cmptemplate.feature.about.ControlFixLinks
import com.cmptemplate.feature.about.report.ReportDraft
import com.cmptemplate.feature.about.report.ReportKind
import com.cmptemplate.feature.about.report.ReportSender
import org.jetbrains.compose.resources.stringResource

/**
 * The action-bar feedback entry: a short menu, "Suggest an idea" (lightbulb, brand colour) or
 * "Report a bug" (bug, error red). Each opens the same popup directly in its state.
 */
@Composable
fun FeedbackMenu(onPick: (bug: Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.testTag("feedback_menu")) {
            // Explicit tint: the screen is not inside a Surface, so the default content colour is black.
            Icon(AppIcons.Feedback, contentDescription = stringResource(Res.string.feedback_entry), tint = MaterialTheme.colorScheme.onBackground)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.feedback_suggest_title)) },
                leadingIcon = { Icon(AppIcons.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { open = false; onPick(false) },
                modifier = Modifier.testTag("feedback_menu_suggest"),
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.feedback_bug_title)) },
                leadingIcon = { Icon(AppIcons.Bug, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onPick(true) },
                modifier = Modifier.testTag("feedback_menu_bug"),
            )
        }
    }
}

/**
 * "Suggest an idea" ([bug] false) or "Report a bug" ([bug] true): one popup, two designed states,
 * no switch inside. A suggestion: lightbulb, brand accent, never a log. A bug report: bug, error
 * accent, and a consent box for the app log that stays OFF until the user ticks it; with a
 * recorded crash ([crashAt], epoch ms) and the box ticked, a note says its log goes too.
 * Send hands the draft to [onSend]; nothing is sent from here.
 */
@Composable
fun FeedbackDialog(
    bug: Boolean,
    crashAt: Long?,
    onSend: (ReportDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    // Consent to send the app log: never assumed.
    var includeLogs by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val accent by animateColorAsState(if (bug) colors.error else colors.primary, label = "accent")
    val onAccent by animateColorAsState(if (bug) colors.onError else colors.onPrimary, label = "onAccent")
    val accentContainer by animateColorAsState(if (bug) colors.errorContainer else colors.primaryContainer, label = "container")
    val onAccentContainer by animateColorAsState(if (bug) colors.onErrorContainer else colors.onPrimaryContainer, label = "onContainer")
    val dialogColor by animateColorAsState(
        lerp(colors.surfaceContainerHigh, if (bug) colors.errorContainer else colors.primaryContainer, 0.4f),
        label = "dialog",
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedLabelColor = accent, cursorColor = accent)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogColor,
        modifier = Modifier.testTag("feedback_dialog"),
        icon = {
            Box(Modifier.size(56.dp).clip(CircleShape).background(accentContainer), contentAlignment = Alignment.Center) {
                Crossfade(bug, label = "icon") { isBug ->
                    Icon(
                        if (isBug) AppIcons.Bug else AppIcons.Lightbulb,
                        contentDescription = null,
                        tint = onAccentContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        },
        title = {
            Text(
                stringResource(if (bug) Res.string.feedback_bug_title else Res.string.feedback_suggest_title),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (bug) stringResource(Res.string.feedback_bug_intro)
                    else stringResource(Res.string.feedback_suggest_intro, stringResource(Res.string.app_name)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(ReportSender.TITLE_MAX).replace('\n', ' ') },
                    label = { Text(stringResource(if (bug) Res.string.feedback_bug_field_title else Res.string.feedback_suggest_field_title)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("feedback_field_title"),
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(ReportSender.MESSAGE_MAX) },
                    label = { Text(stringResource(if (bug) Res.string.feedback_bug_field_message else Res.string.feedback_suggest_field_message)) },
                    placeholder = { Text(stringResource(if (bug) Res.string.feedback_bug_hint else Res.string.feedback_suggest_hint)) },
                    minLines = 4,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).testTag("feedback_field_message"),
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it.take(ReportSender.CONTACT_MAX).trim() },
                    label = { Text(stringResource(Res.string.feedback_field_contact)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag("feedback_field_contact"),
                )
                // The app log helps with a bug only, and goes only with consent.
                AnimatedVisibility(bug) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth()
                                .toggleable(value = includeLogs, role = Role.Checkbox, onValueChange = { includeLogs = it })
                                .testTag("feedback_logs_consent"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = includeLogs, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = accent))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(Res.string.feedback_include_logs), style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(stringResource(Res.string.feedback_logs_hint), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        if (crashAt != null && includeLogs) {
                            Text(
                                stringResource(Res.string.feedback_crash_recorded, formatTime(crashAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                                modifier = Modifier.testTag("feedback_crash_recorded"),
                            )
                        }
                    }
                }
                Text(stringResource(Res.string.feedback_privacy), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kind = if (bug) ReportKind.BUG else ReportKind.SUGGESTION
                    onSend(ReportDraft(kind, title.trim(), message.trim(), contact.trim(), includeLogs = bug && includeLogs))
                },
                enabled = title.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                modifier = Modifier.testTag("feedback_send"),
            ) { Text(stringResource(if (bug) Res.string.feedback_send_bug else Res.string.feedback_send_suggestion)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("feedback_cancel")) { Text(stringResource(Res.string.feedback_cancel)) }
        },
    )
}

/**
 * About, in a bottom sheet from the "i" in the Settings action bar: mark, name, version, what the
 * app does, privacy policy, "By" ControlFix (taps open the website), website and support e-mail.
 * Bug reports and suggestions go through the feedback menu, not the support address.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(app: AppInfo, uriHandler: UriHandler, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("about_sheet"),
    ) {
        AboutSheetContent(app, uriHandler)
    }
}

/** The sheet's body, apart from the sheet so screenshot tests can draw it in place. */
@Composable
fun AboutSheetContent(app: AppInfo, uriHandler: UriHandler) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppMark(size = 84.dp)
        Wordmark(stringResource(Res.string.app_name))
        Text(stringResource(Res.string.about_version, app.version), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(stringResource(Res.string.about_body), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        TextButton(onClick = { uriHandler.openSafely(ControlFixLinks.PRIVACY) }, Modifier.testTag("about_privacy")) {
            Text(stringResource(Res.string.about_privacy))
        }
        Text(
            "—  " + stringResource(Res.string.about_by) + "  —",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // The ControlFix name in the brand style stands in for the logo image; taps open the website.
        Wordmark(
            "ControlFix",
            modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Link) { uriHandler.openSafely(ControlFixLinks.WEBSITE) }
                .heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 4.dp).testTag("about_controlfix"),
            highlight = "Control",
            style = MaterialTheme.typography.headlineMedium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(onClick = { uriHandler.openSafely(ControlFixLinks.WEBSITE) }, Modifier.testTag("about_website")) {
                Icon(AppIcons.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(ControlFixLinks.WEBSITE.removePrefix("https://").trimEnd('/'), Modifier.padding(start = 8.dp), maxLines = 1)
            }
            FilledTonalButton(onClick = { uriHandler.openSafely("mailto:${ControlFixLinks.SUPPORT_EMAIL}") }, Modifier.testTag("about_support")) {
                Icon(AppIcons.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(ControlFixLinks.SUPPORT_EMAIL, Modifier.padding(start = 8.dp), maxLines = 1)
            }
        }
    }
}
