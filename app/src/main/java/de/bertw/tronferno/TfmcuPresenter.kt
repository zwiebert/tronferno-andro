package de.bertw.tronferno

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

    fun data2Mcu(timerData: TfmcuTimerData): Boolean {
        return model.transmitTimer(timer = timerData, mid = TfmcuModel.MSG_ID_AUTO)
    }

    fun data2Mcu(configData: TfmcuConfigData): Boolean {
        model.tcp.transmit(configData.toString())
        return true // FIXME
    }

    fun data2Mcu(configData: TfmcuMcuSettings): Boolean {
        model.tcp.transmit(configData.toString() + ";\n")
        return true // FIXME
    }

    fun data2Mcu(cmdData: TfmcuSendData): Boolean {
        return model.transmitSend(cmdData)
    }

    @Throws(IOException::class)
    fun rtcConfigSend() {
        val c = Calendar.getInstance()
        val sd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
        val st = SimpleDateFormat("HH:mm:ss", Locale.US).format(c.time)
        data2Mcu(TfmcuMcuSettings(rtc="${sd}T$st"))
    }

    fun onConnect() {
        model.getShutterPos()
    }

    fun onPause() {
        model.tcp.close()
    }

    fun onResume() {
        model.tcp.connect()
    }

    fun reset() {
        model.tcp.reconnect()
    }


}