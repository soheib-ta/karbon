package com.soheib_ta.karbon.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Padding around the time picker body, per the Karbon time picker spec. */
private val TimePickerDialogPadding = 24.dp

/**
 * Container width of the horizontal (expanded / landscape) layout. Narrower than the body it
 * holds — see [KarbonTimePickerDialog].
 */
private val HorizontalContainerWidth = 448.dp

/** Width available to the title and the action row inside [HorizontalContainerWidth]. */
private val HorizontalContainerInnerWidth = HorizontalContainerWidth - TimePickerDialogPadding * 2

private val TimePickerDialogElevation = 6.dp

/**
 * A dialog hosting Material 3's [TimePicker] (dial), with a built-in toggle to [TimeInput]
 * (numeric entry) — the mode-toggle + actions anatomy of the Karbon time picker spec.
 *
 * The vertical layout wraps its content into a 328 dp container. The horizontal layout keeps the
 * spec's 448 dp container, which is deliberately narrower than the display + dial body it holds,
 * so the clock dial hangs past the container's trailing edge; the title and the action row stay
 * inside it. That is why the container is drawn as a background rather than a `Surface`, which
 * would clip the overhang away.
 *
 * @param state Hoisted picker state. Use `rememberTimePickerState()`.
 * @param onConfirm Called when OK is pressed; read [state] for the chosen hour/minute.
 * @param initialDisplayMode Whether the dial or the numeric keypad is shown first.
 * @param layoutType Dial layout. Defaults to Material's window-aware choice: horizontal in
 *   landscape/expanded, vertical otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonTimePickerDialog(
    state: TimePickerState,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    initialDisplayMode: TimePickerDisplayMode = TimePickerDisplayMode.Picker,
    layoutType: TimePickerLayoutType = TimePickerDefaults.layoutType(),
) {
    var displayMode by remember { mutableStateOf(initialDisplayMode) }

    val isDial = displayMode == TimePickerDisplayMode.Picker
    val overhangs = isDial && layoutType == TimePickerLayoutType.Horizontal
    val shape = TimePickerDialogDefaults.shape
    val containerColor = TimePickerDialogDefaults.containerColor

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // The dialog node spans the whole body — including the part of the dial that hangs out —
        // so the overhang stays clickable instead of counting as a click outside the dialog. The
        // container is painted behind it at its own, narrower width.
        Box(modifier = modifier) {
            Box(Modifier.matchParentSize()) {
                Box(
                    Modifier
                        .then(
                            if (overhangs) {
                                Modifier.width(HorizontalContainerWidth)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        )
                        .fillMaxHeight()
                        .shadow(TimePickerDialogElevation, shape, clip = false)
                        .background(containerColor, shape)
                )
            }

            CompositionLocalProvider(
                LocalContentColor provides contentColorFor(containerColor)
            ) {
                Column(
                    modifier = Modifier
                        .padding(TimePickerDialogPadding)
                        // Without a fixed container the width follows the picker body, so the
                        // action row's fillMaxWidth cannot stretch the dialog across the window.
                        .then(if (overhangs) Modifier else Modifier.width(IntrinsicSize.Max))
                ) {
                    TimePickerDialogDefaults.Title(displayMode = displayMode)

                    if (isDial) {
                        TimePicker(state = state, layoutType = layoutType)
                        // The vertical layout already ends with a 24 dp margin; the horizontal one
                        // does not, so it gets the spec's 20 dp gap above the action row.
                        if (layoutType == TimePickerLayoutType.Horizontal) {
                            Spacer(Modifier.height(20.dp))
                        }
                    } else {
                        TimeInput(state = state)
                    }

                    Row(
                        modifier = if (overhangs) {
                            // Actions stay inside the painted container, not under the overhang.
                            Modifier.width(HorizontalContainerInnerWidth)
                        } else {
                            Modifier.fillMaxWidth()
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TimePickerDialogDefaults.DisplayModeToggle(
                            displayMode = displayMode,
                            onDisplayModeChange = {
                                displayMode = if (isDial) {
                                    TimePickerDisplayMode.Input
                                } else {
                                    TimePickerDisplayMode.Picker
                                }
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismissRequest) { Text("Cancel") }
                        TextButton(onClick = onConfirm) { Text("OK") }
                    }
                }
            }
        }
    }
}
