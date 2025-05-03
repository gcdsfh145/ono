package moe.ono.hooks.base.api

import android.os.Bundle
import android.view.View
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import moe.ono.bridge.kernelcompat.ContactCompat
import moe.ono.config.CacheConfig
import moe.ono.config.ONOConf
import moe.ono.hooks._base.ApiHookItem
import moe.ono.hooks._base.BaseSwitchFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.ExceptionFactory
import moe.ono.hooks.item.chat.ChatScrollMemory
import moe.ono.reflex.ClassUtils
import moe.ono.reflex.FieldUtils
import moe.ono.reflex.Ignore
import moe.ono.reflex.MethodUtils
import moe.ono.util.Session.getContact


@HookItem(path = "API/监听QQMsgView更新")
class QQMessageViewListener : ApiHookItem() {
    var contact: ContactCompat? = null

    companion object {

        private val ON_AIO_CHAT_VIEW_UPDATE_LISTENER_MAP: HashMap<BaseSwitchFunctionHookItem, OnChatViewUpdateListener> =
            HashMap()

        /**
         * 添加消息监听器 责任链模式
         */
        @JvmStatic
        fun addMessageViewUpdateListener(
            hookItem: BaseSwitchFunctionHookItem,
            onMsgViewUpdateListener: OnChatViewUpdateListener
        ) {
            ON_AIO_CHAT_VIEW_UPDATE_LISTENER_MAP[hookItem] = onMsgViewUpdateListener
        }
    }

    override fun load(loader: ClassLoader) {
        val onMsgViewUpdate =
            MethodUtils.create("com.tencent.mobileqq.aio.msglist.holder.AIOBubbleMsgItemVB")
                .returnType(Void.TYPE)
                .params(Int::class.java, Ignore::class.java, List::class.java, Bundle::class.java)
                .first()
        hookAfter(onMsgViewUpdate) { param ->
            val thisObject = param.thisObject
            val msgView = FieldUtils.create(thisObject)
                .fieldType(View::class.java)
                .firstValue<View>(thisObject)

            val aioMsgItem = FieldUtils.create(thisObject)
                .fieldType(ClassUtils.findClass("com.tencent.mobileqq.aio.msg.AIOMsgItem"))
                .firstValue<Any>(thisObject)

            onViewUpdate(aioMsgItem, msgView)
        }
    }

    private fun onViewUpdate(aioMsgItem: Any, msgView: View) {
        val msgRecord: MsgRecord = MethodUtils.create(aioMsgItem.javaClass)
            .methodName("getMsgRecord")
            .callFirst(aioMsgItem)

        val peerUid = msgRecord.peerUid
        val msgSeq = msgRecord.msgSeq

        ONOConf.setInt("ChatScrollMemory0", peerUid, msgSeq.toInt())



        for ((switchFunctionHookItem, listener) in ON_AIO_CHAT_VIEW_UPDATE_LISTENER_MAP.entries) {
            if (switchFunctionHookItem.isEnabled) {
                try {
                    listener.onViewUpdateAfter(msgView, msgRecord)
                } catch (e: Throwable) {
                    ExceptionFactory.add(switchFunctionHookItem, e)
                }
            }
        }

        ONOConf.setInt("ChatScrollMemory", peerUid, msgSeq.toInt())
    }

    interface OnChatViewUpdateListener {
        fun onViewUpdateAfter(msgItemView: View, msgRecord: Any)
    }
}