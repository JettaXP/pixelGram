package com.pixelgram.desktop.ui.main

import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun DateDivider(unixSeconds: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Wave(Modifier.weight(1f))
        DatePill(unixSeconds, elevation = 0.dp)
        Wave(Modifier.weight(1f))
    }
}

@Composable
fun DatePill(unixSeconds: Int, elevation: Dp, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = elevation,
        modifier = modifier,
    ) {
        Text(
            dayLabel(unixSeconds),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Wave(modifier: Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.height(WaveHeight)) {
        val amplitude = WaveAmplitude.toPx()
        val length = WaveLength.toPx()
        val middle = size.height / 2
        val path = Path().apply {
            moveTo(0f, middle)
            var x = 0f
            while (x <= size.width) {
                lineTo(x, middle + amplitude * sin(2 * PI * x / length).toFloat())
                x += 1f
            }
        }
        drawPath(path, color, style = Stroke(width = WaveStroke.toPx(), cap = StrokeCap.Round))
    }
}

fun sameDay(a: Int, b: Int): Boolean = localDate(a) == localDate(b)

private fun localDate(unixSeconds: Int): LocalDate =
    Instant.ofEpochSecond(unixSeconds.toLong()).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dayLabel(unixSeconds: Int): String {
    val date = localDate(unixSeconds)
    val pattern = if (date.year == LocalDate.now().year) "MMMM d" else "MMMM d, yyyy"
    return date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
}

private val WaveHeight = 10.dp
private val WaveAmplitude = 2.dp
private val WaveLength = 14.dp
private val WaveStroke = 1.5.dp

@Composable
fun FloatingDate(
    listState: androidx.compose.foundation.lazy.LazyListState,
    dateAt: (Int) -> Int?,
    modifier: Modifier = Modifier,
) {
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(listState) {
        var last = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collectLatest { now ->
                val older = now.first > last.first || (now.first == last.first && now.second > last.second)
                last = now
                if (older) shown = true
                kotlinx.coroutines.delay(HideAfterMs)
                shown = false
            }
    }
    val top = androidx.compose.runtime.remember(listState) {
        androidx.compose.runtime.derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.maxByOrNull { it.index }?.index?.let(dateAt)
        }
    }
    var label by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    top.value?.let { label = it }
    androidx.compose.animation.AnimatedVisibility(
        visible = shown && label != 0,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { -it / 2 },
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 },
        modifier = modifier,
    ) {
        DatePill(label, elevation = 3.dp)
    }
}

private const val HideAfterMs = 1000L
