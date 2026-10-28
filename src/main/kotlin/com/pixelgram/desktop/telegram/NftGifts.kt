package com.pixelgram.desktop.telegram

import it.tdlight.jni.TdApi

private val isDemo get() = System.getProperty("pixelgram.demo") == "true"

private fun error(result: TdApi.Object): String? = (result as? TdApi.Error)?.message?.let(::friendlyError)

fun Telegram.wornGiftId(): Long? = (me?.emojiStatus?.type as? TdApi.EmojiStatusTypeUpgradedGift)?.upgradedGiftId

fun Telegram.wearGift(gift: TdApi.UpgradedGift, done: (String?) -> Unit) {
    if (isDemo) {
        me?.emojiStatus = TdApi.EmojiStatus(TdApi.EmojiStatusTypeUpgradedGift(gift.id, gift.title, gift.name, 0, 0, gift.backdrop?.colors), 0)
        return done(null)
    }
    send(TdApi.GetUpgradedGiftEmojiStatuses()) { result ->
        val status = (result as? TdApi.EmojiStatuses)?.emojiStatuses?.firstOrNull {
            (it.type as? TdApi.EmojiStatusTypeUpgradedGift)?.upgradedGiftId == gift.id
        } ?: return@send done(error(result) ?: "This gift can't be worn")
        send(TdApi.SetEmojiStatus(TdApi.EmojiStatus(status.type, 0))) { done(error(it)) }
    }
}

fun Telegram.takeOffGift(done: (String?) -> Unit) {
    if (isDemo) {
        me?.emojiStatus = null
        return done(null)
    }
    send(TdApi.SetEmojiStatus(null as TdApi.EmojiStatus?)) { done(error(it)) }
}

fun Telegram.transferGift(gift: TdApi.ReceivedGift, to: TdApi.MessageSender, done: (String?) -> Unit) {
    if (isDemo) return done(null)
    send(TdApi.TransferGift("", gift.receivedGiftId, to, gift.transferStarCount)) { done(error(it)) }
}

fun Telegram.setGiftResalePrice(gift: TdApi.ReceivedGift, stars: Long?, done: (String?) -> Unit) {
    if (isDemo) return done(null)
    send(TdApi.SetGiftResalePrice(gift.receivedGiftId, stars?.let { TdApi.GiftResalePriceStar(it) })) { done(error(it)) }
}

fun Telegram.giftUpgradePreview(gift: TdApi.Gift, done: (TdApi.GiftUpgradePreview?) -> Unit) {
    if (isDemo) return done(null)
    send(TdApi.GetGiftUpgradePreview(gift.id)) { done(it as? TdApi.GiftUpgradePreview) }
}

fun Telegram.upgradeGift(gift: TdApi.ReceivedGift, price: Long, keepDetails: Boolean, done: (TdApi.UpgradedGift?, String?) -> Unit) {
    if (isDemo) return done(demoUpgradedGift(), null)
    send(TdApi.UpgradeGift("", gift.receivedGiftId, keepDetails, price)) { result ->
        done((result as? TdApi.UpgradeGiftResult)?.gift, error(result))
    }
}
