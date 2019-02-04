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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock


class McuTcp(msgHandler: Handler) {

    @Volatile
    private var mTcpSocket = Socket()
    @Volatile
    var bufferedReader: BufferedReader? = null
    @Volatile
    var outputStream: OutputStream? = null

    private val mMessageHandler: Handler = msgHandler
    private val q = ArrayBlockingQueue<String>(1000)

    private val connectLock = ReentrantLock()

    var mMsgThread = MessageThread(this)

    class MessageThread(val mcuTcp: McuTcp) : Thread() {
        var mHandler: MessageHandler? = null

        override fun run() {
            Looper.prepare()
            mHandler = MessageHandler(mcuTcp)
            Looper.loop()

            while (true) {
                mHandler?.readLoop()
                sleep(100)
            }
        }


    }

    class MessageHandler(mcuTcp: McuTcp) : Handler() {
        private val mWrTcp = WeakReference(mcuTcp)
        var mHandler: MainActivity.MessageHandler? = null

        fun readLoop() {
            val mt = mWrTcp.get() ?: return
            val br = mt.bufferedReader ?: return

            try {
                while (br.ready()) {
                    val line = br.readLine()
                    if (line == null) {
                        mt.mMessageHandler.obtainMessage(MSG_TCP_INPUT_EOF, line).sendToTarget()
                        return // EOF
                    }
                    mt.mMessageHandler.obtainMessage(MSG_LINE_RECEIVED, line).sendToTarget()
                }
            } catch (e: java.lang.Exception) {

            }
        }

        override fun handleMessage(msg: Message) {
            val mt = mWrTcp.get() ?: return

            when (msg.what) {


                McuTcp.MSG_TCP_DO_CONNECT -> {
                    mt.connectSocket()
                }

                McuTcp.MSG_TCP_DO_DISCONNECT -> {
                    mt.closeSocket()
                }

                McuTcp.MSG_TCP_DO_SEND -> {
                    val data = msg.obj as String
                    val os = mt.outputStream ?: return
                    try {
                        os.write(data.toByteArray())
                        mt.mMessageHandler.sendMessageDelayed(mt.mMessageHandler.obtainMessage(MSG_TCP_DO_RECV), 100)
                    } catch (e: Exception) {
                        mt.mMessageHandler.obtainMessage(McuTcp.MSG_TCP_OUTPUT_ERROR, "tcp-wt:error: $e").sendToTarget()
                        mt.mMessageHandler.obtainMessage(McuTcp.MSG_TCP_REQ_RECONNECT, "").sendToTarget()
                        return
                    }
                }

                McuTcp.MSG_TCP_DO_RECV -> {
                    //FIXME: do ping/pong for now because sending a delayed message directly to ourself seems not to work
                    mt.mMessageHandler.obtainMessage(McuTcp.MSG_TCP_DO_RECV)
                    readLoop()
                }

            }

        }
    }


    private fun closeSocket() {
        mTcpSocket.close()
        mTcpSocket = Socket()
    }

    private fun connectSocket(): Boolean {
        closeSocket()
        try {
            //socketAddress = InetSocketAddress(ipAddr, ipPort)
            mTcpSocket.connect(socketAddress, 5 * 1000)
        } catch (e: Exception) {
            mMessageHandler.obtainMessage(MSG_TCP_CONNECTION_FAILED, e.toString()).sendToTarget()
            return false
        }

        if (!mTcpSocket.isConnected) {
            mMessageHandler.obtainMessage(MSG_TCP_CONNECTION_FAILED, "").sendToTarget()
            return false
        } else {
            mMessageHandler.obtainMessage(MSG_TCP_CONNECTED, "").sendToTarget()
        }

        bufferedReader = BufferedReader(InputStreamReader(mTcpSocket.getInputStream()))
        outputStream = mTcpSocket.getOutputStream()

        return true
    }

    val isConnected: Boolean
        get() = mTcpSocket.isConnected

    @Volatile
    var isConnecting = false


    fun close() {
        mMsgThread.mHandler!!.obtainMessage(MSG_TCP_DO_DISCONNECT).sendToTarget()
    }

    fun transmit(s: String) {
        mMsgThread.mHandler!!.obtainMessage(MSG_TCP_DO_SEND, s).sendToTarget()
        doReadTick()
    }

    fun reconnect() {
        connect()
    }

    fun connect() {
        mMsgThread.mHandler!!.obtainMessage(MSG_TCP_DO_CONNECT).sendToTarget()
        doReadTick()
    }

    fun doReadTick() {
        mMsgThread.mHandler!!.sendMessageDelayed(mMsgThread.mHandler!!.obtainMessage(MSG_TCP_DO_RECV), 100)
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
