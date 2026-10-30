package com.pixelgram.setup

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Path
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pixelgram.setup.Strings.tr
import java.io.File
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val isWindows = System.getProperty("os.name").lowercase().startsWith("windows")
private val demo = System.getProperty("pixelgram.setup.demo") == "true" || !isWindows

private sealed interface Step {
    data object Welcome : Step
    data object Folder : Step
    data object Installing : Step
    data object Done : Step
    data class Failed(val message: String, val log: File?, val cancelled: Boolean = false) : Step
}

private fun defaultTarget(): File {
    val base = System.getenv("LOCALAPPDATA")?.let(::File) ?: File(System.getProperty("user.home"), "AppData/Local")
    return File(base, "pixelGram")
}

private fun withAppFolder(dir: File): File {
    if (dir.name.equals("pixelGram", ignoreCase = true)) return dir
    val empty = dir.listFiles().isNullOrEmpty()
    return if (empty) dir else File(dir, "pixelGram")
}

private fun payload(args: Array<String>): File? {
    val given = args.indexOf("--msi").takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }?.let(::File)
    if (given != null && given.isFile) return given
    val appDir = System.getProperty("jpackage.app-path")?.let { File(it).parentFile }
    return listOfNotNull(appDir, File(".").absoluteFile).map { File(it, "pixelGram.msi") }.firstOrNull { it.isFile }
}

private fun darkSystem(): Boolean {
    System.getProperty("pixelgram.setup.dark")?.let { return it == "true" }
    if (!isWindows) return false
    return runCatching {
        val process = ProcessBuilder("reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme")
            .redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        output.contains("0x0") && !output.contains("0x1")
    }.getOrDefault(false)
}

private val Purple = Color(0xFF6750A4)

private val LightScheme = lightColorScheme(primary = Purple)
private val DarkScheme = darkColorScheme(primary = Color(0xFFD0BCFF))

fun main(args: Array<String>) {
    val msi = payload(args)
    val dark = darkSystem()
    val version = runCatching { Thread.currentThread().contextClassLoader.getResource("setup-version.txt")?.readText()?.trim() }.getOrNull().orEmpty()
    application {
        val state = rememberWindowState(size = DpSize(WindowWidth, WindowHeight), position = WindowPosition(Alignment.Center))
        Window(
            onCloseRequest = ::exitApplication,
            state = state,
            title = "pixelGram Setup",
            undecorated = true,
            transparent = true,
            resizable = false,
            icon = BitmapPainter(loadImageBitmap(Thread.currentThread().contextClassLoader.getResourceAsStream("icon-light.png")!!)),
        ) {
            MaterialExpressiveTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
                CompositionLocalProvider(LocalLayoutDirection provides if (Strings.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    WindowDraggableArea {
                        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxSize()) {
                            SetupContent(msi, version, dark, onExit = ::exitApplication)
                        }
                    }
                }
            }
        }
    }
    exitProcess(0)
}

