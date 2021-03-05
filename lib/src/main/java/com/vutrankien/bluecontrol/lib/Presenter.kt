package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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

    /**
     * Start listening.
     */
    suspend fun onStartClick() {
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
        object SENT_MSG : ClientConnectionEvent()
    }

    suspend fun onSendClick(msg: String) {
        log.d("onSendClick:$msg")
        flow {
            if (socket == null) {
                socket = env.connectToDevice(selectedDevice, Conf.uuid)
                emit(ClientConnectionEvent.Connected(socket!!))
            }
            val socket = requireNotNull(socket) {"onSendClick"}
            socket.outputStream.bufferedWriter().use {
                it.write(msg)
                it.newLine()
                emit(ClientConnectionEvent.SENT_MSG)
            }
        }.flowOn(Dispatchers.IO).collect {
            when(it) {
                is ClientConnectionEvent.Connected ->
                    view.updateStatus("Connected to ${selectedDevice}-$socket")
                is ClientConnectionEvent.SENT_MSG ->
                    view.displayMsg("Client:$msg")
            }
        }
    }

    fun onDeviceSelected(device: Environment.BluetoothDevice) {
        log.d("device selected:$device")
        selectedDevice = device
    }

    fun onDestroy() {
        serverSocket?.close()
        socket?.close()
    }

    private var serverSocket: Environment.BlueServerSocket? = null
    private var socket: Environment.BlueSocket? = null
    private var selectedDevice: Environment.BluetoothDevice? = null
    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}