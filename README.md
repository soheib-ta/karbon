# Karbon

Karbon is a Compose Multiplatform chart library implemented in `commonMain`. It provides animated, interactive bar, line, pie, and donut charts without a platform-specific charting dependency.

This README is written as an integration contract for both developers and coding agents. The examples use the public API that exists in this repository.

## At a glance

| Item | Value |
| --- | --- |
| Declared Maven coordinates | `com.soheib-ta:karbon:1.0.1` |
| Chart families | Grouped bar, line, pie, and donut |
| Targets configured in this repository | Android, JVM/Desktop, iOS arm64, iOS Simulator arm64, JavaScript browser, Wasm browser |
| Android minimum SDK | 24 |
| Android JVM bytecode target | Java 11 |
| Repository baseline | Kotlin 2.3.20, Compose Multiplatform 1.10.3 |
| License | Apache License 2.0 |

The Maven coordinates above are declared by `karbon/build.gradle.kts`. Confirm that version `1.0.1` is available in your configured repository before relying on the remote dependency. During local development, Maven Local or a Gradle composite build is deterministic.

## Public package map

| Chart | Main composable | Data models | Configuration | Selection callback |
| --- | --- | --- | --- | --- |
| Bar | `charts.barchart.BarChart` | `BarEntry`, `BarSeries` | `BarChartConfig` | No public callback; tapping shows an internal tooltip |
| Line | `charts.linechart.LineChart` | `LineEntry`, `LineSeries` | `LineChartConfig` | `onPointSelected` |
| Pie/donut | `charts.piechart.PieChart` | `PieEntry` | `PieChartConfig` | `onSliceSelected` |

All packages start with:

```text
com.soheib_ta.karbon
```

Do not import anything from an `.internal` package. Internal geometry, painters, legends, and selection implementations are not public API.

## Add Karbon to another project

The consuming project must be a Compose Multiplatform project and should already include Compose runtime, foundation, UI, and Material 3 in `commonMain`.

### Option 1: published dependency

Add Maven Central to the consuming project's repositories:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then add Karbon to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation("com.soheib-ta:karbon:1.0.1")
        }
    }
}
```

Use this option only when the coordinate resolves from a configured repository.

### Option 2: Maven Local

From this repository, publish the current source to Maven Local:

```shell
./gradlew :karbon:publishToMavenLocal
```

On Windows:

```powershell
.\gradlew.bat :karbon:publishToMavenLocal
```

Add `mavenLocal()` before remote repositories in the consuming project:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
```

Use the same dependency declaration:

```kotlin
commonMain.dependencies {
    implementation("com.soheib-ta:karbon:1.0.1")
}
```

Republish after changing the library source. If Gradle keeps an older local artifact, change the library version before publishing.

### Option 3: composite build for live source changes

If the Karbon repository is next to the consuming repository, add it as an included build in the consumer's `settings.gradle.kts`:

```kotlin
includeBuild("../karbon-library")
```

Then request Karbon by its declared coordinates:

```kotlin
commonMain.dependencies {
    implementation("com.soheib-ta:karbon:1.0.1")
}
```

Gradle substitutes the included `:karbon` project for the external module. Adjust the relative path to match the local directory layout.

### Option 4: copy the module into the consumer

Copy the `karbon/` directory into the consumer repository, include it in `settings.gradle.kts`, and use a project dependency:

```kotlin
include(":karbon")
```

```kotlin
commonMain.dependencies {
    implementation(project(":karbon"))
}
```

The module build script uses aliases from `gradle/libs.versions.toml`. When copying only the module, also copy or adapt the Karbon plugin and library entries in the consumer's version catalog.

## Bar chart

A bar entry represents one X-axis category. Every value map key must match a `BarSeries.key`.

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soheib_ta.karbon.charts.barchart.BarChart
import com.soheib_ta.karbon.charts.barchart.BarChartConfig
import com.soheib_ta.karbon.charts.barchart.DistributionMode
import com.soheib_ta.karbon.charts.barchart.LegendPosition
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries

