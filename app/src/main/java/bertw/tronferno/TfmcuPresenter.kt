package bertw.tronferno

import android.os.Handler
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class TfmcuPresenter(msgHandler: Handler) {

    val model = TfmcuModel(msgHandler)

    var td = TfmcuTimerData()
    fun timerClear() {
        td = TfmcuTimerData()
    }

    fun timerSend() : Boolean {
        return model.transmitTimer(timer = td, mid = TfmcuModel.MSG_ID_AUTO)
    }

    fun configSend(config: String) : Boolean{
        model.tcp.transmit ("config $config;")
        return true; // FIXME
    }

    fun cmdSend(c: TfmcuSendData): Boolean {
        return model.transmitSend(c)
    }

    @Throws(IOException::class)
    fun rtcConfigSend() {
        val c = Calendar.getInstance()
        val sd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
        val st = SimpleDateFormat("HH:mm:ss", Locale.US).format(c.time)
        configSend("rtc=${sd}T$st")
    }

    fun onPause() {
        model.tcp.close()
    }
    fun onResume() {
        model.tcp.connect()
    }

    fun reset() {
        model.tcp.close()
    }


}