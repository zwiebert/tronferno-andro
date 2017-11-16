package bertw.tronferno

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.regex.Pattern

const val DEFAULT_TCP_HOSTNAME = "fernotron.fritz.box"

class MainActivity : AppCompatActivity() {


    internal var tcpHostname = ""
    internal var tcpPort = 0
    private val use_wifi = true
    private val mMessageHandler = MessageHandler(this)

    internal lateinit var view_checkBox_daily_up: CheckBox
    internal lateinit var view_checkBox_daily_down: CheckBox
    internal lateinit var view_checkBox_weekly: CheckBox
    internal lateinit var view_checkBox_astro: CheckBox
    internal lateinit var view_checkBox_random: CheckBox
    internal lateinit var view_checkBox_sun_auto: CheckBox
    internal lateinit var view_checkBox_rtc_only: CheckBox
    internal lateinit var view_checkBox_ferID: CheckBox
    internal lateinit var view_textView_log: TextView
    internal lateinit var view_textView_g: TextView
    internal lateinit var view_textView_e: TextView
    internal lateinit var view_editText_dailyUpTime: EditText
    internal lateinit var view_editText_dailyDownTime: EditText
    internal lateinit var view_editText_weeklyTimer: EditText
    internal lateinit var view_editText_astroMinuteOffset: EditText
    internal lateinit var view_editText_ferID: EditText


    ///////////// wifi //////////////////////
    private var mTcpSocket = Socket()
    private var socketAddress: SocketAddress? = null
    private lateinit var tcpWrite_Thread: Thread
    private lateinit var tcpRead_Thread: Thread
    private val q = ArrayBlockingQueue<String>(1000)

    internal val onCheckedChanged = CompoundButton.OnCheckedChangeListener { button, isChecked ->
        when (button.id) {

            R.id.checkBox_rtc_only -> {

                view_checkBox_daily_up.isEnabled = !isChecked
                view_checkBox_daily_down.isEnabled = !isChecked
                view_checkBox_weekly.isEnabled = !isChecked
                view_checkBox_astro.isEnabled = !isChecked
                view_editText_dailyUpTime.isEnabled = !isChecked && view_checkBox_daily_up.isChecked
                view_editText_dailyDownTime.isEnabled = !isChecked && view_checkBox_daily_down.isChecked
                view_editText_weeklyTimer.isEnabled = !isChecked && view_checkBox_weekly.isChecked
                view_checkBox_random.isEnabled = !isChecked
                view_checkBox_sun_auto.isEnabled = !isChecked
            }

            R.id.checkBox_daily_up -> {
                view_editText_dailyUpTime.isEnabled = isChecked
                if (!isChecked) view_editText_dailyUpTime.setText("")
            }

            R.id.checkBox_daily_down -> {
                view_editText_dailyDownTime.isEnabled = isChecked
                if (!isChecked) view_editText_dailyDownTime.setText("")
            }

            R.id.checkBox_weekly -> {
                view_editText_weeklyTimer.isEnabled = isChecked
                if (!isChecked) view_editText_weeklyTimer.setText("")
            }

            R.id.checkBox_astro -> {
                view_editText_astroMinuteOffset.isEnabled = isChecked
                if (!isChecked) view_editText_astroMinuteOffset.setText("")
            }

            R.id.checkBox_ferID -> {
                view_editText_ferID.isEnabled = isChecked
                view_textView_g.isEnabled = !isChecked
                view_textView_e.isEnabled = !isChecked
            }
        }
    }


    internal var group = 0
    internal var memb = 0
    internal var group_max = 0
    internal val memb_max = intArrayOf(0,0,0,0,0,0,0,0);

    internal var wait_for_saved_timer = false

    internal lateinit var alertDialog: AlertDialog
    internal lateinit var progressDialog: ProgressDialog

    internal var cuasInProgress = false


    internal lateinit var mMenu: Menu

