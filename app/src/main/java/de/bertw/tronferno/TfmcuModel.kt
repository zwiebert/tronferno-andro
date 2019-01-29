package de.bertw.tronferno

import android.os.Handler
import java.io.IOException
import java.util.regex.Pattern

class TfmcuSendData(a: Int = 0, g: Int = 0, m: Int = 0, cmd: String = "", sep: Boolean = false) {
    var a = a
    var g = g
    var m = m
    var cmd = cmd
    var sep = sep

    override fun toString(): String {
        var s = "send"

        if (a != 0) {
            s += String.format(" a=%x", a)
        }

        if (a == 0 || (a and 0xF00000) == 0x800000) {
            if (g != 0) {
                s += " g=" + g.toString()
            }
            if (m != 0) {
                s += " m=" + m.toString()
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

class TfmcuTimerData(a: Int = 0, g: Int = 0, m: Int = 0) {
    var a = a
    var g = g
    var m = m
    var sunAuto = false
    var random = false
    var daily = ""
    var weekly = ""
    var hasAstro = false
    var astro = 0
     var rtcOnly = false
    var rs = 0;

    override fun toString(): String {
        var timer = "timer"

        if (a != 0) {
            timer += " a=" + String.format(" a=%x", a)
        }

        if (a == 0 || (a and 0xF00000) == 0x800000) {
            if (g != 0) {
                timer += " g=" + g.toString()
            }
            if (m != 0) {
                timer += " m=" + m.toString()
            }
        }

        if (rtcOnly) {
            timer += " rtc-only=1"
            return timer
        }

        if (hasAstro) {
            timer += " astro=" + astro.toString()
        }

        if (daily.isNotEmpty()) {
            timer += " daily=" + daily
        }

        if (weekly.isNotEmpty()) {
            timer += " weekly=" + weekly
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

class TfmcuConfigData(s : String = "") {
    var s = s;

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
        val s = cmd.toString() + ";"
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

        if (messagePending != 0 || !tcp.isConnected) {
            if (!tcp.isConnecting) {
                tcp.reconnect()
                //FIXME: ma.get()?.logWriteLine("tcp: try to reconnect...")
            }
            messagePending = 0
            return false
        }

        messagePending = msgid
        tcp.transmit(s)
        return true
    }

    fun getMsgId(): Int {
        return ++msgid
    }

    @Throws(java.io.IOException::class)
    fun getSavedTimer(g: Int, m: Int) {
        transmit("timer g=$g m=$m rs=2;mcu cs=?;send g=$g m=$m c=?;")
    }


    // position code
    private var posArr0 = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    private var posArr50 = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    private var posArr100 = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    private var posDataValid = true;

    private fun clearPosArr() {
        for (i in 0..7) {
            posArr0[i] = 0
            posArr50[i] = 0
            posArr100[i] = 0
        }
    }

    private fun updPosArr(g: Int, m: Int, p: Int) {
        posArr0[g] = if (p == 0) (posArr0[g] or (1 shl m)) else (posArr0[g] and (1 shl m).inv())
        posArr50[g] = if (p == 50) (posArr50[g] or (1 shl m)) else (posArr50[g] and (1 shl m).inv())
        posArr100[g] = if (p == 100) (posArr100[g] or (1 shl m)) else (posArr100[g] and (1 shl m).inv())
    }

    fun showPos(group: Int, memb_count: Int = 7, format: Int = 0): String {
        var posText = ""
        var posText0 = ""
        var posText50 = ""
        var posText100 = ""

        for (i in 1..memb_count) {
            if ((posArr0[group] and (1 shl i)) != 0) {
                posText += "c"
                posText0 += if (posText0.isEmpty()) "$i" else ",$i"
            } else if ((posArr50[group] and (1 shl i)) != 0) {
                posText += "m"
                posText50 += if (posText50.isEmpty()) "$i" else ",$i"
            } else if ((posArr100[group] and (1 shl i)) != 0) {
                posText += "o"
                posText100 += if (posText100.isEmpty()) "$i" else ",$i"
            } else {
                posText += "?"
            }
        }

        if (format == 1) {
            posText = ""
            if (posText0.isNotEmpty()) {
                posText += "0%=$posText0"
            }

            if (posText50.isNotEmpty()) {
                if (posText.isNotEmpty()) {
                    posText += " | "
                }
                posText += "50%=$posText50"
            }

            if (posText100.isNotEmpty()) {
                if (posText.isNotEmpty()) {
                    posText += " | "
                }
                posText += "100%=$posText100"
            }

        }
        return posText
    }

    fun parseReceivedPosition(line: String): Boolean {
        var posChanged = false;

        if (line.startsWith("U:position:start;")) {
            clearPosArr()
        } else if (line.startsWith("U:position:end;")) {
            posDataValid = true
            posChanged = true;
        } else if (line.startsWith("A:position: ") || line.startsWith("U:position: ")) {
            var g = -1;
            var m = -1;
            var p = -1;
            var mm = "";

            var s = line.substringAfter(":position: ")
            while (s.contains('=')) {
                val k = s.substringBefore('=')
                val v = s.substringAfter('=').substringBefore(';').substringBefore(' ')
                s = s.substringAfter(' ', ";")

                when (k) {
                    "g" -> g = v.toInt()
                    "m" -> m = v.toInt()
                    "p" -> p = v.toInt()
                    "mm" -> mm = v;
                }
            }

            if (!mm.isEmpty() && p != -1) {
                var arr = mm.split(',');
                if (arr.size == 8) {
                    for (i in 1..7) {
                        var temp = arr[i].toInt(radix = 16)
                        when (p) {
                            0 -> posArr0[i] = temp
                            50 -> posArr50[i] = temp
                            100 -> posArr100[i] = temp
                        }

                    }
                    posDataValid = true;
                    posChanged = true;

                }
            }

            if (g != -1 && m != -1 && p != -1) {
                updPosArr(g, m, p)
                posDataValid = true;
                posChanged = true;
            }

        }
        return posChanged
    }

    fun parseReceivedTimer(s: String): TfmcuTimerData {
        var s = s
        var td = TfmcuTimerData()
        try {
            s = s.substring(s.indexOf(":rs=data: "))

            //     tvRec.append(String.format("###%s###\n", s));


            if (s.startsWith(":rs=data: none")) {

            } else if (s.startsWith(":rs=data: timer ")) {
                val scIdx = s.indexOf(';')


                if (scIdx > 16) {
                    s = s.substring(16, scIdx)
                } else {
                    s = s.substring(16)
                }

                val p = Pattern.compile("\\s+")
                val arr = p.split(s)


                for (i in arr) {
                    if (i.contains("=")) {
                        val idxES = i.indexOf('=')
                        val key = i.substring(0, idxES)
                        val value = i.substring(idxES + 1)

                        when (key) {
                            "g" -> td.g = value.toInt()
                            "m" -> td.m = value.toInt()
                            "sun-auto" -> td.sunAuto = value != "0"
                            "random" -> td.random = value != "0"
                            "astro" -> {
                                td.hasAstro = true
                                td.astro = value.toInt()
                            }
                            "daily" -> td.daily = value
                            "weekly" -> td.weekly = value

                        }

                    } else {
                        when (i) {
                            "sun-auto" -> td.sunAuto
                            "random" -> td.random
                            "astro" -> {
                                td.hasAstro = true; td.astro = 0
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        return td;
    }


    companion object {
        internal var msgid = 1
        internal const val MSG_ID_NONE = 0
        internal const val MSG_ID_AUTO = -1
    }
}