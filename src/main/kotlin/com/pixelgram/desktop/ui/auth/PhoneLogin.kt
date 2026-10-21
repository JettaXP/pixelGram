package com.pixelgram.desktop.ui.auth

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import it.tdlight.jni.TdApi
import kotlinx.coroutines.delay
import java.util.Locale

private const val MinNationalDigits = 5

@Composable
fun PhoneStep(telegram: Telegram, onQr: () -> Unit) {
    val countries = telegram.countries
    var country by remember { mutableStateOf<Country?>(null) }
    var number by remember { mutableStateOf("") }
    var picking by remember { mutableStateOf(false) }

    // Default country from the system locale, once the list arrives.
    LaunchedEffect(countries) {
        if (country == null && countries.isNotEmpty()) {
            val region = Locale.getDefault().country
            country = countries.firstOrNull { it.code == region } ?: countries.first()
        }
    }

    val callingCode = country?.callingCode ?: ""
    val digits = number.filter(Char::isDigit)
    val info = remember(callingCode, digits) {
        if (callingCode.isEmpty()) null else telegram.phoneInfo(callingCode + digits)
    }
    val pattern = info?.formattedPhoneNumber.orEmpty()
    val missing = pattern.count { it == '-' }
    val complete = digits.length >= MinNationalDigits && missing == 0

    Hint("Choose your country and enter your phone number.")
    OutlinedCard(
        onClick = { picking = true },
        enabled = countries.isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                country?.let { "${it.flag}  ${it.name}" } ?: "Loading countries…",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose a country")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        OutlinedTextField(
            value = if (callingCode.isEmpty()) "+" else "+$callingCode",
            onValueChange = {},
            readOnly = true,
            label = { Text("Code") },
            singleLine = true,
            modifier = Modifier.width(96.dp),
        )
        OutlinedTextField(
            value = number,
            onValueChange = { value ->
                // A full international number pasted with "+" picks the country.
                if (value.startsWith("+")) {
                    val pasted = telegram.phoneInfo(value)
                    val code = pasted?.countryCallingCode
                    val match = countries.firstOrNull {
                        it.callingCode == code && it.code == pasted?.country?.countryCode
                    } ?: countries.firstOrNull { it.callingCode == code }
                    if (match != null && code != null) {
                        country = match
                        number = value.filter(Char::isDigit).removePrefix(code)
                        return@OutlinedTextField
                    }
                }
                number = value.filter { it.isDigit() || it == ' ' || it == '-' }
            },
            label = { Text("Phone number") },
            placeholder = { Text(pattern.replace(Regex("[0-9]"), "0").ifEmpty { "000 000 0000" }) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (complete) telegram.sendPhone("+$callingCode$digits")
            }),
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
    Button(
        onClick = { telegram.sendPhone("+$callingCode$digits") },
        enabled = complete,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Next")
    }
    TextButton(onClick = onQr) { Text("Log in by QR code") }

    if (picking) {
        CountryPicker(
            countries = countries,
            onPick = {
                country = it
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
                            leadingContent = { Text(country.flag, style = MaterialTheme.typography.titleLarge) },
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
            if (length > 0 && code.length == length) telegram.sendCode(code)
        },
        label = { Text("Code") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (code.isNotEmpty()) telegram.sendCode(code) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { telegram.sendCode(code) },
        enabled = code.isNotEmpty() && (length == 0 || code.length == length),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Sign in")
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = onChangeNumber) { Text("Wrong number?") }
        if (info.nextType != null) {
            TextButton(onClick = telegram::resendCode, enabled = secondsLeft == 0) {
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
