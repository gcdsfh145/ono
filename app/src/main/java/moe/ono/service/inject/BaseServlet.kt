package moe.ono.service.inject

import android.content.Intent
import com.tencent.qphone.base.remote.FromServiceMsg
import com.tencent.qphone.base.remote.ToServiceMsg
import moe.ono.util.Logger
import moe.ono.util.toMap
import mqq.app.MSFServlet
import mqq.app.Packet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseServlet: MSFServlet() {

    protected val seqReceiveMap = ConcurrentHashMap<Int, FromServiceMsg>()
    protected val seqFactory = AtomicInteger(0)

    fun generateSeq(): Int {
        return seqFactory.addAndGet(1)
    }

    fun getReceiveAndRemove(seq: Int): FromServiceMsg? {
        if (seqReceiveMap.containsKey(seq)) {
            val fromServiceMsg = seqReceiveMap[seq]
            seqReceiveMap.remove(seq)
            return fromServiceMsg
        }
        return null
    }

    override fun onReceive(intent: Intent, fromServiceMsg: FromServiceMsg) {
        val toServiceMsg: ToServiceMsg =
            intent.getParcelableExtra(ToServiceMsg::class.java.simpleName)!!
        fromServiceMsg.attributes[FromServiceMsg::class.java.simpleName] = toServiceMsg
        Logger.d(toServiceMsg.toString())
        Logger.d(toServiceMsg.extraData.toMap().toString())
        Logger.d("${this::class.java.simpleName} -> onReceive: $fromServiceMsg")
        Logger.d(fromServiceMsg.extraData.toMap().toString())
        seqReceiveMap[toServiceMsg.appSeq] = fromServiceMsg
    }

    override fun onSend(intent: Intent, packet: Packet) {
        val toServiceMsg: ToServiceMsg? =
            intent.getParcelableExtra(ToServiceMsg::class.java.simpleName)
        toServiceMsg?.let {
            packet.setSSOCommand(toServiceMsg.serviceCmd)
            packet.putSendData(toServiceMsg.wupBuffer)
            packet.setTimeout(toServiceMsg.timeout)
            @Suppress("UNCHECKED_CAST")
            packet.attributes = toServiceMsg.attributes as HashMap<String, Any>?
            if (!toServiceMsg.isNeedCallback) {
                packet.setNoResponse()
            }
        }
    }
}