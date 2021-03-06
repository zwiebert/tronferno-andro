package de.bertw.tronferno

import android.os.Handler
import java.io.IOException
import java.util.regex.Pattern

class TfmcuSendData(var a: Int = 0, var g: Int = 0, var m: Int = 0, var cmd: String = "", var sep: Boolean = false) {

    override fun toString(): String {
        var s = "send"

        if (a != 0) {
            s += String.format(" a=%x", a)
        }

        if (a == 0 || (a and 0xF00000) == 0x800000) {
            if (g != 0) {
                s += " g=$g"
            }
            if (m != 0) {
                s += " m=$m"
            }
        }

        if (cmd.isNotEmpty()) {
            s += " c=$cmd"
        }

        if (sep && (cmd == CMD_UP || cmd == CMD_DOWN)) {
            s += " SEP"
        }

        return s
    }

    companion object {
        const val CMD_UP = "up"
        const val CMD_DOWN = "down"
        const val CMD_STOP = "stop"
        const val CMD_SET = "set"
        const val CMD_SUN_DOWN = "sun-down"

    }
}

class TfmcuTimerData(var a: Int = 0, var g: Int = 0, var m: Int = 0) {
    var sunAuto = false
    var random = false
    var daily = ""
    var weekly = ""
    var hasAstro = false
    var astro = 0
    var rtcOnly = false
    var rs = 0
    var manual = false

    override fun toString(): String {
        var timer = "timer"

        if (a != 0) {
            timer += " a=" + String.format(" a=%x", a)
        }

        if (a == 0 || (a and 0xF00000) == 0x800000) {
            if (g != 0) {
                timer += " g=$g"
            }
            if (m != 0) {
                timer += " m=$m"
            }
        }

        if (rtcOnly) {
            timer += " rtc-only=1"
            return timer
        }

        var flag = "f=i"

        flag += if (manual) "M" else "m"
        flag += if (sunAuto) "S" else "s"
        flag += if (random) "R" else "r"

        timer += " $flag"

        if (hasAstro) {
            timer += " astro=$astro"
        }

        if (daily.isNotEmpty()) {
            timer += " daily=$daily"
        }

        if (weekly.isNotEmpty()) {
            timer += " weekly=$weekly"
        }

        if (sunAuto) {
            timer += " sun-auto=1"
        }

        if (random) {
            timer += " random=1"
        }

        return timer
    }
}

class TfmcuConfigData(private var s: String = "") {

    override fun toString(): String {

        return "config $s;"
    }
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
        transmit("timer g=$g m=$m f=ukI;")
    }

    @Throws(java.io.IOException::class)
    fun getShutterPos(g: Int = 0, m: Int = 0) {
        transmit("mcu cs=?;send g=$g m=$m c=?;") // FIXME: remove experimental syntax "mcu cs=?;" here later
    }

    // position code
    private var posArr = arrayOf<Array<Int>>();
    private var posDataValid = true

    init {
        for (i in 0..7) {
            posArr += arrayOf<Int>(0,0,0,0,0,0,0,0);
        }
    }

    private fun clearPosArr() {
        for (i in 0..7) {
            for (k in 0..7) {
                posArr[i][k] = 0;
            }
        }
    }

    fun getPos(g: Int, m: Int):Int {
        return posArr[g][m];
    }

    private fun updPosArr(g: Int, m: Int, p: Int) {
        if (g == 0) {
            for (gi in 1..7)
                updPosArr(gi, m, p)
            return
        }
        posArr[g][m] = p;
    }

    fun parseReceivedPosition(line: String): Boolean {
        var posChanged = false

        if (line.startsWith("U:position:start;")) {
            clearPosArr()
        } else if (line.startsWith("U:position:end;")) {
            posDataValid = true
            posChanged = true
        } else if (line.startsWith("A:position: ") || line.startsWith("U:position: ")) {
            var g = -1
            var m = -1
            var p = -1
            var mm = ""

            var s = line.substringAfter(":position: ")
            while (s.contains('=')) {
                val k = s.substringBefore('=')
                val v = s.substringAfter('=').substringBefore(';').substringBefore(' ')
                s = s.substringAfter(' ', ";")

                when (k) {
                    "g" -> g = v.toInt()
                    "m" -> m = v.toInt()
                    "p" -> p = v.toInt()
                    "mm" -> mm = v
                }
            }

            if (!mm.isEmpty() && p != -1) {
                val arr = mm.split(',')
                if (arr.size == 8) {
                    for (i in 0..7) {
                        var temp = arr[i].toInt(radix = 16)
                        for (k in 0..7) {
                            if (1 == (temp and 1)) {
                                posArr[i][k] = p;
                            }
                            temp = temp shr 1;
                        }
                    }
                    posDataValid = true
                    posChanged = true

                }
            }

            if (g != -1 && m != -1 && p != -1) {
                updPosArr(g, m, p)
                posDataValid = true
                posChanged = true
            }

        }
        return posChanged
    }

    fun parseReceivedTimer(timer: String): TfmcuTimerData {
        var s = timer
        val td = TfmcuTimerData()
        try {
            val p = Pattern.compile("\\s+")
            val arr = p.split(s)


            for (i in arr) {
                if (i.contains("=")) {
                    val idxES = i.indexOf('=')
                    val key = i.substring(0, idxES)
                    val value = i.substring(idxES + 1)

                    when (key) {
                        "f" -> {
                            td.sunAuto = value.contains("S")
                            td.random = value.contains("R")
                            td.manual = value.contains("M")
                            td.hasAstro = value.contains("A")
                        }
                        "g" -> td.g = value.toInt()
                        "m" -> td.m = value.toInt()
                        "astro" -> td.astro = value.toInt()
                        "daily" -> td.daily = value
                        "weekly" -> td.weekly = value
                    }
                }
            }

        } catch (e: Exception) {
        }
        return td
    }


    companion object {
        internal var msgid = 1
        internal const val MSG_ID_NONE = 0
        internal const val MSG_ID_AUTO = -1
    }
}