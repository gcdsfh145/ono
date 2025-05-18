package moe.ono.hooks.item.developer

import android.annotation.SuppressLint
import de.robv.android.xposed.XposedHelpers
import moe.ono.ext.toHex
import moe.ono.hooks._base.BaseSwitchFunctionHookItem
import moe.ono.hooks._core.annotation.HookItem
import moe.ono.service.PlatformUtils.getQUA
import moe.ono.service.http.HttpServer
import moe.ono.util.Initiator.loadClass
import moe.ono.util.Logger
import moe.ono.util.SyncUtils
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap


@SuppressLint("DiscouragedApi")
@HookItem(path = "开发者选项/???", description = "没事别开")
class QSignHook : BaseSwitchFunctionHookItem() {
    companion object {
        val lastResult: ConcurrentHashMap<String, String> = ConcurrentHashMap()

        data class SignTriple(val extra: ByteArray, val sign: ByteArray, val token: ByteArray)

        fun callGetSign(
            cmd: String,
            buffer: ByteArray,
            seq: ByteArray,
            uin: String,
        ): SignTriple {
            return runCatching {
                val signClass = loadClass("com.tencent.mobileqq.sign.QQSecuritySign")
                val qsecClass = loadClass("com.tencent.mobileqq.qsec.qsecurity.QSec")

                val qsecObj = XposedHelpers.newInstance(qsecClass)
                val instance = XposedHelpers.callStaticMethod(signClass, "getInstance")

                val qua = getQUA()
                Logger.d("callGetSign: $cmd, $buffer, $seq, $uin, $qua")
                val resultObj = XposedHelpers.callMethod(
                    instance, "getSign",
                    qsecObj, qua, cmd, buffer, seq, uin
                )

                SignTriple(
                    XposedHelpers.getObjectField(resultObj, "extra") as ByteArray,
                    XposedHelpers.getObjectField(resultObj, "sign") as ByteArray,
                    XposedHelpers.getObjectField(resultObj, "token") as ByteArray
                )
            }.getOrElse {
                Logger.e("callGetSign ERROR", it)
                throw it
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun load(classLoader: ClassLoader) {
        HttpServer.doStart()

        val signClass = loadClass("com.tencent.mobileqq.sign.QQSecuritySign")
        val qsecClass = loadClass("com.tencent.mobileqq.qsec.qsecurity.QSec")

        val m = XposedHelpers.findMethodExact(
            signClass,
            "getSign",
            qsecClass,
            String::class.java,
            String::class.java,
            ByteArray::class.java,
            ByteArray::class.java,
            Long::class.javaPrimitiveType,
        )
        hookAfter(m) { param ->
            val result = param.result ?: return@hookAfter
            val extra  = XposedHelpers.getObjectField(result, "extra")  as ByteArray
            val sign   = XposedHelpers.getObjectField(result, "sign")   as ByteArray
            val token  = XposedHelpers.getObjectField(result, "token")  as ByteArray

            val json = JSONObject().apply {
                put("extra", extra.toHex())
                put("sign",  sign.toHex())
                put("token", token.toHex())
                put("timestamp", System.currentTimeMillis())
            }
            lastResult["latest"] = json.toString()
        }
    }

    override fun targetProcess(): Int {
        return SyncUtils.PROC_MSF
    }
}