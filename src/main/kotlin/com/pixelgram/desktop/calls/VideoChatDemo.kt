package com.pixelgram.desktop.calls

import com.pixelgram.desktop.telegram.DemoLiveStreamId
import com.pixelgram.desktop.telegram.DemoVideoChatId
import com.pixelgram.desktop.telegram.endDemoVideoChat
import it.tdlight.jni.TdApi

internal object VideoChatDemo {
    private const val TickMs = 66L
    private const val SpeakEveryTicks = 14
    private const val ElapsedMs = 754_000L
    private const val VideoEndpoint = "demo-bob"
    private const val MeId = 1L
    private const val FullVolume = 10_000

    private fun main(block: () -> Unit) = javax.swing.SwingUtilities.invokeLater(block)

    private fun participant(
        userId: Long,
        order: Int,
        speaking: Boolean = false,
        muted: Boolean = false,
        hand: Boolean = false,
        video: String? = null,
        me: Boolean = false,
    ) = TdApi.GroupCallParticipant().apply {
        participantId = TdApi.MessageSenderUser(userId)
        audioSourceId = userId.toInt()
        videoInfo = video?.let { TdApi.GroupCallParticipantVideoInfo(emptyArray(), it, false) }
        bio = ""
        isCurrentUser = me
        isSpeaking = speaking
        isHandRaised = hand
        canBeMutedForAllUsers = !me && !muted
        canBeUnmutedForAllUsers = !me && muted
        canBeMutedForCurrentUser = !me
        isMutedForAllUsers = muted
        canUnmuteSelf = me
        volumeLevel = FullVolume
        this.order = order.toString().padStart(4, '0')
    }

    private fun copy(source: TdApi.GroupCallParticipant): TdApi.GroupCallParticipant = TdApi.GroupCallParticipant(
        source.participantId, source.audioSourceId, source.screenSharingAudioSourceId, source.videoInfo, source.screenSharingVideoInfo,
        source.bio, source.isCurrentUser, source.isSpeaking, source.isHandRaised, source.canBeMutedForAllUsers,
        source.canBeUnmutedForAllUsers, source.canBeMutedForCurrentUser, source.canBeUnmutedForCurrentUser,
        source.isMutedForAllUsers, source.isMutedForCurrentUser, source.canUnmuteSelf, source.volumeLevel, source.order,
    )

    fun start(call: ActiveVideoChat) {
        val stream = call.groupCallId == DemoLiveStreamId
        call.phase = VideoChatPhase.Active
        call.startedAt = System.currentTimeMillis() - if (call.groupCallId == DemoVideoChatId || stream) ElapsedMs else 0L
        call.stream = stream
        listOfNotNull(
            participant(MeId, 9000, muted = true, me = true),
            participant(2, 8000, speaking = true).takeIf { call.groupCallId == DemoVideoChatId },
            participant(3, 7000, video = VideoEndpoint).takeIf { call.groupCallId == DemoVideoChatId },
            participant(4, 6000, muted = true, hand = true).takeIf { call.groupCallId == DemoVideoChatId },
            participant(5, 5000).takeIf { call.groupCallId == DemoVideoChatId },
            participant(6, 4000, muted = true).takeIf { call.groupCallId == DemoVideoChatId },
        ).filter { !stream || it.isCurrentUser }.forEach { call.participants[senderKey(it.participantId)] = it }
        if (call.groupCallId == DemoVideoChatId) VideoChats.demoEndpoint(call, VideoEndpoint, 3)
        Thread {
            var tick = 0
            while (VideoChats.current == call && call.phase != VideoChatPhase.Ended) {
                val phase = tick * 0.08f
                if (stream) {
                    VideoChats.demoFrame(call, "stream", VideoChats.demoImage(480, 270, phase, 0.62f))
                } else if (call.groupCallId == DemoVideoChatId) {
                    VideoChats.demoFrame(call, VideoEndpoint, VideoChats.demoImage(320, 180, phase, 0.08f))
                }
                if (call.camera) VideoChats.demoFrame(call, "camera", VideoChats.demoImage(240, 180, -phase, 0.38f))
                if (call.screen != null) VideoChats.demoFrame(call, "screen", VideoChats.demoImage(320, 180, phase * 0.3f, 0.75f))
                if (tick % SpeakEveryTicks == 0 && !stream) {
                    val turn = tick / SpeakEveryTicks
                    main {
                        edit(call, 2L) { isSpeaking = turn % 3 != 2 }
                        edit(call, 5L) { isSpeaking = turn % 2 == 1 }
                        edit(call, 3L) { isSpeaking = turn % 4 == 0 }
                    }
                }
                tick++
                Thread.sleep(TickMs)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun edit(call: ActiveVideoChat, key: Long, block: TdApi.GroupCallParticipant.() -> Unit) {
        val old = call.participants[key] ?: return
        call.participants[key] = copy(old).apply(block)
    }

    fun edit(call: ActiveVideoChat, participant: TdApi.GroupCallParticipant, block: TdApi.GroupCallParticipant.() -> Unit) =
        edit(call, senderKey(participant.participantId), block)

    fun updateMe(call: ActiveVideoChat) = edit(call, MeId) {
        isMutedForAllUsers = call.muted
        isSpeaking = !call.muted
        videoInfo = if (call.camera) TdApi.GroupCallParticipantVideoInfo(emptyArray(), "demo-me", false) else null
        screenSharingVideoInfo = if (call.screen != null) TdApi.GroupCallParticipantVideoInfo(emptyArray(), "demo-me-screen", false) else null
    }

    fun raiseHand(call: ActiveVideoChat, raised: Boolean) = edit(call, MeId) { isHandRaised = raised }

    fun end(call: ActiveVideoChat) = call.telegram.endDemoVideoChat(call.groupCallId)
}
