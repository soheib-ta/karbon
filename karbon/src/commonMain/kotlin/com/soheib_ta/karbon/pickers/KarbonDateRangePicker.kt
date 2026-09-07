package com.soheib_ta.karbon.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Width of the range selection surface, per the Karbon range picker spec. */
private val RangeContainerWidth = 412.dp

/** Height of the dismiss / Save bar that replaces the modal picker's button row. */
private val RangeTopBarHeight = 64.dp

/**
 * Height one month occupies in the scrolling list: 6 week rows of 48 dp plus the month subhead
 * and its padding, as laid out by Material 3's `DateRangePicker`.
 */
private val RangeMonthHeight = 336.dp

/** Height of the range header (supporting text + headline) plus the pinned weekday row. */
private val RangeHeaderHeight = 148.dp

/**
 * A dialog hosting Material 3's [DateRangePicker], with a Karbon top bar (dismiss / Save) in place
 * of the modal date picker's Cancel/OK row.
 *
 * The surface is [RangeContainerWidth] wide and tall enough to show [visibleMonths] months at
 * once; further months scroll. It never exceeds the window, so a two-month layout degrades to
 * whatever fits rather than overflowing.
 *
 * @param visibleMonths How many months are visible without scrolling. Chosen by the app, not the
 *   user; must be at least 1.
 * @param onConfirm Called with the selected start/end millis when Save is pressed.
 */
@Composable
fun KarbonDateRangePickerDialog(
    state: DateRangePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: (start: Long?, end: Long?) -> Unit,
    modifier: Modifier = Modifier,
    visibleMonths: Int = 1,
    showModeToggle: Boolean = true,
    title: @Composable () -> Unit = {
        Text("Select range", modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp))
    },
) {
    require(visibleMonths >= 1) { "visibleMonths must be at least 1, was $visibleMonths." }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            val desiredHeight =
                RangeTopBarHeight + RangeHeaderHeight + RangeMonthHeight * visibleMonths

            Surface(
                modifier = modifier
                    .width(minOf(RangeContainerWidth, maxWidth))
                    .height(minOf(desiredHeight, maxHeight)),
                shape = DatePickerDefaults.shape,
                tonalElevation = DatePickerDefaults.TonalElevation,
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RangeTopBarHeight)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = KarbonCloseIcon,
                                contentDescription = "Cancel range selection",
                            )
                        }
                        TextButton(
                            onClick = {
                                onConfirm(
                                    state.selectedStartDateMillis,
                                    state.selectedEndDateMillis,
                                )
                            },
                            enabled = state.selectedStartDateMillis != null &&
                                state.selectedEndDateMillis != null,
                        ) { Text("Save") }
                    }

                    DateRangePicker(
                        state = state,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        title = title,
                        showModeToggle = showModeToggle,
                    )
                }
            }
        }
    }
}
