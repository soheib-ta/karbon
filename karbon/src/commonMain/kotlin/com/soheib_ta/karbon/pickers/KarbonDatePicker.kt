package com.soheib_ta.karbon.pickers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.flow.drop

/** Width of the docked calendar surface, per the Karbon docked date picker spec. */
private val DockedContainerWidth = 360.dp

/** Gap between the anchor field and the docked calendar surface. */
private val DockedAnchorGap = 4.dp

/**
 * A modal date picker: [DatePickerDialog] hosting Material 3's [DatePicker] with Karbon's
 * default Cancel/OK actions.
 *
 * Colour, spacing and type all come from `MaterialTheme` via the underlying Material 3
 * [DatePicker] — nothing here is hardcoded.
 *
 * @param state Hoisted picker state. Use `rememberDatePickerState()`.
 * @param onConfirm Called with [DatePickerState.selectedDateMillis] when OK is pressed.
 * @param showModeToggle Whether the calendar ⇄ text-input toggle is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonDatePickerDialog(
    state: DatePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    showModeToggle: Boolean = true,
    shape: Shape = DatePickerDefaults.shape,
    tonalElevation: Dp = DatePickerDefaults.TonalElevation,
    colors: DatePickerColors = DatePickerDefaults.colors(),
) {
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        tonalElevation = tonalElevation,
        colors = colors,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.selectedDateMillis) },
                enabled = state.selectedDateMillis != null,
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
    ) {
        DatePicker(state = state, showModeToggle = showModeToggle)
    }
}

/**
 * A read-only date field with a trailing calendar button that opens a docked calendar anchored
 * below it. Selection commits on click and the calendar dismisses itself — the docked variant has
 * no confirm step.
 *
 * The field shows the selection formatted by [dateFormatter] in the state's locale, never a raw
 * timestamp.
 *
 * @param state Hoisted picker state. Use `rememberDatePickerState()`.
 * @param onDateSelected Called with the newly selected date when the calendar dismisses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonDatePickerField(
    state: DatePickerState,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
    onDateSelected: (Long?) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    KarbonDatePickerDocked(
        state = state,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onDateSelected = onDateSelected,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = formatSelectedDate(state.selectedDateMillis, state, dateFormatter),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = label,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }, enabled = enabled) {
                    Icon(
                        imageVector = KarbonCalendarIcon,
                        contentDescription = "Show date picker",
                    )
                }
            },
        )
    }
}

/**
 * Anchors a bare [DatePicker] (no title, headline or actions) below [content] in a [Popup], for
 * desktop/expanded "docked" placement. The surface is [DockedContainerWidth] wide rather than
 * filling the window, and flips above the anchor when there is no room below.
 *
 * Selection commits immediately on click and the popup dismisses itself.
 *
 * @param expanded Whether the popup is currently shown. Hoist alongside [state].
 * @param onDateSelected Called once with the newly selected date, right before the popup closes.
 * @param content The anchor — typically a text field showing the current selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonDatePickerDocked(
    state: DatePickerState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDateSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tonalElevation: Dp = 3.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (expanded) {
            val positionProvider = rememberAnchoredBelowPositionProvider(DockedAnchorGap)
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.width(DockedContainerWidth),
                    shape = shape,
                    tonalElevation = tonalElevation,
                    shadowElevation = tonalElevation,
                ) {
                    DatePicker(
                        state = state,
                        title = null,
                        headline = null,
                        showModeToggle = false,
                    )
                }
            }

            LaunchedEffect(state) {
                snapshotFlow { state.selectedDateMillis }
                    .drop(1)
                    .collect { millis ->
                        onDateSelected(millis)
                        onExpandedChange(false)
                    }
            }
        }
    }
}

/** Formats [millis] in the picker's own locale, or returns an empty string when nothing is set. */
@OptIn(ExperimentalMaterial3Api::class)
internal fun formatSelectedDate(
    millis: Long?,
    state: DatePickerState,
    formatter: DatePickerFormatter,
): String = millis?.let { formatter.formatDate(it, state.locale) }.orEmpty()

@Composable
private fun rememberAnchoredBelowPositionProvider(gap: Dp): PopupPositionProvider {
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    return remember(gapPx) { AnchoredBelowPositionProvider(gapPx) }
}

/** Places the popup directly under the anchor, flipping above it when it would run off-screen. */
private class AnchoredBelowPositionProvider(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val below = anchorBounds.bottom + gapPx
        val above = anchorBounds.top - popupContentSize.height - gapPx
        val fitsBelow = below + popupContentSize.height <= windowSize.height
        val y = if (fitsBelow || above < 0) {
            below.coerceAtMost((windowSize.height - popupContentSize.height).coerceAtLeast(0))
        } else {
            above
        }
        return IntOffset(x, y)
    }
}
