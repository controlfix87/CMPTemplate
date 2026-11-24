package com.cmptemplate.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Outlined buttons in the brand accent. Material 3's default outlined content colour is
 * onSurfaceVariant, which on the navy cards reads as a disabled button (screenshot review).
 */
@Composable
fun brandOutlinedButtonColors(): ButtonColors =
    ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)

/** Readable width for text and cards on tablets, unfolded foldables and desktop windows. */
val ContentMaxWidth = 720.dp

/**
 * Window width from which list screens switch to two panes (list + detail): unfolded foldables,
 * tablets, landscape phones and desktop windows. Narrower stays single-pane. Matches the Material
 * "medium" window width class lower bound.
 */
val TwoPaneMinWidth = 600.dp

/**
 * A fold or hinge that separates the window into two usable areas. [bounds] are in window pixels;
 * [horizontal] is a hinge running left-right (tabletop posture: panes stack), otherwise vertical
 * (book posture: panes sit side by side). [None] = no separating fold (flat, folded, or no support).
 */
data class FoldingState(val bounds: Rect? = null, val horizontal: Boolean = false) {
    companion object { val None = FoldingState() }
}

/** Provided by the platform host (Android: androidx.window); other targets keep the default. */
val LocalFoldingState = staticCompositionLocalOf { FoldingState.None }

/**
 * Two side-by-side panes with a divider ([firstWeight] of the width for the list/master pane).
 * Each pane is its own traversal group so TalkBack and keyboard focus finish one pane before the
 * next; the Row mirrors in right-to-left locales. The caller supplies the insets and app bar.
 *
 * With a separating fold ([LocalFoldingState]) the panes sit on either side of the hinge instead
 * (side by side for a vertical hinge, stacked for a horizontal one) and no pane is drawn under it.
 */
@Composable
fun TwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstWeight: Float = 0.4f,
) {
    val firstContent by rememberUpdatedState(first)
    val secondContent by rememberUpdatedState(second)
    // Movable so a fold/unfold keeps scroll and text state instead of rebuilding the panes.
    val firstPane = remember { movableContentOf { firstContent() } }
    val secondPane = remember { movableContentOf { secondContent() } }
    val fold = LocalFoldingState.current
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val hinge = fold.bounds
    val fits = hinge != null && size != IntSize.Zero && if (fold.horizontal) {
        hinge.top >= origin.y && hinge.bottom <= origin.y + size.height
    } else {
        hinge.left >= origin.x && hinge.right <= origin.x + size.width
    }

    Box(modifier.onGloballyPositioned { origin = it.positionInWindow(); size = it.size }) {
        if (hinge != null && fits && fold.horizontal) {
            val topDp = with(density) { (hinge.top - origin.y).toDp() }
            val gapDp = with(density) { hinge.height.toDp() }
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(topDp).semantics { isTraversalGroup = true }) { firstPane() }
                Spacer(Modifier.height(gapDp))
                Box(Modifier.fillMaxWidth().weight(1f).semantics { isTraversalGroup = true }) { secondPane() }
            }
        } else if (hinge != null && fits) {
            val startDp = with(density) { (hinge.left - origin.x).toDp() }
            val gapDp = with(density) { hinge.width.toDp() }
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(startDp).fillMaxHeight().semantics { isTraversalGroup = true }) { firstPane() }
                Spacer(Modifier.width(gapDp))
                Box(Modifier.weight(1f).fillMaxHeight().semantics { isTraversalGroup = true }) { secondPane() }
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(firstWeight).fillMaxHeight().semantics { isTraversalGroup = true }) { firstPane() }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(1f - firstWeight).fillMaxHeight().semantics { isTraversalGroup = true }) { secondPane() }
            }
        }
    }
}

/** Screen title with a large (48dp) back button; [onBack] null = top-level screen, no button. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, backLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().widthIn(max = ContentMaxWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("screen_back")) {
                Icon(AppIcons.Back, contentDescription = backLabel)
            }
        }
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
    }
}

/** A card section with consistent padding and the readable width. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth().widthIn(max = ContentMaxWidth),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Section title with an optional leading icon. */
@Composable
fun SectionLabel(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

/**
 * A tappable choice card (radio semantics): big icon or flag on top, label below, optional detail
 * line. Selected gets the accent frame. Used for language and theme in onboarding and About.
 */
@Composable
fun OptionCard(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    icon: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceContainer)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.heightIn(min = 34.dp), contentAlignment = Alignment.Center) { icon() }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (selected) colors.onPrimaryContainer else colors.onSurface,
        )
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}

/**
 * Progress bar split per step, each segment with its title (LockdownControl). Upcoming steps are
 * visible; finished steps can be tapped to go back to them.
 */
@Composable
fun StepProgress(titles: List<String>, current: Int, onStep: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.fillMaxWidth().testTag("step_progress"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        titles.forEachIndexed { i, title ->
            val reached = i <= current
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .then(if (i < current) Modifier.clickable(role = Role.Button) { onStep(i) } else Modifier)
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) { selected = i == current }
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(4.dp)
                        .background(if (reached) colors.primary else colors.outlineVariant, RoundedCornerShape(2.dp)),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (i == current) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        i == current -> colors.primary
                        reached -> colors.onSurface
                        else -> colors.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
