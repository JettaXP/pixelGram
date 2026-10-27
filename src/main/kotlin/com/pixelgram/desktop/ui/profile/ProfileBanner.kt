package com.pixelgram.desktop.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.outlined.AccountBox
import com.pixelgram.desktop.ui.icons.outlined.ContentPaste
import com.pixelgram.desktop.ui.icons.outlined.Delete
import com.pixelgram.desktop.ui.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.pixelgram.desktop.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.Banners
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.components.loadImage
import com.pixelgram.desktop.ui.main.Attachments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val StatusMs = 4_000L
private const val FadeFrom = 0.45f

@Composable
fun ProfileBanner(
    userId: Long,
    telegram: Telegram,
    height: Dp,
    shape: Shape,
    fade: Color,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
) {
    val file = Banners.banner(userId)
    val image by produceState(com.pixelgram.desktop.ui.components.cachedImage(file?.absolutePath), file) { value = file?.let { loadImage(it.absolutePath) } }
    val shown by animateFloatAsState(if (image != null) 1f else 0f, MaterialTheme.motionScheme.slowEffectsSpec())
    Box(modifier.fillMaxWidth().height(height).clip(shape)) {
        Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surfaceContainerHighest))
        image?.let {
            Image(
                it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().graphicsLayer { alpha = shown },
            )
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(FadeFrom to Color.Transparent, 1f to fade)))
        if (editable) {
            BannerPill(userId, telegram, hasBanner = file != null, Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
    }
}

@Composable
private fun BannerPill(userId: Long, telegram: Telegram, hasBanner: Boolean, modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(status) {
        if (status != null) {
            delay(StatusMs)
            status = null
        }
    }
    val finish: (String?) -> Unit = { error ->
        busy = false
        status = error ?: "Banner updated. Others will see it in a few minutes."
    }
    val upload: (File?) -> Unit = { source ->
        if (source != null) {
            busy = true
            scope.launch {
                val prepared = withContext(Dispatchers.IO) { Banners.prepare(source) }
                if (prepared == null) {
                    finish("This image can't be used as a banner")
                } else {
                    Banners.setOwn(userId, prepared)
                    telegram.sendBanner(prepared.absolutePath, finish)
                }
            }
        }
    }
    Box(modifier) {
        Surface(
            onClick = { if (!busy) menu = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                if (busy) {
                    LoadingIndicator(Modifier.size(20.dp))
                } else {
                    Icon(Icons.Outlined.AccountBox, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text("Banner", style = MaterialTheme.typography.labelLarge)
            }
        }
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            DropdownMenuItem(
                text = { Text("From computer") },
                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                onClick = {
                    menu = false
                    scope.launch { upload(Attachments.pick().firstOrNull()) }
                },
            )
            DropdownMenuItem(
                text = { Text("From clipboard") },
                leadingIcon = { Icon(Icons.Outlined.ContentPaste, contentDescription = null) },
                onClick = {
                    menu = false
                    upload(Attachments.fromClipboard().firstOrNull())
                },
            )
            if (hasBanner) {
                DropdownMenuItem(
                    text = { Text("Remove banner", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menu = false
                        busy = true
                        Banners.setOwn(userId, null)
                        telegram.sendBanner(null) { error ->
                            busy = false
                            status = error ?: "Banner removed"
                        }
                    },
                )
            }
        }
        AnimatedVisibility(
            visible = status != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
                Text(status.orEmpty(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}
