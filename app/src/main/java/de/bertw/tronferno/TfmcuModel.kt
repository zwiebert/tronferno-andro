package de.bertw.tronferno

import android.os.Handler
import java.io.IOException

val USE_JSON = true

fun agmToJson(a: Int, g: Int, m: Int): String {
    var s = ""

    if (a != 0) {
        s += String.format("\"a\"=\"%x\"", a) + ","
    }

    if (a == 0 || (a and 0xF00000) == 0x800000) {
        if (g != 0) {
            s += "\"g\":$g,"
        }
        if (m != 0) {
            s += "\"m\":$m,"
        }
    }

    return s
}


class TfmcuModel(msgHandler: Handler) {
    var tcp = McuTcp(msgHandler)
    var messagePending = 0

    @Throws(IOException::class)
    fun transmit(s: String): Boolean {
        //FIXME: ma.get()?.logWriteLine("transmit: " + s)
        return tcpSocketTransmit(s)
    }

    fun transmitSend(cmd: TfmcuSendData): Boolean {
        val s = "$cmd;"
        return transmit(s)
    }

    fun transmitTimer(timer: TfmcuTimerData, mid: Int = 0): Boolean {

        var s = timer.toString()

        if (mid != 0) {
            s += String.format(" mid=%d", if (mid == -1) getMsgId() else mid)
        }

        return transmit("$s;")
    }

    private fun tcpSocketTransmit(s: String): Boolean {


        if (!tcp.isConnected) {
            if (!tcp.isConnecting) {
                tcp.reconnect()
            }
            messagePending = 0
            return false
        }

        messagePending = msgid
        tcp.transmit(s)
        return true
    }

    private fun getMsgId(): Int {
        return ++msgid
    }

    @Throws(java.io.IOException::class)
    fun getSavedTimer(g: Int, m: Int) {
        transmit("{\"auto\":{\"g\":$g,\"m\":$m,\"f\":\"ukI\"}};")
    }

    @Throws(java.io.IOException::class)
    fun getShutterPos(g: Int = 0, m: Int = 0) {
        transmit("{\"cmd\":{\"g\":$g,\"m\":$m,\"p\":\"?\"}};")
    }

    // position code
    private var positions = TfmcuPositions()

    fun getPos(g: Int, m: Int):Int {
        return positions.getPos(g,m)
    }

    fun parseReceivedPosition(line: String): Boolean {
        return ferParseReceivedPosition(positions, line)
    }

    fun parseReceivedPositionJson(line: String): Boolean {
        return ferParseReceivedPositionJson(positions, line)
    }



    companion object {
        internal var msgid = 1
        internal const val MSG_ID_NONE = 0
        internal const val MSG_ID_AUTO = -1
    }
}