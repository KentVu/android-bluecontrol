package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.util.concurrent.Executors

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
            onStartClick()
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
        log.d("onSendClick:client.connected?${client.connected}")
        if (!client.connected) {
            withContext(Dispatchers.IO) {
                client.connectTo(selectedDevice!!)
            }
            view.updateStatus("Connected to $selectedDevice")
            client.startListenChannel()
            //return@coroutineScope
        }
        view.displayMsg("Client:$msg")
        client.msgChan.send(msg)
    }

    fun onDeviceSelected(device: Environment.BluetoothDevice) {
        log.d("device selected:$device")
        selectedDevice = device
    }

    suspend fun onStartClick() = coroutineScope {
        view.updateStatus("Server socket listening...")
        view.startBtn.disable()
        try {
            withContext(Dispatchers.IO) {
                server.startListening()
            }
        } catch (e: Exception) {
            log.w("onStartClick: Can startListening:${e.message}")
            return@coroutineScope
        }
        view.updateStatus("Server socket accepted!")
        launch {
            for (msg in server.receiveFromClient(CoroutineScope(Dispatchers.Default))) {
                view.displayMsg("Client:${msg}")
            }
            view.updateStatus("Client exited.")
            view.startBtn.enable()
        }
    }

    fun onDestroy() {
        server.end()
        client.closeSendChannel()
    }

    private val server: Server = Server(logFactory, env)

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
            serverSocket = env.listenBluetoothConnection(Conf.serviceName, Conf.uuid)
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

    private val client: Client = Client(logFactory, env)

    class Client(
        logFactory: LogFactory,
        private val env: Environment
    ) {
        private val log = logFactory.newLog("Client")
        private var socket: Environment.BlueSocket? = null

        val connected: Boolean
            get() = socket != null

        fun connectTo(dev: Environment.BluetoothDevice) {
            socket = env.connectToDevice(dev, Conf.uuid)
            log.d("Connected-$socket")
        }

        private var sendMsgJob: Job? = null
        private lateinit var chan: Channel<String>
        val msgChan: SendChannel<String>
            get() = chan

        private val sendChannelOpen = sendMsgJob != null

        @Suppress("BlockingMethodInNonBlockingContext")
        fun startListenChannel() {
            val socket = requireNotNull(socket)
            require(!sendChannelOpen) { "A channel already created" }
            chan = Channel()
            sendMsgJob = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
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
            //return chan
        }

        private fun end() {
            sendMsgJob?.cancel()
            sendMsgJob = null
            socket?.close()
            socket = null
        }

        fun closeSendChannel() {
            if (sendChannelOpen) {
                chan.close()
            }
        }

    }

    private var selectedDevice: Environment.BluetoothDevice? = null
    private val log = logFactory.newLog("Presenter")
}