@Composable
fun RevenueBarChart(modifier: Modifier = Modifier) {
    val entries = listOf(
        BarEntry("Jan", mapOf("billed" to 82_000f, "paid" to 65_000f)),
        BarEntry("Feb", mapOf("billed" to 71_000f, "paid" to 60_000f)),
        BarEntry("Mar", mapOf("billed" to 93_000f, "paid" to 85_000f)),
        BarEntry("Apr", mapOf("billed" to 86_000f, "paid" to 75_000f)),
    )

    val series = listOf(
        BarSeries(
            key = "billed",
            label = "Billed",
            colors = listOf(Color(0xFF82B1FF), Color(0xFF2962FF)),
            radius = 6.dp,
        ),
        BarSeries(
            key = "paid",
            label = "Paid",
            colors = listOf(Color(0xFF00A67E)),
            radius = 6.dp,
        ),
    )

    BarChart(
        entries = entries,
        series = series,
        config = BarChartConfig(
            distributionMode = DistributionMode.Even,
            legendPosition = LegendPosition.TopRight,
        ),
        title = {
            Text("Billing and collections", style = MaterialTheme.typography.titleMedium)
        },
        description = {
            Text("Monthly totals", style = MaterialTheme.typography.bodySmall)
        },
        modifier = modifier,
    )
}
```

### Bar data rules

- Entry order is the left-to-right X-axis order.
- `BarEntry.values` must use the exact keys declared by `BarSeries.key`.
- A missing, non-finite, or negative value is rendered as zero.
- Use non-negative values. The current bar chart does not render a negative axis.
- One `BarSeries.colors` value creates a solid bar. Two or more create a vertical gradient.
- `BarSeries.key` must not be blank, `colors` must not be empty, and `radius` must not be negative.

### Bar layout and behavior

- `DistributionMode.Even` shares the available width between groups.
- `DistributionMode.Fixed(groupWidth)` gives every group a fixed width and allows overflow to pan.
- X-axis labels rotate automatically when they do not fit. Override with `forceDiagonalLabels`.
- Tap a bar to show its value tooltip for about two seconds.
- On touch devices, pinch zooms and horizontal dragging pans.
- On desktop, Ctrl + mouse wheel zooms and the mouse wheel pans.
- The Y-axis stays fixed while the plot pans.

### Bar configuration

`BarChartConfig` exposes:

| Property | Default | Purpose |
| --- | --- | --- |
| `distributionMode` | `DistributionMode.Even` | Even or fixed-width groups |
| `legendPosition` | `LegendPosition.TopRight` | Top/bottom and left/right placement |
| `innerHorizontalPadding` | `8.dp` | Plot padding before the first and after the last group |
| `outerHorizontalPadding` | `32.dp` | Width reserved for the fixed Y-axis |
| `groupSpacing` | `20.dp` | Space between groups |
| `barSpacing` | `4.dp` | Space between bars in a group |
| `chartHeight` | `280.dp` | Height of the drawing area |
| `forceDiagonalLabels` | `null` | `true`, `false`, or automatic rotation |
| `yTickCount` | `5` | Number of Y-axis ticks; must be at least 2 |
| `zoomEnabled` | `true` | Enables zoom gestures |
| `showZoomIndicator` | `true` | Shows the current scale when zoomed |
| `animationEnabled` | `true` | Enables bar grow-in animation |
| `animationTrigger` | `AnimationTrigger.OnVisible` | Animate on composition or visibility |
| `visibilityThreshold` | `0.2f` | Visible fraction required to start animation |
| `animationSpec` | Spring | Compose animation specification |
| `colors` | `BarChartColors()` | Grid, axis, label, tooltip, and zoom-indicator colors |

Bar charts use `com.soheib_ta.karbon.charts.barchart.LegendPosition` and `com.soheib_ta.karbon.charts.barchart.AnimationTrigger`. They do not use the shared line/pie enums with similar names.

## Line chart

A line entry represents one X-axis position. Its map may contain a value, a null, or no key for each series.

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.linechart.LineChart
import com.soheib_ta.karbon.charts.linechart.LineChartConfig
import com.soheib_ta.karbon.charts.linechart.LineChartSelection
import com.soheib_ta.karbon.charts.linechart.data.LineAreaFill
import com.soheib_ta.karbon.charts.linechart.data.LineEntry
import com.soheib_ta.karbon.charts.linechart.data.LineSeries

@Composable
fun ActivityLineChart(modifier: Modifier = Modifier) {
    var selection by remember { mutableStateOf<LineChartSelection?>(null) }
    val blue = Color(0xFF2962FF)

    val entries = listOf(
        LineEntry("Mon", mapOf("visits" to 32f, "orders" to 12f)),
        LineEntry("Tue", mapOf("visits" to 44f, "orders" to 18f)),
        LineEntry("Wed", mapOf("visits" to 39f, "orders" to null)),
        LineEntry("Thu", mapOf("visits" to 55f, "orders" to 24f)),
        LineEntry("Fri", mapOf("visits" to 61f, "orders" to 29f)),
    )

    LineChart(
        entries = entries,
        series = listOf(
            LineSeries(
                key = "visits",
                label = "Visits",
                color = blue,
                areaFill = LineAreaFill.VerticalGradient(
                    colors = listOf(
                        blue.copy(alpha = 0.24f),
                        blue.copy(alpha = 0f),
                    ),
                ),
            ),
            LineSeries(
                key = "orders",
                label = "Orders",
                color = Color(0xFF00A67E),
            ),
        ),
        config = LineChartConfig(
            legendPosition = ChartLegendPosition.TopRight,
        ),
        title = {
            Text("Weekly activity", style = MaterialTheme.typography.titleMedium)
        },
        onPointSelected = { selection = it },
        modifier = modifier,
    )

    selection?.let {
        Text("${it.seriesLabel}: ${it.value} at ${it.xLabel}")
    }
}
```

