package com.soheib_ta.karbon.pickers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Drawn inline so the library needs no material-icons dependency. Both are stroked outlines that
// take their colour from the tint applied by Icon().

/** Outline calendar glyph. */
internal val KarbonCalendarIcon: ImageVector = pickerIcon("KarbonCalendar") {
    moveTo(4f, 6f)
    lineTo(20f, 6f)
    lineTo(20f, 20f)
    lineTo(4f, 20f)
    close()
    moveTo(4f, 10f)
    lineTo(20f, 10f)
    moveTo(8f, 4f)
    lineTo(8f, 8f)
    moveTo(16f, 4f)
    lineTo(16f, 8f)
}

/** Outline close (✕) glyph. */
internal val KarbonCloseIcon: ImageVector = pickerIcon("KarbonClose") {
    moveTo(6f, 6f)
    lineTo(18f, 18f)
    moveTo(18f, 6f)
    lineTo(6f, 18f)
}

private fun pickerIcon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}.build()