    private fun LoadPreferences() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)


        tcpHostname = sharedPreferences.getString("tcpHostName", DEFAULT_TCP_HOSTNAME)
        if (tcpHostname.contains(":")) {
            val pos = tcpHostname.indexOf(':')
            tcpPort = Integer.parseInt(tcpHostname.substring(pos + 1))
            tcpHostname = tcpHostname.substring(0, pos)
        } else {
            tcpPort = 7777
        }

        val sgam = sharedPreferences.getString("groupsAndMembers", "77777777")
        val sgam_length_1 = Math.min(7, sgam!!.length - 1)
        group_max = Math.min(7, Integer.parseInt(sgam.substring(0, 1)))

        for (i in 1..sgam_length_1) {
            memb_max[i] = Math.min(7, Integer.parseInt(sgam.substring(i, i + 1)))
        }
        for (i in sgam_length_1 + 1..7) {
            memb_max[i] = 0
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        if (android.os.Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        LoadPreferences()

        view_checkBox_daily_up = findViewById(R.id.checkBox_daily_up) as CheckBox
        view_checkBox_daily_down = findViewById(R.id.checkBox_daily_down) as CheckBox
        view_checkBox_weekly = findViewById(R.id.checkBox_weekly) as CheckBox
        view_checkBox_astro = findViewById(R.id.checkBox_astro) as CheckBox
        view_checkBox_random = findViewById(R.id.checkBox_random) as CheckBox
        view_checkBox_sun_auto = findViewById(R.id.checkBox_sun_auto) as CheckBox
        view_checkBox_rtc_only = findViewById(R.id.checkBox_rtc_only) as CheckBox
        view_checkBox_ferID = findViewById(R.id.checkBox_ferID) as CheckBox

        view_editText_dailyUpTime = findViewById(R.id.editText_dailyUpTime) as EditText
        view_editText_dailyDownTime = findViewById(R.id.editText_dailyDownTime) as EditText
        view_editText_weeklyTimer = findViewById(R.id.editText_weeklyTimer) as EditText
        view_editText_astroMinuteOffset = findViewById(R.id.editText_astroMinuteOffset) as EditText
        view_editText_ferID = findViewById(R.id.editText_ferID) as EditText

        view_textView_log = findViewById(R.id.textView_log) as TextView
        view_textView_g = findViewById(R.id.textView_g) as TextView
        view_textView_e = findViewById(R.id.textView_e) as TextView

        view_checkBox_daily_up.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_daily_down.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_weekly.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_astro.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_random.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_sun_auto.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_rtc_only.setOnCheckedChangeListener(onCheckedChanged)
        view_checkBox_ferID.setOnCheckedChangeListener(onCheckedChanged)

        view_editText_dailyUpTime.setText("")
        view_editText_dailyDownTime.setText("")
        view_editText_weeklyTimer.setText("")
        view_editText_astroMinuteOffset.setText("")

        progressDialog = ProgressDialog(this)

        tcpWrite_Thread = object : Thread() {
                //private Socket mTcpSocket;
                override fun run() {
                    while (!mTcpSocket.isClosed) {
                        try {
                            val data = q.take()

                            var i = 0
                            val retry = 10
                            while (true) {
                                try {
                                    mTcpSocket.getOutputStream().write(data.toByteArray())
                                    break
                                } catch (e: IOException) {
                                    if (i < retry) {
                                        reconnect_tcpSocket()
                                    } else {
                                        mMessageHandler.obtainMessage(0, e.toString().toByteArray().toString() + "\n").sendToTarget()
                                        break
                                    }
                                }

                                ++i
                            }

                        } catch (e: InterruptedException) {
                            return;
                            // e.printStackTrace();
                        }
                    }
                }
            }


        tcpRead_Thread = object : Thread() {
            internal var buf = ByteArray(256)

            override fun run() {
                try {
                    val br = BufferedReader(InputStreamReader(mTcpSocket.getInputStream()))
                     while (!mTcpSocket.isClosed) {
                            try {
                                val line = br.readLine()
                                mMessageHandler.obtainMessage(MSG_LINE_RECEIVED, line).sendToTarget()
                            } catch (e: IOException) {
                                // reconnect_tcpSocket();
                            }

                        }
                } catch (e: IOException) {
                    return;
                }
            }
        }


    }


    private fun start_tcpRead_Thread() {
        if (tcpRead_Thread.state == Thread.State.NEW || tcpRead_Thread.state == Thread.State.TERMINATED) {
            tcpRead_Thread.start()
        }
    }


    internal fun tcpSocket_transmit(s: String) {
        if (tcpWrite_Thread.state == Thread.State.NEW || tcpWrite_Thread.state == Thread.State.TERMINATED) {
            tcpWrite_Thread.start()
        }
        q.add(s)
    }

    internal fun reconnect_tcpSocket(): Boolean {
        try {
            mTcpSocket.connect(socketAddress)
            return true
        } catch (e: IOException) {
        }

        return false
    }

    internal fun connect_tcpSocket(): Boolean {
        try {
            if (mTcpSocket.isClosed) {
                mTcpSocket = Socket()
            }
            socketAddress = InetSocketAddress(tcpHostname, tcpPort)
            mTcpSocket.connect(socketAddress)
            return mTcpSocket.isConnected
        } catch (e: IOException) {
            view_textView_log.append("TCP-Error: " + e.toString() + "\n")
        } catch (e: NullPointerException) {
            view_textView_log.append("TCP-Error: cannot connect to tcp server\n")
        }

        return false
    }

    internal fun start_tcp() {
        connect_tcpSocket()
        start_tcpRead_Thread()
    }

    internal fun stop_tcp() {
        try {
            mTcpSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

      override fun onResume() {
        super.onResume()

        if (use_wifi) {
            start_tcp()

        }
    }

    public override fun onPause() {
        super.onPause()

        if (use_wifi)
            stop_tcp()
    }


    private class MessageHandler(activity: MainActivity) : Handler() {
        private val mActivity: WeakReference<MainActivity>

        init {
            mActivity = WeakReference(activity)
        }

        override fun handleMessage(msg: Message) {
            val ma = mActivity.get()
            if (ma == null)
                return;
            when (msg.what) {

                MainActivity.MSG_LINE_RECEIVED -> try {
                    val s = msg.obj as String
                    ma.view_textView_log.append(s + "\n")
                    if (s.contains("rs=data")) {
                        ma.parse_received_data(s)
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
                    ma.view_textView_log.text = "error: " + e.toString()

                }

                MainActivity.MSG_CUAS_TIME_OUT -> if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                    ma.cuasInProgress = false
                    ma.progressDialog.hide()
                    ma.showAlertDialog("Time-Out. Please try again.")
                }


                MainActivity.MSG_SEND_ENABLE -> ma.enableSend(true, 0)
            }
        }
    }

    @Throws(IOException::class)
    private fun transmit(s: String) {
        if (use_wifi) tcpSocket_transmit(s)
        view_textView_log.append("transmit: " + s + "\n")
    }

    fun onCheckedClick(view: View) {
        val isChecked = (view as CheckBox).isChecked


        if (isChecked)
            when (view.getId()) {
                R.id.checkBox_daily_up -> view_editText_dailyUpTime.setText(def_dailyUp)

                R.id.checkBox_daily_down -> view_editText_dailyDownTime.setText(def_dailyDown)

                R.id.checkBox_weekly -> view_editText_weeklyTimer.setText(def_weekly)

                R.id.checkBox_astro -> view_editText_astroMinuteOffset.setText(def_astro)
            }

    }

    @Throws(IOException::class)
    private fun fer_send_time() {
        val c = Calendar.getInstance()

        val sd = SimpleDateFormat("yyyy-MM-dd").format(c.time)
        val st = SimpleDateFormat("HH:mm:ss").format(c.time)


        val cmd = String.format("config rtc=%sT%s;", sd, st)
        view_textView_log.append(cmd + "\n")
        transmit(cmd)

    }

    @Throws(java.io.IOException::class)
    internal fun get_saved_timer(g: Int, m: Int) {
        transmit(String.format("timer mid=%d g=%d m=%d rs=2;", getMsgid(), g, m))
        wait_for_saved_timer = true
    }

    internal fun parse_received_data(s: String) {
        var s = s;
        try {
            s = s.substring(s.indexOf(":rs=data: "))

            //     tvRec.append(String.format("###%s###\n", s));

            var g = 0
            var m = 0
            var sun_auto = 0
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
                        //        tvRec.append(String.format("##%s##%s\n", key, val));

                        if (key == "g") {
                            g = value.toInt()

                        } else if (key == "m") {
                            m = value.toInt()

                        } else if (key == "sun-auto") {
                            sun_auto = value.toInt()

                        } else if (key == "random") {
                            random = value.toInt()

                        } else if (key == "astro") {
                            hasAstro = true
                            astro = value.toInt()

                        } else if (key == "daily") {
                            daily = value

                        } else if (key == "weekly") {
                            weekly = value

                        }

                    }
                }


            }

            view_checkBox_sun_auto.isChecked = sun_auto == 1
            view_checkBox_random.isChecked = random == 1
            view_checkBox_weekly.isChecked = !weekly.isEmpty()
            view_checkBox_astro.isChecked = hasAstro
            view_editText_weeklyTimer.setText(weekly)
            view_checkBox_daily_up.isChecked = !(daily.isEmpty() || daily.startsWith("-"))
            view_checkBox_daily_down.isChecked = !(daily.isEmpty() || daily.endsWith("-"))

            view_editText_astroMinuteOffset.setText(if (hasAstro) astro.toString() else "")

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

            view_editText_dailyUpTime.setText(dailyUp)
            view_editText_dailyDownTime.setText(dailyDown)

        } catch (e: Exception) {
        }

    }

    internal fun getMsgid(): Int {
        return ++msgid
    }

    private fun getFerId(): Int {
        if (view_checkBox_ferID.isChecked) {
            var s = view_editText_ferID.text.toString()
            if (s.length == 5) {
                s = "9$s";
            } else if (s.length != 6) {
                throw Exception("id must have 6 digits ($s)")
            }
            val result = Integer.parseInt(s, 16);
            return result;
        }
        return 0;
    }

    fun onClick(view: View) {

        try {
            when (view.id) {
                R.id.button_stop -> transmit(String.format(sendFmt, getMsgid(), getFerId(), group, memb, "stop"))
                R.id.button_up -> transmit(String.format(sendFmt, getMsgid(), getFerId(), group, memb, "up"))
                R.id.button_down -> transmit(String.format(sendFmt, getMsgid(), getFerId(), group, memb, "down"))
                R.id.button_g -> {
                    group = ++group % (group_max + 1)
                    view_textView_g.text = if (group == 0) "A" else group.toString()
                    if (memb > memb_max[group])
                        memb = 1
                    view_textView_e.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
                    get_saved_timer(group, memb)
                }
                R.id.button_e -> {
                    memb = ++memb % (memb_max[group] + 1)
                    view_textView_e.text = if (group == 0) "" else if (memb == 0) "A" else memb.toString()
                    get_saved_timer(group, memb)
                }
                R.id.button_sun_pos -> transmit(String.format(sendFmt, getMsgid(), getFerId(), group, memb, "sun-down"))

                R.id.button_timer -> {
                    val upTime = view_editText_dailyUpTime.text.toString()
                    val downTime = view_editText_dailyDownTime.text.toString()
                    val astroOffset = view_editText_astroMinuteOffset.text.toString()

                    var timer = ""

                    val rtc_only = view_checkBox_rtc_only.isChecked;

                    if (rtc_only) {
                        timer += " rtc-only=1"
                    } else {
                        val dailyUpChecked = view_checkBox_daily_up.isChecked
                        val dailyDownChecked = view_checkBox_daily_down.isChecked
                        val weeklyChecked = view_checkBox_weekly.isChecked

                        if (dailyUpChecked || dailyDownChecked) {
                            timer += " daily="
                            timer += if (dailyUpChecked) upTime.substring(0, 2) + upTime.substring(3, 5) else "-"
                            timer += if (dailyDownChecked) downTime.substring(0, 2) + downTime.substring(3, 5) else "-"
                        }

                        if (view_checkBox_astro.isChecked) {
                            timer += " astro="
                            timer += astroOffset
                        }

                        if (weeklyChecked) {
                            val weeklyTimer = view_editText_weeklyTimer.text.toString()

                            timer += " weekly="
                            timer += weeklyTimer
                        }

                    }


                    if (view_checkBox_sun_auto.isChecked) {
                        timer += " sun-auto=1"
                    }

                    if (view_checkBox_random.isChecked) {
                        timer += " random=1"
                    }


                    // timer = upTime.substring(0,2);


                    transmit(String.format(timerFmt, getMsgid(), getFerId(), group, memb, timer))
                    if (!rtc_only) {
                        enableSend(false, 5)
                    }
                }
            }


        } catch (e: Exception) {
            view_textView_log.append("Error: " + e.toString() + "...\n")
        }

    }

    internal fun showAlertDialog(msg: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(msg)

        builder.setPositiveButton("OK") { dialog, id -> alertDialog.hide() }

        alertDialog = builder.create()
        alertDialog.show()
    }

    internal fun showProgressDialog(msg: String, time_out: Int) {

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

    internal fun enableSend(enable: Boolean, timeout: Int) {

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

        findViewById(R.id.button_timer).isEnabled = enable
        findViewById(R.id.button_up).isEnabled = enable
        findViewById(R.id.button_stop).isEnabled = enable
        findViewById(R.id.button_sun_pos).isEnabled = enable
        findViewById(R.id.button_down).isEnabled = enable
        mMenu.findItem(R.id.action_cuAutoSet).isEnabled = enable
        mMenu.findItem(R.id.action_setFunc).isEnabled = enable

    }

    fun onMenuClick(mi: MenuItem) {

        try {
            when (mi.itemId) {
                R.id.action_cuAutoSet -> {
                    transmit(String.format(configFmt, getMsgid(), "cu=auto"))
                    showProgressDialog("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...", 60)
                }

                R.id.action_setFunc -> {
                    transmit(String.format(sendFmt, getMsgid(), 0, group, memb, "set"))
                    showAlertDialog("You now have 60 seconds remaining to press STOP on the transmitter you want to add/remove. Beware: If you press STOP on the central unit, the device will be removed from it. To add it again, you would need the code. If you don't have the code, then you would have to press the physical set-button on the device")
                }
            }

        } catch (e: IOException) {
            view_textView_log.text = e.toString()
        }

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
        val id = item.itemId


        if (id == R.id.action_settings) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        internal var msgid = 1
        internal const val MSG_DATA_RECEIVED = 0
        internal const val MSG_CUAS_TIME_OUT = 3
        internal const val MSG_SEND_ENABLE = 4
        internal const val MSG_LINE_RECEIVED = 5
        internal const val def_dailyUp = "07:30"
        internal const val def_dailyDown = "19:30"
        internal const val def_weekly = "0700-++++0900-+"
        internal const val def_astro = "0"
        internal const val sendFmt = "send mid=%d a=%x g=%d m=%d c=%s;"
        internal const val timerFmt = "timer mid=%d a=%x g=%d m=%d%s;"
        internal const val configFmt = "config mid=%d %s;"
        internal const val timeFormat = "%4d-%02d-%02dT%02d:%02d:%02d"
    }


}
