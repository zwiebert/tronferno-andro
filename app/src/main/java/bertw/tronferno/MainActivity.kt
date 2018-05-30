package bertw.tronferno

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

const val DEFAULT_TCP_HOSTNAME = "fernotron.fritz.box."
const val DEFAULT_TCP_PORT = 7777


// Integer.min/max not available for SDK < 24
fun <T : Comparable<T>> min(a: T, b: T): T {
    return if (a < b) a else b
}

fun <T : Comparable<T>> max(a: T, b: T): T {
    return if (a > b) a else b
}

class MainActivity : AppCompatActivity() {

    private val useWifi = true
    private val mMessageHandler = MessageHandler(this)

    private lateinit var vcbDailyUp: CheckBox
    private lateinit var vcbDailyDown: CheckBox
    private lateinit var vcbWeekly: CheckBox
    private lateinit var vcbAstro: CheckBox
    private lateinit var vcbRandom: CheckBox
    private lateinit var vcbSunAuto: CheckBox
    private lateinit var vcbRtcOnly: CheckBox
    private lateinit var vcbFerId: CheckBox

    private lateinit var vtvLog: TextView
    private lateinit var vtvG: TextView
    private lateinit var vtvE: TextView

    private lateinit var vetDailyUpTime: EditText
    private lateinit var vetDailyDownTime: EditText
    private lateinit var vetWeeklyTimer: EditText
    private lateinit var vetAstroMinuteOffset: EditText
    private lateinit var vetFerId: EditText

    private lateinit var vbtUp: Button
    private lateinit var vbtDown: Button
    private lateinit var vbtStop: Button
    private lateinit var vbtG: Button
    private lateinit var vbtE: Button
    private lateinit var vbtTimer: Button
    private lateinit var vbtSunPos: Button

    private var mode = VIS_NORMAL;

    private fun visibility_mode(mode: Int = VIS_NORMAL) {
        val iv = View.INVISIBLE
        val vi = View.VISIBLE

        when(mode) {
            VIS_NORMAL -> {
                vcbDailyUp.visibility = vi
                vcbDailyDown.visibility = vi
                vcbWeekly.visibility = vi
                vcbAstro.visibility = vi
                vcbRandom.visibility = vi
                vcbSunAuto.visibility = vi
                vcbRtcOnly.visibility = vi
                vcbFerId.visibility = vi

                vtvLog.visibility = vi
                vtvG.visibility = vi
                vtvE.visibility = vi

                vetDailyUpTime.visibility = vi
                vetDailyDownTime.visibility = vi
                vetWeeklyTimer.visibility = vi
                vetAstroMinuteOffset.visibility = vi
                vetFerId.visibility = vi

                vbtUp.visibility = vi
                vbtDown.visibility = vi
                vbtStop.visibility = vi
                vbtG.visibility = vi
                vbtE.visibility = vi
                vbtTimer.visibility = vi
                vbtSunPos.visibility = vi
            }

            VIS_SEP -> {
                vcbDailyUp.visibility = iv
                vcbDailyDown.visibility = iv
                vcbWeekly.visibility = iv
                vcbAstro.visibility = iv
                vcbRandom.visibility = iv
                vcbSunAuto.visibility = iv
                vcbRtcOnly.visibility = iv
                vcbFerId.visibility = vi

                vtvLog.visibility = vi
                vtvG.visibility = vi
                vtvE.visibility = vi

                vetDailyUpTime.visibility = iv
                vetDailyDownTime.visibility = iv
                vetWeeklyTimer.visibility = iv
                vetAstroMinuteOffset.visibility = iv
                vetFerId.visibility = vi

                vbtUp.visibility = vi
                vbtDown.visibility = vi
                vbtStop.visibility = vi
                vbtG.visibility = vi
                vbtE.visibility = vi
                vbtTimer.visibility = iv
                vbtSunPos.visibility = iv
            }
        }
    }

    private var tcp = McuTcp(mMessageHandler);