### Line data rules

- Entry order defines the X-axis order.
- `LineEntry.values` keys must match `LineSeries.key`.
- A null, missing, or non-finite value creates a gap in that series.
- Negative and positive values are supported.
- The Y range includes zero by default. Set `includeZeroInRange = false` to fit the data more tightly.
- `LineCurveType.Smooth` is the default. Use `LineCurveType.Straight` for straight segments.
- `LineAreaFill` can be `None`, `Solid`, or `VerticalGradient`.
- The selection callback receives `null` when the temporary selection clears.

### Line layout and behavior

- `LineDistributionMode.Even` distributes points across the available width.
- `LineDistributionMode.Fixed(pointSpacing)` is useful for long, horizontally pannable data sets.
- Tap near a point to select it and show a tooltip for about two seconds.
- Touch and desktop zoom/pan controls match the bar chart.
- Read or reset zoom by hoisting `LineChartZoomState`.

### Line configuration

`LineChartConfig` exposes:

| Group | Properties |
| --- | --- |
| Layout | `distributionMode`, `chartHeight`, `outerHorizontalPadding`, `innerHorizontalPadding` |
| Legend | `legendPosition` |
| Y-axis range | `yTickCount`, `yMin`, `yMax`, `includeZeroInRange` |
| Grid and axes | `showHorizontalGrid`, `showVerticalGrid`, `showXAxis`, `showYAxis` |
| X labels | `maxVisibleXLabels`, `xLabelFormatter` |
| Interaction | `zoomEnabled`, `showZoomIndicator`, `selectionRadius` |
| Animation | `animationEnabled`, `animationTrigger`, `visibilityThreshold`, `animationSpec` |
| Text and empty state | `valueFormatter`, `emptyStateLabel` |
| Theme | `colors: LineChartColors` |

Important constraints:

- `yTickCount` must be at least 2.
- `maxVisibleXLabels` must be at least 1.
- `selectionRadius` must not be negative.
- If both `yMin` and `yMax` are set, they must be finite and `yMin < yMax`.

## Pie and donut chart

`PieChart` renders both forms. Set `innerRadiusRatio = 0f` for a full pie. Set it above zero and below one for a donut.

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
import com.soheib_ta.karbon.charts.piechart.PieChart
import com.soheib_ta.karbon.charts.piechart.PieChartConfig
import com.soheib_ta.karbon.charts.piechart.PieChartSelection
import com.soheib_ta.karbon.charts.piechart.data.PieEntry

