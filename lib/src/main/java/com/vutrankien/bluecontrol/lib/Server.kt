package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class Server(
    logFactory: LogFactory,
    private val env: Environment
) {
    private val log = logFactory.newLog("Server")
    private var serverSocket: Environment.BlueServerSocket? = null
    private var socket: Environment.BlueSocket? = null
    private var readClientJob: Job? = null

    /**
     * Start listening.
     */
    internal fun startListening() {
        serverSocket = env.listenBluetoothConnection(
            Conf.serviceName,
            Conf.uuid
        )
        log.d("listening...")
        socket = serverSocket!!.accept()
        log.d("accepted ($socket)...")
    }

    fun receiveFromClient(scope: CoroutineScope): ReceiveChannel<String> {
        val channel = Channel<String>()
        serverSocket!!.close()
        require(readClientJob == null) {"Server already reading!"}
        readClientJob = scope.launch {
            val s2cSocket = requireNotNull(this@Server.socket)
            try {
                s2cSocket.inputStream.bufferedReader().use {
                    while (s2cSocket.isConnected) {
                        log.d("reading...")
                        @Suppress("BlockingMethodInNonBlockingContext")
                        val line = it.readLine()
                        log.d("received msg:$line")
                        channel.send(line.toString())
                    }
                }
            } catch (e: Exception) {
                log.w("Read failed:${e.message}")
                channel.close()
                end()
            }
        }
        return channel
    }

    fun end() {
        readClientJob?.cancel()
        readClientJob = null
        serverSocket?.close()
        serverSocket = null
    }
}