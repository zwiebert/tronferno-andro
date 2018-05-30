package bertw.tronferno

import android.os.Handler
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue


class McuTcp(msgHandler: Handler) {

    private var mTcpSocket = Socket()
    private var tcpConnectThread: Thread
    private var tcpWriteThread: Thread
    private var tcpReadThread: Thread
    private var mMessageHandler: Handler = msgHandler;
    private val q = ArrayBlockingQueue<String>(1000)

    @Volatile
    private var stopThread = false


    init {

        tcpWriteThread = object : Thread() {
            override fun run() {
                while (!stopThread && !mTcpSocket.isClosed) {
                    try {
                        val data = q.take()
                        mTcpSocket.getOutputStream().write(data.toByteArray())
                    } catch (e: Exception) {
                        mMessageHandler.obtainMessage(MSG_TCP_OUTPUT_ERROR, "tcp-wt:error: ${e.toString()}").sendToTarget()
                        return
                    }
                }
            }
        }

        tcpReadThread = object : Thread() {
            internal var buf = ByteArray(256)

            override fun run() {
                try {
                    val br = BufferedReader(InputStreamReader(mTcpSocket.getInputStream()))
                    while (!stopThread && !mTcpSocket.isClosed) {
                        val line = br.readLine()
                        if (line == null) {
                            mMessageHandler.obtainMessage(MSG_TCP_INPUT_EOF, line).sendToTarget()
                            return; // EOF
                        }
                        mMessageHandler.obtainMessage(MSG_LINE_RECEIVED, line).sendToTarget()
                    }
                } catch (e: Exception) {
                    mMessageHandler.obtainMessage(MSG_TCP_INPUT_ERROR, "tcp-rt:error: ${e.toString()}").sendToTarget()
                    return
                }
            }
        }

        tcpConnectThread = object : Thread() {

            override fun run() {
                try {
                    stopThread = true;
                    if (mTcpSocket.isClosed) {
                        mTcpSocket = Socket()
                    }
                    while (true) {
                        mTcpSocket.connect(socketAddress)
                        if (mTcpSocket.isConnected) {
                            mMessageHandler.obtainMessage(MSG_TCP_CONNECTED, "").sendToTarget()
                            break;
                        }
                    }

                    stopThread = false;
                    if (!tcpWriteThread.isAlive) tcpWriteThread.start()
                    tcpReadThread.start()
                } catch (e: Exception) {
                    mMessageHandler.obtainMessage(MSG_TCP_CONNECTION_FAILED, e.toString()).sendToTarget()
                    return;
                }
            }
        }

    }

    val isConnected: Boolean
        get() = mTcpSocket.isConnected

    val isConnecting: Boolean
        get() = (tcpConnectThread.state != Thread.State.NEW && tcpConnectThread.state != Thread.State.TERMINATED)

    fun close() {
        try {
            mTcpSocket.close()
        } catch (e: IOException) {
            //?// vtvLog.append(e.toString())
        }
    }

    fun transmit(s: String) { q.add(s) }

    fun reconnect() {
        if (tcpConnectThread.state == Thread.State.NEW || tcpConnectThread.state == Thread.State.TERMINATED) {
            mTcpSocket.close()
            tcpConnectThread.start()
        }
    }

    fun connect() { tcpConnectThread.start() }

    companion object {
        var socketAddress: InetSocketAddress? = null

        internal const val MSG_LINE_RECEIVED = 5
        internal const val MSG_TCP_CONNECTED = 7
        internal const val MSG_TCP_CONNECTION_FAILED = 8
        internal const val MSG_TCP_INPUT_EOF = 9
        internal const val MSG_TCP_INPUT_ERROR = 10
        internal const val MSG_TCP_OUTPUT_ERROR = 11

    }

}
