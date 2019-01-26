package bertw.tronferno

import java.io.IOException
import java.lang.ref.WeakReference
import java.util.regex.Pattern


class TfmcuTimerData {
    var g = 0
    var m = 0
    var sunAuto = false
    var random = false
    var daily = ""
    var weekly = ""
    var hasAstro = false
    var astro = 0
    var dailyUp = ""
    var dailyDown = ""
    var rtcOnly = false

    override fun toString() : String {
        var timer = ""

        if (rtcOnly) {
            return "rtc-only=1"
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


class TfmcuModel(activity: MainActivity) {

    var ma = WeakReference(activity)
    var tcp = McuTcp(activity.mMessageHandler)
    var messagePending = 0

    @Throws(IOException::class)
    fun transmit(s: String): Boolean {
        ma.get()?.logWriteLine("transmit: " + s)
        return tcpSocketTransmit(s)
    }


    fun transmitSend(c: String, mid: Int = 0, a: Int = 0, g: Int = 0, m: Int = 0, sep: Boolean = false): Boolean {
        val s = makeSendString(c, mid, a, g, m, sep)
        return transmit(s)
    }

    fun transmitTimer(timer: String, mid: Int = 0, a: Int = 0, g: Int = 0, m: Int = 0): Boolean {
        val s = makeTimerString(timer, mid, a, g, m)
        return transmit(s)
    }

    fun transmitTimer(timer: TfmcuTimerData, mid: Int = 0, a: Int = 0, g: Int = 0, m: Int = 0): Boolean {
        val s = makeTimerString(timer.toString(), mid, a, g, m)
        return transmit(s)
    }

    private fun makeSendString(c: String, mid: Int = 0, a: Int = 0, g: Int = 0, m: Int = 0, sep: Boolean = false): String {
        var s = "send c=" + c;

        if (mid != 0) {
            s += String.format(" mid=%d", mid)
        }
        if (a != 0) {
            s += String.format(" a=%x", a)

        }
        if (a == 0 || (a and 0xF00000) == 0x800000) {
            if (g != 0) {
                s += String.format(" g=%d", g)

                if (m != 0) {
                    s += String.format(" m=%d", m)
                }
            }
        }

        if (sep != false) {
            s += " SEP"
        }

        return s + ";"
    }

    private fun tcpSocketTransmit(s: String): Boolean {

        if (messagePending != 0 || !tcp.isConnected) {
            if (!tcp.isConnecting) {
                tcp.reconnect()
                ma.get()?.logWriteLine("tcp: try to reconnect...")
            }
            messagePending = 0
            return false
        }

        messagePending = MainActivity.msgid
        tcp.transmit(s)
        return true
    }

    private fun makeTimerString(timer: String, mid: Int = 0, a: Int = 0, g: Int = 0, m: Int = 0): String {
        var s = "timer";
        var mid = mid

        if (mid == -1) {
            mid = getMsgId();
        }

        if (mid != 0) {
            s += String.format(" mid=%d", mid)
        }

        if (a != 0) {
            s += String.format(" a=%x", a)

        }
        if (a == 0 || (a and 0xF00000) == 0x800000) {
            if (g != 0) {
                s += String.format(" g=%d", g)
                if (m != 0) {
                    s += String.format(" m=%d", m)
                }
            }

        }

        return s + " " + timer + ";"
    }

    fun getMsgId(): Int {
        return ++MainActivity.msgid
    }

    @Throws(java.io.IOException::class)
    fun getSavedTimer(g: Int, m: Int) {
        transmit(makeTimerString(timer = " rs=2", mid = getMsgId(), g = g, m = m) + "mcu cs=?;" + makeSendString(g = g, m = m, c = "?"))
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

    fun showPos(group: Int): String {
        var posText = ""
        for (i in 1..7) {
            if ((posArr0[group] and (1 shl i)) != 0) {
                posText += "c"
            } else if ((posArr50[group] and (1 shl i)) != 0) {
                posText += "m"
            } else if ((posArr100[group] and (1 shl i)) != 0) {
                posText += "o"
            } else {
                posText += "?"
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




            if (!td.daily.startsWith("-")) {
                td.dailyUp = td.daily.substring(0, 2) + ":" + td.daily.substring(2, 4)
                td.daily = td.daily.substring(4)
            } else {
                td.daily = td.daily.substring(1)
            }

            if (!td.daily.startsWith("-")) {
                td.dailyDown = td.daily.substring(0, 2) + ":" + td.daily.substring(2, 4)
            }


        } catch (e: Exception) {
        }
        return td;
    }
}