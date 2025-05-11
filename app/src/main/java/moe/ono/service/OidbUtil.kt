package moe.ono.service

import com.tencent.mobileqq.pb.ByteStringMicro
import com.tencent.mobileqq.pb.MessageMicro
import com.tencent.qphone.base.remote.FromServiceMsg
import com.tencent.qphone.base.remote.ToServiceMsg
import moe.ono.hostInfo
import moe.ono.util.QAppUtils
import mqq.observer.BusinessObserver
import tencent.im.oidb.oidb_sso.OIDBSSOPkg


object OidbUtil {

    val Boolean.int
        get() = if (this) 1 else 0
    fun makeOIDBPkg(cmdString: String, cmdFlag: Int, serviceType: Int, bodyBuffer: ByteArray): ToServiceMsg {
        return makeOIDBPkg(cmdString, cmdFlag, serviceType, bodyBuffer, 30000L)
    }

    private fun makeOIDBPkg(cmdString: String, cmdFlag: Int, serviceType: Int, bodyBuffer: ByteArray, timeout: Long): ToServiceMsg {
        return makeOIDBPkg(cmdString, cmdFlag, serviceType, bodyBuffer, timeout, null, false)
    }

    private fun makeOIDBPkg(cmdString: String, cmdFlag: Int, serviceType: Int, bodyBuffer: ByteArray, timeout: Long, businessObserver: BusinessObserver?, z: Boolean): ToServiceMsg {
        val oIDBSSOPkg = OIDBSSOPkg()
        oIDBSSOPkg.uint32_command.set(cmdFlag)
        oIDBSSOPkg.uint32_service_type.set(serviceType)
        oIDBSSOPkg.str_client_version.set("android ${hostInfo.versionName}")
        oIDBSSOPkg.bytes_bodybuffer.set(ByteStringMicro.copyFrom(bodyBuffer))
        val createToServiceMsg: ToServiceMsg = createToServiceMsg(cmdString)
        createToServiceMsg.putWupBuffer(oIDBSSOPkg.toByteArray())
        createToServiceMsg.timeout = timeout
//        addBusinessObserver(createToServiceMsg, businessObserver, z)
        return createToServiceMsg
    }

    private fun createToServiceMsg(str: String?): ToServiceMsg {
        return ToServiceMsg("mobileqq.service", QAppUtils.getCurrentUin(), str)
    }

    fun parseOIDBPkg(fromServiceMsg: FromServiceMsg, obj: Any?, messageMicro: MessageMicro<*>): Int {
        return parseOIDBPkg(fromServiceMsg, obj, OIDBSSOPkg(), messageMicro)
    }

    private fun parseOIDBPkg(fromServiceMsg: FromServiceMsg, obj: Any?, oIDBSSOPkg: OIDBSSOPkg?, messageMicro: MessageMicro<*>): Int {
        var oIDBSSOPkg = oIDBSSOPkg
        var resultCode = fromServiceMsg.resultCode
        if (resultCode == 1000) {
            if (obj != null) {
                try {
                    oIDBSSOPkg = oIDBSSOPkg!!.mergeFrom(obj as ByteArray?) as OIDBSSOPkg
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (!(oIDBSSOPkg == null || oIDBSSOPkg.uint32_result.get().also { resultCode = it } == 0)) {
                val str = oIDBSSOPkg.str_error_msg.get()
                fromServiceMsg.extraData.putString("str_error_msg", str)
            }
            try {
                messageMicro.mergeFrom(oIDBSSOPkg!!.bytes_bodybuffer.get().toByteArray())
            } catch (e2: Exception) {
            }
        }
        return resultCode
    }


}