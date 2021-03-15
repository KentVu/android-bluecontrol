package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class Presenter(
    logFactory: LogFactory,
    private val env: Environment,
    private val view: View
) {
    suspend fun onCreate() {
        view.apply {
            if (!env.bluetoothSupported()) {
                // Device doesn't support Bluetooth
                val msg = "Device does not support Bluetooth"
                log.e(msg)
                alert(msg) { finish() }
                return
            }
            if (!env.bluetoothEnabled()) {
                if (!askEnableBluetooth()) {
                    alert("Please enable bluetooth!") { finish() }
                    return
                }
                // user enabled bluetooth!
            }
            // bluetooth enabled
            populatePairedDevices()
            view.updateStatus("Server socket listening...")
            withContext(Dispatchers.IO) {
                server.startListening()
            }
            view.updateStatus("Server socket accepted!")
            withContext(Dispatchers.Default) {
                for (msg in server.receiveFromClient(this)) {
                    view.updateStatus("Client:${msg}")
                }
            }
        }
    }

    suspend fun onDiscoverClick() {
        if (!env.locationPermissionGranted) {
            if (!view.askLocationPermission()) {
                view.alert("please enable location permission for bluetooth to function.") {
                    //view.finish()  // TODO location permission not required for listing paired device
                }
                //return@launch
            }
        }
    }

    private fun populatePairedDevices() {
        val pairedDevices = env.queryPairedDevices()
        pairedDevices.also {
            log.d("Paired devices:${it.count()}")
            it.forEach { device ->
                log.i("d:$device")
            }
            view.populateDevices(it)
            //selectedDevice = it.first()
        }
    }

    sealed class ServerConnectionEvent {
        object LISTENING : ServerConnectionEvent()
        object ACCEPTED : ServerConnectionEvent()
        data class ReceivedMsg(val msg: String) : ServerConnectionEvent()
    }

    sealed class ClientConnectionEvent {
        data class Connected(val socket: Environment.BlueSocket) : ClientConnectionEvent()
        data class SentMsg(val msg: String) : ClientConnectionEvent()
    }

    suspend fun onSendClick(msg: String) = coroutineScope {
        log.d("onSendClick:$msg")
        if (socket == null) {
            withContext(Dispatchers.IO) {
                socket = env.connectToDevice(selectedDevice, Conf.uuid)
            }
            view.updateStatus("Connected to ${selectedDevice}-$socket")
            launch {
                val receiveChannel = withContext(Dispatchers.Default) {
                    startSendChannel()
                }
                for (srvMsg in receiveChannel) {
                    view.displayMsg("Client:$srvMsg")
                }
            }
        }
        msgChan.send(msg)
    }

    private suspend fun startSendChannel(): ReceiveChannel<String> {
        val socket = requireNotNull(socket) {"onSendClick"}
        val chan = Channel<String>()
        socket.outputStream.bufferedWriter().use {
            for (msg in msgChan) {
                withContext(Dispatchers.Default) {
                    log.d("startSendChannel:$msg")
                    it.write("$msg\n")
                    //it.write(msg)
                    //it.newLine()
                }
                chan.send(msg)
            }
        }
        return chan
    }

    fun onDeviceSelected(device: Environment.BluetoothDevice) {
        log.d("device selected:$device")
        selectedDevice = device
    }

    fun onDestroy() {
        server.end()
        socket?.close()
    }

    private val server: Server = Server(logFactory, env)

    class Server(
        logFactory: LogFactory,
        private val env: Environment
    ) {
        private var serverSocket: Environment.BlueServerSocket? = null
        private var socket: Environment.BlueSocket? = null
        private val log = logFactory.newLog("Server")

        /**
         * Start listening.
         */
        internal fun startListening() {
            serverSocket = env.listenBluetoothConnection(Conf.serviceName, Conf.uuid)
            log.d("listening...")
            socket = serverSocket!!.accept()
        }

        fun receiveFromClient(scope: CoroutineScope): Channel<String> {
            val channel = Channel<String>()
            scope.launch {
                val s2cSocket = this@Server.socket!!
                s2cSocket.inputStream.bufferedReader().use {
                    while (s2cSocket.isConnected) {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        val line = it.readLine()
                        this@Server.log.d("received msg:$line")
                        channel.send(line)
                    }
                }
            }
            return channel
        }

        fun end() {
            serverSocket?.close()
        }
    }

    private val msgChan: Channel<String> = Channel()
    private var socket: Environment.BlueSocket? = null
    private var selectedDevice: Environment.BluetoothDevice? = null
    private val log = logFactory.newLog("Presenter")
}