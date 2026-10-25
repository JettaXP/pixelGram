package com.pixelgram.desktop.ui.auth

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.pixelgram.desktop.ui.icons.filled.QrCode2
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.pixelgram.desktop.ui.icons.Icons
import com.pixelgram.desktop.ui.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.telegram.CodeInfo
import com.pixelgram.desktop.telegram.Country
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.theme.EmojiText
import it.tdlight.jni.TdApi
import java.util.Locale
import kotlinx.coroutines.delay

private const val MinNationalDigits = 5
private const val MaxCodeDigits = 4

@Composable
fun PhoneStep(telegram: Telegram, onQr: () -> Unit, onCancel: (() -> Unit)? = null) {
    val countries = telegram.countries
    var picked by remember { mutableStateOf<Country?>(null) }
    var code by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }

    LaunchedEffect(countries) {
        if (code.isEmpty() && countries.isNotEmpty()) {
            val region = Locale.getDefault().country
            val initial = countries.firstOrNull { it.code == region } ?: countries.first()
            picked = initial
            code = initial.callingCode
        }
    }

    val digits = number.filter(Char::isDigit)
    val info = remember(code, digits) {
        if (code.isEmpty()) null else telegram.phoneInfo(code + digits)
    }
    val country = when {
        picked?.callingCode == code -> picked
        else -> countries.firstOrNull { it.callingCode == code && it.code == info?.country?.countryCode }
            ?: countries.firstOrNull { it.callingCode == code }
    }
    val callingCode = if (country != null) code else ""
    val pattern = info?.formattedPhoneNumber.orEmpty()
    val missing = pattern.count { it == '-' }
    val complete = country != null && digits.length >= MinNationalDigits && missing == 0
    val busy = telegram.authBusy
    val submit = { if (complete && !busy) telegram.sendPhone("+$callingCode$digits") }

    Hint("Choose your country and enter your phone number.")
    OutlinedCard(
        onClick = { picking = true },
        enabled = countries.isNotEmpty() && !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmojiText(
                (when {
                    country != null -> "${country.flag}  ${country.name}"
                    countries.isEmpty() -> "Loading countries…"
                    else -> "Invalid country code"
                }),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose a country")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        OutlinedTextField(
            value = "+$code",
            onValueChange = { value ->
                code = value.filter(Char::isDigit).take(MaxCodeDigits)
                picked = null
            },
            label = { Text("Code") },
            singleLine = true,
            isError = code.isNotEmpty() && countries.isNotEmpty() && country == null,
            enabled = !busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier.width(96.dp),
        )
        OutlinedTextField(
            value = number,
            onValueChange = { value ->
                if (value.startsWith("+")) {
                    val pasted = telegram.phoneInfo(value)
                    val pastedCode = pasted?.countryCallingCode
                    val match = countries.firstOrNull {
                        it.callingCode == pastedCode && it.code == pasted?.country?.countryCode
                    } ?: countries.firstOrNull { it.callingCode == pastedCode }
                    if (match != null && pastedCode != null) {
                        picked = match
                        code = pastedCode
                        number = value.filter(Char::isDigit).removePrefix(pastedCode)
                        return@OutlinedTextField
                    }
                }
                number = value.filter { it.isDigit() || it == ' ' || it == '-' }
            },
            label = { Text("Phone number") },
            placeholder = { Text(pattern.replace(Regex("[0-9]"), "0").ifEmpty { "000 000 0000" }) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            enabled = !busy,
            modifier = Modifier.weight(1f),
        )
    }
    Text(
        when {
            digits.isEmpty() -> " "
            missing > 0 -> "$missing more digit${if (missing == 1) "" else "s"}"
            else -> "+$callingCode ${info?.formattedPhoneNumber ?: digits}"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton("Next", busy, enabled = complete, onClick = submit)
    PhoneActions(busy, onQr, onCancel)

    if (picking) {
        CountryPicker(
            countries = countries,
            onPick = {
                picked = it
                code = it.callingCode
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun CountryPicker(
    countries: List<Country>,
    onPick: (Country) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, countries) {
        val q = query.trim().removePrefix("+").lowercase()
        if (q.isEmpty()) countries else countries.filter {
            it.name.lowercase().contains(q) || it.callingCode.startsWith(q) || it.code.lowercase() == q
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a country") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp).padding(top = 8.dp)) {
                    items(filtered, key = { it.code + it.callingCode }) { country ->
                        ListItem(
                            modifier = Modifier.clickable { onPick(country) },
                            leadingContent = { EmojiText(country.flag, style = MaterialTheme.typography.titleLarge) },
                            headlineContent = { Text(country.name) },
                            trailingContent = { Text("+${country.callingCode}") },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun describeCodeType(type: TdApi.AuthenticationCodeType?): String = when (type) {
    is TdApi.AuthenticationCodeTypeTelegramMessage -> "in Telegram on your other device"
    is TdApi.AuthenticationCodeTypeSms,
    is TdApi.AuthenticationCodeTypeSmsWord,
    is TdApi.AuthenticationCodeTypeSmsPhrase -> "by SMS"
    is TdApi.AuthenticationCodeTypeCall -> "by a phone call"
    is TdApi.AuthenticationCodeTypeFlashCall,
    is TdApi.AuthenticationCodeTypeMissedCall -> "with a missed call"
    is TdApi.AuthenticationCodeTypeFragment -> "on Fragment"
    else -> ""
}

private fun describeNextType(type: TdApi.AuthenticationCodeType?): String = when (type) {
    is TdApi.AuthenticationCodeTypeSms,
    is TdApi.AuthenticationCodeTypeSmsWord,
    is TdApi.AuthenticationCodeTypeSmsPhrase -> "Send code by SMS"
    is TdApi.AuthenticationCodeTypeCall -> "Call me with the code"
    is TdApi.AuthenticationCodeTypeFlashCall,
    is TdApi.AuthenticationCodeTypeMissedCall -> "Get a missed call"
    else -> "Send the code again"
}

@Composable
fun CodeStep(info: CodeInfo, telegram: Telegram, onChangeNumber: () -> Unit) {
    var code by remember(info) { mutableStateOf("") }
    var secondsLeft by remember(info) { mutableIntStateOf(info.timeoutSeconds) }
    LaunchedEffect(info) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft = (info.timeoutSeconds - ((System.currentTimeMillis() - info.receivedAt) / 1000).toInt())
                .coerceAtLeast(0)
        }
    }
    val length = info.length
    val busy = telegram.authBusy
    val submit = { if (code.isNotEmpty() && !busy) telegram.sendCode(code) }
    val via = describeCodeType(info.type)

    Text(
        "+" + info.phoneNumber.removePrefix("+"),
        style = MaterialTheme.typography.titleMedium,
    )
    Hint(if (via.isNotEmpty()) "We've sent the code $via." else "Enter the code you received.")
    OutlinedTextField(
        value = code,
        onValueChange = { value ->
            code = value.filter(Char::isLetterOrDigit).let { if (length > 0) it.take(length) else it }
            if (length > 0 && code.length == length) submit()
        },
        label = { Text("Code") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    )
    BusyButton(
        "Sign in",
        busy,
        enabled = code.isNotEmpty() && (length == 0 || code.length == length),
        onClick = submit,
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = onChangeNumber, enabled = !busy) { Text("Wrong number?") }
        if (info.nextType != null) {
            TextButton(onClick = telegram::resendCode, enabled = secondsLeft == 0 && !busy) {
                Text(
                    if (secondsLeft > 0) {
                        "%s in %d:%02d".format(describeNextType(info.nextType), secondsLeft / 60, secondsLeft % 60)
                    } else {
                        describeNextType(info.nextType)
                    },
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhoneActions(busy: Boolean, onQr: () -> Unit, onCancel: (() -> Unit)?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val qrSource = remember { MutableInteractionSource() }
        val qrPressed by qrSource.collectIsPressedAsState()
        FilledTonalButton(
            onClick = onQr,
            enabled = !busy,
            interactionSource = qrSource,
            shape = when {
                onCancel == null -> CircleShape
                qrPressed -> ButtonGroupDefaults.connectedLeadingButtonPressShape
                else -> ButtonGroupDefaults.connectedLeadingButtonShape
            },
            modifier = Modifier.weight(1f).height(ActionHeight),
        ) {
            Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(if (onCancel == null) "Log in by QR code" else "QR code")
        }
        if (onCancel != null) {
            val cancelSource = remember { MutableInteractionSource() }
            val cancelPressed by cancelSource.collectIsPressedAsState()
            OutlinedButton(
                onClick = onCancel,
                interactionSource = cancelSource,
                shape = if (cancelPressed) ButtonGroupDefaults.connectedTrailingButtonPressShape else ButtonGroupDefaults.connectedTrailingButtonShape,
                modifier = Modifier.weight(1f).height(ActionHeight),
            ) {
                Text("Cancel")
            }
        }
    }
}

private val ActionHeight = 48.dp