@Composable
private fun SetupContent(msi: File?, version: String, dark: Boolean, onExit: () -> Unit) {
    var step by remember { mutableStateOf<Step>(Step.Welcome) }
    var target by remember { mutableStateOf(defaultTarget()) }
    var launch by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    var action by remember { mutableStateOf(tr("Preparing")) }
    var cancelled by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val logo = remember(scheme.primaryContainer, scheme.onPrimaryContainer) { themedLogo(scheme.primaryContainer, scheme.onPrimaryContainer) }
    fun start() {
        if (msi == null && !demo) {
            step = Step.Failed(tr("The installer package is missing."), null)
            return
        }
        cancelled = false
        progress = 0f
        step = Step.Installing
        thread(name = "pixelgram-setup", isDaemon = true) {
            val engine = if (demo) DemoInstaller else WindowsInstaller
            val result = engine.install(msi ?: File("pixelGram.msi"), target, object : InstallListener {
                override fun progress(fraction: Float) {
                    javax.swing.SwingUtilities.invokeLater { progress = fraction }
                }
                override fun action(text: String) {
                    javax.swing.SwingUtilities.invokeLater { action = tr(text) }
                }
            }) { cancelled }
            javax.swing.SwingUtilities.invokeLater {
                step = when (result) {
                    InstallResult.Done -> Step.Done
                    InstallResult.Cancelled -> Step.Failed(tr("Installation was cancelled"), null, cancelled = true)
                    is InstallResult.Failed -> Step.Failed(result.message, result.log)
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (step != Step.Installing) {
            IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(SetupIcons.Close, contentDescription = tr("Close"))
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = { (fadeIn(tween(FadeMs)) + scaleIn(initialScale = EnterScale)) togetherWith fadeOut(tween(FadeMs)) },
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 28.dp),
        ) { shown ->
            when (shown) {
                Step.Welcome -> Welcome(logo, version, target, onChange = { step = Step.Folder }, onCancel = onExit, onInstall = ::start)
                Step.Folder -> FolderPicker(target, onPicked = { picked ->
                    target = withAppFolder(picked)
                    step = Step.Welcome
                }, onBack = { step = Step.Welcome })
                Step.Installing -> Installing(logo, progress, action, onCancel = { cancelled = true })
                Step.Done -> Finished(logo, launch, { launch = it }, onDone = {
                    if (launch && !demo) openApp(target)
                    onExit()
                })
                is Step.Failed -> Failed(shown, onRetry = ::start, onClose = onExit)
            }
        }
    }
}

@Composable
private fun Logo(logo: androidx.compose.ui.graphics.ImageBitmap?, pulsing: Boolean) {
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(1f, PulseScale, infiniteRepeatable(tween(PulseMs), RepeatMode.Reverse))
    val scale = if (pulsing) pulse else 1f
    logo?.let {
        Image(it, contentDescription = "pixelGram", modifier = Modifier.size(LogoBox).graphicsLayer { scaleX = scale; scaleY = scale })
    }
}

@Composable
private fun Welcome(
    logo: androidx.compose.ui.graphics.ImageBitmap?,
    version: String,
    target: File,
    onChange: () -> Unit,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Logo(logo, pulsing = false)
        Text("pixelGram", style = MaterialTheme.typography.headlineMedium)
        Text(
            tr("Telegram client in Material 3 Expressive") + if (version.isNotEmpty()) " · $version" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)) {
                Icon(SetupIcons.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(tr("Install to"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(target.absolutePath, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.StartEllipsis)
                }
                FilledTonalButton(onClick = onChange) { Text(tr("Change")) }
            }
        }
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text(tr("Cancel")) }
            Button(onClick = onInstall) { Text(tr("Install")) }
        }
    }
}

@Composable
private fun Installing(logo: androidx.compose.ui.graphics.ImageBitmap?, progress: Float, action: String, onCancel: () -> Unit) {
    val shown by animateFloatAsState(progress, tween(ProgressMs))
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Logo(logo, pulsing = true)
        Text(tr("Installing pixelGram…"), style = MaterialTheme.typography.titleLarge)
        LinearWavyProgressIndicator(
            progress = { shown },
            waveSpeed = 0.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
            Text("${(shown * Percent).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onCancel) { Text(tr("Cancel")) }
    }
}

@Composable
private fun Finished(logo: androidx.compose.ui.graphics.ImageBitmap?, launch: Boolean, onLaunch: (Boolean) -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.weight(1f))
        DoneBadge(logo)
        Text(tr("pixelGram is installed"), style = MaterialTheme.typography.headlineSmall)
        Surface(
            onClick = { onLaunch(!launch) },
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, end = 18.dp, top = 4.dp, bottom = 4.dp)) {
                Checkbox(checked = launch, onCheckedChange = onLaunch)
                Text(tr("Open pixelGram when done"), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Box(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onDone) { Text(tr("Done")) }
        }
    }
}

