package moe.ono.hooks.item.entertainment

import android.annotation.SuppressLint
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import moe.ono.R
import moe.ono.bridge.ntapi.MsgServiceHelper
import moe.ono.hooks._base.BaseSwitchFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.HookItemFactory.getItem
import moe.ono.hooks.dispatcher.OnMenuBuilder
import moe.ono.hooks.protocol.sendPacket
import moe.ono.reflex.Reflex
import moe.ono.util.AppRuntimeHelper
import moe.ono.util.CustomMenu
import moe.ono.util.Logger
import moe.ono.util.Session
import moe.ono.util.SyncUtils
import kotlin.random.Random


@SuppressLint("DiscouragedApi")
@HookItem(path = "娱乐功能/回应表情", description = "回应 20 个小表情\n\n* 长按消息菜单 “回应” 按钮")
class RespondFace : BaseSwitchFunctionHookItem(), OnMenuBuilder {

    override fun entry(classLoader: ClassLoader) {}

    override fun onGetMenu(aioMsgItem: Any, targetType: String, param: MethodHookParam) {
        if (!getItem(this.javaClass).isEnabled) {
            return
        }

        val item: Any = CustomMenu.createItemIconNt(
            aioMsgItem,
            "回应",
            R.drawable.ic_baseline_auto_fix_high_24,
            R.id.item_respond_face
        ) {
            try {
                val msgID = Reflex.invokeVirtual(aioMsgItem, "getMsgId") as Long
                val msgIDs = java.util.ArrayList<Long>()
                msgIDs.add(msgID)
                AppRuntimeHelper.getAppRuntime()
                    ?.let {
                        MsgServiceHelper.getKernelMsgService(
                            it
                        )
                    }?.getMsgsByMsgId(
                        Session.getContact(),
                        msgIDs
                    ) { _, _, msgList ->
                        SyncUtils.runOnUiThread {
                            for (msgRecord in msgList) {
                                val peerUid = msgRecord.peerUid
                                val msgSeq = msgRecord.msgSeq

                                Thread {
                                    for (i in 1 .. 50) {
                                        val faceId = Random.nextInt(1, 200)
                                        val body = """{"1":36994,"2":1,"4":{"2":$peerUid,"3":$msgSeq,"4":"$faceId","5":1,"6":0,"7":0},"12":1}"""

                                        sendPacket("OidbSvcTrpcTcp.0x9082_2", body)
                                        Thread.sleep(100)
                                    }
                                }.start()
                            }
                        }
                    }
            } catch (e: Exception) {
                Logger.e("QQPullMsgEntry.msgLongClick", e)
            }
            Unit
        }
        param.result = listOf(item) + param.result as List<*>
    }
}