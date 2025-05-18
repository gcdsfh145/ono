package moe.ono.hooks.item.chat

import android.annotation.SuppressLint
import com.google.protobuf.ByteString
import com.google.protobuf.UnknownFieldSet
import com.tencent.mobileqq.fe.FEKit
import de.robv.android.xposed.XC_MethodHook
import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.readBytes
import kotlinx.io.core.writeFully
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import moe.ono.config.ConfigManager
import moe.ono.ext.getUnknownObject
import moe.ono.ext.getUnknownObjects
import moe.ono.ext.toInnerValuesString
import moe.ono.hooks._base.BaseClickableFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.hooks.base.api.QQHookCodec
import moe.ono.hooks.protocol.entries.QQSsoSecureInfo
import moe.ono.hooks.protocol.entries.TextMsgExtPbResvAttr
import moe.ono.loader.hookapi.IHijacker
import moe.ono.util.AesUtils.aesEncrypt
import moe.ono.util.AesUtils.md5
import moe.ono.util.Logger
import moe.ono.util.SyncUtils

@SuppressLint("DiscouragedApi")
@HookItem(path = "聊天与消息/群聊加密消息", description = "发送的消息将加密抄送，仅针对群聊\n* 禁止用于非法用途，违者后果自负")
class MessageEncryptor : BaseClickableFunctionHookItem() {
    override fun entry(classLoader: ClassLoader) {
        QQHookCodec.hijackers.add(object: IHijacker {
            override fun onHandle(
                param: XC_MethodHook.MethodHookParam,
                uin: String,
                cmd: String,
                seq: Int,
                buffer: ByteArray,
                bufferIndex: Int
            ): Boolean {
                this@MessageEncryptor.onHandle(param, uin, cmd, seq, buffer, bufferIndex)
                return false
            }
            override val command: String = "MessageSvc.PbSendMsg"
        })
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun onHandle(param: XC_MethodHook.MethodHookParam, uin: String, cmd: String, seq: Int, buffer: ByteArray, bufferIndex: Int) {
        if (!ConfigManager.isEnable(this.javaClass)) return
        if (buffer.size <= 4) return
        val unknownFields = UnknownFieldSet.parseFrom(buffer.copyOfRange(4, buffer.size))
        if (!unknownFields.hasField(1)) return
        val routingHead  = unknownFields.getUnknownObject(1)
        if (routingHead.hasField(1)) return // 私聊消息不加密
        val msgBody = unknownFields.getUnknownObject(3)

        val builder = UnknownFieldSet.newBuilder(unknownFields)
        builder.clearField(3) // 清除原消息体

        val newMsgBody = generateEncryptedMsgBody(msgBody)
        builder.addField(3, UnknownFieldSet.Field.newBuilder().also {
            it.addLengthDelimited(newMsgBody.toByteString())
        }.build())

        val data = builder.build().toByteArray()

        if (bufferIndex == 15 && param.args[13] != null) {
            // 因为包体改变，重新签名
            val qqSecurityHead = UnknownFieldSet.parseFrom(param.args[13] as ByteArray)
            val qqSecurityHeadBuilder = UnknownFieldSet.newBuilder(qqSecurityHead)
            qqSecurityHeadBuilder.clearField(24)
            val sign = FEKit.getInstance().getSign(cmd, data, seq, uin)
            Logger.d("sign ->" + sign.toInnerValuesString())
            qqSecurityHeadBuilder.addField(24, UnknownFieldSet.Field.newBuilder().also {
                it.addLengthDelimited(
                    ByteString.copyFrom(
                        ProtoBuf.encodeToByteArray(QQSsoSecureInfo(
                    secSig = sign.sign,
                    extra = sign.extra,
                    deviceToken = sign.token
                ))))
            }.build())
            param.args[13] = qqSecurityHeadBuilder.build().toByteArray()
        }

        if (bufferIndex == 15 && param.args[14] != null) {
            val qqSecurityHead = UnknownFieldSet.parseFrom(param.args[14] as ByteArray)
            val qqSecurityHeadBuilder = UnknownFieldSet.newBuilder(qqSecurityHead)
            qqSecurityHeadBuilder.clearField(24)
            val sign = FEKit.getInstance().getSign(cmd, data, seq, uin)
            qqSecurityHeadBuilder.addField(24, UnknownFieldSet.Field.newBuilder().also {
                it.addLengthDelimited(
                    ByteString.copyFrom(
                        ProtoBuf.encodeToByteArray(QQSsoSecureInfo(
                    secSig = sign.sign,
                    extra = sign.extra,
                    deviceToken = sign.token
                ))))
            }.build())
            param.args[14] = qqSecurityHeadBuilder.build().toByteArray()
        }

        param.args[bufferIndex] = BytePacketBuilder().also {
            it.writeInt(data.size + 4)
            it.writeFully(data)
        }.build().readBytes()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun generateEncryptedMsgBody(msgBody: UnknownFieldSet): UnknownFieldSet {
        val encryptKey = ConfigManager.getStringConfig(this.javaClass, "ono")
        if (encryptKey.isBlank()) {
            // 未设置加密密钥
            return msgBody
        }

        val elements = UnknownFieldSet.Field.newBuilder()
        msgBody.getUnknownObject(1).let { richText ->
            richText.getUnknownObjects(2).forEach { element ->
                if (element.hasField(37) || element.hasField(9)) {
                    elements.addLengthDelimited(element.toByteString()) // 通用字段，不自己合成
                }
            }
        }

        val newMsgBody = UnknownFieldSet.newBuilder()
        val richText = UnknownFieldSet.newBuilder()

        elements.addLengthDelimited(UnknownFieldSet.newBuilder().also { builder ->
            builder.addField(1, UnknownFieldSet.Field.newBuilder().also { it ->
                it.addLengthDelimited(UnknownFieldSet.newBuilder().also { textElement ->
                    textElement.addField(1, UnknownFieldSet.Field.newBuilder().also { content ->
                        content.addLengthDelimited(ByteString.copyFromUtf8("[このメッセージは暗号化されています。🔒]\nKono messeeji wa angōka sareteimasu."))
                    }.build())

                    textElement.addField(12, UnknownFieldSet.Field.newBuilder().also { content ->
                        content.addLengthDelimited(
                            ByteString.copyFrom(
                                ProtoBuf.encodeToByteArray(TextMsgExtPbResvAttr(
                            wording = BytePacketBuilder().also {
                                it.writeInt(0x114514)
                                it.writeInt(encryptKey.hashCode())
                                it.writeFully(aesEncrypt(msgBody.toByteArray(), md5(encryptKey)))
                            }.build().readBytes()
                        ))))
                    }.build())
                }.build().toByteString())
            }.build())
        }.build().toByteString()) // add text

        richText.addField(2, elements.build())

        newMsgBody.addField(1, UnknownFieldSet.Field.newBuilder().also {
            it.addLengthDelimited(richText.build().toByteString())
        }.build())

        return newMsgBody.build()
    }



    override fun targetProcess(): Int {
        return SyncUtils.PROC_MSF
    }
}

