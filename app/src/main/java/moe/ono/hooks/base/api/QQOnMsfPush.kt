package moe.ono.hooks.base.api

import de.robv.android.xposed.XC_MethodHook
import moe.ono.hooks._base.ApiHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks.item.chat.HoldRevokeMessageCore
import moe.ono.hooks.item.chat.SelfMessageReactor
import moe.ono.hooks.item.entertainment.BlockBadlanguage
import moe.ono.hooks.item.entertainment.DoNotBrushMeOff
import moe.ono.reflex.ClassUtils
import moe.ono.reflex.MethodUtils
import moe.ono.util.Logger
import moe.ono.util.QAppUtils
import top.artmoe.inao.entries.InfoSyncPushOuterClass
import top.artmoe.inao.entries.MsgPushOuterClass
import top.artmoe.inao.entries.QQMessageOuterClass

@HookItem(path = "API/监听MsfPush")
class QQOnMsfPush : ApiHookItem() {
    override fun load(classLoader: ClassLoader) {
        val onMSFPushMethod = MethodUtils.create("com.tencent.qqnt.kernel.nativeinterface.IQQNTWrapperSession\$CppProxy")
            .params(
                String::class.java,
                ByteArray::class.java,
                ClassUtils.findClass("com.tencent.qqnt.kernel.nativeinterface.PushExtraInfo")
            )
            .methodName("onMsfPush")
            .first()


        hookBefore(onMSFPushMethod) { param ->
            val cmd = param.args[0] as String
            val protoBuf = param.args[1] as ByteArray
            when (cmd) {
                "trpc.msg.register_proxy.RegisterProxy.InfoSyncPush" -> {
                    handleInfoSyncPush(protoBuf, param)
                }
                "trpc.msg.olpush.OlPushService.MsgPush" -> {
                    handleMsgPush(protoBuf, param)
                }
            }
        }
    }

    private fun handleInfoSyncPush(buffer: ByteArray, param: XC_MethodHook.MethodHookParam) {
        val infoSyncPush = InfoSyncPushOuterClass.InfoSyncPush.parseFrom(buffer)
        infoSyncPush.syncRecallContent.syncInfoBodyList.forEach { syncInfoBody ->
            syncInfoBody.msgList.forEach { qqMessage ->
                val msgType = qqMessage.messageContentInfo.msgType
                val msgSubType = qqMessage.messageContentInfo.msgSubType
                if ((msgType == 732 && msgSubType == 17) || (msgType == 528 && msgSubType == 138)) {
                    val newInfoSyncPush = infoSyncPush.toBuilder().apply {
                        syncRecallContent = syncRecallContent.toBuilder().apply {
                            for (i in 0 until syncInfoBodyCount) {
                                setSyncInfoBody(
                                    i, getSyncInfoBody(i).toBuilder().clearMsg().build()
                                )
                            }
                        }.build()
                    }.build()
                    param.args[1] = newInfoSyncPush.toByteArray()
                }
            }
        }
    }

    private fun handleMsgPush(buffer: ByteArray, param: XC_MethodHook.MethodHookParam) {
        val msgPush = MsgPushOuterClass.MsgPush.parseFrom(buffer)
        val msg = msgPush.qqMessage
//         if (msgTargetUid != EnvHelper.getQQAppRuntime().currentUid) return  //  不是当前用户接受就返回
        val msgType = msg.messageContentInfo.msgType
        val msgSubType = msg.messageContentInfo.msgSubType


        val operationInfoByteArray = msg.messageBody.operationInfo.toByteArray()


        when (msgType) {
            // 82 - Group | 166 - C2C
            82, 166 -> {
                BlockBadlanguage().filter(param)
                DoNotBrushMeOff().filter(param)

                if (msgType == 82) {
                    if (msgPush.qqMessage.messageHead.senderPeerId == QAppUtils.getCurrentUin().toLong()) {
                        SelfMessageReactor().react(msgPush.qqMessage.messageHead.senderInfo.peerId, msgPush.qqMessage.messageContentInfo.msgSeq.toLong())
                    }
                }
            }

            732 -> when (msgSubType) {
                17 -> HoldRevokeMessageCore.onGroupRecallByMsgPush(operationInfoByteArray, msgPush, param)
            }

            528 -> when (msgSubType) {
                138 -> HoldRevokeMessageCore.onC2CRecallByMsgPush(operationInfoByteArray, msgPush, param)
            }
        }
    }
}