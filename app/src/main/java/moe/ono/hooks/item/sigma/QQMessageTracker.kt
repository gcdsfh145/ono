package moe.ono.hooks.item.sigma

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import moe.ono.R
import moe.ono.bridge.ntapi.MsgServiceHelper
import moe.ono.config.CacheConfig.getRKeyGroup
import moe.ono.config.CacheConfig.getRKeyPrivate
import moe.ono.creator.QQMessageTrackerResultDialog
import moe.ono.creator.stickerPanel.Env
import moe.ono.creator.stickerPanel.PanelUtils
import moe.ono.hooks._base.BaseClickableFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.HookItemFactory.getItem
import moe.ono.hooks.dispatcher.OnMenuBuilder
import moe.ono.reflex.Reflex
import moe.ono.ui.CommonContextWrapper
import moe.ono.util.AppRuntimeHelper
import moe.ono.util.ContextUtils
import moe.ono.util.CustomMenu
import moe.ono.util.Logger
import moe.ono.util.Session
import moe.ono.util.SyncUtils
import java.util.Locale

@HookItem(
    path = "Sigma/已读追踪",
    description = "发送一种特殊的 ARK 消息用于追踪看到此类型消息的用户\n* 此功能依赖快捷菜单\n* 重启生效"
)
class QQMessageTracker : BaseClickableFunctionHookItem(), OnMenuBuilder {
    @Throws(Throwable::class)
    override fun load(classLoader: ClassLoader) {
    }

    data class AppData(
        val app: String,
        val config: Config,
        val desc: String,
        val meta: Meta,
        val prompt: String,
        val ver: String,
        val view: String
    )

    data class Config(
        val ctime: Long,
        val token: String
    )

    data class Meta(
        val detail_1: Detail
    )

    data class Detail(
        val desc: String,
        val icon: String,
        val preview: String,
        val title: String,
        val url: String
    )

    override val targetTypes = arrayOf(
        "com.tencent.mobileqq.aio.msglist.holder.component.ark.AIOArkContentComponent",
    )

    override fun onGetMenu(aioMsgItem: Any, targetType: String, param: MethodHookParam) {
        if (!getItem(this.javaClass).isEnabled) {
            return
        }

        val item: Any = CustomMenu.createItemIconNt(
            aioMsgItem,
            "跟踪",
            R.drawable.ic_baseline_checklist_24,
            R.id.item_check
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
                                for (element in msgRecord.elements) {
                                    if (element.arkElement != null) {
                                        val arkElement = element.arkElement
                                        val json = arkElement.bytesData
                                        val gson = Gson()
                                        val appData = gson.fromJson(json, AppData::class.java)
                                        val preview = appData?.meta?.detail_1?.preview ?: return@runOnUiThread
                                        var regex = Regex("s/(.*?).png")
                                        var matchResult = regex.find(preview)
                                        var id = matchResult?.groupValues?.get(1)
                                        if (id == null) {
                                            regex = Regex("s/(.*?)/")
                                            matchResult = regex.find(preview)
                                            id = matchResult?.groupValues?.get(1)
                                        }
                                        QQMessageTrackerResultDialog.createView(CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()), id)
                                        return@runOnUiThread
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Logger.e("QQMessageTracker.msgLongClick", e)
            }
            Unit
        }

        param.result = listOf(item) + param.result as List<*>

    }
}
