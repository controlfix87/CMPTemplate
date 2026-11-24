package com.cmptemplate.feature.example.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.cmptemplate.core.designsystem.components.AppIcons
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cmptemplate.core.common.DataError
import com.cmptemplate.core.designsystem.components.TwoPane
import com.cmptemplate.core.designsystem.components.TwoPaneMinWidth
import com.cmptemplate.core.designsystem.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExampleRoot(onNavigateToDetail: (String) -> Unit, onAbout: () -> Unit = {}, viewModel: ExampleViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Language and theme live in About (and onboarding): the same LanguageChoices component, the
    // only supported way to change the language (see AppLocaleProvider for why that matters).
    ExampleScreen(state, viewModel::onAction, onNavigateToDetail, onAbout = onAbout)
}

/**
 * Scaffold owns safe insets. Children consume them before adding IME padding.
 *
 * Compact windows: the list alone; tapping an item calls [onDetail] (a separate screen).
 * Windows from [TwoPaneMinWidth]: list on the left, the selected item's detail on the right
 * (placeholder until one is picked); the selection lives in [ExampleState.selectedId].
 * [selectedId] overrides the state's selection (the detail route passes its own item).
 */
@Composable
fun ExampleScreen(
    state: ExampleState,
    onAction: (ExampleAction) -> Unit,
    onDetail: (String) -> Unit,
    onAbout: () -> Unit = {},
    selectedId: String? = state.selectedId,
) {
    // Hoisted above the pane switch so the scroll position survives rotation, folding and resizing.
    val listState = rememberLazyListState()
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            if (maxWidth >= TwoPaneMinWidth) {
                TwoPane(
                    first = {
                        ExampleList(state, onAction, listState, selectedId, onItem = { onAction(ExampleAction.Select(it)) }, onAbout, Modifier.fillMaxSize())
                    },
                    second = {
                        if (selectedId != null) {
                            ExampleDetailContent(selectedId, onBack = null, Modifier.fillMaxSize().testTag("example_detail_pane"))
                        } else {
                            Box(Modifier.fillMaxSize().testTag("example_detail_placeholder"), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(Res.string.example_select_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(24.dp),
                                )
                            }
                        }
                    },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    ExampleList(state, onAction, listState, null, onItem = { onAction(ExampleAction.Select(it)); onDetail(it) }, onAbout, Modifier.widthIn(max = 840.dp).fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ExampleList(
    state: ExampleState,
    onAction: (ExampleAction) -> Unit,
    listState: LazyListState,
    selectedId: String?,
    onItem: (String) -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag("example_list"),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.example_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = onAbout, modifier = Modifier.testTag("example_about")) {
                    Text(stringResource(Res.string.about_settings))
                }
            }
        }
        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onAction(ExampleAction.QueryChanged(it)) },
                label = { Text(stringResource(Res.string.example_search)) },
                modifier = Modifier.fillMaxWidth().testTag("example_search"),
                singleLine = true,
            )
        }
        if (state.isLoading) item(key = "loading") {
            val loading = stringResource(Res.string.example_loading)
            CircularProgressIndicator(Modifier.testTag("example_loading").semantics { contentDescription = loading })
        }
        state.error?.let { error ->
            item(key = "error") {
                Text(stringResource(if (error == DataError.Remote.NO_INTERNET) Res.string.example_error_no_internet else Res.string.example_error_generic))
                Button(onClick = { onAction(ExampleAction.Retry) }, enabled = !state.isLoading) { Text(stringResource(Res.string.example_retry)) }
            }
        }
        if (!state.isLoading && state.error == null && state.items.isEmpty()) item(key = "empty") {
            Text(stringResource(Res.string.example_empty), Modifier.testTag("example_empty"))
        }
        items(state.items, key = { "item-${it.id}" }, contentType = { "example" }) { item ->
            val selected = item.id == selectedId
            ListItem(
                headlineContent = { Text(item.title, fontWeight = if (selected) FontWeight.Bold else null) },
                // The selection is also shown without colour (check mark); the state itself is
                // announced through selectable(), so the icon stays decorative.
                trailingContent = if (selected) ({ Icon(AppIcons.Check, contentDescription = null) }) else null,
                colors = ListItemDefaults.colors(
                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                ),
                modifier = Modifier.testTag("example_item_${item.id}")
                    .clip(MaterialTheme.shapes.medium)
                    .heightIn(min = 48.dp)
                    .selectable(selected = selected, role = Role.Button, onClick = { onItem(item.id) }),
            )
        }
    }
}

/**
 * The detail route. On a wide window (after a rotation or unfold while a detail was open) it is
 * the two-pane screen with [itemId] selected; on a compact one the full-screen detail.
 */
@Composable
fun ExampleDetailRoot(
    itemId: String,
    onBack: () -> Unit,
    onDetail: (String) -> Unit = {},
    onAbout: () -> Unit = {},
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= TwoPaneMinWidth) {
            ExampleDetailTwoPane(itemId, onDetail, onAbout)
        } else {
            ExampleDetailScreen(itemId, onBack)
        }
    }
}

@Composable
private fun ExampleDetailTwoPane(itemId: String, onDetail: (String) -> Unit, onAbout: () -> Unit, viewModel: ExampleViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // This entry has its own ViewModel: take over the route's item once, then selections made in
    // the pane update the same state.
    LaunchedEffect(itemId) { viewModel.onAction(ExampleAction.Select(itemId)) }
    ExampleScreen(state, viewModel::onAction, onDetail, onAbout, selectedId = state.selectedId ?: itemId)
}

@Composable
fun ExampleDetailScreen(itemId: String, onBack: () -> Unit) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            ExampleDetailContent(itemId, onBack, Modifier.widthIn(max = 840.dp).fillMaxSize())
        }
    }
}

/** The detail body, shared by the full-screen detail and the right-hand pane ([onBack] null = no back button). */
@Composable
internal fun ExampleDetailContent(itemId: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    LazyColumn(modifier.testTag("example_detail"), contentPadding = PaddingValues(16.dp)) {
        item { Text(stringResource(Res.string.example_detail, itemId), Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall) }
        if (onBack != null) {
            item { Button(onClick = onBack, modifier = Modifier.testTag("detail_back")) { Text(stringResource(Res.string.example_back)) } }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExampleContentPreview() {
    com.cmptemplate.core.designsystem.CMPTemplateTheme {
        ExampleScreen(
            ExampleState(items = listOf(com.cmptemplate.core.model.ExampleItem("1", "Sample 1")), isLoading = false),
            onAction = {}, onDetail = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExampleErrorPreview() {
    com.cmptemplate.core.designsystem.CMPTemplateTheme {
        ExampleScreen(ExampleState(isLoading = false, error = DataError.Remote.NO_INTERNET), {}, {})
    }
}
