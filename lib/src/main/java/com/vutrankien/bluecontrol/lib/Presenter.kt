package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Presenter(private val env: Environment, private val view: View) : KoinComponent {
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
            startListening()
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

    /**
     * Start listening.
     */
    private suspend fun startListening() {
        flow {
            serverSocket = env.listenBluetoothConnection(Conf.serviceName, Conf.uuid)
            emit(ServerConnectionEvent.LISTENING)
            val s2cSocket = serverSocket!!.accept()
            emit(ServerConnectionEvent.ACCEPTED)
            s2cSocket.inputStream.bufferedReader().use {
                while (true) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val line = it.readLine()
                    log.d("received msg:$line")
                    emit(ServerConnectionEvent.ReceivedMsg(line))
                }
            }
        }.flowOn(Dispatchers.IO).collect {
            when(it) {
                ServerConnectionEvent.LISTENING -> view.updateStatus("Server socket listening...")
                ServerConnectionEvent.ACCEPTED -> view.updateStatus("Server socket accepted!")
                is ServerConnectionEvent.ReceivedMsg -> view.updateStatus("Client:${it.msg}")
            }
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
                for (msg in receiveChannel) {
                    view.displayMsg("Client:$msg")
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
                withContext(Dispatchers.IO) {
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
        serverSocket?.close()
        socket?.close()
    }

    private val msgChan: Channel<String> = Channel()
    private var serverSocket: Environment.BlueServerSocket? = null
    private var socket: Environment.BlueSocket? = null
    private var selectedDevice: Environment.BluetoothDevice? = null
    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}