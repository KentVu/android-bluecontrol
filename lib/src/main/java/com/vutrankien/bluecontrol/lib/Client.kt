package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class Client(
    logFactory: LogFactory,
    private val env: Environment
) {
    private val log = logFactory.newLog("Client")

    private var socket: Environment.BlueSocket? = null
    private val _serverResponses: Channel<String> = Channel()
    val serverResponses: ReceiveChannel<String> = _serverResponses

    val connected: Boolean
        get() = socket != null

    fun connectTo(dev: Environment.BluetoothDevice) {
        socket = env.connectToDevice(dev,
            Conf.uuid
        )
        log.d("Connected-$socket")
    }

    private var sendMsgJob: Job? = null
    private var readServerJob: Job? = null
    private lateinit var chan: Channel<String>
    val msgChan: SendChannel<String>
        get() = chan

    private val sendChannelOpen = sendMsgJob != null

    @Suppress("BlockingMethodInNonBlockingContext")
    fun startListenChannel() {
        val socket = requireNotNull(socket)
        require(!sendChannelOpen) { "A channel already created" }
        chan = Channel()
        sendMsgJob = CoroutineScope(
            Executors.newSingleThreadExecutor()
                .asCoroutineDispatcher()
        ).launch {
            socket.outputStream.bufferedWriter().use {
                for (msg in chan) {
                    log.d("sendMsgFrom:$msg")
                    it.write("$msg\n")
                    //it.write(msg)
                    //it.newLine()
                    it.flush()
                    //chan.send(msg)
                }
                end()
            }
        }
        readServerJob = CoroutineScope(
            Executors.newSingleThreadExecutor()
                .asCoroutineDispatcher()
        ).launch {
            socket.inputStream.bufferedReader().use {
                while (!sendMsgJob!!.isCancelled) {
                    _serverResponses.send(it.readLine())
                }
            }
        }
        //return chan
    }

    private fun end() {
        sendMsgJob?.cancel()
        sendMsgJob = null
        readServerJob?.cancel()
        readServerJob = null
        socket?.close()
        socket = null
    }

    fun closeSendChannel() {
        if (sendChannelOpen) {
            chan.close()
        }
    }

}