    private var group = 0
    private var memb = 0
    private var groupMax = 0
    private val membMax = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    val membMap = arrayOf(
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false));


    private fun makeSendString(c : String, mid : Int = 0, a : Int = 0, g : Int = 0, m : Int = 0, sep : Boolean = false ) : String {
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

    private fun makeTimerString(timer : String, mid : Int = 0, a : Int = 0, g : Int = 0, m : Int = 0) : String {
        var s = "timer";

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

        return s + timer + ";"
    }


    private var waitForSavedTimer = false
    private lateinit var alertDialog: AlertDialog
    private lateinit var progressDialog: ProgressDialog
    private var cuasInProgress = false
    private var mMenu: Menu? = null



    private val onCheckedChanged = CompoundButton.OnCheckedChangeListener { button, isChecked ->
        when (button.id) {

            R.id.checkBox_rtc_only -> {

                vcbDailyUp.isEnabled = !isChecked
                vcbDailyDown.isEnabled = !isChecked
                vcbWeekly.isEnabled = !isChecked
                vcbAstro.isEnabled = !isChecked
                vetDailyUpTime.isEnabled = !isChecked && vcbDailyUp.isChecked
                vetDailyDownTime.isEnabled = !isChecked && vcbDailyDown.isChecked
                vetWeeklyTimer.isEnabled = !isChecked && vcbWeekly.isChecked
                vetAstroMinuteOffset.isEnabled = !isChecked && vcbAstro.isChecked
                vcbRandom.isEnabled = !isChecked
                vcbSunAuto.isEnabled = !isChecked
            }

            R.id.checkBox_daily_up -> {
                vetDailyUpTime.isEnabled = isChecked && !vcbRtcOnly.isChecked
                if (!isChecked) vetDailyUpTime.setText("")
            }

            R.id.checkBox_daily_down -> {
                vetDailyDownTime.isEnabled = isChecked && !vcbRtcOnly.isChecked
                if (!isChecked) vetDailyDownTime.setText("")
            }

            R.id.checkBox_weekly -> {
                vetWeeklyTimer.isEnabled = isChecked
                if (!isChecked) vetWeeklyTimer.setText("")
            }

            R.id.checkBox_astro -> {
                vetAstroMinuteOffset.isEnabled = isChecked && !vcbRtcOnly.isChecked
                if (!isChecked) vetAstroMinuteOffset.setText("")
            }

            R.id.checkBox_ferID -> {
                vetFerId.isEnabled = isChecked
                vtvG.isEnabled = !isChecked
                vtvE.isEnabled = !isChecked
            }
        }
    }

    private fun set_member_max(m: Int, arr: Set<String>) {

    }

    private fun set_member_max(g: Int, max: Int) {
        membMax[g] = max; // old
         for (i in 0..6) {
           membMap[g-1][i] = i < max;
        }
    }

    private fun loadPreferences() {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)


        var tcpHostname = pref.getString("tcpHostName", DEFAULT_TCP_HOSTNAME)
        var tcpPort = DEFAULT_TCP_PORT
        if (tcpHostname.contains(":")) {
            val pos = tcpHostname.indexOf(':')
            tcpPort = Integer.parseInt(tcpHostname.substring(pos + 1))
            tcpHostname = tcpHostname.substring(0, pos)
        }

        McuTcp.socketAddress = InetSocketAddress(tcpHostname, tcpPort)


        val sgam = pref.getString("groupsAndMembers", "77777777")
        val sgamLength1 = Math.min(7, sgam!!.length - 1)
        groupMax = Math.min(7, Integer.parseInt(sgam.substring(0, 1)))

        for (i in 1..sgamLength1) {
            membMax[i] = Math.min(7, Integer.parseInt(sgam.substring(i, i + 1)))
        }
        for (i in sgamLength1 + 1..7) {
            membMax[i] = 0
        }

        set_member_max(1, Integer.parseInt(pref.getString("group1_members", "7")))
        set_member_max(2, Integer.parseInt(pref.getString("group2_members", "7")))
        set_member_max(3, Integer.parseInt(pref.getString("group3_members", "7")))
        set_member_max(4, Integer.parseInt(pref.getString("group4_members", "7")))
        set_member_max(5, Integer.parseInt(pref.getString("group5_members", "7")))
        set_member_max(6, Integer.parseInt(pref.getString("group6_members", "7")))
        set_member_max(7, Integer.parseInt(pref.getString("group7_members", "7")))


        group = pref.getInt("mGroup", 0);
        memb = pref.getInt("mMemb", 0);

        vcbRtcOnly.isChecked = pref.getBoolean("vcbRtcOnlyIsChecked", false)
        vcbFerId.isChecked = pref.getBoolean("vcbFerIdIsChecked", false)

        vcbDailyUp.isChecked = pref.getBoolean("vcbDailyUpIsChecked", false)
        vcbDailyDown.isChecked = pref.getBoolean("vcbDailyDownIsChecked", false)
        vcbWeekly.isChecked = pref.getBoolean("vcbWeeklyIsChecked", false)
        vcbAstro.isChecked = pref.getBoolean("vcbAstroIsChecked", false)
        vcbRandom.isChecked = pref.getBoolean("vcbRandomIsChecked", false)
        vcbSunAuto.isChecked = pref.getBoolean("vcbSunAutoIsChecked", false)
//                .isChecked = pref.getBoolean("IsChecked", false)


        vetFerId.setText(pref.getString("vetFerIdText", "90ABCD"))
        vetDailyUpTime.setText(pref.getString("vetDailyUpTimeText", ""))
        vetDailyDownTime.setText(pref.getString("vetDailyDownTimeText", ""))
        vetWeeklyTimer.setText(pref.getString("vetWeeklyTimerText", ""))
        vetAstroMinuteOffset.setText(pref.getString("vetAstroMinuteOffsetText", ""))

        vtvLog.setText(pref.getString("vtvLogText", ""))

    }

    private fun savePreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val ed = sharedPreferences.edit();

        ed.putInt("mGroup", group);
        ed.putInt("mMemb", memb);

        ed.putBoolean("vcbRtcOnlyIsChecked", vcbRtcOnly.isChecked)
        ed.putBoolean("vcbFerIdIsChecked", vcbFerId.isChecked)
        ed.putBoolean("vcbDailyUpIsChecked", vcbDailyUp.isChecked)
        ed.putBoolean("vcbDailyDownIsChecked", vcbDailyDown.isChecked)
        ed.putBoolean("vcbWeeklyIsChecked", vcbWeekly.isChecked)
        ed.putBoolean("vcbAstroIsChecked", vcbAstro.isChecked)
        ed.putBoolean("vcbRandomIsChecked", vcbRandom.isChecked)
        ed.putBoolean("vcbSunAutoIsChecked", vcbSunAuto.isChecked)

