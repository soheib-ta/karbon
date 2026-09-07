package com.soheib_ta.karbon.samples

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.pickers.KarbonDateRangePickerDialog
import com.soheib_ta.karbon.pickers.KarbonDatePickerDialog
import com.soheib_ta.karbon.pickers.KarbonDatePickerField
import com.soheib_ta.karbon.pickers.KarbonDateTimePickerDialog
import com.soheib_ta.karbon.pickers.KarbonTimePickerDialog

/** Minimal hoisted-state usage of [KarbonDatePickerDialog]. */
@Composable
fun KarbonDatePickerDialogSample(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val state = rememberDatePickerState()

    Column(modifier.padding(16.dp)) {
        Button(onClick = { open = true }) { Text("Pick a date") }
    }

    if (open) {
        KarbonDatePickerDialog(
            state = state,
            onDismissRequest = { open = false },
            onConfirm = { open = false },
        )
    }
}

/** Minimal usage of [KarbonDatePickerField] — a date field with a docked calendar. */
@Composable
fun KarbonDatePickerFieldSample(modifier: Modifier = Modifier) {
    val state = rememberDatePickerState()

    KarbonDatePickerField(
        state = state,
        label = { Text("Start date") },
        modifier = modifier.padding(16.dp),
    )
}

/** Minimal hoisted-state usage of [KarbonDateRangePickerDialog]. */
@Composable
fun KarbonDateRangePickerDialogSample(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val state = rememberDateRangePickerState()

    Column(modifier.padding(16.dp)) {
        Button(onClick = { open = true }) { Text("Pick a range") }
    }

    if (open) {
        KarbonDateRangePickerDialog(
            state = state,
            onDismissRequest = { open = false },
            onConfirm = { _, _ -> open = false },
            visibleMonths = 2,
        )
    }
}

/** Minimal hoisted-state usage of [KarbonTimePickerDialog]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonTimePickerDialogSample(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val state = rememberTimePickerState()

    Column(modifier.padding(16.dp)) {
        Button(onClick = { open = true }) { Text("Pick a time") }
    }

    if (open) {
        KarbonTimePickerDialog(
            state = state,
            onDismissRequest = { open = false },
            onConfirm = { open = false },
        )
    }
}

/** Minimal hoisted-state usage of [KarbonDateTimePickerDialog], adapting to the window width. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarbonDateTimePickerDialogSample(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()
    val timeState = rememberTimePickerState()

    Column(modifier.padding(16.dp)) {
        Button(onClick = { open = true }) { Text("Pick a date and time") }
    }

    if (open) {
        KarbonDateTimePickerDialog(
            dateState = dateState,
            timeState = timeState,
            onDismissRequest = { open = false },
            onConfirm = { open = false },
        )
    }
}