@Composable
fun OrderStatusDonut(modifier: Modifier = Modifier) {
    var selection by remember { mutableStateOf<PieChartSelection?>(null) }

    PieChart(
        entries = listOf(
            PieEntry("Completed", 412f, Color(0xFF2962FF)),
            PieEntry("Pending", 82f, Color(0xFFFFB300)),
            PieEntry("Cancelled", 34f, Color(0xFFD32F2F)),
        ),
        config = PieChartConfig(
            innerRadiusRatio = 0.62f,
            legendPosition = ChartLegendPosition.BottomLeft,
            legendColumns = 2,
            showPercentInLegend = true,
        ),
        title = {
            Text("Order status", style = MaterialTheme.typography.titleMedium)
        },
        centerValue = "528",
        centerLabel = "TOTAL",
        onSliceSelected = { selection = it },
        modifier = modifier,
    )

    selection?.let {
        Text("${it.label}: ${it.value}")
    }
}
```

For a full pie:

```kotlin
PieChart(
    entries = entries,
    config = PieChartConfig(innerRadiusRatio = 0f),
)
```

For custom donut-center UI, use `centerContent` instead of `centerValue` and `centerLabel`:

```kotlin
PieChart(
    entries = entries,
    centerContent = {
        Text("Custom center")
    },
)
```

Center content is rendered only when `innerRadiusRatio > 0f`.

### Pie data rules

- Entry order defines slice and legend order.
- Only finite values greater than zero produce slices.
- Zero, negative, and non-finite values are omitted.
- If no positive finite values remain, the chart displays `emptyStateLabel`.
- Percentages are calculated from the remaining positive values.
- `onSliceSelected` receives selection details and later receives `null` when the temporary selection clears.

### Pie configuration

| Property | Default | Purpose |
| --- | --- | --- |
| `legendPosition` | `ChartLegendPosition.BottomLeft` | Legend placement |
| `chartSize` | `220.dp` | Height of the pie/donut drawing area |
| `innerRadiusRatio` | `0.58f` | `0f` for pie; `0f..<1f` for donut |
| `startAngle` | `-90f` | First-slice angle in degrees |
| `clockwise` | `true` | Slice direction |
| `gapAngle` | `2f` | Angular gap between slices |
| `outerPadding` | `10.dp` | Space around the circle |
| `selectedOffset` | `7.dp` | Selected-slice displacement |
| `showPercentInLegend` | `false` | Show percentages instead of raw values |
| `legendColumns` | `2` | Legend column count; must be at least 1 |
| `animationEnabled` | `true` | Enables sweep animation |
| `animationTrigger` | `ChartAnimationTrigger.OnVisible` | Animation trigger |
| `visibilityThreshold` | `0.2f` | Visible fraction required to animate |
| `animationSpec` | Spring | Compose animation specification |
| `valueFormatter` | `compactChartValue` | Raw-value formatter |
| `percentageFormatter` | `defaultPiePercentageFormatter` | Percentage formatter |
| `emptyStateLabel` | `"No data available"` | Empty-state text |
| `colors` | `PieChartColors()` | Tooltip, center, and empty-state colors |

## Shared customization

### Legends and animation enums

Line and pie charts share:

```kotlin
import com.soheib_ta.karbon.charts.common.ChartAnimationTrigger
import com.soheib_ta.karbon.charts.common.ChartLegendPosition
```

Both enums provide `TopLeft`, `TopRight`, `BottomLeft`, and `BottomRight` for legends, and `OnCompose` or `OnVisible` for animation.

Bar charts currently use their own:

```kotlin
import com.soheib_ta.karbon.charts.barchart.AnimationTrigger
import com.soheib_ta.karbon.charts.barchart.LegendPosition
```

Do not mix the bar enums with the shared line/pie enums.

### Value formatting

The public `compactChartValue` formatter is locale-independent:

```kotlin
import com.soheib_ta.karbon.charts.common.compactChartValue

compactChartValue(950f)       // "950"
compactChartValue(1_200f)     // "1.2k"
compactChartValue(2_500_000f) // "2.5M"
```

Line and pie configs accept custom formatter lambdas:

```kotlin
LineChartConfig(
    valueFormatter = { value -> "${value.toInt()} USD" },
)

PieChartConfig(
    valueFormatter = { value -> "${value.toInt()} orders" },
    percentageFormatter = { fraction -> "${(fraction * 100).toInt()}%" },
)
```

### Hoisted zoom state

Bar and line charts create zoom state automatically. Hoist it when another control must inspect or reset zoom:

```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.soheib_ta.karbon.charts.barchart.BarChart
import com.soheib_ta.karbon.charts.barchart.data.BarEntry
import com.soheib_ta.karbon.charts.barchart.data.BarSeries
import com.soheib_ta.karbon.charts.barchart.data.rememberBarChartZoomState

