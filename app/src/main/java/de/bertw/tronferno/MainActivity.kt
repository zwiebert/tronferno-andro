package de.bertw.tronferno

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.cmd_buttons_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.position_indicator.*
import kotlinx.android.synthetic.main.position_indicators.*
import kotlinx.android.synthetic.main.timers_main.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.lang.ref.WeakReference
import java.net.InetSocketAddress


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

    val mMessageHandler = MessageHandler(this)
    val pr = TfmcuPresenter(mMessageHandler)
    //val tfmcuModel = pr.model

    private var mode = VIS_NORMAL
    private var mHideViewTimer = false
    private var mHideViewLog = false
    private var mPosAdapter = ShutterPositionsAdapter(this)


    private fun switchVisibility(mode: Int = VIS_NORMAL) {
        val iv = View.INVISIBLE
        val vi = View.VISIBLE

        when (mode) {
            VIS_NORMAL -> {
                timer_group_main.visibility = vi

                vtvLog.visibility = vi
                vtvG.visibility = vi
                vtvE.visibility = vi

                vetShutterPos.visibility = vi
                // vetFerId.visibility = vi

                vbtUp.visibility = vi
                vbtDown.visibility = vi
                vbtStop.visibility = vi
                // vbtG.visibility = vi
                // vbtE.visibility = vi
                vbtSunPos.visibility = vi
            }

            VIS_SEP -> {
                timer_group_main.visibility = View.GONE


                vtvLog.visibility = vi
                // vtvG.visibility = vi
                // vtvE.visibility = vi


                vetShutterPos.visibility = iv


                vbtUp.visibility = vi
                vbtDown.visibility = vi
                vbtStop.visibility = vi
                vbtG.visibility = vi
                vbtE.visibility = vi
                vbtSunPos.visibility = iv
            }
        }
    }

    fun getSelectedMember(): Int {
        return if (group == 0) 0 else memb
    }

    fun getSelectedGroup(): Int {
        return group
    }

    fun getGroupIndex(g: Int = group): Int {
        return usedGroups.groupToIdxArr[g]
    }

    var group = 0
    var memb = 0
    var groupMax = 0

    val membMax = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)

    private val membMap = arrayOf(
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false))

    private var mMemberNames = HashMap<String, String>()
    private lateinit var alertDialog: AlertDialog
    private lateinit var progressDialog: ProgressDialog
    private var cuasInProgress = false
    private var mMenu: Menu? = null


    fun getMemberName(g: Int = group, m: Int = memb): String {
        val key = "memberName_$g$m"
        val value = mMemberNames.get(key)

        return if (value.isNullOrBlank()) "$m" else value
    }

    private val onCheckedChanged = CompoundButton.OnCheckedChangeListener { button, isChecked ->
        when (button.id) {

            R.id.vcbDailyUp -> {
                vetDailyUpTime.isEnabled = isChecked
                if (!isChecked) vetDailyUpTime.setText("")
            }

            R.id.vcbDailyDown -> {
                vetDailyDownTime.isEnabled = isChecked
                if (!isChecked) vetDailyDownTime.setText("")
            }

            R.id.vcbWeekly -> {
                vetWeeklyTimer.isEnabled = isChecked
                if (!isChecked) vetWeeklyTimer.setText("")
            }

            R.id.vcbAstro -> {
                vetAstroMinuteOffset.isEnabled = isChecked
                if (!isChecked) vetAstroMinuteOffset.setText("")
            }

        }
    }

    private fun setMemberCount(g: Int, max: Int) {
        membMax[g] = max // old
        for (i in 0..6) {
            membMap[g - 1][i] = i < max
        }
    }

    private fun loadPreferences() {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        var tcpPort = DEFAULT_TCP_PORT

        var tcpHostname = pref.getString("tcpHostName", DEFAULT_TCP_HOSTNAME)!!

        if (tcpHostname.contains(":")) {
            val pos = tcpHostname.indexOf(':')
            tcpPort = Integer.parseInt(tcpHostname.substring(pos + 1))
            tcpHostname = tcpHostname.substring(0, pos)
        }

        McuTcp.socketAddress = InetSocketAddress(tcpHostname, tcpPort)

        val sgam = pref.getString("groupsAndMembers", "77777777")
        val sgamLength1 = kotlin.math.min(7, sgam!!.length - 1)
        groupMax = kotlin.math.min(7, Integer.parseInt(sgam.substring(0, 1)))

        for (i in 1..sgamLength1) {
            membMax[i] = kotlin.math.min(7, Integer.parseInt(sgam.substring(i, i + 1)))
        }
        for (i in sgamLength1 + 1..7) {
            membMax[i] = 0
        }

        setMemberCount(1, Integer.parseInt(pref.getString("group1_members", "7")!!))
        setMemberCount(2, Integer.parseInt(pref.getString("group2_members", "7")!!))
        setMemberCount(3, Integer.parseInt(pref.getString("group3_members", "7")!!))
        setMemberCount(4, Integer.parseInt(pref.getString("group4_members", "7")!!))
        setMemberCount(5, Integer.parseInt(pref.getString("group5_members", "7")!!))
        setMemberCount(6, Integer.parseInt(pref.getString("group6_members", "7")!!))
        setMemberCount(7, Integer.parseInt(pref.getString("group7_members", "7")!!))


        group = pref.getInt("mGroup", 0)
        memb = pref.getInt("mMemb", 0)

        vcbDailyUp.isChecked = pref.getBoolean("vcbDailyUpIsChecked", false)
        vcbDailyDown.isChecked = pref.getBoolean("vcbDailyDownIsChecked", false)
        vcbWeekly.isChecked = pref.getBoolean("vcbWeeklyIsChecked", false)
        vcbAstro.isChecked = pref.getBoolean("vcbAstroIsChecked", false)
        vcbRandom.isChecked = pref.getBoolean("vcbRandomIsChecked", false)
        vcbSunAuto.isChecked = pref.getBoolean("vcbSunAutoIsChecked", false)
        mHideViewLog = pref.getBoolean("mHideViewLog", mHideViewLog)
        mHideViewTimer = pref.getBoolean("mHideViewTimer", mHideViewTimer)


        vetFerId.setText(pref.getString("vetFerIdText", "90ABCD"))
        vetDailyUpTime.setText(pref.getString("vetDailyUpTimeText", ""))
        vetDailyDownTime.setText(pref.getString("vetDailyDownTimeText", ""))
        vetWeeklyTimer.setText(pref.getString("vetWeeklyTimerText", ""))
        vetAstroMinuteOffset.setText(pref.getString("vetAstroMinuteOffsetText", ""))

        vtvLog.text = pref.getString("vtvLogText", "")

        for (g in 1..7) {
            for (m in 1..7) {
                val key = "memberName_$g$m"
                val value = pref.getString(key, "") ?: continue
                mMemberNames[key] = value
            }
        }

        fun updateUsedGroups() {
            var ugi = 0
            for (gi in 1..7) {
                if (membMax[gi] > 0) {
                    usedGroups.arr[ugi] = gi
                    if (group == gi)
                        usedGroups.selectedIdx = ugi
                    usedGroups.groupToIdxArr[gi] = ugi

                    ++ugi
                }
            }
            usedGroups.arr[ugi] = 0 // terminate array with 0
            usedGroups.size = ugi
        }

        updateUsedGroups()

    }

    private fun savePreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val ed = sharedPreferences.edit()

        ed.putInt("mGroup", group)
        ed.putInt("mMemb", memb)

        ed.putBoolean("vcbDailyUpIsChecked", vcbDailyUp.isChecked)
        ed.putBoolean("vcbDailyDownIsChecked", vcbDailyDown.isChecked)
        ed.putBoolean("vcbWeeklyIsChecked", vcbWeekly.isChecked)
        ed.putBoolean("vcbAstroIsChecked", vcbAstro.isChecked)
        ed.putBoolean("vcbRandomIsChecked", vcbRandom.isChecked)
        ed.putBoolean("vcbSunAutoIsChecked", vcbSunAuto.isChecked)
        ed.putBoolean("mHideViewLog", mHideViewLog)
        ed.putBoolean("mHideViewTimer", mHideViewTimer)


        ed.putString("vetFerIdText", vetFerId.text.toString())
        ed.putString("vetDailyUpTimeText", vetDailyUpTime.text.toString())
        ed.putString("vetDailyDownTimeText", vetDailyDownTime.text.toString())
        ed.putString("vetWeeklyTimerText", vetWeeklyTimer.text.toString())
        ed.putString("vetAstroMinuteOffsetText", vetAstroMinuteOffset.text.toString())

        val start = vtvLog.layout.getLineStart(max(0, vtvLog.lineCount - 20))
        val end = vtvLog.layout.getLineEnd(vtvLog.lineCount - 1)
        val logText = vtvLog.text.toString().substring(start, end)
        ed.putString("vtvLogText", logText)

        // ed.putString("Text", .text.toString())

        for ((key, value) in mMemberNames) {
            ed.putString(key, value)
        }

        ed.apply()
    }

    lateinit var pbsArr: Array<ProgressBar>
    lateinit var ptvArr: Array<TextView>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        vcbDailyUp.setOnCheckedChangeListener(onCheckedChanged)
        vcbDailyDown.setOnCheckedChangeListener(onCheckedChanged)
        vcbWeekly.setOnCheckedChangeListener(onCheckedChanged)
        vcbAstro.setOnCheckedChangeListener(onCheckedChanged)
        vcbRandom.setOnCheckedChangeListener(onCheckedChanged)
        vcbSunAuto.setOnCheckedChangeListener(onCheckedChanged)

        loadPreferences()
        vtvLog.movementMethod = ScrollingMovementMethod()



        pbsArr = arrayOf(vpbPiM1, vpbPiM2, vpbPiM3, vpbPiM4, vpbPiM5, vpbPiM6, vpbPiM7)
        ptvArr = arrayOf(vtvPiM1, vtvPiM2, vtvPiM3, vtvPiM4, vtvPiM5, vtvPiM6, vtvPiM7)

        progressDialog = ProgressDialog(this)

        timer_frameLayout.visibility = if (mHideViewTimer) View.GONE else View.VISIBLE
        vtvLog.visibility = if (mHideViewLog) View.GONE else View.VISIBLE


        val mLayoutManager = LinearLayoutManager(applicationContext)

        vrvPositions.apply {
            layoutManager = mLayoutManager
            itemAnimator = DefaultItemAnimator()
            adapter = mPosAdapter
        }

        mPosAdapter.notifyDataSetChanged()
    }


    override fun onResume() {
        super.onResume()
        //  vtvLog.append(String.format("read alive: %b, wa: %b, ca: %b\n", tcpReadThread.isAlive, tcpWriteThread.isAlive, tcpConnectThread.isAlive))

        enableSendButtons(false, 0)
        pr.onResume()
        vtvG.text = if (group != 0) group.toString() else "A"
        selectMember(memb)
        //  vtvLog.append("-----onResume----\n")
    }


    public override fun onPause() {
        super.onPause()
        //  vtvLog.append("-----onPause----\n")
        pr.onPause()
        savePreferences()
    }

    private fun logWriteLine(line: String) {
        vtvLog.append(line + "\n")
    }

    class MessageHandler(activity: MainActivity) : Handler() {
        private val mActivity = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val ma = mActivity.get() ?: return
            var s = ""
            when (msg.what) {

                McuTcp.MSG_TCP_INPUT_EOF -> {
                    //FIXME: what to do here?
                    //ma.pr.reset()
                }

                McuTcp.MSG_TCP_OUTPUT_ERROR -> {
                    if (msg.obj is String)
                        ma.vtvLog.append("tcp write error: " + msg.obj as String + "\n")
                    //ma.pr.reset()
                }

                McuTcp.MSG_TCP_INPUT_ERROR -> {
                    if (msg.obj is String)
                        ma.vtvLog.append("tcp read error: " + msg.obj as String + "\n")
                    //ma.pr.reset()
                }

                McuTcp.MSG_TCP_CONNECTED -> {
                    ma.enableSendButtons(true, 0)
                    ma.pr.onConnect()
                    ma.vtvLog.append("tcp connected\n")

                    if (mcuConfig_changed) {
                        mcuConfig_changed = false
                        ma.configureMcu()
                    }

                    // ma.pr.data2Mcu(TfmcuConfigData("longitude=? latitude=? time-zone=? dst=? wlan-ssid=? cu=? baud=? verbose=? dst=?"))
                    ma.pr.data2Mcu(TfmcuMcuSettings(geta=arrayOf("longitude", "latitude", "tz", "wlan-ssid", "cu", "verbose")))
                }

                McuTcp.MSG_TCP_CONNECTION_FAILED -> {
                    if (msg.obj is String)
                        ma.vtvLog.append("tcp connect error: " + msg.obj as String + "\n")
                }

                McuTcp.MSG_TCP_REQ_RECONNECT -> {
                    ma.pr.reset()
                }

                McuTcp.MSG_LINE_RECEIVED -> try {
                    s = msg.obj as String
                    val json = if (s.startsWith("{"))  s.removeSuffix(";") else ""

                    when {
                        s == "ready:" -> {

                        }
                        s.isEmpty() -> {

                        }
                        else -> ma.vtvLog.append(s + "\n")
                    }

                    if (s.startsWith("tf:") && s.contains(" timer:")) {
                        ma.parseReceivedTimer(s.substringAfter(" timer:"))
                    } else if (json.contains("\"auto\":")) {
                        ma.parseReceivedTimerJson(json)
                    }
                    if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                        if (s.contains(":cuas=ok:")) {
                            ma.progressDialog.hide()
                            ma.showAlertDialog(ma.getString(R.string.cuas_success))
                            ma.cuasInProgress = false
                        } else if (s.contains(":cuas=time-out:")) {
                            ma.cuasInProgress = false
                            ma.progressDialog.hide()
                            ma.showAlertDialog(ma.getString(R.string.cuas_timeout))
                        }
                    }
                    if (s.startsWith("tf:") && s.contains(" config:")) {
                        ma.parseReceivedConfig(s.substringAfter(" config:"))
                    } else if (json.contains("\"config\":")) {
                        ma.parseReceivedConfigJson(json)
                    }
                    if (s.startsWith("A:position:")) {
                        ma.parseReceivedPosition(s)
                    } else if (s.startsWith("U:position:")) {
                        ma.parseReceivedPosition(s)
                    } else if  (json.contains("\"pct\":")) {
                        ma.parseReceivedPositionJson(json)
                    }

                } catch (e: Exception) {
                    ma.vtvLog.append("MLR:error: $e\n...line: $s")

                }

                MSG_CUAS_TIME_OUT -> if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                    ma.cuasInProgress = false
                    ma.progressDialog.hide()
                    ma.showAlertDialog(ma.getString(R.string.cuas_timeout))
                }


                MSG_SEND_ENABLE -> ma.enableSendButtons(true, 0)

                MSG_ERROR -> {
                    val errMsg = msg.obj as String
                    ma.vtvLog.append(errMsg + "\n")
                }
            }
        }
    }


    fun onCheckedClick(view: View) {
        val isChecked = (view as CheckBox).isChecked


        if (isChecked)
            when (view.getId()) {
                R.id.vcbDailyUp -> vetDailyUpTime.setText(def_dailyUp)

                R.id.vcbDailyDown -> vetDailyDownTime.setText(def_dailyDown)

                R.id.vcbWeekly -> vetWeeklyTimer.setText(def_weekly)

                R.id.vcbAstro -> vetAstroMinuteOffset.setText(def_astro)
            }

    }


    private fun td2dailyUp(td: TfmcuTimerData): String {
        val daily = td.daily

        if (daily.isEmpty()) {
            return ""
        }

        return if (daily.startsWith("-")) "" else daily.substring(0, 2) + ":" + daily.substring(2, 4)
    }

    private fun td2dailyDown(td: TfmcuTimerData): String {
        var daily = td.daily

        if (daily.isEmpty()) {
            return ""
        }

        daily = if (daily.startsWith("-")) daily.substring(1) else daily.substring(4)
        return if (daily.startsWith("-")) "" else daily.substring(0, 2) + ":" + daily.substring(2, 4)
    }

    internal fun parseReceivedTimerJson(s: String) {
        val td = ferParseReceivedTimerJson2(s)

        vcbSunAuto.isChecked = td.sunAuto
        vcbRandom.isChecked = td.random
        vcbManu.isChecked = td.manual
        vcbWeekly.isChecked = td.weekly.isNotEmpty()
        vcbAstro.isChecked = td.hasAstro
        vetWeeklyTimer.setText(td.weekly)
        vcbDailyUp.isChecked = !(td.daily.isEmpty() || td.daily.startsWith("-"))
        vcbDailyDown.isChecked = !(td.daily.isEmpty() || td.daily.endsWith("-"))
        vetAstroMinuteOffset.setText(if (td.hasAstro) td.astro.toString() else "")
        vetDailyUpTime.setText(td2dailyUp(td))
        vetDailyDownTime.setText(td2dailyDown(td))
    }

    internal fun parseReceivedTimer(s: String) {
        val td = ferParseReceivedTimer(s)

        vcbSunAuto.isChecked = td.sunAuto
        vcbRandom.isChecked = td.random
        vcbManu.isChecked = td.manual
        vcbWeekly.isChecked = td.weekly.isNotEmpty()
        vcbAstro.isChecked = td.hasAstro
        vetWeeklyTimer.setText(td.weekly)
        vcbDailyUp.isChecked = !(td.daily.isEmpty() || td.daily.startsWith("-"))
        vcbDailyDown.isChecked = !(td.daily.isEmpty() || td.daily.endsWith("-"))
        vetAstroMinuteOffset.setText(if (td.hasAstro) td.astro.toString() else "")
        vetDailyUpTime.setText(td2dailyUp(td))
        vetDailyDownTime.setText(td2dailyDown(td))
    }

    private fun sendRtc(broadcast: Boolean = false) {
        pr.timerClear()
        pr.td.a = getFerId()
        if (!broadcast) {
            pr.td.g = group
            pr.td.m = memb
        }

        pr.td.rtcOnly = true
        pr.data2Mcu(pr.td)
    }

    private fun sendTimer() {
        pr.timerClear()
        pr.td.apply {
            a = getFerId()
            g = getSelectedGroup()
            m = getSelectedMember()
            sunAuto = vcbSunAuto.isChecked
            random = vcbRandom.isChecked
            manual = vcbManu.isChecked
        }

        val dailyUpChecked = vcbDailyUp.isChecked
        val dailyDownChecked = vcbDailyDown.isChecked
        if (dailyUpChecked || dailyDownChecked) {
            val upTime = vetDailyUpTime.text.toString()
            val downTime = vetDailyDownTime.text.toString()
            pr.td.daily += if (dailyUpChecked) upTime.substring(0, 2) + upTime.substring(3, 5) else "-"
            pr.td.daily += if (dailyDownChecked) downTime.substring(0, 2) + downTime.substring(3, 5) else "-"
        }

        if (vcbAstro.isChecked) {
            pr.td.hasAstro = true
            pr.td.astro = vetAstroMinuteOffset.text.toString().toInt()
        }

        if (vcbWeekly.isChecked) {
            pr.td.weekly = vetWeeklyTimer.text.toString()
        }





        if (pr.data2Mcu(pr.td)) {
            enableSendButtons(false, 5)
        }
    }


    private fun saveMcuPreferecence() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val ed = sharedPreferences.edit()


        for (i in mcuCfgPrefKeys.indices) {
            ed.putString(mcuCfgPrefKeys[i], mcuCfgPrefVals[i])
            ed.putString(mcuCfgPrefKeys[i] + "_old", mcuCfgPrefVals[i])
        }

        ed.apply()

    }

    private val mcuCfgPrefKeys = arrayOf("geo_latitude", "geo_longitude", "geo_time_zone", "wlan_ssid", "cu_id", "cli_verbosity")
    private val mcuCfgMcuKeys = arrayOf("latitude", "longitude", "tz", "wlan-ssid", "cu", "verbose")
    private var mcuCfgPrefVals = arrayOf("", "", "", "", "", "")


    fun configureMcu() {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        for (i in mcuCfgPrefKeys.indices) {
            val pv = pref.getString(mcuCfgPrefKeys[i], "")
            val pvOld = pref.getString(mcuCfgPrefKeys[i] + "_old", "")
            val mk = mcuCfgMcuKeys[i]

            if (pv != pvOld) {
                pr.data2Mcu(TfmcuConfigData("$mk=$pv"))
            }
        }

    }

    fun parseReceivedConfigJson(line: String) {
        val jco = Json.parseToJsonElement(line).jsonObject["config"] ?: return

        for ((k, v) in jco.jsonObject) {
            // listAdapter.add(k + "=" + v)
            for (i in mcuCfgPrefKeys.indices) {
                if (mcuCfgMcuKeys[i] == k) {
                    mcuCfgPrefVals[i] = v.jsonPrimitive.content
                }
            }
        }
        saveMcuPreferecence()
        vtvLog.append("mcu preference saved\n")
    }

    fun parseReceivedConfig(line: String) {
        var s = line
            while (s.contains('=')) {
                val k = s.substringBefore('=')
                val v = s.substringAfter('=').substringBefore(';').substringBefore(' ')
                s = s.substringAfter(' ', ";")

                // listAdapter.add(k + "=" + v)
                for (i in mcuCfgPrefKeys.indices) {
                    if (mcuCfgMcuKeys[i] == k) {
                        mcuCfgPrefVals[i] = v
                    }
                }

                // vtvLog.append("key: " + k + "\nval: " + v + "\n")

            }
            saveMcuPreferecence()
            vtvLog.append("mcu preference saved\n")
    }

    // position code
    fun parseReceivedPosition(line: String) {
        if (pr.model.parseReceivedPosition(line)) {
            mPosAdapter.notifyDataSetChanged()
        }
    }

    fun parseReceivedPositionJson(line: String) {
        if (pr.model.parseReceivedPositionJson(line)) {
            mPosAdapter.notifyDataSetChanged()
        }
    }

    private fun getFerId(): Int {
        var result = 0
        if (vetFerId.visibility == View.VISIBLE) {
            var s = vetFerId.text.toString()
            if (s.length == 5) {
                s = "9$s"
            } else if (s.length != 6) {
                throw Exception("id must have 6 digits ($s)") // FIXME
            }
            result = Integer.parseInt(s, 16)

        }
        return result
    }

    fun onClick(view: View) {
        val g = getSelectedGroup()
        val m = getSelectedMember()
        val cd = TfmcuSendData(a = getFerId(), g = g, m = m, sep = (mode == MODE_SEP))
        try {
            // vtvLog.append(String.format("ra: %b, wa: %b, ca: %b\n", tcpReadThread.isAlive, tcpWriteThread.isAlive, tcpConnectThread.isAlive))

            when (view.id) {
                R.id.vbtStop -> {
                    cd.cmd = TfmcuSendData.CMD_STOP; pr.data2Mcu(cd)
                }
                R.id.vbtUp -> {
                    cd.cmd = TfmcuSendData.CMD_UP; pr.data2Mcu(cd)
                }
                R.id.vbtDown -> {
                    cd.cmd = TfmcuSendData.CMD_DOWN; pr.data2Mcu(cd)
                }

                R.id.vbtG -> if (enableFerId(false)) {
                    selectNextGroup()
                }

                R.id.vbtE -> if (enableFerId(false)) {
                    selectNextMember()
                }

                R.id.vbtSunPos -> {
                    cd.cmd = TfmcuSendData.CMD_SUN_DOWN; pr.data2Mcu(cd)
                }

                R.id.vbtTimerSend -> {
                    sendTimer()
                }

                R.id.vbtEdWeekly -> {
                    val intent = Intent(this, WTimerActivity::class.java)
                    val tws = if (vetWeeklyTimer.text.isBlank()) def_weekly else vetWeeklyTimer.text.toString()
                    intent.putExtra("wtimer", tws)
                    startActivityForResult(intent, REQ_WEEKLY_EDITOR)

                }

                R.id.vpbPiM1 -> selectMember(1, view.tag)
                R.id.vpbPiM2 -> selectMember(2, view.tag)
                R.id.vpbPiM3 -> selectMember(3, view.tag)
                R.id.vpbPiM4 -> selectMember(4, view.tag)
                R.id.vpbPiM5 -> selectMember(5, view.tag)
                R.id.vpbPiM6 -> selectMember(6, view.tag)
                R.id.vpbPiM7 -> selectMember(7, view.tag)
            }


        } catch (e: Exception) {
            vtvLog.append("OCH:error: $e...\n")
        }

    }

    private val usedGroups = object {
        var arr = Array(8) { 0 }
        var groupToIdxArr = Array(8) { 0 }
        var size = 0
        var selectedIdx = 0
    }

    val nonEmptyGroupsCount: Int
        get () = usedGroups.size

    fun getGroupNumberByIdx(idx: Int): Int {
        return usedGroups.arr[idx]
    }

    fun selectNextGroup() {
        if (usedGroups.arr[usedGroups.selectedIdx] == 0)
            usedGroups.selectedIdx = -1

        selectGroup(usedGroups.arr[++usedGroups.selectedIdx])
    }

    fun selectGroupByIdx(idx: Int) {
        usedGroups.selectedIdx = idx
        selectGroup(usedGroups.arr[idx])
    }

    fun selectGroup(g: Int) {
        val old_group = group
        group = g

        // update index if needed //FIXME
        if (group != usedGroups.arr[usedGroups.selectedIdx]) {
            for (i in 0..7) {
                if (usedGroups.arr[i] == group) {
                    usedGroups.selectedIdx = i
                    break
                }
            }
        }

        if (old_group != group) {
            if (old_group != 0) {
                mPosAdapter.notifyItemChanged(getGroupIndex(old_group))
            } else mPosAdapter.notifyDataSetChanged()
            if (group != 0) {
                mPosAdapter.notifyItemChanged(getGroupIndex())
                vrvPositions.scrollToPosition(usedGroups.selectedIdx)
            } else mPosAdapter.notifyDataSetChanged()
        }

        vtvG.text = if (group == 0) "A" else group.toString()
        if (memb > membMax[group])
            memb = 1
        vtvE.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
        pr.model.getSavedTimer(group, memb)
        mMenu?.findItem(R.id.action_editShutterName)?.isEnabled = (group != 0 && memb != 0)
        enableFerId(false)
    }

    fun selectNextMember() {
        selectMember((memb + 1) % (membMax[group] + 1))
    }

    fun selectMember(m: Int, tag: Any?) {
        if (tag is Int) {
            selectGroup(tag)
        }
        selectMember(m)
    }

    fun selectMember(m: Int) {
        memb = m
        vtvE.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
        mMenu?.findItem(R.id.action_editShutterName)?.isEnabled = (group != 0 && memb != 0)
        pr.model.getSavedTimer(group, memb)

        val colorNormal = ContextCompat.getColor(this, R.color.background_material_light)
        val colorSelected = ContextCompat.getColor(this, R.color.colorAccent)

        for (i in 0 until membMax[group]) {
            ptvArr[i].setBackgroundColor(if (m == 0 || i == (m - 1)) colorSelected else colorNormal)
        }
        mPosAdapter.notifyItemChanged(getGroupIndex())
        vrvPositions.scrollToPosition(getGroupIndex())
        enableFerId(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_WEEKLY_EDITOR -> {
                    val result = data?.getStringExtra("message_return") ?: ""
                    vetWeeklyTimer.setText(result)
                }
            }
        }
    }

    internal fun showAlertDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { _, _ -> alertDialog.hide() }

        alertDialog = builder.create()
        alertDialog.show()
    }

    private fun showProgressDialog(msg: String, time_out: Int) {

        progressDialog.setMessage(msg)
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
                        sleep(1000)
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
                            sleep(1000)
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

        vbtTimerSend.isEnabled = enable
        vbtUp.isEnabled = enable
        vbtStop.isEnabled = enable
        vbtSunPos.isEnabled = enable
        vbtDown.isEnabled = enable

        mMenu?.findItem(R.id.action_cuAutoSet)?.isEnabled = enable
        mMenu?.findItem(R.id.action_setFunc)?.isEnabled = enable


    }

    private fun enableFerId(enable: Boolean): Boolean {

        val stateHasChanged = (vetFerId.visibility == View.VISIBLE) == enable

        mMenu?.findItem(R.id.action_motorCode)?.isChecked = enable

        vetFerId.isEnabled = enable
        vetFerId.visibility = if (enable) View.VISIBLE else View.INVISIBLE
        vtvG.isEnabled = !enable
        vtvE.isEnabled = !enable

        vtvG.visibility = if (!enable) View.VISIBLE else View.INVISIBLE
        vtvE.visibility = if (!enable) View.VISIBLE else View.INVISIBLE

        return stateHasChanged
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_hideLog).isChecked = mHideViewLog
        menu.findItem(R.id.action_hideTimer).isChecked = mHideViewTimer
        mMenu = menu
        return true
    }

    fun editShutterNameDialog() {
        if (group == 0 || memb == 0)
            return
        val builder = AlertDialog.Builder(this)
         builder.setTitle(getString(R.string.edname_title, memb, group))
// Set up the input
        val input = EditText(this)
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(getMemberName())
        builder.setView(input)


// Set up the buttons
        builder.setPositiveButton("OK") { dialog, which ->
            val shutterName = input.text.toString()
            ptvArr[memb - 1].text = if (shutterName.isNotBlank()) shutterName else "$memb"
            mMemberNames["memberName_$group$memb"] = shutterName
            mPosAdapter.notifyItemChanged(getGroupIndex())

        }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        builder.show()
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
                pr.data2Mcu(TfmcuMcuSettings(cu="auto"))
                showProgressDialog(getString(R.string.cuas_info), 60)
            }

            R.id.action_setFunc -> {
                val g = getSelectedGroup()
                val m = getSelectedMember()
                pr.data2Mcu(TfmcuSendData(a = getFerId(), g = g, m = m, cmd = TfmcuSendData.CMD_SET))
                showAlertDialog(getString(R.string.setf_info))
            }

            R.id.action_setEndPos -> {
                item.isChecked = !item.isChecked
                switchVisibility(if (item.isChecked) VIS_SEP else VIS_NORMAL)
                mode = if (item.isChecked) MODE_SEP else MODE_NORMAL
            }

            R.id.action_sendRtc -> {
                sendRtc()
            }

            R.id.action_broadcastRtc -> {
                sendRtc(true)
            }

            R.id.action_motorCode -> {
                enableFerId(!item.isChecked)
            }

            R.id.action_editShutterName -> {
                editShutterNameDialog()
            }

            R.id.action_hideLog -> {
                item.isChecked = !item.isChecked
                mHideViewLog = item.isChecked
                vtvLog.visibility = if (mHideViewLog) View.GONE else View.VISIBLE
            }

            R.id.action_hideTimer -> {
                item.isChecked = !item.isChecked
                mHideViewTimer = item.isChecked
                val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                timer_frameLayout.visibility = if (mHideViewTimer) View.GONE else View.VISIBLE
                //timer_frameLayout.visibility = if (mHideViewTimer) (if (landscape) View.INVISIBLE else View.GONE) else View.VISIBLE
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    companion object {


        internal const val MODE_NORMAL = 0
        internal const val MODE_SEP = 1

        internal const val VIS_NORMAL = 0
        internal const val VIS_SEP = 1
        //internal const val VIS_XXX = 2

        internal const val MSG_CUAS_TIME_OUT = 3
        internal const val MSG_SEND_ENABLE = 4
        internal const val MSG_ERROR = 6

        internal const val def_dailyUp = "07:30"
        internal const val def_dailyDown = "19:30"
        internal const val def_weekly = "0700-++++0900-+"
        internal const val def_astro = "0"

        var mcuConfig_changed = false
        internal const val REQ_WEEKLY_EDITOR = 1
    }


}
