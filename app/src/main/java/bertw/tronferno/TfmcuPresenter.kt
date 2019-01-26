package bertw.tronferno

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class TfmcuPresenter (tfmcuModel: TfmcuModel) {
    val tfmcuModel = tfmcuModel

    var td = TfmcuTimerData()
    fun timerClear() {
        td = TfmcuTimerData()
    }

    fun timerSend() {
        tfmcuModel.transmitTimer(timer = td, mid = -1, g = td.g, m = td.m)
    }

    @Throws(IOException::class)
    fun rtcConfig() {
        val c = Calendar.getInstance()

        val sd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
        val st = SimpleDateFormat("HH:mm:ss", Locale.US).format(c.time)

        val cmd = String.format("config rtc=%sT%s;", sd, st)
        //vtvLog.append(cmd + "\n")
        //transmit(cmd)

    }
}