//        ed.putBoolean("IsChecked", .isChecked)


        ed.putString("vetFerIdText", vetFerId.text.toString())
        ed.putString("vetDailyUpTimeText", vetDailyUpTime.text.toString())
        ed.putString("vetDailyDownTimeText", vetDailyDownTime.text.toString())
        ed.putString("vetWeeklyTimerText", vetWeeklyTimer.text.toString())
        ed.putString("vetAstroMinuteOffsetText", vetAstroMinuteOffset.text.toString())

        val start = vtvLog.getLayout().getLineStart(max(0, vtvLog.getLineCount() - 20))
        val end = vtvLog.getLayout().getLineEnd(vtvLog.getLineCount() - 1)
        val logText = vtvLog.getText().toString().substring(start, end)
        ed.putString("vtvLogText", logText)


        // ed.putString("Text", .text.toString())

        ed.apply();
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)



        vcbDailyUp = findViewById(R.id.checkBox_daily_up)
        vcbDailyDown = findViewById(R.id.checkBox_daily_down)
        vcbWeekly = findViewById(R.id.checkBox_weekly)
        vcbAstro = findViewById(R.id.checkBox_astro)
        vcbRandom = findViewById(R.id.checkBox_random)
        vcbSunAuto = findViewById(R.id.checkBox_sun_auto)
        vcbRtcOnly = findViewById(R.id.checkBox_rtc_only)
        vcbFerId = findViewById(R.id.checkBox_ferID)

        vetDailyUpTime = findViewById(R.id.editText_dailyUpTime)
        vetDailyDownTime = findViewById(R.id.editText_dailyDownTime)
        vetWeeklyTimer = findViewById(R.id.editText_weeklyTimer)
        vetAstroMinuteOffset = findViewById(R.id.editText_astroMinuteOffset)
        vetFerId = findViewById(R.id.editText_ferID)

        vtvLog = findViewById(R.id.textView_log)
        vtvG = findViewById(R.id.textView_g)
        vtvE = findViewById(R.id.textView_e)

        vbtTimer = findViewById(R.id.button_timer)
        vbtUp = findViewById(R.id.button_up)
        vbtStop = findViewById(R.id.button_stop)
        vbtSunPos = findViewById(R.id.button_sun_pos)
        vbtDown = findViewById(R.id.button_down)
        vbtE = findViewById(R.id.button_e)
        vbtG = findViewById(R.id.button_g)


        vcbDailyUp.setOnCheckedChangeListener(onCheckedChanged)
        vcbDailyDown.setOnCheckedChangeListener(onCheckedChanged)
        vcbWeekly.setOnCheckedChangeListener(onCheckedChanged)
        vcbAstro.setOnCheckedChangeListener(onCheckedChanged)
        vcbRandom.setOnCheckedChangeListener(onCheckedChanged)
        vcbSunAuto.setOnCheckedChangeListener(onCheckedChanged)
        vcbRtcOnly.setOnCheckedChangeListener(onCheckedChanged)
        vcbFerId.setOnCheckedChangeListener(onCheckedChanged)


        loadPreferences()
        vtvLog.setMovementMethod(ScrollingMovementMethod())

        vtvG.text = if (group != 0) group.toString() else "A"
        vtvE.text = if (memb != 0) memb.toString() else if (group != 0) "A" else ""


        progressDialog = ProgressDialog(this)



    }


    override fun onResume() {
        super.onResume()
      //  vtvLog.append(String.format("read alive: %b, wa: %b, ca: %b\n", tcpReadThread.isAlive, tcpWriteThread.isAlive, tcpConnectThread.isAlive))

        if (useWifi) {
            enableSendButtons(false, 0)
            tcp.connect()
        }
      //  vtvLog.append("-----onResume----\n")
    }

    override fun onBackPressed() {
      //  supportFragmentManager.fragments[supportFragmentManager.backStackEntryCount - 1].onResume()
       super.onBackPressed()
     }


    public override fun onPause() {
        super.onPause()
      //  vtvLog.append("-----onPause----\n")

        if (useWifi)
            tcp.close()

        savePreferences()
    }

    private var messagePending = 0;

    fun tcpSocketTransmit(s: String) {

        if (messagePending != 0 || !tcp.isConnected) {
            if (!tcp.isConnecting) {
                enableSendButtons(false, 0)
                tcp.reconnect();
                vtvLog.append("tcp: try to reconnect...\n")
            }
            messagePending = 0;
            return;
        }

        messagePending = MainActivity.msgid
        tcp.transmit(s)
    }


    public class MessageHandler(activity: MainActivity) : Handler() {
        private val mActivity = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val ma = mActivity.get()
            if (ma == null)
                return
            when (msg.what) {

                McuTcp.MSG_TCP_INPUT_EOF -> {
                    //FIXME: what to do here?
                    ma.tcp.close()
                }


                McuTcp.MSG_TCP_OUTPUT_ERROR, McuTcp.MSG_TCP_INPUT_ERROR -> {
                    ma.tcp.close()
                }

                McuTcp.MSG_TCP_CONNECTED -> {
                    ma.enableSendButtons(true, 0)
                    ma.vtvLog.append("tcp connected\n")
                }

                McuTcp.MSG_TCP_CONNECTION_FAILED -> {
                    val s = msg.obj as String
                    ma.vtvLog.append("tcp connection failed: " + s + "\n")
                }

                McuTcp.MSG_LINE_RECEIVED -> try {
                    val s = msg.obj as String
                    if (s.equals("ready:")) {

                    } else if (s.isEmpty()) {

                    } else {
                        ma.vtvLog.append(s + "\n")
                    }
                    ma.messagePending = 0;  // FIXME: check msgid?

                    if (s.contains("rs=data")) {
                        ma.parseReceivedData(s)
                    }
                    if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                        if (s.contains(":cuas=ok:")) {
                            ma.progressDialog.hide()
                            ma.showAlertDialog("Success. Data has been received and stored.")
                            ma.cuasInProgress = false
                        } else if (s.contains(":cuas=time-out:")) {
                            ma.cuasInProgress = false
                            ma.progressDialog.hide()
                            ma.showAlertDialog("Time-Out. Please try again.")
                        }
                    }


                } catch (e: Exception) {
                    ma.vtvLog.append("MLR:error: " + e.toString() + "\n")

                }

                MainActivity.MSG_CUAS_TIME_OUT -> if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                    ma.cuasInProgress = false
                    ma.progressDialog.hide()
                    ma.showAlertDialog("Time-Out. Please try again.")
                }


                MainActivity.MSG_SEND_ENABLE -> ma.enableSendButtons(true, 0)

                MainActivity.MSG_ERROR -> {
                    val s = msg.obj as String
                    ma.vtvLog.append(s + "\n")
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun transmit(s: String) {
        if (useWifi) tcpSocketTransmit(s)
        vtvLog.append("transmit: " + s + "\n")
    }

    fun onCheckedClick(view: View) {
        val isChecked = (view as CheckBox).isChecked


        if (isChecked)
            when (view.getId()) {
                R.id.checkBox_daily_up -> vetDailyUpTime.setText(def_dailyUp)

                R.id.checkBox_daily_down -> vetDailyDownTime.setText(def_dailyDown)

                R.id.checkBox_weekly -> vetWeeklyTimer.setText(def_weekly)

                R.id.checkBox_astro -> vetAstroMinuteOffset.setText(def_astro)
            }

    }

    @Throws(IOException::class)
    private fun ferSendTime() {
        val c = Calendar.getInstance()

        val sd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
        val st = SimpleDateFormat("HH:mm:ss", Locale.US).format(c.time)


        val cmd = String.format("config rtc=%sT%s;", sd, st)
        vtvLog.append(cmd + "\n")
        transmit(cmd)

    }

    @Throws(java.io.IOException::class) private fun getSavedTimer(g: Int, m: Int) {
        transmit(makeTimerString(timer = " rs=2", mid = getMsgId(), g = g, m = m))

        waitForSavedTimer = true
    }

    internal fun parseReceivedData(s: String) {
        var s = s
        try {
            s = s.substring(s.indexOf(":rs=data: "))

            //     tvRec.append(String.format("###%s###\n", s));

            var g = 0
            var m = 0
            var sunAuto = 0
            var random = 0
            var astro = 0
            var daily = ""
            var weekly = ""
            var hasAstro = false

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
                            "g" -> g = value.toInt()
                            "m" -> m = value.toInt()
                            "sun-auto" -> sunAuto = value.toInt()
                            "random" -> random = value.toInt()
                            "astro" -> {
                                hasAstro = true; astro = value.toInt()
                            }
                            "daily" -> daily = value
                            "weekly" -> weekly = value

                        }

                    } else {
                        when (i) {
                            "sun-auto" -> sunAuto = 1
                            "random" -> random = 1
                            "astro" -> {
                                hasAstro = true; astro = 0
                            }
                        }
                    }
                }
            }

            vcbSunAuto.isChecked = sunAuto == 1
            vcbRandom.isChecked = random == 1
            vcbWeekly.isChecked = !weekly.isEmpty()
            vcbAstro.isChecked = hasAstro
            vetWeeklyTimer.setText(weekly)
            vcbDailyUp.isChecked = !(daily.isEmpty() || daily.startsWith("-"))
            vcbDailyDown.isChecked = !(daily.isEmpty() || daily.endsWith("-"))

            vetAstroMinuteOffset.setText(if (hasAstro) astro.toString() else "")

            var dailyUp = ""
            var dailyDown = ""

            if (!daily.startsWith("-")) {
                dailyUp = daily.substring(0, 2) + ":" + daily.substring(2, 4)
                daily = daily.substring(4)
            } else {
                daily = daily.substring(1)
            }

            if (!daily.startsWith("-")) {
                dailyDown = daily.substring(0, 2) + ":" + daily.substring(2, 4)
            }

            vetDailyUpTime.setText(dailyUp)
            vetDailyDownTime.setText(dailyDown)

        } catch (e: Exception) {
        }

    }

    private fun getMsgId(): Int {
        return ++msgid
    }

    private fun getFerId(): Int {
        if (vcbFerId.isChecked) {
            var s = vetFerId.text.toString()
            if (s.length == 5) {
                s = "9$s"
            } else if (s.length != 6) {
                throw Exception("id must have 6 digits ($s)")
            }
            val result = Integer.parseInt(s, 16)
            return result
        }
        return 0
    }

    fun onClick(view: View) {

        try {
           // vtvLog.append(String.format("ra: %b, wa: %b, ca: %b\n", tcpReadThread.isAlive, tcpWriteThread.isAlive, tcpConnectThread.isAlive))

            when (view.id) {
                R.id.button_stop -> transmit(makeSendString(c = "stop", mid = getMsgId(), a = getFerId(), g = group, m = memb))
                R.id.button_up -> transmit(makeSendString(c = "up", mid = getMsgId(), a = getFerId(), g = group, m = memb, sep = (mode == MODE_SEP)))


                R.id.button_down -> transmit(makeSendString(c = "down", mid = getMsgId(), a = getFerId(), g = group, m = memb, sep = (mode == MODE_SEP)))
                R.id.button_g -> {
                    for (i in 0..7) {
                        group = ++group % 8
                        if (group == 0 || membMax[group] != 0) {
                            break;
                        }
                    }

                    vtvG.text = if (group == 0) "A" else group.toString()
                    if (memb > membMax[group])
                        memb = 1
                    vtvE.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
                    getSavedTimer(group, memb)
                }
                R.id.button_e -> {
                    memb = ++memb % (membMax[group] + 1)
                    vtvE.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
                    getSavedTimer(group, memb)
                }
                R.id.button_sun_pos -> transmit(makeSendString(c = "sun-down", mid = getMsgId(), a = getFerId(), g = group, m = memb))

                R.id.button_timer -> {
                    val upTime = vetDailyUpTime.text.toString()
                    val downTime = vetDailyDownTime.text.toString()
                    val astroOffset = vetAstroMinuteOffset.text.toString()

                    var timer = ""

                    val rtcOnly = vcbRtcOnly.isChecked

                    if (rtcOnly) {
                        timer += " rtc-only"
                    } else {
                        val dailyUpChecked = vcbDailyUp.isChecked
                        val dailyDownChecked = vcbDailyDown.isChecked
                        val weeklyChecked = vcbWeekly.isChecked

                        if (dailyUpChecked || dailyDownChecked) {
                            timer += " daily="
                            timer += if (dailyUpChecked) upTime.substring(0, 2) + upTime.substring(3, 5) else "-"
                            timer += if (dailyDownChecked) downTime.substring(0, 2) + downTime.substring(3, 5) else "-"
                        }

                        if (vcbAstro.isChecked) {
                            timer += " astro=$astroOffset"
                        }

                        if (weeklyChecked) {
                            val weeklyTimer = vetWeeklyTimer.text.toString()

                            timer += " weekly="
                            timer += weeklyTimer
                        }

                    }


                    if (vcbSunAuto.isChecked) {
                        timer += " sun-auto"
                    }

                    if (vcbRandom.isChecked) {
                        timer += " random"
                    }


                    // timer = upTime.substring(0,2);


                    transmit(makeTimerString(timer = timer, mid = getMsgId(), a = getFerId(), g = group, m = memb))
                    if (!rtcOnly) {
                        enableSendButtons(false, 5)
                    }
                }
            }


        } catch (e: Exception) {
            vtvLog.append("OCH:error: " + e.toString() + "...\n")
        }

    }

    internal fun showAlertDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { dialog, id -> alertDialog.hide() }

        alertDialog = builder.create()
        alertDialog.show()
    }

    private fun showProgressDialog(msg: String, time_out: Int) {

        progressDialog.setMessage("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.isIndeterminate = false
        progressDialog.max = 60
        progressDialog.show()
        cuasInProgress = true


        val t = object : Thread() {
            override fun run() {
                var jumpTime = 0

                while (jumpTime < time_out && cuasInProgress) {
                    try {
                        Thread.sleep(1000)
                        jumpTime += 1
                        progressDialog.progress = jumpTime

                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }

                }
                mMessageHandler.obtainMessage(MSG_CUAS_TIME_OUT, "timeout").sendToTarget()
            }
        }
        t.start()
    }

    internal fun enableSendButtons(enable: Boolean, timeout: Int) {

        if (timeout > 0) {
            val t = object : Thread() {
                override fun run() {
                    var jumpTime = 0

                    while (jumpTime < timeout) {
                        try {
                            Thread.sleep(1000)
                            jumpTime += 1
                        } catch (e: InterruptedException) {
                            // TODO Auto-generated catch block
                            e.printStackTrace()
                        }

                    }
                    mMessageHandler.obtainMessage(MSG_SEND_ENABLE, "timeout").sendToTarget()
                }
            }
            t.start()


        }
        vbtTimer.isEnabled = enable;
        vbtUp.isEnabled = enable;
        vbtStop.isEnabled = enable;
        vbtSunPos.isEnabled = enable;
        vbtDown.isEnabled = enable;

        mMenu?.findItem(R.id.action_cuAutoSet)?.isEnabled = enable
        mMenu?.findItem(R.id.action_setFunc)?.isEnabled = enable


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        mMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }


            R.id.action_cuAutoSet -> {
                transmit(String.format(configFmt, getMsgId(), "cu=auto"))
                showProgressDialog("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...", 60)
            }

            R.id.action_setFunc -> {
                transmit(makeSendString(c = "set", mid = getMsgId(), a = getFerId(), g = group, m = memb))
                showAlertDialog("You now have 60 seconds remaining to press STOP on the transmitter you want to add/remove. Beware: If you press STOP on the central unit, the device will be removed from it. To add it again, you would need the code. If you don't have the code, then you would have to press the physical set-button on the device")
            }

            R.id.action_setEndPos -> {
                item.isChecked = !item.isChecked
                visibility_mode(if (item.isChecked) VIS_SEP else VIS_NORMAL)
                mode = if (item.isChecked) MODE_SEP else MODE_NORMAL
            }

            R.id.action_openMcuCfg -> {
                val intent = Intent(this, McuConfigActivity::class.java)
                startActivity(intent)
            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true;
    }

    companion object {
        internal var msgid = 1

        internal const val MODE_NORMAL = 0
        internal const val MODE_SEP = 1

        internal const val VIS_NORMAL = 0
        internal const val VIS_SEP = 1
        internal const val VIS_XXX = 2

        internal const val MSG_CUAS_TIME_OUT = 3
        internal const val MSG_SEND_ENABLE = 4
        internal const val MSG_ERROR = 6

        internal const val def_dailyUp = "07:30"
        internal const val def_dailyDown = "19:30"
        internal const val def_weekly = "0700-++++0900-+"
        internal const val def_astro = "0"
        internal const val configFmt = "config mid=%d %s;"
        internal const val timeFormat = "%4d-%02d-%02dT%02d:%02d:%02d"
    }


}
