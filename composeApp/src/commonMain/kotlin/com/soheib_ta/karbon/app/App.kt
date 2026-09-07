package com.soheib_ta.karbon.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.samples.AppointmentOutcomesDonutSample
import com.soheib_ta.karbon.samples.BarChartSample
import com.soheib_ta.karbon.samples.ClinicalActivityLineChartSample
import com.soheib_ta.karbon.samples.KarbonDatePickerDialogSample
import com.soheib_ta.karbon.samples.KarbonDatePickerFieldSample
import com.soheib_ta.karbon.samples.KarbonDateRangePickerDialogSample
import com.soheib_ta.karbon.samples.KarbonDateTimePickerDialogSample
import com.soheib_ta.karbon.samples.KarbonTimePickerDialogSample

@Composable
fun App() {
    MaterialTheme {
        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 960.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Karbon Components",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Interactive examples for every component in the library.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                item { SectionHeader("Charts") }

                item {
                    ExampleCard(
                        title = "Bar chart",
                        description = "Compare multiple series across categories.",
                    ) {
                        BarChartSample()
                    }
                }

                item {
                    ExampleCard(
                        title = "Line chart",
                        description = "Track several series and their trends over time.",
                    ) {
                        ClinicalActivityLineChartSample()
                    }
                }

                item {
                    ExampleCard(
                        title = "Pie / donut chart",
                        description = "Show how individual values contribute to a whole.",
                    ) {
                        AppointmentOutcomesDonutSample()
                    }
                }

                item { SectionHeader("Date & time pickers") }

                item {
                    ExampleCard(
                        title = "Date picker · modal",
                        description = "KarbonDatePickerDialog — calendar dialog with Cancel/OK.",
                    ) {
                        KarbonDatePickerDialogSample()
                    }
                }

                item {
                    ExampleCard(
                        title = "Date picker · docked",
                        description = "KarbonDatePickerField — calendar anchored under the field.",
                    ) {
                        KarbonDatePickerFieldSample()
                    }
                }

                item {
                    ExampleCard(
                        title = "Date range picker",
                        description = "KarbonDateRangePickerDialog — start/end selection, 2 months visible.",
                    ) {
                        KarbonDateRangePickerDialogSample()
                    }
                }

                item {
                    ExampleCard(
                        title = "Time picker",
                        description = "KarbonTimePickerDialog — dial ⇄ numeric entry toggle.",
                    ) {
                        KarbonTimePickerDialogSample()
                    }
                }

                item {
                    ExampleCard(
                        title = "Date-time picker",
                        description = "KarbonDateTimePickerDialog — one shell, DATE/TIME tabs.",
                    ) {
                        KarbonDateTimePickerDialogSample()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .widthIn(max = 960.dp)
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun ExampleCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 960.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}
