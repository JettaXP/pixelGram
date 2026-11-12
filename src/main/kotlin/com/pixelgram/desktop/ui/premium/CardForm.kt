package com.pixelgram.desktop.ui.premium

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelgram.desktop.core.tr
import com.pixelgram.desktop.telegram.Telegram
import com.pixelgram.desktop.ui.auth.PasswordChars
import com.pixelgram.desktop.ui.components.Text
import com.pixelgram.desktop.ui.components.expressive.Button
import it.tdlight.jni.TdApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.YearMonth

object Card {
    fun luhn(number: String): Boolean {
        if (number.length !in 12..19 || number.any { it !in '0'..'9' }) return false
        var sum = 0
        number.reversed().forEachIndexed { i, c ->
            var d = c - '0'
            if (i % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }

    fun brand(number: String): String? = when {
        number.startsWith("4") -> "Visa"
        number.take(2).toIntOrNull() in 51..55 || number.take(4).toIntOrNull() in 2221..2720 -> "Mastercard"
        number.take(4).toIntOrNull() in 2200..2204 -> "МИР"
        number.startsWith("34") || number.startsWith("37") -> "American Express"
        number.startsWith("62") -> "UnionPay"
        number.take(4).toIntOrNull() in 3528..3589 -> "JCB"
        number.startsWith("6011") || number.startsWith("65") -> "Discover"
        else -> null
    }

    fun maxLength(number: String) = when {
        number.startsWith("34") || number.startsWith("37") -> 15
        number.startsWith("4") || number.startsWith("5") || number.startsWith("2") -> 16
        else -> 19
    }

    fun cvcLength(number: String) = if (number.startsWith("34") || number.startsWith("37")) 4 else 3

    fun digits(text: String, max: Int) = text.filter { it in '0'..'9' }.take(max)

    fun month(text: String): String {
        val d = digits(text, 2)
        return if (d.length == 1 && d[0] > '1') "0$d" else d
    }

    fun monthValid(month: String) = month.length == 2 && month.toInt() in 1..12

    fun expiryValid(month: String, year: String): Boolean {
        if (!monthValid(month) || year.length != 2) return false
        val date = YearMonth.of(2000 + year.toInt(), month.toInt())
        val now = YearMonth.now()
        return !date.isBefore(now) && date.year <= now.year + 20
    }

    val Grouping = VisualTransformation { text ->
        val raw = text.text
        val out = raw.chunked(4).joinToString(" ")
        TransformedText(
            AnnotatedString(out),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = if (offset == 0) 0 else (offset + (offset - 1) / 4).coerceAtMost(out.length)
                override fun transformedToOriginal(offset: Int) = (offset - offset / 5).coerceIn(0, raw.length)
            },
        )
    }

    data class Details(val number: String, val month: String, val year: String, val cvc: String, val holder: String, val country: String, val postal: String)

    fun providerName(provider: TdApi.PaymentProvider?) = when (provider) {
        is TdApi.PaymentProviderStripe -> "Stripe"
        is TdApi.PaymentProviderSmartGlocal -> "Smart Glocal"
        else -> null
    }

    private val http by lazy { HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build() }

    private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun field(json: String, name: String) = Regex("\"" + name + "\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)

    suspend fun tokenize(provider: TdApi.PaymentProvider, test: Boolean, card: Details): String? = withContext(Dispatchers.IO) {
        runCatching {
            when (provider) {
                is TdApi.PaymentProviderStripe -> {
                    val fields = buildList {
                        add("card[number]" to card.number)
                        add("card[exp_month]" to card.month)
                        add("card[exp_year]" to "20${card.year}")
                        add("card[cvc]" to card.cvc)
                        if (card.holder.isNotBlank()) add("card[name]" to card.holder)
                        if (card.country.isNotBlank()) add("card[address_country]" to card.country)
                        if (card.postal.isNotBlank()) add("card[address_zip]" to card.postal)
                    }
                    val body = fields.joinToString("&") { (k, v) -> URLEncoder.encode(k, Charsets.UTF_8) + "=" + URLEncoder.encode(v, Charsets.UTF_8) }
                    val response = post("https://api.stripe.com/v1/tokens", body, "application/x-www-form-urlencoded", "Authorization", "Bearer ${provider.publishableKey}") ?: return@runCatching null
                    val id = field(response, "id") ?: return@runCatching null
                    "{\"type\":${quote(field(response, "type") ?: "card")},\"id\":${quote(id)}}"
                }
                is TdApi.PaymentProviderSmartGlocal -> {
                    val url = provider.tokenizeUrl.takeIf { it.startsWith("https://") && it.endsWith(".smart-glocal.com/cds/v1/tokenize/card") }
                        ?: if (test) "https://tgb-playground.smart-glocal.com/cds/v1/tokenize/card" else "https://tgb.smart-glocal.com/cds/v1/tokenize/card"
                    val body = "{\"card\":{\"number\":${quote(card.number)},\"expiration_month\":${quote(card.month)},\"expiration_year\":${quote("20" + card.year)},\"security_code\":${quote(card.cvc)}}}"
                    val response = post(url, body, "application/json", "X-PUBLIC-TOKEN", provider.publicToken) ?: return@runCatching null
                    val token = field(response, "token") ?: return@runCatching null
                    "{\"token\":${quote(token)},\"type\":\"card\"}"
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun post(url: String, body: String, type: String, header: String, value: String): String? {
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(80))
            .header("Content-Type", type)
            .header(header, value)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body().takeIf { response.statusCode() in 200..299 }
    }
}

private val FieldShape = RoundedCornerShape(16.dp)

private fun TextFieldValue.end() = copy(selection = TextRange(text.length))

private fun Modifier.backTo(empty: () -> Boolean, action: () -> Unit) = onPreviewKeyEvent {
    if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace && empty()) {
        action()
        true
    } else false
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CardForm(
    telegram: Telegram,
    invoice: TdApi.InputInvoice,
    form: TdApi.PaymentForm,
    type: TdApi.PaymentFormTypeRegular,
    onPaid: (verificationUrl: String?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val stripe = type.paymentProvider as? TdApi.PaymentProviderStripe
    val needs = type.invoice
    var number by remember { mutableStateOf(TextFieldValue("")) }
    var month by remember { mutableStateOf(TextFieldValue("")) }
    var year by remember { mutableStateOf(TextFieldValue("")) }
    var cvc by remember { mutableStateOf(TextFieldValue("")) }
    var holder by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var postal by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(type.savedOrderInfo?.name.orEmpty()) }
    var phone by remember { mutableStateOf(type.savedOrderInfo?.phoneNumber.orEmpty()) }
    var email by remember { mutableStateOf(type.savedOrderInfo?.emailAddress.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val monthFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    val cvcFocus = remember { FocusRequester() }
    var cvcFocused by remember { mutableStateOf(false) }

    val digits = number.text
    val numberError = digits.length >= Card.maxLength(digits) && !Card.luhn(digits)
    val expiryError = month.text.length == 2 && (!Card.monthValid(month.text) || (year.text.length == 2 && !Card.expiryValid(month.text, year.text)))
    val cvcLength = Card.cvcLength(digits)
    val orderOk = (!needs.needName || name.isNotBlank()) && (!needs.needPhoneNumber || phone.isNotBlank()) && (!needs.needEmailAddress || email.contains('@'))
    val valid = Card.luhn(digits) && Card.expiryValid(month.text, year.text) && cvc.text.length == cvcLength &&
        (stripe?.needCardholderName != true || holder.isNotBlank()) &&
        (stripe?.needCountry != true || country.isNotBlank()) &&
        (stripe?.needPostalCode != true || postal.isNotBlank()) && orderOk
    val total = needs.priceParts.sumOf { it.amount }

    fun pay(orderId: String) {
        scope.launch {
            val token = Card.tokenize(type.paymentProvider, needs.isTest, Card.Details(digits, month.text, year.text, cvc.text, holder.trim(), country.trim(), postal.trim()))
            if (token == null) {
                busy = false
                error = tr("Couldn't verify the card. Check the details and try again.")
                return@launch
            }
            telegram.send(TdApi.SendPaymentForm(invoice, form.id, orderId, "", TdApi.InputCredentialsNew(token, false), 0)) { result ->
                busy = false
                when (result) {
                    is TdApi.PaymentResult -> onPaid(result.verificationUrl.takeIf { it.isNotBlank() })
                    is TdApi.Error -> error = result.message
                }
            }
        }
    }

    val submit = submit@{
        if (!valid || busy) return@submit
        busy = true
        error = null
        if (needs.needName || needs.needPhoneNumber || needs.needEmailAddress) {
            telegram.send(TdApi.ValidateOrderInfo(invoice, TdApi.OrderInfo(name.trim(), phone.trim(), email.trim(), null), true)) { result ->
                when (result) {
                    is TdApi.ValidatedOrderInfo -> pay(result.orderInfoId)
                    is TdApi.Error -> {
                        busy = false
                        error = result.message
                    }
                }
            }
        } else pay("")
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = number,
            onValueChange = { v ->
                val clean = Card.digits(v.text, 19)
                number = TextFieldValue(clean, TextRange(clean.length))
                if (clean.length == Card.maxLength(clean) && Card.luhn(clean)) runCatching { monthFocus.requestFocus() }
            },
            label = { Text("Card number") },
            trailingIcon = Card.brand(digits)?.let { b -> { Text(b, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 12.dp)) } },
            isError = numberError,
            supportingText = if (numberError) ({ Text("Invalid card number") }) else null,
            visualTransformation = Card.Grouping,
            singleLine = true,
            shape = FieldShape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = month,
                onValueChange = { v ->
                    val m = Card.month(v.text)
                    month = TextFieldValue(m, TextRange(m.length))
                    if (m.length == 2 && Card.monthValid(m)) runCatching { yearFocus.requestFocus() }
                },
                placeholder = { Text("MM", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                isError = expiryError,
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.weight(1f).focusRequester(monthFocus),
            )
            Text("/", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = year,
                onValueChange = { v ->
                    val y = Card.digits(v.text, 2)
                    year = TextFieldValue(y, TextRange(y.length))
                    if (y.length == 2 && Card.expiryValid(month.text, y)) runCatching { cvcFocus.requestFocus() }
                },
                placeholder = { Text("YY", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                isError = expiryError,
                singleLine = true,
                shape = FieldShape,
                modifier = Modifier.weight(1f).focusRequester(yearFocus).backTo({ year.text.isEmpty() }) {
                    month = month.end()
                    runCatching { monthFocus.requestFocus() }
                },
            )
            val scheme = MaterialTheme.colorScheme
            Box(
                Modifier
                    .weight(1.3f)
                    .height(56.dp)
                    .border(if (cvcFocused) 2.dp else 1.dp, if (cvcFocused) scheme.primary else scheme.outline, FieldShape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (cvc.text.isEmpty()) Text("CVC", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                PasswordChars(length = cvc.text.length, selection = cvc.selection, cursorVisible = cvcFocused, modifier = Modifier.fillMaxWidth().fillMaxHeight())
                CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(Color.Transparent, Color.Transparent)) {
                    BasicTextField(
                        value = cvc,
                        onValueChange = { v ->
                            val c = Card.digits(v.text, cvcLength)
                            cvc = TextFieldValue(c, TextRange(c.length))
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Transparent),
                        cursorBrush = SolidColor(Color.Transparent),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(cvcFocus)
                            .onFocusChanged { cvcFocused = it.isFocused }
                            .backTo({ cvc.text.isEmpty() }) {
                                year = year.end()
                                runCatching { yearFocus.requestFocus() }
                            }
                            .onPreviewKeyEvent {
                                if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                    submit()
                                    true
                                } else false
                            },
                    )
                }
            }
        }
        if (expiryError) Text("Invalid expiry date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 16.dp))
        if (stripe?.needCardholderName == true) Input(holder, { holder = it.filter { c -> c.isLetter() || c == ' ' || c == '-' || c == '\'' }.uppercase() }, "Cardholder name")
        if (stripe?.needCountry == true || stripe?.needPostalCode == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stripe.needCountry) Input(country, { country = it.filter { c -> c.isLetter() }.take(2).uppercase() }, "Country", Modifier.weight(1f))
                if (stripe.needPostalCode) Input(postal, { postal = it.filter { c -> c.isLetterOrDigit() || c == ' ' || c == '-' }.take(10) }, "Postal code", Modifier.weight(1f))
            }
        }
        if (needs.needName) Input(name, { name = it }, "Name")
        if (needs.needPhoneNumber) Input(phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16) }, "Phone number")
        if (needs.needEmailAddress) Input(email, { email = it.trim() }, "Email")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        Button(onClick = { submit() }, enabled = valid && !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) LoadingIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text(tr("Pay {sum}").replace("{sum}", price(total, needs.currency)), style = MaterialTheme.typography.titleMedium)
        }
        Text(
            tr("Card details go straight to {provider} and are never stored in pixelGram.").replace("{provider}", Card.providerName(type.paymentProvider) ?: "the payment provider"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Input(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = FieldShape,
        modifier = modifier,
    )
}
