package de.bertw.tronferno

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Socket


class McuTcp(msgHandler: Handler) {


    private val mMessageHandler: Handler = msgHandler
    var mMsgThread = MessageThread(this)

    private fun sendMsgToGui(msg : Int, text : String? = null) {
        mMessageHandler.obtainMessage(msg, text).sendToTarget()
    }

    class MessageThread(val mcuTcp: McuTcp) : Thread() {
        var mHandler: MessageHandler? = null

        override fun run() {
            Looper.prepare()
            mHandler = MessageHandler(mcuTcp)
            Looper.loop()
        }
    }

    class MessageHandler(mcuTcp: McuTcp) : Handler() {
        private val mWrTcp = WeakReference(mcuTcp)
        private var mReadTickThread: ReadTickThread? = null
        private var bufferedReader: BufferedReader? = null
        private var outputStream: OutputStream? = null
        @Volatile
        var mTcpSocket = Socket()

        private inner class ReadTickThread : Thread() {
            var stop = false
            override fun run() {
                while (!stop) {
                    mWrTcp.get()?.sendMsgToTcp(MSG_TCP_DO_RECV)
                    sleep(50)
                }
            }
        }

        private fun startReadTickThread() {
            mReadTickThread?.stop = true
            mReadTickThread = ReadTickThread()
            mReadTickThread?.start()
        }

        private fun stopReadTickThread() {
            mReadTickThread?.stop = true
            mReadTickThread = null
        }

        private fun readLoop() {
            val mt = mWrTcp.get() ?: return
            val br = bufferedReader ?: return

            try {
                while (br.ready()) {
                    val line = br.readLine()
                    if (line == null) {
                        mt.sendMsgToGui(MSG_TCP_INPUT_EOF, line)
                        return // EOF
                    }
                    mt.sendMsgToGui(MSG_LINE_RECEIVED, line)
                }
            } catch (e: java.lang.Exception) {

            }
        }

        private fun closeSocket() {
            mTcpSocket.close()
            mTcpSocket = Socket()
        }

        private fun connectSocket(): Boolean {
            val mt = mWrTcp.get() ?: return false

            closeSocket()
            try {
                //socketAddress = InetSocketAddress(ipAddr, ipPort)
                mTcpSocket.connect(socketAddress, 5 * 1000)
            } catch (e: Exception) {
                mt.sendMsgToGui(MSG_TCP_CONNECTION_FAILED, e.toString())
                return false
            }

            if (!mTcpSocket.isConnected) {
                mt.sendMsgToGui(MSG_TCP_CONNECTION_FAILED, "")
                return false
            } else {
                mt.sendMsgToGui(MSG_TCP_CONNECTED)
            }

            bufferedReader = BufferedReader(InputStreamReader(mTcpSocket.getInputStream()))
            outputStream = mTcpSocket.getOutputStream()

            return true
        }

        override fun handleMessage(msg: Message) {
            val mt = mWrTcp.get() ?: return

            when (msg.what) {


                MSG_TCP_DO_CONNECT -> {
                    if (connectSocket())
                        startReadTickThread()
                    mt.isConnecting = false
                }

                MSG_TCP_DO_DISCONNECT -> {
                    stopReadTickThread()
                    closeSocket()
                }

                MSG_TCP_DO_SEND -> {
                    val data = msg.obj as String
                    val os = outputStream ?: return
                    try {
                        os.write(data.toByteArray())
                    } catch (e: Exception) {
                        mt.sendMsgToGui(MSG_TCP_OUTPUT_ERROR, "tcp-wt:error: $e")
                        mt.sendMsgToGui(MSG_TCP_REQ_RECONNECT)
                        return
                    }
                }

                MSG_TCP_DO_RECV -> {
                    readLoop()
                }

            }

        }
    }


    val isConnected: Boolean
        get() = mMsgThread.mHandler!!.mTcpSocket.isConnected

    @Volatile
    var isConnecting = false


    fun close() {
        sendMsgToTcp(MSG_TCP_DO_DISCONNECT)
    }

    fun transmit(s: String) {
        sendMsgToTcp(MSG_TCP_DO_SEND, s)
    }

    fun reconnect() {
        connect()
    }

    fun connect() {
        isConnecting = true
        sendMsgToTcp(MSG_TCP_DO_CONNECT)
    }

    private fun sendMsgToTcp(msg : Int, text : String? = null) {
        mMsgThread.mHandler?.obtainMessage(msg, text)?.sendToTarget()
    }

    init {
        mMsgThread.start()

    }


    companion object {
        var socketAddress: InetSocketAddress? = null

        internal const val MSG_LINE_RECEIVED = 5
        internal const val MSG_TCP_CONNECTED = 7
        internal const val MSG_TCP_CONNECTION_FAILED = 8
        internal const val MSG_TCP_INPUT_EOF = 9
        internal const val MSG_TCP_INPUT_ERROR = 10
        internal const val MSG_TCP_OUTPUT_ERROR = 11
        internal const val MSG_TCP_REQ_RECONNECT = 12

        internal const val MSG_TCP_DO_CONNECT = 13
        internal const val MSG_TCP_DO_DISCONNECT = 14
        internal const val MSG_TCP_DO_SEND = 15
        internal const val MSG_TCP_DO_RECV = 16

    }


}
