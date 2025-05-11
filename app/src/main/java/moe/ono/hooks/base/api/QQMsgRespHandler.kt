package moe.ono.hooks.base.api

import com.tencent.qphone.base.remote.FromServiceMsg
import com.tencent.qphone.base.remote.ToServiceMsg
import moe.ono.R
import moe.ono.config.CacheConfig.setRKeyGroup
import moe.ono.config.CacheConfig.setRKeyPrivate
import moe.ono.creator.PacketHelperDialog
import moe.ono.creator.QQMessageFetcherResultDialog
import moe.ono.hooks._base.ApiHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.reflex.XField
import moe.ono.reflex.XMethod
import moe.ono.service.QQInterfaces
import moe.ono.ui.CommonContextWrapper
import moe.ono.util.ContextUtils
import moe.ono.util.FunProtoData
import moe.ono.util.FunProtoData.getUnpPackage
import moe.ono.util.Logger
import moe.ono.util.SyncUtils
import moe.ono.util.Utils
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Arrays
import java.util.UUID
import java.util.zip.Deflater

@HookItem(path = "API/QQMsgRespHandler")
class QQMsgRespHandler : ApiHookItem() {
    private fun update() {
        hookBefore(XMethod.clz("mqq.app.msghandle.MsgRespHandler").name("dispatchRespMsg").ignoreParam().get()) { param ->
            val serviceMsg: ToServiceMsg = XField.obj(param.args[1]).name("toServiceMsg").get()
            val fromServiceMsg: FromServiceMsg =
                XField.obj(param.args[1]).name("fromServiceMsg").get()

            val data = FunProtoData()
            data.fromBytes(
                getUnpPackage(
                    fromServiceMsg.wupBuffer
                )
            )

            val obj: JSONObject = data.toJSON()
            when (fromServiceMsg.serviceCmd) {
                "OidbSvcTrpcTcp.0x9067_202" -> {
                    Logger.d("on OidbSvcTrpcTcp.0x9067_202")
                    QQInterfaces.update()

                    val rkeyGroup =
                        obj.getJSONObject("4")
                            .getJSONObject("4")
                            .getJSONArray("1")
                            .getJSONObject(0).getString("1")

                    val rkeyPrivate =
                        obj.getJSONObject("4")
                            .getJSONObject("4")
                            .getJSONArray("1")
                            .getJSONObject(1).getString("1")

                    setRKeyGroup(rkeyGroup)
                    setRKeyPrivate(rkeyPrivate)
                }
                "OidbSvcTrpcTcp.0xfe1_2" -> {
                    Logger.d("on OidbSvcTrpcTcp.0xfe1_2")
                    Logger.i(obj.toString())
                }
                "MessageSvc.PbGetGroupMsg" -> {
                    Logger.d("on MessageSvc.PbGetGroupMsg")

                    Logger.d("obj: " + obj.toString(4))
                    SyncUtils.runOnUiThread { QQMessageFetcherResultDialog.createView(
                        CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()), obj) }

                }
                "MessageSvc.PbGetOneDayRoamMsg" -> {
                    Logger.d("on MessageSvc.PbGetOneDayRoamMsg")

                    Logger.d("obj: " + obj.toString(4))
                    SyncUtils.runOnUiThread { QQMessageFetcherResultDialog.createView(
                        CommonContextWrapper.createAppCompatContext(ContextUtils.getCurrentActivity()), obj) }
                }

                "trpc.group.long_msg_interface.MsgService.SsoSendLongMsg" -> {
                    Logger.d("on trpc.group.long_msg_interface.MsgService.SsoSendLongMsg")
                    Logger.d("obj: " + obj.toString(4))

                    val resid = obj.getJSONObject("2").getString("3")

                    Logger.d("resid", resid)
                    try {
                        if (PacketHelperDialog.mRgSendBy.checkedRadioButtonId == R.id.rb_send_by_longmsg){
                            val content = "{\n" +
                                    "    \"37\": {\n" +
                                    "        \"6\": 1,\n" +
                                    "        \"7\": \"$resid\",\n" +
                                    "        \"17\": 0,\n" +
                                    "        \"19\": {\n" +
                                    "            \"15\": 0,\n" +
                                    "            \"31\": 0,\n" +
                                    "            \"41\": 0\n" +
                                    "        }\n" +
                                    "    }\n" +
                                    "}"

                            PacketHelperDialog.setContent(content)
                        } else if (PacketHelperDialog.mRgSendBy.checkedRadioButtonId == R.id.rb_send_by_forwarding) {
                            if (!PacketHelperDialog.mRbXmlForward.isChecked) {
                                val json = "{\n" +
                                        "  \"app\": \"com.tencent.multimsg\",\n" +
                                        "  \"config\": {\n" +
                                        "    \"autosize\": 1,\n" +
                                        "    \"forward\": 1,\n" +
                                        "    \"round\": 1,\n" +
                                        "    \"type\": \"normal\",\n" +
                                        "    \"width\": 300\n" +
                                        "  },\n" +
                                        "  \"desc\": \"${PacketHelperDialog.etHint.text}\",\n" +
                                        "  \"extra\": \"{\\\"filename\\\":\\\"${UUID.randomUUID()}\\\",\\\"tsum\\\":1}\\n\",\n" +
                                        "  \"meta\": {\n" +
                                        "    \"detail\": {\n" +
                                        "      \"news\": [\n" +
                                        "        {\n" +
                                        "          \"text\": \"${PacketHelperDialog.etDesc.text}\"\n" +
                                        "        }\n" +
                                        "      ],\n" +
                                        "      \"resid\": \"$resid\",\n" +
                                        "      \"source\": \"聊天记录\",\n" +
                                        "      \"summary\": \"PacketHelper@ouom_pub\",\n" +
                                        "      \"uniseq\": \"${UUID.randomUUID()}\"\n" +
                                        "    }\n" +
                                        "  },\n" +
                                        "  \"prompt\": \"${PacketHelperDialog.etHint.text}\",\n" +
                                        "  \"ver\": \"0.0.0.5\",\n" +
                                        "  \"view\": \"contact\"\n" +
                                        "}"

                                Logger.d(json)
                                val content = "{\n" +
                                        "    \"51\": {\n" +
                                        "        \"1\": \"hex->${Utils.bytesToHex(compressData(json))}\"\n" +
                                        "    }\n" +
                                        "}"

                                PacketHelperDialog.setContent(content)
                            } else {
                                val xml = """<?xml version="1.0" encoding="utf-8"?><msg brief="${PacketHelperDialog.etDesc.text}" m_fileName="${UUID.randomUUID()}" action="viewMultiMsg" tSum="1" flag="3" m_resid="$resid" serviceID="35" m_fileSize="0"><item layout="1"><title color="#000000" size="34">聊天记录</title><title color="#777777" size="26">${PacketHelperDialog.etDesc.text}</title><hr></hr><summary color="#808080" size="26">PacketHelper@ouom_pub</summary></item><source name="@ouom_pub"></source></msg>"""
                                Logger.d("xml", xml)

                                val json = """{
    "12": {
        "1": "hex->${Utils.bytesToHex(compressData(xml))}",
        "2": 60
    }
}""".trim()

                                Logger.d(json)

                                PacketHelperDialog.setContentForLongmsg(json)
                            }

                        }

                    } catch (e: Exception) {
                        Logger.e("QQMsgRespHandler", e)
                    }


                }
            }
        }

    }

    private fun compressData(data: String): ByteArray {
        val inputBytes = data.toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, false)
        deflater.setInput(inputBytes)
        deflater.finish()

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()
        val compressedBytes = outputStream.toByteArray()
        val result = ByteArray(compressedBytes.size + 1)
        result[0] = 0x01
        System.arraycopy(compressedBytes, 0, result, 1, compressedBytes.size)

        return result
    }




    @Throws(Throwable::class)
    override fun load(classLoader: ClassLoader) {
        update()
    }
}