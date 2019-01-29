package de.bertw.tronferno

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView

import kotlinx.android.synthetic.main.activity_mcu_config.*
import java.lang.ref.WeakReference

class McuConfigActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mcu_config)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        vtvLog = findViewById(R.id.textView_mcuCfgLog)
        vetLongitude = findViewById(R.id.editText_mcuLongitude)
        vetLatitude = findViewById(R.id.editText_mcuLatitude)
        lvMcuCfg = findViewById(R.id.listView_mcuConfig)

        listAdapter = ArrayAdapter<String>(this, R.layout.simplerow, stringList)

        lvMcuCfg.setAdapter(listAdapter)

    }

    override fun onResume() {
        super.onResume()
        tcp.connect()
        tcp.transmit("config longitude=? latitude=? time-zone=? dst=?;")
        tcp.transmit("config wlan-ssid=? baud=? cu=?;")
        tcp.transmit("config verbose=? rtc=?;")


    }

    public override fun onPause() {
        super.onPause()
        tcp.close()
    }
    private var stringList = ArrayList<String>();
    private lateinit var listAdapter : ArrayAdapter<String>

    private lateinit var vtvLog: TextView
    private lateinit var vetLongitude: EditText
    private lateinit var vetLatitude: EditText
    private lateinit var lvMcuCfg: ListView

    private val mMessageHandler = MessageHandler(this)
    private var tcp = McuTcp(mMessageHandler);

    fun parseReceivedData(line: String) {

        if (line.startsWith("config ")) {
            var s = line.substringAfter("config ")
            while (s.contains('=')) {
                val k = s.substringBefore('=')
                val v = s.substringAfter('=').substringBefore(';').substringBefore(' ')
                s = s.substringAfter(' ', ";")

                listAdapter.add(k + "=" + v)

                when (k) {
                    "longitude" -> vetLongitude.setText(v)
                    "latitude" -> vetLatitude.setText(v)

                }

                vtvLog.append("key: " + k + "\nval: " + v + "\n")

            }

        }
    }



    class MessageHandler(activity: McuConfigActivity) : Handler() {
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
                    //ma.messagePending = 0;  // FIXME: check msgid?

                    if (s.startsWith("config ")) {
                        ma.parseReceivedData(s)
                    }
                    /*
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
*/

                } catch (e: Exception) {
                    ma.vtvLog.append("MLR:error: " + e.toString() + "\n")

                }
/*
                MainActivity.MSG_CUAS_TIME_OUT -> if (ma.progressDialog.isShowing && ma.cuasInProgress) {
                    ma.cuasInProgress = false
                    ma.progressDialog.hide()
                    ma.showAlertDialog("Time-Out. Please try again.")
                }


                MainActivity.MSG_SEND_ENABLE -> ma.enableSendButtons(true, 0)
*/

                MainActivity.MSG_ERROR -> {
                    val s = msg.obj as String
                    ma.vtvLog.append(s + "\n")
                }
            }
        }
    }

}