@Composable
private fun DoneBadge(logo: androidx.compose.ui.graphics.ImageBitmap?) {
    val reveal = remember { Animatable(0f) }
    val draw = remember { Animatable(0f) }
    val turn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(RevealMs, easing = FastOutSlowInEasing))
        draw.animateTo(1f, tween(CheckDrawMs, easing = LinearOutSlowInEasing))
        while (true) {
            turn.snapTo(0f)
            turn.animateTo(FullTurn, tween(TurnMs, easing = LinearEasing))
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val mark = MaterialTheme.colorScheme.onPrimary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(LogoBox)) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = turn.value
                    alpha = reveal.value
                    val grow = CookieStartScale + (1f - CookieStartScale) * reveal.value
                    scaleX = grow
                    scaleY = grow
                }
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(primary),
        )
        logo?.let {
            Image(
                it,
                contentDescription = null,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    alpha = 1f - reveal.value
                    val shrink = 1f - LogoShrink * reveal.value
                    scaleX = shrink
                    scaleY = shrink
                },
            )
        }
        Canvas(Modifier.size(CheckSize)) {
            val progress = draw.value
            if (progress <= 0f) return@Canvas
            val path = Path().apply {
                moveTo(size.width * 0.16f, size.height * 0.53f)
                lineTo(size.width * 0.41f, size.height * 0.77f)
                lineTo(size.width * 0.86f, size.height * 0.29f)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * progress, drawn, true)
            drawPath(drawn, mark, style = Stroke(width = size.width * CheckStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun Failed(step: Step.Failed, onRetry: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(LogoBox).clip(MaterialShapes.Burst.toShape())) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxSize()) {}
            Icon(SetupIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(CheckSize))
        }
        Text(tr(if (step.cancelled) "Installation was cancelled" else "Installation failed"), style = MaterialTheme.typography.headlineSmall)
        if (!step.cancelled) {
            Text(step.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 4)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            step.log?.takeIf { it.isFile }?.let { log ->
                TextButton(onClick = { runCatching { java.awt.Desktop.getDesktop().open(log) } }) { Text(tr("Show log")) }
            }
            TextButton(onClick = onClose) { Text(tr("Close")) }
            Button(onClick = onRetry) { Text(tr("Try again")) }
        }
    }
}

private fun themedLogo(background: Color, glyph: Color): androidx.compose.ui.graphics.ImageBitmap? {
    val source = Thread.currentThread().contextClassLoader.getResourceAsStream("icon-dark.png")?.use { javax.imageio.ImageIO.read(it) } ?: return null
    val bg = background.toArgb()
    val fg = glyph.toArgb()
    val result = java.awt.image.BufferedImage(source.width, source.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until source.height) {
        for (x in 0 until source.width) {
            val pixel = source.getRGB(x, y)
            val alpha = pixel ushr 24
            if (alpha == 0) continue
            val gray = ((pixel shr 16 and 0xFF) + (pixel shr 8 and 0xFF) + (pixel and 0xFF)) / 3f
            val t = ((gray - MaskBackground) / (255f - MaskBackground)).coerceIn(0f, 1f)
            fun mix(shift: Int) = ((bg shr shift and 0xFF) * (1 - t) + (fg shr shift and 0xFF) * t).toInt()
            result.setRGB(x, y, (alpha shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0))
        }
    }
    return result.toComposeImageBitmap()
}

private fun openApp(target: File) {
    val exe = File(target, "pixelGram.exe")
    if (exe.isFile) runCatching { ProcessBuilder(exe.absolutePath).directory(target).start() }
}

private val WindowWidth = 560.dp
private val WindowHeight = 460.dp
private val LogoBox = 112.dp
private val CheckSize = 48.dp
private const val FadeMs = 220
private const val ProgressMs = 300
private const val PulseMs = 900
private const val PulseScale = 1.06f
private const val EnterScale = 0.94f
private const val Percent = 100
private const val MaskBackground = 74f
private const val RevealMs = 420
private const val CheckDrawMs = 340
private const val TurnMs = 14000
private const val FullTurn = 360f
private const val CookieStartScale = 0.7f
private const val LogoShrink = 0.25f
private const val CheckStroke = 0.13f
