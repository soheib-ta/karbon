package com.soheib_ta.karbon.pickers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Width of the single-body (tabs / stepped) shell. */
private val DateTimeCompactWidth = 360.dp

/** Width of the two-body shell: calendar + divider + dial column, inside 24 dp padding. */
private val DateTimeSideBySideWidth = 744.dp

/**
 * Material's date picker container width. The calendar body fills whatever width it is given, so
 * in the side-by-side row it has to be pinned or it would squeeze the dial out.
 */
private val DateTimeCalendarWidth = 360.dp

/** Both bodies are within a few dp of this, so swapping tabs does not resize the dialog. */
private val DateTimeBodyMinHeight = 400.dp

/** Material's expanded breakpoint — above it both bodies fit in one row. */
private val ExpandedWidthBreakpoint = 840.dp

/**
 * How [KarbonDateTimePickerDialog] hosts its date and time bodies. A date-time picker is not a
 * third Material component — it is a shell around the same [DatePicker] and [TimePicker] bodies
 * already specified, so only the shell varies with [KarbonDateTimeLayout].
 */
enum class KarbonDateTimeLayout {
    /** Two tabs, DATE and TIME, in one dialog — both values stay reachable. */
    Tabs,

    /** Date, then time, with Next replacing OK on step one. For pairs where time depends on date. */
    Stepped,

    /** Calendar and dial in one row, no tabs or steps — for expanded/wide windows. */
    SideBySide,
}

/** Defaults for [KarbonDateTimePickerDialog]. */
object KarbonDateTimePickerDefaults {

    /**
     * Picks the shell from the current window width: [KarbonDateTimeLayout.SideBySide] once there
     * is room for both bodies at once (Material's 840 dp expanded breakpoint), otherwise
     * [KarbonDateTimeLayout.Tabs].
     */
    @Composable
    fun adaptiveLayout(): KarbonDateTimeLayout {
        val containerWidth = LocalWindowInfo.current.containerSize.width
        val widthDp = with(LocalDensity.current) { containerWidth.toDp() }
        return if (widthDp >= ExpandedWidthBreakpoint) {
            KarbonDateTimeLayout.SideBySide
        } else {
            KarbonDateTimeLayout.Tabs
        }
    }
}

/**
 * A dialog combining Material 3's [DatePicker] and [TimePicker] bodies behind one header, one
 * confirm action and one result, per [layout].
 *
 * The dial always uses [TimePickerLayoutType.Vertical]: the shell is at most 360 dp wide for a
 * single body, which the horizontal dial layout would overflow.
 *
 * @param dateState Hoisted date state. Use `rememberDatePickerState()`.
 * @param timeState Hoisted time state. Use `rememberTimePickerState()`.
 * @param layout Shell to use. Defaults to adapting to the window width.
 * @param onConfirm Called when the result is confirmed; read [dateState] and [timeState] for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonDateTimePickerDialog(
    dateState: DatePickerState,
    timeState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    layout: KarbonDateTimeLayout = KarbonDateTimePickerDefaults.adaptiveLayout(),
    dateFormatter: DatePickerFormatter = remember { DatePickerDefaults.dateFormatter() },
) {
    val headline = dateTimeHeadline(dateState, timeState, dateFormatter)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            val desiredWidth = if (layout == KarbonDateTimeLayout.SideBySide) {
                DateTimeSideBySideWidth
            } else {
                DateTimeCompactWidth
            }

            Surface(
                modifier = modifier.width(minOf(desiredWidth, maxWidth)),
                shape = DatePickerDefaults.shape,
                tonalElevation = DatePickerDefaults.TonalElevation,
            ) {
                Column {
                    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                        Text(
                            text = "Select date and time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        )
                    }

                    when (layout) {
                        KarbonDateTimeLayout.Tabs ->
                            TabsBody(dateState, timeState, onDismissRequest, onConfirm)

                        KarbonDateTimeLayout.Stepped ->
                            SteppedBody(dateState, timeState, onDismissRequest, onConfirm)

                        KarbonDateTimeLayout.SideBySide ->
                            SideBySideBody(dateState, timeState, onDismissRequest, onConfirm)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabsBody(
    dateState: DatePickerState,
    timeState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }

    PrimaryTabRow(selectedTabIndex = tab) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("DATE") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("TIME") })
    }

    Box(
        modifier = Modifier.heightIn(min = DateTimeBodyMinHeight).fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (tab == 0) {
            DatePicker(state = dateState, title = null, headline = null, showModeToggle = false)
        } else {
            TimeBody(timeState)
        }
    }

    ActionsRow(onDismissRequest, onConfirm, dateState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SteppedBody(
    dateState: DatePickerState,
    timeState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    var step by remember { mutableStateOf(0) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step == 1) {
            TextButton(onClick = { step = 0 }) { Text("←") }
        }
        Text(
            text = if (step == 0) "Step 1 of 2 · Date" else "Step 2 of 2 · Time",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }

    Box(
        modifier = Modifier.heightIn(min = DateTimeBodyMinHeight).fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (step == 0) {
            DatePicker(state = dateState, title = null, headline = null, showModeToggle = false)
        } else {
            TimeBody(timeState)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismissRequest) { Text("Cancel") }
        if (step == 0) {
            TextButton(
                onClick = { step = 1 },
                enabled = dateState.selectedDateMillis != null,
            ) { Text("Next") }
        } else {
            Button(onClick = onConfirm) { Text("Confirm") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SideBySideBody(
    dateState: DatePickerState,
    timeState: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    HorizontalDivider()
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DatePicker(
            state = dateState,
            modifier = Modifier.width(DateTimeCalendarWidth),
            title = null,
            headline = null,
            showModeToggle = false,
        )
        VerticalDivider(modifier = Modifier.height(DateTimeBodyMinHeight))
        TimeBody(timeState)
    }

    ActionsRow(onDismissRequest, onConfirm, dateState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeBody(timeState: TimePickerState) {
    Box(modifier = Modifier.padding(top = 24.dp)) {
        TimePicker(state = timeState, layoutType = TimePickerLayoutType.Vertical)
    }
}

@Composable
private fun ActionsRow(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    dateState: DatePickerState,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismissRequest) { Text("Cancel") }
        TextButton(
            onClick = onConfirm,
            enabled = dateState.selectedDateMillis != null,
        ) { Text("OK") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun dateTimeHeadline(
    dateState: DatePickerState,
    timeState: TimePickerState,
    formatter: DatePickerFormatter,
): String {
    val dateText = formatSelectedDate(dateState.selectedDateMillis, dateState, formatter)
        .ifEmpty { "Select date" }
    return "$dateText, ${formatClock(timeState)}"
}

@OptIn(ExperimentalMaterial3Api::class)
private fun formatClock(state: TimePickerState): String {
    val displayHour = if (state.is24hour) {
        state.hour
    } else {
        val h12 = state.hour % 12
        if (h12 == 0) 12 else h12
    }
    val hh = displayHour.toString().padStart(2, '0')
    val mm = state.minute.toString().padStart(2, '0')
    return if (state.is24hour) "$hh:$mm" else "$hh:$mm ${if (state.hour >= 12) "PM" else "AM"}"
}
