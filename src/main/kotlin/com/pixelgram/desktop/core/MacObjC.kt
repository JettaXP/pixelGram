package com.pixelgram.desktop.core

import com.sun.jna.Callback
import com.sun.jna.Function
import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Minimal Objective-C runtime bridge for the few AppKit frameworks the desktop app needs. */
internal object MacObjC {
    interface Runtime : Library {
        fun objc_getClass(name: String): Pointer?
        fun sel_registerName(name: String): Pointer
        fun objc_allocateClassPair(superclass: Pointer?, name: String, extraBytes: Long): Pointer?
        fun class_addMethod(cls: Pointer, sel: Pointer, imp: Callback, types: String): Boolean
        fun objc_registerClassPair(cls: Pointer)
        fun objc_retain(obj: Pointer): Pointer
    }

    interface Messages : Library {
        fun send0(receiver: Pointer, selector: Pointer): Pointer
        fun send1(receiver: Pointer, selector: Pointer, arg: Pointer): Pointer
        fun send2(receiver: Pointer, selector: Pointer, arg1: Pointer, arg2: Pointer): Pointer
        fun send3(receiver: Pointer, selector: Pointer, arg1: Pointer, arg2: Pointer, arg3: Pointer): Pointer
        fun void0(receiver: Pointer, selector: Pointer)
        fun void1(receiver: Pointer, selector: Pointer, arg1: Pointer)
        fun void2(receiver: Pointer, selector: Pointer, arg1: Pointer, arg2: Pointer)
        fun void3(receiver: Pointer, selector: Pointer, arg1: Pointer, arg2: Pointer, arg3: Pointer)
        fun void4(receiver: Pointer, selector: Pointer, arg1: Pointer, arg2: Pointer, arg3: Pointer, arg4: Pointer)
        fun voidLongBlock(receiver: Pointer, selector: Pointer, arg: Long, block: Pointer)
    }

    private val mapper = FunctionMapper { _, _ -> "objc_msgSend" }
    val runtime: Runtime by lazy { Native.load("objc", Runtime::class.java) }
    private val messages: Messages by lazy {
        Native.load("objc", Messages::class.java, mapOf<String, Any>(Library.OPTION_FUNCTION_MAPPER to mapper))
    }

    private val classes = ConcurrentHashMap<String, Pointer>()
    private val selectors = ConcurrentHashMap<String, Pointer>()
    private val alive = CopyOnWriteArrayList<Any>()

    fun cls(name: String): Pointer = classes.getOrPut(name) {
        runtime.objc_getClass(name) ?: error("Objective-C class $name is unavailable")
    }

    fun sel(name: String): Pointer = selectors.getOrPut(name) { runtime.sel_registerName(name) }

    fun loadFramework(path: String) {
        NativeLibrary.getInstance(path)
    }

    fun send(receiver: Pointer, selector: String): Pointer = messages.send0(receiver, sel(selector))

    fun send(receiver: Pointer, selector: String, arg: Pointer): Pointer = messages.send1(receiver, sel(selector), arg)

    fun send(receiver: Pointer, selector: String, arg1: Pointer, arg2: Pointer): Pointer =
        messages.send2(receiver, sel(selector), arg1, arg2)

    fun send(receiver: Pointer, selector: String, arg1: Pointer, arg2: Pointer, arg3: Pointer): Pointer =
        messages.send3(receiver, sel(selector), arg1, arg2, arg3)

    fun sendVoid(receiver: Pointer, selector: String) = messages.void0(receiver, sel(selector))

    fun sendVoid(receiver: Pointer, selector: String, arg: Pointer) = messages.void1(receiver, sel(selector), arg)

    fun sendVoid(receiver: Pointer, selector: String, arg1: Pointer, arg2: Pointer) =
        messages.void2(receiver, sel(selector), arg1, arg2)

    fun sendVoid(receiver: Pointer, selector: String, arg1: Pointer, arg2: Pointer, arg3: Pointer) =
        messages.void3(receiver, sel(selector), arg1, arg2, arg3)

    fun sendVoid(receiver: Pointer, selector: String, arg1: Pointer, arg2: Pointer, arg3: Pointer, arg4: Pointer) =
        messages.void4(receiver, sel(selector), arg1, arg2, arg3, arg4)

    fun sendVoidAuthorization(receiver: Pointer, selector: String, options: Long, block: Pointer) =
        messages.voidLongBlock(receiver, sel(selector), options, block)

    fun string(value: String): Pointer {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val memory = Memory((bytes.size + 1).toLong())
        memory.write(0, bytes, 0, bytes.size)
        memory.setByte(bytes.size.toLong(), 0.toByte())
        return send(cls("NSString"), "stringWithUTF8String:", memory)
    }

    fun text(value: Pointer?): String? {
        value ?: return null
        val chars = send(value, "UTF8String")
        if (chars == Pointer.NULL) return null
        return chars.getString(0)
    }

    fun retain(value: Pointer): Pointer {
        alive += value
        return runtime.objc_retain(value)
    }

    fun makeObject(name: String, methods: List<Method>): Pointer {
        val cls = runtime.objc_allocateClassPair(cls("NSObject"), "PixelGram$name", 0)
            ?: error("Could not create Objective-C class $name")
        methods.forEach { method ->
            check(runtime.class_addMethod(cls, sel(method.selector), method.callback, method.types)) {
                "Could not add ${method.selector}"
            }
            alive += method.callback
        }
        runtime.objc_registerClassPair(cls)
        val instance = send(send(cls, "alloc"), "init")
        retain(instance)
        return instance
    }

    fun block(callback: Callback): Pointer {
        val globalBlock = NativeLibrary.getProcess().getGlobalVariableAddress("_NSConcreteGlobalBlock")
        val descriptor = Memory(16)
        descriptor.setLong(0, 0)
        descriptor.setLong(8, BlockSize)
        val block = Memory(BlockSize)
        block.setPointer(0, globalBlock)
        block.setInt(8, GlobalBlockFlags)
        block.setInt(12, 0)
        block.setPointer(16, com.sun.jna.CallbackReference.getFunctionPointer(callback))
        block.setPointer(24, descriptor)
        alive += descriptor
        alive += block
        alive += callback
        return block
    }

    fun callBlock(block: Pointer, vararg arguments: Any) {
        val all = arrayOfNulls<Any>(arguments.size + 1)
        all[0] = block
        arguments.forEachIndexed { index, argument -> all[index + 1] = argument }
        Function.getFunction(block.getPointer(16)).invokeVoid(all)
    }

    class Method(val selector: String, val callback: Callback, val types: String)

    private const val BlockSize = 32L
    private const val GlobalBlockFlags = 0x10000000
}
