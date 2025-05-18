package moe.ono.hooks.item.entertainment

import android.annotation.SuppressLint
import de.robv.android.xposed.XC_MethodHook
import moe.ono.hooks._base.BaseSwitchFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks._core.factory.HookItemFactory.getItem
import moe.ono.util.Logger
import top.artmoe.inao.entries.MsgPushOuterClass

@SuppressLint("DiscouragedApi")
@HookItem(path = "娱乐功能/反弹", description = "替换不文明词汇的主语\n* 通过拦截MsgPush实现，会影响本地聊天记录")
class BlockBadlanguage : BaseSwitchFunctionHookItem() {
    override fun entry(classLoader: ClassLoader) {}

    fun filter(param: XC_MethodHook.MethodHookParam) {
        if (!getItem(this.javaClass).isEnabled) {
            return
        }
        val msgPush = MsgPushOuterClass.MsgPush.parseFrom(param.args[1] as ByteArray)

        kotlin.runCatching {
            val oldMessage = msgPush.qqMessage

            val newMessageBody = oldMessage.messageBody.toBuilder().apply {
                if (hasRichMsg()) {
                    richMsg = richMsg.toBuilder().apply {
                        msgContentList.forEachIndexed { index, msgContent ->
                            if (msgContent.hasTextMsg()) {
                                val originalText = msgContent.textMsg.text
                                val replacementText = getReplacement(originalText)
                                if (replacementText != null) {
                                    setMsgContent(
                                        index,
                                        msgContent.toBuilder().apply {
                                            textMsg = textMsg.toBuilder().apply {
                                                text = replacementText
                                            }.build()
                                        }.build()
                                    )
                                }
                            }
                        }
                    }.build()
                }
            }.build()

            val newMessage = oldMessage.toBuilder().apply {
                messageBody = newMessageBody
            }.build()

            val newMsgPush = msgPush.toBuilder().apply {
                qqMessage = newMessage
            }.build()

            param.args[1] = newMsgPush.toByteArray()
        }.onFailure {
            Logger.e(it)
        }
    }
    private fun getReplacement(text: String?): String? {
        val badWords = listOf("你妈四了", "你妈死了", "密码死了", "你妈比", "你妈逼", "你码四了", "nmsl", "操你妈", "你妈了个", "操腻妈")
        return text?.trim()?.let { originalText ->
            if (badWords.any { originalText.contains(it) }) {
                var modifiedText = originalText
                modifiedText = modifiedText.replace("nmsl", "wmsl")
                modifiedText = modifiedText.replace(Regex("操.*妈"), "操我妈")
                modifiedText = modifiedText.replace(Regex("你妈"), "我妈") // Replace "你妈" with "我妈" here

                val rules = listOf(
                    Pair(Regex("[你腻妮泥密]"), "我"),
                )

                rules.forEach { (pattern, replacement) ->
                    modifiedText = modifiedText.replace(pattern, replacement)
                }

                modifiedText
            } else {
                originalText
            }
        }
    }
}