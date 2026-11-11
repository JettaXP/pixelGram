package com.pixelgram.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.BasicAlertDialog
import com.pixelgram.desktop.ui.components.expressive.Button
import com.pixelgram.desktop.ui.components.expressive.TextButton
import com.pixelgram.desktop.ui.components.rememberPixelgramLogo
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.Check
import com.pixelgram.desktop.ui.icons.filled.Favorite
import com.pixelgram.desktop.ui.icons.outlined.Celebration
import com.pixelgram.desktop.ui.icons.outlined.ContentCopy
import com.pixelgram.desktop.ui.icons.outlined.Image
import com.pixelgram.desktop.ui.icons.outlined.Palette
import com.pixelgram.desktop.ui.icons.outlined.Payments
import com.pixelgram.desktop.ui.icons.outlined.Star
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object SupportProject {
    var open by mutableStateOf(System.getProperty("pixelgram.demo.support") == "true")
    const val Link = "pg://support"
    const val Contact = "jettaxp"
    val Wallets = listOf(
        Wallet("USDT · TON", "UQCDaQUP33fPSuaAVDlyHk2ayYzcz8mDST9GvdFFAMJdlAvs"),
        Wallet("USDT · TRC20", "TUMktFcyp7WtBnVfYP9gYPY92rdnFXUzgx"),
        Wallet("TON", "UQCDaQUP33fPSuaAVDlyHk2ayYzcz8mDST9GvdFFAMJdlAvs"),
    )

    data class Wallet(val network: String, val address: String)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SupportOverlay(onContact: () -> Unit) {
    if (!SupportProject.open) return
    val close = { SupportProject.open = false }
    BasicAlertDialog(onDismissRequest = close) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(DialogWidth),
        ) {
            Column(Modifier.heightIn(max = DialogMaxHeight)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(24.dp),
                ) {
                    val logo = rememberPixelgramLogo()
                    Box(
                        Modifier.size(88.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (logo != null) Icon(logo, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(56.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Support Development", style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
                    Text(
                        "pixelGram is made by one person in their free time. Every donation helps keep it going.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    SectionTitle("Supporters get")
                    Perk(Icons.Outlined.Star, "Numbered badge · 2 TON or 2 USDT", "A pixelGram badge with your own number next to your name, visible to everyone using pixelGram on desktop and Android.")
                    Perk(Icons.Outlined.Palette, "Custom badge · 5 TON or 5 USDT", "Pick any emoji or premium emoji and your own caption for the badge. You can change it whenever you like.")
                    SectionTitle("How to get it")
                    Perk(Icons.Outlined.Payments, "Make a donation", "Send TON or USDT to one of the addresses below.")
                    SupportProject.Wallets.forEach { WalletRow(it) }
                    Perk(Icons.Outlined.Image, "Send proof of payment", "Send @${SupportProject.Contact} a screenshot of the transfer with the amount and date.")
                    Perk(Icons.Outlined.Celebration, "Receive your badge", "Once the payment is checked, the badge appears on your profile within a few minutes.")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 20.dp, top = 4.dp),
                ) {
                    TextButton(onClick = close) { Text("Close") }
                    Button(onClick = {
                        close()
                        onContact()
                    }) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Message @${SupportProject.Contact}", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Perk(icon: ImageVector, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            Modifier.size(40.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WalletRow(wallet: SupportProject.Wallet) {
    var copied by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, top = 2.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(wallet.address), null)
                copied = true
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(wallet.network, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(wallet.address, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
            contentDescription = "Copy",
            tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp).size(20.dp),
        )
    }
}

private val DialogWidth = 480.dp
private val DialogMaxHeight = 680.dp
