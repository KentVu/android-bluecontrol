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
            emit(ConnectionEvent.LISTENING)
            val s2cSocket = serverSocket!!.accept()
            emit(ConnectionEvent.ACCEPTED)
            s2cSocket.inputStream.bufferedReader().use {
                while (true) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    val line = it.readLine()
                    log.d("received msg:$line")
                    emit(ConnectionEvent.ReceivedMsg(line))
                }
            }
        }.flowOn(Dispatchers.IO).collect {
            when(it) {
                ConnectionEvent.LISTENING -> view.updateStatus("Server socket listening...")
                ConnectionEvent.ACCEPTED -> view.updateStatus("Server socket accepted!")
                is ConnectionEvent.ReceivedMsg -> view.updateStatus("Client:${it.msg}")
            }
        }
    }

    sealed class ConnectionEvent {
        object LISTENING : ConnectionEvent()
        object ACCEPTED : ConnectionEvent()
        data class Connected(val socket: Environment.BlueSocket) : ConnectionEvent()
        data class ReceivedMsg(val msg: String) : ConnectionEvent() {

        }
    }

    suspend fun onSendClick(msg: String) {
        log.d("onSendClick:$msg")
        view.displayMsg("Client:$msg")
        if (socket == null) {
            socket = env.connectToDevice(selectedDevice, Conf.uuid)
        }
        val socket = requireNotNull(socket) {"onSendClick"}
        view.updateStatus("Connected to ${selectedDevice}-$socket")
        withContext(Dispatchers.IO) {
            socket.outputStream.bufferedWriter().use {
                it.write(msg)
                it.newLine()
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