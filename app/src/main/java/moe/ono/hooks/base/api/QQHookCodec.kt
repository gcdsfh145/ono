package moe.ono.hooks.base.api

import moe.ono.ext.EMPTY_BYTE_ARRAY
import moe.ono.ext.beforeHook
import moe.ono.ext.hookMethod
import moe.ono.hooks._base.ApiHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.loader.hookapi.IHijacker
import moe.ono.util.Initiator
import moe.ono.util.Logger
import moe.ono.util.SyncUtils

@HookItem(path = "API/注入 CodecWarpper", description = "用于捕获请求，如 MessageSvc.PbSendMsg")
class QQHookCodec : ApiHookItem() {
    override fun entry(classLoader: ClassLoader) {
        val codecWarpper = Initiator.load("com.tencent.qphone.base.util.CodecWarpper")
        if (codecWarpper == null) {
            Logger.e("[QQHookCodec] CodecWarpper cannot be injected")
            return
        }

        codecWarpper.hookMethod("nativeEncodeRequest", beforeHook { it ->
            val uin: String
            val seq: Int
            val buffer: ByteArray
            val cmd: String
            val bufferIndex: Int
            val msgCookie: ByteArray?
            when(it.args.size) {
                14 -> {
                    seq = it.args[0] as Int
                    cmd = it.args[5] as String
                    buffer = it.args[12] as ByteArray
                    bufferIndex = 12
                    msgCookie = it.args[6] as? ByteArray
                    uin = it.args[9] as String
                }
                16 -> {
                    seq = it.args[0] as Int
                    cmd = it.args[5] as String
                    buffer = it.args[14] as ByteArray
                    bufferIndex = 14
                    msgCookie = it.args[6] as? ByteArray
                    uin = it.args[9] as String
                }
                17 -> {
                    seq = it.args[0] as Int
                    cmd = it.args[5] as String
                    buffer = it.args[15] as ByteArray
                    bufferIndex = 15
                    msgCookie = it.args[6] as? ByteArray
                    uin = it.args[9] as String
                }
                else -> throw RuntimeException("[QQHookCodec] nativeEncodeRequest received incorrect number of parameters")
            }


            if (hijackers.firstOrNull { it.command == cmd }?.onHandle(it, uin, cmd, seq, buffer, bufferIndex) == true) {
                it.result = EMPTY_BYTE_ARRAY
            }

        })
    }

    companion object {
        val hijackers = arrayListOf<IHijacker>()
    }

    override fun targetProcess(): Int {
        return SyncUtils.PROC_MSF
    }
}