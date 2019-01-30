package de.bertw.tronferno

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
import android.widget.CheckBox
import android.widget.CompoundButton
import kotlinx.android.synthetic.main.content_main.*
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

    private fun switchVisibility(mode: Int = VIS_NORMAL) {
        val iv = View.INVISIBLE
        val vi = View.VISIBLE

        when (mode) {
            VIS_NORMAL -> {
                vcbDailyUp.visibility = vi
                vcbDailyDown.visibility = vi
                vcbWeekly.visibility = vi
                vcbAstro.visibility = vi
                vcbRandom.visibility = vi
                vcbSunAuto.visibility = vi

                vtvLog.visibility = vi
                vtvG.visibility = vi
                vtvE.visibility = vi

                vetDailyUpTime.visibility = vi
                vetDailyDownTime.visibility = vi
                vetWeeklyTimer.visibility = vi
                vetAstroMinuteOffset.visibility = vi
               // vetFerId.visibility = vi

                vbtUp.visibility = vi
                vbtDown.visibility = vi
                vbtStop.visibility = vi
               // vbtG.visibility = vi
               // vbtE.visibility = vi
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

                vtvLog.visibility = vi
               // vtvG.visibility = vi
               // vtvE.visibility = vi

                vetDailyUpTime.visibility = iv
                vetDailyDownTime.visibility = iv
                vetWeeklyTimer.visibility = iv
                vetAstroMinuteOffset.visibility = iv
               // vetFerId.visibility = vi

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


    private var group = 0
    private var memb = 0
    private var groupMax = 0
    private val membMax = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    private val membMap = arrayOf(
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false),
            booleanArrayOf(false, false, false, false, false, false, false, false))


    private lateinit var alertDialog: AlertDialog
    private lateinit var progressDialog: ProgressDialog
    private var cuasInProgress = false
    private var mMenu: Menu? = null


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
        val sgamLength1 = Math.min(7, sgam!!.length - 1)
        groupMax = Math.min(7, Integer.parseInt(sgam.substring(0, 1)))

        for (i in 1..sgamLength1) {
            membMax[i] = Math.min(7, Integer.parseInt(sgam.substring(i, i + 1)))
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
//                .isChecked = pref.getBoolean("IsChecked", false)


        vetFerId.setText(pref.getString("vetFerIdText", "90ABCD"))
        vetDailyUpTime.setText(pref.getString("vetDailyUpTimeText", ""))
        vetDailyDownTime.setText(pref.getString("vetDailyDownTimeText", ""))
        vetWeeklyTimer.setText(pref.getString("vetWeeklyTimerText", ""))
        vetAstroMinuteOffset.setText(pref.getString("vetAstroMinuteOffsetText", ""))

        vtvLog.text = pref.getString("vtvLogText", "")

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

//        ed.putBoolean("IsChecked", .isChecked)


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

        ed.apply()
    }


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

        vtvG.text = if (group != 0) group.toString() else "A"
        vtvE.text = if (memb != 0) memb.toString() else if (group != 0) "A" else ""


        progressDialog = ProgressDialog(this)


    }


    override fun onResume() {
        super.onResume()
        //  vtvLog.append(String.format("read alive: %b, wa: %b, ca: %b\n", tcpReadThread.isAlive, tcpWriteThread.isAlive, tcpConnectThread.isAlive))

        enableSendButtons(false, 0)
        pr.onResume()
        showShutterPositions()
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
                    ma.pr.reset()
                }


                McuTcp.MSG_TCP_OUTPUT_ERROR, McuTcp.MSG_TCP_INPUT_ERROR -> {
                    ma.pr.reset()
                }

                McuTcp.MSG_TCP_CONNECTED -> {
                    ma.enableSendButtons(true, 0)
                    ma.vtvLog.append("tcp connected\n")

                    if (mcuConfig_changed) {
                        mcuConfig_changed = false
                        ma.configureMcu()
                    }

                   // ma.pr.data2Mcu(TfmcuConfigData("longitude=? latitude=? time-zone=? dst=? wlan-ssid=? cu=? baud=? verbose=? dst=?"))
                    ma.pr.data2Mcu(TfmcuConfigData("longitude=? latitude=? tz=? wlan-ssid=?"))
                    ma.pr.data2Mcu(TfmcuConfigData("cu=? baud=? verbose=?"))
                }

                McuTcp.MSG_TCP_CONNECTION_FAILED -> {
                    s = msg.obj as String
                    ma.vtvLog.append("tcp connection failed: $s\n")
                }

                McuTcp.MSG_LINE_RECEIVED -> try {
                    s = msg.obj as String
                    when {
                        s == "ready:" -> {

                        }
                        s.isEmpty() -> {

                        }
                        else -> ma.vtvLog.append(s + "\n")
                    }

                    ma.pr.model.messagePending = 0  // FIXME: check msgid?

                    if (s.contains("rs=data")) {
                        ma.parseReceivedTimer(s)
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
                    if (s.startsWith("config ")) {
                        ma.parseReceivedConfig(s)
                    }
                    if (s.startsWith("A:position:")) {
                        ma.parseReceivedPosition(s)
                    }
                    if (s.startsWith("U:position:")) {
                        ma.parseReceivedPosition(s)
                    }

                } catch (e: Exception) {
                    ma.vtvLog.append("MLR:error: $e\n...line: $s")

                }

                MainActivity.MSG_CUAS_TIME_OUT -> if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                    ma.cuasInProgress = false
                    ma.progressDialog.hide()
                    ma.showAlertDialog("Time-Out. Please try again.")
                }


                MainActivity.MSG_SEND_ENABLE -> ma.enableSendButtons(true, 0)

                MainActivity.MSG_ERROR -> {
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

    internal fun parseReceivedTimer(s: String) {
        val td = pr.model.parseReceivedTimer(s)

        vcbSunAuto.isChecked = td.sunAuto
        vcbRandom.isChecked = td.random
        vcbWeekly.isChecked = !td.weekly.isEmpty()
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
        pr.td.a = getFerId()
        pr.td.g = group
        pr.td.m = memb

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



        pr.td.sunAuto = vcbSunAuto.isChecked
        pr.td.random = vcbRandom.isChecked

        if (pr.data2Mcu(pr.td)) {
            enableSendButtons(false, 5)
        }
    }

    private fun saveMcuPreferecence () {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val ed = sharedPreferences.edit()


       for(i in 0 until mcuCfgPrefKeys.size) {
           ed.putString(mcuCfgPrefKeys[i], mcuCfgPrefVals[i])
           ed.putString(mcuCfgPrefKeys[i] + "_old", mcuCfgPrefVals[i])
       }

        ed.apply()

    }

    private val mcuCfgPrefKeys = arrayOf("geo_latitude", "geo_longitude", "geo_time_zone", "wlan_ssid", "cu_id", "serial_baud", "cli_verbosity")
    private val mcuCfgMcuKeys = arrayOf("latitude", "longitude", "tz", "wlan-ssid", "cu", "baud", "verbose")
    private var mcuCfgPrefVals = arrayOf("","","","","","", "")


    fun configureMcu() {
        val pref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        for(i in 0 until mcuCfgPrefKeys.size) {
            val pv = pref.getString(mcuCfgPrefKeys[i], "")
            val pvOld = pref.getString(mcuCfgPrefKeys[i] + "_old", "")
            val mk = mcuCfgMcuKeys[i]

            if (pv != pvOld) {
                pr.data2Mcu(TfmcuConfigData("$mk=$pv"))
            }
        }

    }

    fun parseReceivedConfig(line: String) {

        if (line.startsWith("config ")) {

            var s = line.substringAfter("config ")
            while (s.contains('=')) {
                val k = s.substringBefore('=')
                val v = s.substringAfter('=').substringBefore(';').substringBefore(' ')
                s = s.substringAfter(' ', ";")

               // listAdapter.add(k + "=" + v)
                for(i in 0 until mcuCfgPrefKeys.size) {
                    if (mcuCfgMcuKeys[i] == k) {
                        mcuCfgPrefVals[i] = v
                    }
                }

               // vtvLog.append("key: " + k + "\nval: " + v + "\n")

            }
            saveMcuPreferecence()
            vtvLog.append("mcu preference saved\n")
        }
    }

    private fun showShutterPositions() {
        val positions =  pr.model.showPos(group, memb_count = membMax[group], format = 1)
        vetShutterPos.setText(if (positions.isEmpty()) "" else "Pos-G$group: $positions" )
    }

    // position code
    fun parseReceivedPosition(line: String) {
        if (pr.model.parseReceivedPosition(line)) {
            showShutterPositions()
        }
    }



    private fun getFerId(): Int {
        var result = 0
        if (vetFerId.visibility == View.VISIBLE) {
            var s = vetFerId.text.toString()
            if (s.length == 5) {
                s = "9$s"
            } else if (s.length != 6) {
                throw Exception("id must have 6 digits ($s)")
            }
            result = Integer.parseInt(s, 16)

        }
        return result
    }

    fun onClick(view: View) {
        val cd = TfmcuSendData(a=getFerId(), g = group, m = memb, sep = (mode == MODE_SEP) )
        try {
           // vtvLog.append(String.format("ra: %b, wa: %b, ca: %b\n", tcpReadThread.isAlive, tcpWriteThread.isAlive, tcpConnectThread.isAlive))

            when (view.id) {
                R.id.vbtStop -> { cd.cmd =  TfmcuSendData.CMD_STOP; pr.data2Mcu(cd) }
                R.id.vbtUp -> { cd.cmd =  TfmcuSendData.CMD_UP; pr.data2Mcu(cd) }
                R.id.vbtDown -> { cd.cmd =  TfmcuSendData.CMD_DOWN; pr.data2Mcu(cd) }

                R.id.vbtG -> if (enableFerId(false)) {
                        for (i in 0..7) {
                            group = ++group % 8
                            if (group == 0 || membMax[group] != 0) {
                                break
                            }
                        }

                        vtvG.text = if (group == 0) "A" else group.toString()
                        if (memb > membMax[group])
                            memb = 1
                        vtvE.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
                        pr.model.getSavedTimer(group, memb)

                }
                R.id.vbtE -> if (enableFerId(false)) {
                    memb = ++memb % (membMax[group] + 1)
                    vtvE.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
                    logWriteLine("getSavedTimer(g=$group, m=$memb)")
                    pr.model.getSavedTimer(group, memb)
                }

                R.id.vbtSunPos -> { cd.cmd =  TfmcuSendData.CMD_SUN_DOWN; pr.data2Mcu(cd) }

                R.id.vbtTimer -> {
                    sendTimer()

                }
            }


        } catch (e: Exception) {
            vtvLog.append("OCH:error: $e...\n")
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
        vbtTimer.isEnabled = enable
        vbtUp.isEnabled = enable
        vbtStop.isEnabled = enable
        vbtSunPos.isEnabled = enable
        vbtDown.isEnabled = enable

        mMenu?.findItem(R.id.action_cuAutoSet)?.isEnabled = enable
        mMenu?.findItem(R.id.action_setFunc)?.isEnabled = enable


    }

    private fun enableFerId(enable: Boolean) : Boolean {

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
                pr.data2Mcu(TfmcuConfigData("cu=auto"))
                showProgressDialog("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...", 60)
            }

            R.id.action_setFunc -> {
                pr.data2Mcu(TfmcuSendData(a = getFerId(), g = group, m = memb, cmd = TfmcuSendData.CMD_SET))
                showAlertDialog("You now have 60 seconds remaining to press STOP on the transmitter you want to add/remove. Beware: If you press STOP on the central unit, the device will be removed from it. To add it again, you would need the code. If you don't have the code, then you would have to press the physical set-button on the device")
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
    }


}
