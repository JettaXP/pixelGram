package com.pixelgram.desktop.telegram

import androidx.compose.runtime.mutableStateMapOf

object ReactionEffects {
    class Shot(val emoji: String, val big: Boolean)

    val pending = mutableStateMapOf<Pair<Long, Long>, Shot>()

    fun play(owner: Pair<Long, Long>, emoji: String, big: Boolean) {
        pending[owner] = Shot(emoji, big)
    }

    fun done(owner: Pair<Long, Long>, shot: Shot) {
        if (pending[owner] === shot) pending.remove(owner)
    }
}
