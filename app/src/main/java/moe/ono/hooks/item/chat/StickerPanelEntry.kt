package moe.ono.hooks.item.chat

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import moe.ono.R
import moe.ono.bridge.ntapi.MsgServiceHelper
import moe.ono.config.CacheConfig.getRKeyGroup
import moe.ono.config.CacheConfig.getRKeyPrivate
import moe.ono.constants.Constants
import moe.ono.config.ONOConf
import moe.ono.creator.stickerPanel.Env
import moe.ono.creator.stickerPanel.ICreator
import moe.ono.creator.stickerPanel.PanelUtils
import moe.ono.hooks._base.BaseClickableFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.HookItemFactory.getItem
import moe.ono.hooks.dispatcher.OnMenuBuilder
import moe.ono.reflex.Reflex
import moe.ono.reflex.XMethod
import moe.ono.ui.CommonContextWrapper
import moe.ono.util.AppRuntimeHelper
import moe.ono.util.ContextUtils
import moe.ono.util.CustomMenu
import moe.ono.util.Logger
import moe.ono.util.Session
import moe.ono.util.SyncUtils
import java.util.Locale


@SuppressLint("DiscouragedApi")
@HookItem(
    path = "聊天与消息/表情面板",
    description = "点击配置表情面板"
)
class StickerPanelEntry : BaseClickableFunctionHookItem(), OnMenuBuilder {
    private fun hookEmoBtn() {
        try {
            val method = XMethod.clz(Constants.CLAZZ_PANEL_ICON_LINEAR_LAYOUT).ret(
                ImageView::class.java
            ).ignoreParam().get()

            hookAfter(method) { param: MethodHookParam ->
                val imageView = param.result as ImageView
                if ("表情".contentEquals(imageView.contentDescription)) {
                    imageView.setOnLongClickListener { view: View ->
                        ICreator.createPanel(view.context)
                        true
                    }
                }
            }
        } catch (e: NoSuchMethodException) {
            Logger.e(this.itemName, e)
        }
    }


    @SuppressLint("ResourceType")
    private fun createStickerPanelIcon() {
        Logger.d("StickerPanelEntry", "on createStickerPanelIcon")
    }


    override fun load(classLoader: ClassLoader) {
        hookEmoBtn()
//        createStickerPanelIcon()

        val sendMsgMethod = XMethod
            .clz("com.tencent.qqnt.kernel.nativeinterface.IKernelMsgService\$CppProxy")
            .name("sendMsg")
            .ignoreParam().get()

        hookBefore(sendMsgMethod) { param ->
            val elements = param.args[2] as ArrayList<MsgElement>
            if (ONOConf.getBoolean("global", "sticker_panel_set_ch_change_title", false)) {
                val text: String =
                    ONOConf.getString("global", "sticker_panel_set_ed_change_title", "")
                if (!TextUtils.isEmpty(text)) {
                    for (element in elements) {
                        if (element.picElement != null) {
                            val picElement = element.picElement
                            picElement.summary = text
                        }
                    }
                }
            }

        }


    }

    override val targetTypes = arrayOf(
        "com.tencent.mobileqq.aio.msglist.holder.component.pic.AIOPicContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.mix.AIOMixContentComponent",
        "com.tencent.mobileqq.aio.msglist.holder.component.marketface.AIOMarketFaceComponent"
    )

    override fun onGetMenu(aioMsgItem: Any, targetType: String, param: MethodHookParam) {
        if (!getItem(this.javaClass).isEnabled) {
            return
        }
        val item: Any = CustomMenu.createItemIconNt(
            aioMsgItem,
            "保存",
            R.drawable.ic_item_save_72dp,
            R.id.item_save_to_panel
        ) {
            val rkeyGroup = getRKeyGroup()
            val rkeyPrivate = getRKeyPrivate()

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
                                val md5s =
                                    java.util.ArrayList<String>()
                                val urls =
                                    java.util.ArrayList<String>()

                                for (element in msgRecord.elements) {
                                    if (element.marketFaceElement != null) {
                                        val marketFaceElement = element.marketFaceElement
                                        val id = marketFaceElement.emojiId
                                        val localPath = Env.app_path + marketFaceElement.staticFacePath
                                        Logger.d("local path: $localPath")
                                        PanelUtils.preSaveMarketPicToList(
                                            localPath, id,
                                            CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity())
                                        )
                                        return@runOnUiThread
                                    }
                                    if (element.picElement != null) {
                                        val picElement = element.picElement

                                        md5s.add(
                                            picElement.md5HexStr.uppercase(Locale.getDefault())
                                        )
                                        val originUrl = picElement.originImageUrl
                                        if (TextUtils.isEmpty(originUrl)) {
                                            urls.add(
                                                "https://gchat.qpic.cn/gchatpic_new/0/0-0-" + picElement.md5HexStr
                                                    .uppercase(Locale.getDefault()) + "/0"
                                            )
                                        } else {
                                            Logger.d("rkeyGroup: $rkeyGroup, rkeyPrivate: $rkeyPrivate")
                                            if (originUrl.startsWith("/download")) {
                                                if (originUrl.contains("appid=1406")) {
                                                    urls.add("https://multimedia.nt.qq.com.cn$originUrl$rkeyGroup")
                                                } else {
                                                    urls.add("https://multimedia.nt.qq.com.cn$originUrl$rkeyPrivate")
                                                }
                                            } else {
                                                urls.add("https://gchat.qpic.cn" + picElement.originImageUrl)
                                            }
                                        }
                                    }
                                }
                                if (md5s.isNotEmpty()) {
                                    Logger.d("urls: $urls")
                                    if (md5s.size > 1) {
                                        PanelUtils.preSaveMultiPicList(
                                            urls,
                                            md5s,
                                            CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity())
                                        )
                                    } else {
                                        PanelUtils.preSavePicToList(
                                            urls[0], md5s[0],
                                            CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity())
                                        )
                                    }
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Logger.e("StickerPanelEntryHooker.msgLongClickSaveToLocal", e)
            }
            Unit
        }
        param.result = listOf(item) + param.result as List<*>
    }

}