@Composable
fun ZoomableBars(
    entries: List<BarEntry>,
    series: List<BarSeries>,
) {
    val zoomState = rememberBarChartZoomState(
        initialScale = 1f,
        minScale = 0.75f,
        maxScale = 4f,
    )

    Button(onClick = zoomState::reset) {
        Text("Reset ${zoomState.scale}x")
    }

    BarChart(
        entries = entries,
        series = series,
        zoomState = zoomState,
    )
}
```

The corresponding line API is `rememberLineChartZoomState`. `scale` and `scrollOffset` are publicly readable, but chart gestures own their setters. `reset()` is public.

### Colors and dark themes

Bar and line series, and individual pie entries, own their data colors. Each chart config also accepts a colors object for axes, grid lines, labels, tooltips, zoom indicators, center text, or empty-state text:

- `BarChartColors`
- `LineChartColors`
- `PieChartColors`

The built-in color defaults target a light surface. Override the colors from `MaterialTheme.colorScheme` when the application supports a dark theme.

### Layout

- Give charts a finite width, normally `Modifier.fillMaxWidth()`.
- Bar and line drawing areas default to `280.dp` high.
- Pie/donut drawing areas default to `220.dp` high.
- Titles, descriptions, and legends add height outside the drawing area.
- Put multiple charts in a `LazyColumn` or another vertical scrolling container.
- The default `OnVisible` animation is designed to start when a chart enters the viewport.
- Wrap the application in `MaterialTheme`; chart labels, legends, titles, and empty states use Material 3 typography.

## Public composable signatures

These signatures are useful when generating wrappers or adapting application models.

```kotlin
@Composable
fun BarChart(
    entries: List<BarEntry>,
    series: List<BarSeries>,
    config: BarChartConfig = BarChartConfig(),
    zoomState: BarChartZoomState = rememberBarChartZoomState(),
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

```kotlin
@Composable
fun LineChart(
    entries: List<LineEntry>,
    series: List<LineSeries>,
    config: LineChartConfig = LineChartConfig(),
    zoomState: LineChartZoomState = rememberLineChartZoomState(),
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    onPointSelected: ((LineChartSelection?) -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

```kotlin
@Composable
fun PieChart(
    entries: List<PieEntry>,
    config: PieChartConfig = PieChartConfig(),
    title: (@Composable () -> Unit)? = null,
    description: (@Composable () -> Unit)? = null,
    centerValue: String? = null,
    centerLabel: String? = null,
    centerContent: (@Composable BoxScope.() -> Unit)? = null,
    onSliceSelected: ((PieChartSelection?) -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

## Coding-agent checklist

When adding Karbon to another application:

1. Confirm the consumer is a Compose Multiplatform project with Material 3.
2. Choose published, Maven Local, composite-build, or copied-module integration.
3. Add the dependency to `commonMain`, not only to a platform source set.
4. Import only public packages; never use `.internal`.
5. Match every entry-map key to its series key exactly.
6. Use non-negative finite values for bar and pie charts.
7. Use null or a missing key to create a line-chart gap.
8. Use the bar-specific legend and animation enums for `BarChartConfig`.
9. Use the shared legend and animation enums for line and pie configs.
10. Hoist zoom or selection state only when surrounding UI needs it.
11. Give the chart a finite width and place multiple charts in a vertical scroller.
12. Override structural colors for dark-theme support.
13. Compile at least one consumer target after integration.

## Repository examples

Copy-ready sample composables live in:

```text
karbon/src/commonMain/kotlin/com/soheib_ta/karbon/samples/ChartSamples.kt
```

The Compose demo that displays all chart families lives in:

```text
composeApp/src/commonMain/kotlin/com/soheib_ta/karbon/app/App.kt
```

## Build and test

From the repository root:

```powershell
.\gradlew.bat :composeApp:compileKotlinJvm
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
.\gradlew.bat :karbon:jvmTest
```

On macOS or Linux, replace `.\gradlew.bat` with `./gradlew`.

## License

Karbon is available under the Apache License 2.0. See `LICENSE`.
