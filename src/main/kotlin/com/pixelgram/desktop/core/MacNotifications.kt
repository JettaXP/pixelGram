package com.pixelgram.desktop.core

import com.sun.jna.Callback
import com.sun.jna.Pointer
import java.awt.EventQueue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Native Notification Center support. UNUserNotificationCenter refuses to run
 * outside an application bundle, so development runs fall back to in-app toasts.
 */
object MacNotifications {
    private const val Framework = "/System/Library/Frameworks/UserNotifications.framework/UserNotifications"
    private const val AlertOption = 4L
    private const val BannerAndList = 24L
    private const val MaxCallbacks = 256

    private data class Delivery(val id: String, val title: String, val body: String)

    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private val pending = ArrayDeque<Delivery>()
    private var center: Pointer? = null
    private var authorizationStarted = false
    private var authorized: Boolean? = null
    private var failed = false
    private var prepared = false

    val supported: Boolean by lazy {
        AppPaths.isMac && AppPaths.isMacBundle && runCatching {
            MacObjC.loadFramework(Framework)
            MacObjC.cls("UNUserNotificationCenter")
            true
        }.getOrDefault(false)
    }

    val available: Boolean get() = supported && !failed && authorized != false

    fun prepare() {
        if (!supported || prepared) return
        prepared = true
        EventQueue.invokeLater {
            runCatching {
                install()
                requestAuthorization()
            }.onFailure { fail("Notification setup failed", it) }
        }
    }

    fun show(title: String, body: String, onClick: () -> Unit) {
        if (!supported) return
        EventQueue.invokeLater {
            runCatching {
                install()
                val id = UUID.randomUUID().toString()
                if (callbacks.size >= MaxCallbacks) callbacks.keys.firstOrNull()?.let(callbacks::remove)
                callbacks[id] = onClick
                when (authorized) {
                    true -> deliver(Delivery(id, title, body))
                    null -> {
                        pending.addLast(Delivery(id, title, body))
                        requestAuthorization()
                    }
                    else -> callbacks.remove(id)
                }
            }.onFailure { fail("System notification failed", it) }
        }
    }

    private fun install() {
        if (center != null) return
        MacObjC.loadFramework(Framework)
        val notificationCenter = MacObjC.send(MacObjC.cls("UNUserNotificationCenter"), "currentNotificationCenter")
        val willPresent = object : WillPresentCallback {
            override fun callback(self: Pointer, command: Pointer, center: Pointer, notification: Pointer, completion: Pointer) {
                MacObjC.callBlock(completion, BannerAndList)
            }
        }
        val didRespond = object : DidRespondCallback {
            override fun callback(self: Pointer, command: Pointer, center: Pointer, response: Pointer, completion: Pointer) {
                runCatching {
                    val request = MacObjC.send(MacObjC.send(response, "notification"), "request")
                    val id = MacObjC.text(MacObjC.send(request, "identifier"))
                    if (id != null) callbacks.remove(id)?.let { action -> EventQueue.invokeLater(action) }
                }
                MacObjC.callBlock(completion)
            }
        }
        val instance = MacObjC.makeObject(
            "NotificationDelegate",
            listOf(
                MacObjC.Method(
                    "userNotificationCenter:willPresentNotification:withCompletionHandler:",
                    willPresent,
                    "v@:@@@",
                ),
                MacObjC.Method(
                    "userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:",
                    didRespond,
                    "v@:@@@",
                ),
            ),
        )
        MacObjC.sendVoid(notificationCenter, "setDelegate:", instance)
        center = notificationCenter
    }

    private fun requestAuthorization() {
        if (authorizationStarted) return
        val notificationCenter = center ?: return
        authorizationStarted = true
        val completion = object : AuthorizationCallback {
            override fun callback(block: Pointer, granted: Int, error: Pointer?) {
                val accepted = granted != 0
                EventQueue.invokeLater {
                    authorized = accepted
                    authorizationStarted = false
                    if (accepted) {
                        while (pending.isNotEmpty()) deliver(pending.removeFirst())
                    } else {
                        pending.forEach { callbacks.remove(it.id) }
                        pending.clear()
                        AppLog.write("macOS notification permission denied")
                    }
                }
            }
        }
        MacObjC.sendVoidAuthorization(
            notificationCenter,
            "requestAuthorizationWithOptions:completionHandler:",
            AlertOption,
            MacObjC.block(completion),
        )
    }

    private fun deliver(delivery: Delivery) {
        val notificationCenter = center ?: return
        val content = MacObjC.send(MacObjC.send(MacObjC.cls("UNMutableNotificationContent"), "alloc"), "init")
        MacObjC.sendVoid(content, "setTitle:", MacObjC.string(delivery.title))
        MacObjC.sendVoid(content, "setBody:", MacObjC.string(delivery.body))
        val request = MacObjC.send(
            MacObjC.cls("UNNotificationRequest"),
            "requestWithIdentifier:content:trigger:",
            MacObjC.string(delivery.id),
            content,
            Pointer.NULL,
        )
        val completion = object : AddCallback {
            override fun callback(block: Pointer, error: Pointer?) {
                error ?: return
                AppLog.write("Notification ${delivery.id} was rejected")
                callbacks.remove(delivery.id)
            }
        }
        MacObjC.sendVoid(
            notificationCenter,
            "addNotificationRequest:withCompletionHandler:",
            request,
            MacObjC.block(completion),
        )
    }

    private fun fail(message: String, error: Throwable) {
        failed = true
        AppLog.write(message, error)
    }

    interface WillPresentCallback : Callback {
        fun callback(self: Pointer, command: Pointer, center: Pointer, notification: Pointer, completion: Pointer)
    }

    interface DidRespondCallback : Callback {
        fun callback(self: Pointer, command: Pointer, center: Pointer, response: Pointer, completion: Pointer)
    }

    interface AuthorizationCallback : Callback {
        fun callback(block: Pointer, granted: Int, error: Pointer?)
    }

    interface AddCallback : Callback {
        fun callback(block: Pointer, error: Pointer?)
    }
}
