package moe.ono.hooks.item.chat

import android.annotation.SuppressLint
import moe.ono.config.ConfigManager
import moe.ono.constants.Constants
import moe.ono.hooks._base.BaseClickableFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.HookItemFactory.getItem
import moe.ono.hooks.protocol.sendPacket
import moe.ono.util.Logger


@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天与消息/自我回应", description = "点击配置表情 ID")
class SelfMessageReactor : BaseClickableFunctionHookItem() {
    override fun entry(classLoader: ClassLoader) {}

    private fun parseFaceList(cfg: String?, defaultFace: Int = 355): List<Int> {
        return cfg
            ?.split(',')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it >= 0 }
            .takeIf { it!!.isNotEmpty() }
            ?: listOf(defaultFace)
    }

    fun react(peerUid: Long, msgSeq: Long) {
        if (!ConfigManager.dGetBoolean(Constants.PrekClickableXXX + getItem(SelfMessageReactor::class.java).path)){
            return
        }
        val faceCfg = ConfigManager.dGetString(
            Constants.PrekCfgXXX + getItem(SelfMessageReactor::class.java).path, "355")
        val faceList = parseFaceList(faceCfg)

        Thread {
            faceList.forEach { faceIndex ->
                // 利用发包回应，更通用
                // serviceType = 1 为回应, 2 则是取消回应
                val packetContent =
                    """{"1":36994,"2":1,"4":{"2":$peerUid,"3":$msgSeq,"4":"$faceIndex","5":1,"6":0,"7":0},"12":1}"""
                sendPacket("OidbSvcTrpcTcp.0x9082_2", packetContent)
                Thread.sleep(100)
            }
        }.start()

    }
}

