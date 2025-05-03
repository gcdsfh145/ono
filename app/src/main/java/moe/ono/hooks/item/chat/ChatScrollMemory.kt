package moe.ono.hooks.item.chat

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import de.robv.android.xposed.XposedHelpers.findMethodExact
import moe.ono.bridge.kernelcompat.ContactCompat
import moe.ono.bridge.ntapi.ChatTypeConstants
import moe.ono.bridge.ntapi.NtGrayTipHelper
import moe.ono.config.CacheConfig
import moe.ono.config.ONOConf
import moe.ono.hooks._base.BaseSwitchFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.HookItemFactory.getItem
import moe.ono.hooks.base.util.Toasts
import moe.ono.hooks.clazz
import moe.ono.util.AppRuntimeHelper
import moe.ono.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Method


@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天与消息/记住上次查看位置", description = "浏览聊天记录时若上次查看位置与当前位置相差过大会有灰字提示")
class ChatScrollMemory : BaseSwitchFunctionHookItem() {
    override fun load(classLoader: ClassLoader) {}

    companion object {
        @JvmStatic
        fun createGrayTip(seq: Int, cseq: Int, peer: String, type: Int) {
            if (seq != -1 && cseq - seq > 100) {
                val builder = NtGrayTipHelper.NtGrayTipJsonBuilder()

                builder.appendText("上次查看位置")
                builder.append(NtGrayTipHelper.NtGrayTipJsonBuilder.MsgRefItem(
                    "seq: $seq",
                    seq.toLong()
                ))

                NtGrayTipHelper.addLocalJsonGrayTipMsg(
                    AppRuntimeHelper.getAppRuntime()!!,
                    ContactCompat(type, peer, ""),
                    NtGrayTipHelper.createLocalJsonElement(NtGrayTipHelper.AIO_AV_GROUP_NOTICE.toLong(), builder.build().toString(), ""),
                    true,
                    true
                ) { result, uin ->
                    if (result != 0) {
                        Logger.e("GagInfoDisclosure error: addLocalJsonGrayTipMsg failed, result=$result, uin=$uin")
                    }
                }
            }
        }
    }
}

