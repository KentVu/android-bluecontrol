package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
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
            client.sendMsgFrom(msgChan)
            return@coroutineScope
        }
        msgChan.send(msg)
    }

    fun onDeviceSelected(device: Environment.BluetoothDevice) {
        log.d("device selected:$device")
        selectedDevice = device
    }

    suspend fun onStartClick() = coroutineScope {
        view.updateStatus("Server socket listening...")
        view.disableStartBtn()
        withContext(Dispatchers.IO) {
            server.startListening()
        }
        view.updateStatus("Server socket accepted!")
        launch {
            for (msg in server.receiveFromClient(CoroutineScope(Dispatchers.Default))) {
                view.updateStatus("Client:${msg}")
            }
        }
    }

    fun onDestroy() {
        server.end()
        client.end()
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
            log.d("accepted ($socket)...")
        }

        fun receiveFromClient(scope: CoroutineScope): ReceiveChannel<String> {
            val channel = Channel<String>()
            serverSocket!!.close()
            scope.launch {
                val s2cSocket = requireNotNull(this@Server.socket)
                //s2cSocket.outputStream.write(byteArrayOf(3))
                s2cSocket.inputStream.bufferedReader().use {
                //s2cSocket.inputStream.use {
                    while (s2cSocket.isConnected) {
                        log.d("reading...")
                        @Suppress("BlockingMethodInNonBlockingContext")
                        val line = it.readLine()
                        log.d("received msg:$line")
                        channel.send(line.toString())
                    }
                }
            }
            return channel
        }

        fun end() {
            serverSocket?.close()
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

        fun sendMsgFrom(chan: ReceiveChannel<String>) {
            val socket = requireNotNull(socket)
            require(sendMsgJob == null) { "A channel already created" }
            sendMsgJob = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
                socket.outputStream.bufferedWriter().use {
                    for (msg in chan) {
                        log.d("sendMsgFrom:$msg")
                        //it.write("$msg\n")
                        it.write(msg)
                        it.newLine()
                        //chan.send(msg)
                    }
                }
            }
            //return chan
        }

        fun end() {
            sendMsgJob?.cancel()
            sendMsgJob = null
            socket?.close()
            socket = null
        }

    }

    private val msgChan: Channel<String> = Channel()
    private var selectedDevice: Environment.BluetoothDevice? = null
    private val log = logFactory.newLog("Presenter")
}